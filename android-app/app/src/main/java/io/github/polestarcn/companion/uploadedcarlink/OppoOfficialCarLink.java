package io.github.polestarcn.companion.uploadedcarlink;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 路径 A 核心实现：对应 CarAppDemo (platform_sdk / car_control 分支) 的官方车企插件 Service
 * 支持三种 OPPO 宿主连接方式：
 * 1. 如果 libs/ 下导入了 CarAppDemo 的 classes.jar，自动通过反射挂载官方 SDK Stub / Callback
 * 2. 如果宿主通过 AIDL Binder 直接绑定，通过自定义 UniversalCarControlBinder 动态拦截 onTransact
 * 3. 如果宿主通过 Messenger 或 onStartCommand 下发指令，直接解析并转交 VehicleProtocolGateway
 */
public class OppoOfficialCarLink extends Service {

    private static final String TAG = "OppoOfficialCarLink";

    // 自定义通用 Binder：兼容任意 platform_sdk AIDL 描述符与 Messenger 通信
    private final IBinder universalBinder = new UniversalCarControlBinder();

    private final Messenger serviceMessenger = new Messenger(new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData() != null ? new Bundle(msg.getData()) : new Bundle();
            if (!data.containsKey(CarLinkDexContract.KEY_EVENT_ID) && msg.what > 0) {
                data.putInt(CarLinkDexContract.KEY_EVENT_ID, msg.what);
            }
            Log.i(TAG, "Received Messenger car command: what=" + msg.what + ", data=" + data);
            VehicleProtocolGateway.getInstance(getApplicationContext())
                    .executeCarEventAsync(data.getInt(CarLinkDexContract.KEY_EVENT_ID, msg.what), data);

            // 若包含 replyTo Messenger，立即回传最新 Session 快照
            if (msg.replyTo != null) {
                try {
                    Message reply = Message.obtain(null, msg.what, 0, 0);
                    reply.setData(VirtualVehicleSessionManager.getInstance(getApplicationContext()).buildSessionBundle());
                    msg.replyTo.send(reply);
                } catch (RemoteException e) {
                    Log.w(TAG, "Messenger reply failed", e);
                }
            }
        }
    });

    @Override
    public void onCreate() {
        super.onCreate();
        tryAttachPlatformSdkJar();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            handleIncomingIntent(intent);
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        String action = intent != null ? intent.getAction() : "";
        Log.i(TAG, "onBind called by OPPO host, action=" + action);

        // 1. 尝试返回 CarAppDemo classes.jar 中的官方 SDK Stub IBinder
        IBinder sdkBinder = tryGetPlatformSdkBinder(intent);
        if (sdkBinder != null) {
            return sdkBinder;
        }

        // 2. 若 Intent 要求 Messenger 协议，返回 Messenger Binder
        if (intent != null && intent.getBooleanExtra("use_messenger", false)) {
            return serviceMessenger.getBinder();
        }

        // 3. 默认返回支持任意 AIDL Transaction 动态解析的 UniversalCarControlBinder
        return universalBinder;
    }

    private void handleIncomingIntent(Intent intent) {
        Bundle extras = intent.getExtras() != null ? new Bundle(intent.getExtras()) : new Bundle();
        int eventId = extras.getInt(CarLinkDexContract.KEY_EVENT_ID, -1);
        String cmdName = extras.getString(CarLinkDexContract.KEY_SDK_CMD_NAME, intent.getStringExtra("command"));
        if (cmdName != null && !cmdName.isEmpty()) {
            extras.putString(CarLinkDexContract.KEY_SDK_CMD_NAME, cmdName);
        }
        if (eventId != -1 || (cmdName != null && !cmdName.isEmpty())) {
            VehicleProtocolGateway.getInstance(getApplicationContext()).executeCarEventAsync(eventId, extras);
        }
    }

    /**
     * 如果工程 app/libs/ 下放入了 CarAppDemo 的 classes.jar，自动注册车控回调实现
     */
    private void tryAttachPlatformSdkJar() {
        String[] sdkManagerCandidates = new String[]{
                "com.oppo.carlink.platform.sdk.CarControlSdkManager",
                "com.oplus.carlink.platform.sdk.CarControlSdkManager",
                "com.oppo.carcontrol.sdk.CarControlManager"
        };
        for (String clsName : sdkManagerCandidates) {
            try {
                Class<?> mgrClass = Class.forName(clsName);
                Log.i(TAG, "Detected official platform_sdk class: " + clsName);
                registerDynamicSdkCallback(mgrClass);
                return;
            } catch (ClassNotFoundException ignored) {
            }
        }
    }

    private void registerDynamicSdkCallback(Class<?> mgrClass) {
        try {
            Object mgrInstance = null;
            for (Method m : mgrClass.getDeclaredMethods()) {
                if (java.lang.reflect.Modifier.isStatic(m.getModifiers())
                        && mgrClass.isAssignableFrom(m.getReturnType())) {
                    m.setAccessible(true);
                    mgrInstance = m.getParameterTypes().length == 0
                            ? m.invoke(null)
                            : m.invoke(null, getApplicationContext());
                    break;
                }
            }
            if (mgrInstance == null) return;

            for (Method m : mgrClass.getDeclaredMethods()) {
                Class<?>[] paramTypes = m.getParameterTypes();
                if (paramTypes.length == 1 && paramTypes[0].isInterface()) {
                    Class<?> callbackInterface = paramTypes[0];
                    Object proxy = Proxy.newProxyInstance(
                            callbackInterface.getClassLoader(),
                            new Class<?>[]{callbackInterface},
                            new PlatformSdkInvocationHandler()
                    );
                    m.setAccessible(true);
                    m.invoke(mgrInstance, proxy);
                    Log.i(TAG, "Registered dynamic proxy for SDK interface: " + callbackInterface.getName());
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to register dynamic SDK callback: " + t.getMessage());
        }
    }

    private IBinder tryGetPlatformSdkBinder(Intent intent) {
        try {
            Class<?> sdkServiceHelper = Class.forName("com.oppo.carlink.platform.sdk.CarControlServiceHelper");
            for (Method m : sdkServiceHelper.getDeclaredMethods()) {
                if (IBinder.class.isAssignableFrom(m.getReturnType())) {
                    m.setAccessible(true);
                    if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == Intent.class) {
                        return (IBinder) m.invoke(null, intent);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * 动态接管 platform_sdk 回调接口中的 hasCarBinding 与 4 个控车指令方法
     */
    private class PlatformSdkInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("hasCarBinding".equals(name) || "checkCarBind".equals(name) || "isCarBound".equals(name)) {
                return OppoOfficialVehicleBindState.hasCarBinding(getApplicationContext());
            }

            Bundle extras = new Bundle();
            extras.putString(CarLinkDexContract.KEY_SDK_CMD_NAME, name);
            if (args != null && args.length > 0) {
                if (args[0] instanceof Integer) {
                    extras.putInt(CarLinkDexContract.KEY_EVENT_ID, (Integer) args[0]);
                } else if (args[0] instanceof String) {
                    extras.putString(CarLinkDexContract.KEY_SDK_CMD_NAME, (String) args[0]);
                } else if (args[0] instanceof Boolean) {
                    extras.putBoolean(CarLinkDexContract.KEY_ACTION_TARGET, (Boolean) args[0]);
                }
                if (args.length > 1 && args[1] instanceof Boolean) {
                    extras.putBoolean(CarLinkDexContract.KEY_ACTION_TARGET, (Boolean) args[1]);
                }
            }

            VehicleProtocolGateway.getInstance(getApplicationContext())
                    .executeCarEventAsync(extras.getInt(CarLinkDexContract.KEY_EVENT_ID, -1), extras);

            if (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class) {
                return true;
            } else if (method.getReturnType() == int.class || method.getReturnType() == Integer.class) {
                return 0;
            }
            return null;
        }
    }

    /**
     * 底层 AIDL 通用事务解析器：
     * 即使未导入外部 AIDL 文件，也能直接响应 OPPO CarLink 发起的 Binder 事务
     */
    private class UniversalCarControlBinder extends Binder {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == INTERFACE_TRANSACTION) {
                if (reply != null) {
                    reply.writeString("com.oppo.carlink.platform.ICarControlService");
                }
                return true;
            }

            try {
                data.enforceInterface("com.oppo.carlink.platform.ICarControlService");
            } catch (Throwable ignored) {
                data.setDataPosition(0);
            }

            // Transaction Code 1: 默认约定为 checkCarBind / hasCarBinding
            if (code == FIRST_CALL_TRANSACTION) {
                boolean bound = OppoOfficialVehicleBindState.hasCarBinding(getApplicationContext());
                if (reply != null) {
                    reply.writeNoException();
                    reply.writeInt(bound ? 1 : 0);
                }
                return true;
            }

            // Transaction Code 2..5: 对应四个核心 CAR 控制事件
            if (code >= FIRST_CALL_TRANSACTION + 1 && code <= FIRST_CALL_TRANSACTION + 10) {
                int mappedEventId;
                switch (code - FIRST_CALL_TRANSACTION) {
                    case 1:
                        mappedEventId = CarLinkDexContract.CAR_EVENT_LOCK_UNLOCK;
                        break;
                    case 2:
                        mappedEventId = CarLinkDexContract.CAR_EVENT_CLIMATE;
                        break;
                    case 3:
                        mappedEventId = CarLinkDexContract.CAR_EVENT_WINDOW;
                        break;
                    case 4:
                        mappedEventId = CarLinkDexContract.CAR_EVENT_FIND_CAR;
                        break;
                    default:
                        mappedEventId = code;
                        break;
                }
                Bundle extras = new Bundle();
                extras.putInt(CarLinkDexContract.KEY_EVENT_ID, mappedEventId);
                VehicleProtocolGateway.getInstance(getApplicationContext())
                        .executeCarEventAsync(mappedEventId, extras);
                if (reply != null) {
                    reply.writeNoException();
                    reply.writeInt(0);
                }
                return true;
            }

            return super.onTransact(code, data, reply, flags);
        }
    }
}

