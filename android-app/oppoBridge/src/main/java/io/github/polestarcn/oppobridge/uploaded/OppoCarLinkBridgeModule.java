package io.github.polestarcn.oppobridge.uploaded;

import io.github.polestarcn.oppobridge.uploaded.LegacyApi.AndroidAppHelper;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.github.polestarcn.oppobridge.uploaded.LegacyApi.XC_MethodHook;
import io.github.polestarcn.oppobridge.uploaded.LegacyApi.XposedBridge;
import io.github.polestarcn.oppobridge.uploaded.LegacyApi.XposedHelpers;
import io.github.polestarcn.oppobridge.uploaded.LegacyApi.XC_LoadPackage;
import io.github.polestarcn.companion.uploadedcarlink.CarLinkDexContract;

/**
 * OPPO CarLink / SceneService 双路径完整 LSPosed 模组：
 *
 * 【路径 A 实现】：
 * 1. 强授 com.oppo.permission.safe.IOT / com.oplus.permission.safe.IOT 给 PsAssistant，并放行签名校验
 * 2. Hook OPPO / SceneService / CarLink 官方 SQLite 数据库与白名单集合，将 PsAssistant 注册为官方支持控车 App
 *
 * 【路径 B 实现】：
 * 3. 接管 auth/checkCarBind/hasCarBinding 状态查询，联动 PsAssistant 实时返回绑定状态
 * 4. 在 CarLink 内部构造并维持活跃的虚拟车辆 Session (Virtual Vehicle Session)
 * 5. 拦截系统下发的 4 个 CAR 控制事件 (解闭锁/空调/车窗/寻车)，通过 ContentProvider.call (带显式广播兜底)
 *    安全转交至 PsAssistant 主 APK 的真实 Polestar 控车接口
 */
public class OppoCarLinkBridgeModule {

    public static void install(io.github.polestarcn.oppobridge.OppoBridgeModule module,String pkg,ClassLoader loader){
        LegacyApi.bind(module);XC_LoadPackage.LoadPackageParam p=new XC_LoadPackage.LoadPackageParam();p.packageName=pkg;p.classLoader=loader;
        try{new OppoCarLinkBridgeModule().handleLoadPackage(p);}catch(Throwable error){XposedBridge.log("Uploaded adapter install failed: "+error.getClass().getSimpleName());}
    }
    private static final String TAG = "[PsaOppoCarLinkBridge] ";
    private static volatile Bundle cachedSession = new Bundle();

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        String pkg = lpparam.packageName;
        if (pkg == null) return;

        // 1. 系统核心进程 (android)：实现路径 A 的 com.oppo.permission.safe.IOT 强授与签名校验放行
        if ("android".equals(pkg)) {
            hookSystemServerPermissionGrant(lpparam);
            return;
        }

        // 2. OPPO 车联、智慧场景服务 (SceneService)、智慧车载、负一屏宿主进程：同时激活路径 A 与路径 B
        if (isTargetOppoCarHost(pkg)) {
            XposedBridge.log(TAG + "Initializing Path A + Path B hooks in process: " + pkg);
            hookLocalIotPermissionAndSignatures(lpparam);
            hookOfficialDatabaseAndWhitelist(lpparam);
            hookHasCarBindingCheck(lpparam);
            hookVirtualVehicleSession(lpparam);
            hookFourCarControlEvents(lpparam);
        }
    }

    private boolean isTargetOppoCarHost(String pkg) {
        return pkg.contains("carlink")
                || pkg.contains("sceneservice")
                || pkg.contains("smartdrive")
                || pkg.contains("digitalkey")
                || pkg.contains("assistantscreen")
                || pkg.contains("Launcher")
                || pkg.contains("systemui");
    }

    // =========================================================================
    // 模块一 (路径 A)：强授 com.oppo.permission.safe.IOT 与放行 PMS / 宿主签名校验
    // =========================================================================

    private void hookSystemServerPermissionGrant(XC_LoadPackage.LoadPackageParam lpparam) {
        String[] pmsClasses = new String[]{
                "com.android.server.pm.permission.PermissionManagerServiceImpl",
                "com.android.server.pm.permission.PermissionManagerService",
                "com.android.server.pm.PackageManagerService"
        };

        for (String clsName : pmsClasses) {
            Class<?> cls = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader);
            if (cls == null) continue;

            // 拦截 checkUidPermission / checkPermission
            for (Method m : cls.getDeclaredMethods()) {
                String name = m.getName();
                if ("checkUidPermission".equals(name) || "checkPermission".equals(name)) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.args.length < 2) return;
                            String perm = String.valueOf(param.args[0]);
                            if (isOppoIotPermission(perm)) {
                                // 如果第二个参数是包名且匹配 PsAssistant，或为运行时校验，直接授予
                                if (param.args[1] instanceof String
                                        && CarLinkDexContract.HOST_PACKAGE.equals(param.args[1])) {
                                    param.setResult(PackageManager.PERMISSION_GRANTED);
                                } else if (param.args[1] instanceof Integer) {
                                    // 针对系统内部对 safe.IOT 的 UID 鉴权予以放行
                                    param.setResult(PackageManager.PERMISSION_GRANTED);
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    private void hookLocalIotPermissionAndSignatures(XC_LoadPackage.LoadPackageParam lpparam) {
        // 1. 放行宿主进程内的 ContextImpl.checkCallingPermission / enforceCallingOrSelfPermission
        Class<?> contextImplClass = XposedHelpers.findClassIfExists("android.app.ContextImpl", lpparam.classLoader);
        if (contextImplClass != null) {
            XC_MethodHook permGrantHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args.length > 0 && param.args[0] instanceof String) {
                        String perm = (String) param.args[0];
                        if (isOppoIotPermission(perm)) {
                            if (param.method instanceof Method
                                    && ((Method) param.method).getReturnType() == int.class) {
                                param.setResult(PackageManager.PERMISSION_GRANTED);
                            } else {
                                // enforce* 方法返回 void，直接阻断异常抛出
                                param.setResult(null);
                            }
                        }
                    }
                }
            };
            XposedBridge.hookAllMethods(contextImplClass, "checkCallingPermission", permGrantHook);
            XposedBridge.hookAllMethods(contextImplClass, "checkCallingOrSelfPermission", permGrantHook);
            XposedBridge.hookAllMethods(contextImplClass, "checkPermission", permGrantHook);
            XposedBridge.hookAllMethods(contextImplClass, "enforceCallingPermission", permGrantHook);
            XposedBridge.hookAllMethods(contextImplClass, "enforceCallingOrSelfPermission", permGrantHook);
        }

        // 2. 放行 ApplicationPackageManager.checkSignatures (让 CarLink 认为 PsAssistant 具备平台签名)
        Class<?> appPmClass = XposedHelpers.findClassIfExists("android.app.ApplicationPackageManager", lpparam.classLoader);
        if (appPmClass != null) {
            XposedBridge.hookAllMethods(appPmClass, "checkSignatures", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args.length >= 2) {
                        String p0 = String.valueOf(param.args[0]);
                        String p1 = String.valueOf(param.args[1]);
                        if (CarLinkDexContract.HOST_PACKAGE.equals(p0) || CarLinkDexContract.HOST_PACKAGE.equals(p1)) {
                            param.setResult(PackageManager.SIGNATURE_MATCH);
                        }
                    }
                }
            });
        }
    }

    private boolean isOppoIotPermission(String perm) {
        if (perm == null) return false;
        return perm.equals(CarLinkDexContract.PERMISSION_OPPO_SAFE_IOT)
                || perm.equals("com.oplus.permission.safe.IOT")
                || perm.contains("permission.safe.IOT")
                || perm.contains("carlink.permission");
    }

    // =========================================================================
    // 模块二 (路径 A + B)：Hook OPPO / SceneService / CarLink 官方数据库与白名单
    // =========================================================================

    private void hookOfficialDatabaseAndWhitelist(XC_LoadPackage.LoadPackageParam lpparam) {
        // 1. Hook SQLiteDatabase 查询，向支持车企 App 白名单表中动态注入 PsAssistant (Polestar)
        Class<?> sqliteClass = XposedHelpers.findClassIfExists(
                "android.database.sqlite.SQLiteDatabase", lpparam.classLoader);
        if (sqliteClass != null) {
            XC_MethodHook queryHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Cursor original = (Cursor) param.getResult();
                    if (original == null) return;

                    String sqlOrTable = extractSqlOrTableFromArgs(param.args);
                    if (isCarWhitelistQuery(sqlOrTable, original)) {
                        Cursor merged = appendPolestarRowToCursor(original);
                        if (merged != null) {
                            param.setResult(merged);
                        }
                    }
                }
            };
            XposedBridge.hookAllMethods(sqliteClass, "rawQueryWithFactory", queryHook);
            XposedBridge.hookAllMethods(sqliteClass, "queryWithFactory", queryHook);
        }

        // 2. Hook 内存中的白名单校验类 (支持 CarLink 与 SceneService 常见白名单类名)
        String[] whitelistClasses = new String[]{
                "com.oppo.carlink.utils.WhitelistManager",
                "com.oplus.carlink.utils.WhitelistManager",
                "com.coloros.sceneservice.car.CarAppWhitelistManager",
                "com.oppo.carlink.config.CarSupportConfigManager"
        };
        for (String clsName : whitelistClasses) {
            Class<?> cls = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader);
            if (cls == null) continue;
            for (Method m : cls.getDeclaredMethods()) {
                Class<?> retType = m.getReturnType();
                if (retType == boolean.class || retType == Boolean.class) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.args != null && param.args.length > 0
                                    && CarLinkDexContract.HOST_PACKAGE.equals(String.valueOf(param.args[0]))) {
                                param.setResult(true);
                            }
                        }
                    });
                } else if (List.class.isAssignableFrom(retType) || Set.class.isAssignableFrom(retType)) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object col = param.getResult();
                            if (col instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Object> list = (List<Object>) col;
                                if (!list.isEmpty() && list.get(0) instanceof String
                                        && !list.contains(CarLinkDexContract.HOST_PACKAGE)) {
                                    list.add(CarLinkDexContract.HOST_PACKAGE);
                                }
                            } else if (col instanceof Set) {
                                @SuppressWarnings("unchecked")
                                Set<Object> set = (Set<Object>) col;
                                if (!set.isEmpty() && set.iterator().next() instanceof String) {
                                    set.add(CarLinkDexContract.HOST_PACKAGE);
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    private String extractSqlOrTableFromArgs(Object[] args) {
        if (args == null) return "";
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            if (arg instanceof String) {
                sb.append((String) arg).append(" ");
            }
        }
        return sb.toString().toLowerCase();
    }

    private boolean isCarWhitelistQuery(String sqlOrTable, Cursor cursor) {
        if (sqlOrTable.contains("car") || sqlOrTable.contains("vehicle")
                || sqlOrTable.contains("whitelist") || sqlOrTable.contains("scene")) {
            return true;
        }
        String[] cols = cursor.getColumnNames();
        if (cols == null) return false;
        for (String col : cols) {
            String c = col.toLowerCase();
            if (c.contains("car_brand") || c.contains("app_pkg") || c.contains("vehicle") || c.contains("has_bind")) {
                return true;
            }
        }
        return false;
    }

    private Cursor appendPolestarRowToCursor(Cursor original) {
        try {
            String[] cols = original.getColumnNames();
            if (cols == null || cols.length == 0) return null;

            Bundle session = syncSessionFromPsAssistant();
            String vin = session.getString(CarLinkDexContract.KEY_VIN, "LPSVSEDEENL000001");

            MatrixCursor extra = new MatrixCursor(cols);
            Object[] row = new Object[cols.length];
            for (int i = 0; i < cols.length; i++) {
                String c = cols[i].toLowerCase();
                if (c.contains("pkg") || c.contains("package")) {
                    row[i] = CarLinkDexContract.HOST_PACKAGE;
                } else if (c.contains("brand")) {
                    row[i] = "Polestar";
                } else if (c.contains("model") || c.contains("vehicle_name") || c.contains("car_name")) {
                    row[i] = "Polestar 2";
                } else if (c.contains("vin") || c.contains("device_id")) {
                    row[i] = vin;
                } else if (c.contains("action") || c.contains("service")) {
                    row[i] = CarLinkDexContract.ACTION_OPPO_CAR_CONTROL_SERVICE;
                } else if (c.contains("authority") || c.contains("uri")) {
                    row[i] = CarLinkDexContract.AUTHORITY;
                } else if (c.contains("cap") || c.contains("feature") || c.contains("mask")) {
                    row[i] = CarLinkDexContract.DEFAULT_POLESTAR_CAPABILITY_MASK;
                } else {
                    row[i] = 1;
                }
            }
            extra.addRow(row);
            return new MergeCursor(new Cursor[]{original, extra});
        } catch (Throwable t) {
            XposedBridge.log(TAG + "Failed appending Polestar row to cursor: " + t.getMessage());
            return null;
        }
    }

    // =========================================================================
    // 模块三 (路径 A + B)：接管 auth/checkCarBind/hasCarBinding 返回值
    // =========================================================================

    private void hookHasCarBindingCheck(XC_LoadPackage.LoadPackageParam lpparam) {
        String[] bindCandidateClasses = new String[]{
                "com.oppo.carlink.auth.CarBindManager",
                "com.oplus.carlink.auth.CarBindManager",
                "com.oppo.carlink.manager.CarBindingManager",
                "com.oplus.carlink.manager.CarBindingManager",
                "com.coloros.sceneservice.car.CarBindHelper",
                "com.coloros.sceneservice.car.CarDataRepository"
        };

        for (String clsName : bindCandidateClasses) {
            Class<?> cls = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader);
            if (cls == null) continue;

            for (Method m : cls.getDeclaredMethods()) {
                String name = m.getName();
                if ("hasCarBinding".equals(name) || "checkCarBind".equals(name)
                        || "isCarBound".equals(name) || "getCarBindState".equals(name)) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Bundle res = callPsAssistantBridge(CarLinkDexContract.METHOD_CHECK_BIND, null);
                            boolean bound = res == null || res.getBoolean(CarLinkDexContract.KEY_HAS_BINDING, true);
                            Class<?> retType = m.getReturnType();
                            if (retType == boolean.class || retType == Boolean.class) {
                                param.setResult(bound);
                            } else if (retType == int.class || retType == Integer.class) {
                                param.setResult(bound ? 1 : 0);
                            }
                        }
                    });
                }
            }
        }

        // 同时拦截 JSON / 网络响应解析中针对 "auth/checkCarBind/hasCarBinding" 的字段读取
        Class<?> jsonObjectClass = XposedHelpers.findClassIfExists("org.json.JSONObject", lpparam.classLoader);
        if (jsonObjectClass != null) {
            XposedHelpers.findAndHookMethod(jsonObjectClass, "optBoolean", String.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String key = (String) param.args[0];
                    if ("hasCarBinding".equals(key) || "has_car_binding".equals(key)) {
                        param.setResult(true);
                    }
                }
            });
        }
    }

    // =========================================================================
    // 模块四 (路径 B)：构建并维持虚拟车辆 Session (Virtual Vehicle Session)
    // =========================================================================

    private void hookVirtualVehicleSession(XC_LoadPackage.LoadPackageParam lpparam) {
        String[] sessionManagerClasses = new String[]{
                "com.oppo.carlink.session.CarSessionManager",
                "com.oplus.carlink.session.CarSessionManager",
                "com.coloros.sceneservice.car.CarSessionController"
        };

        for (String clsName : sessionManagerClasses) {
            Class<?> cls = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader);
            if (cls == null) continue;

            for (Method m : cls.getDeclaredMethods()) {
                String name = m.getName();
                if ("getConnectState".equals(name) || "getSessionState".equals(name)) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Bundle session = syncSessionFromPsAssistant();
                            int state = session.getInt(
                                    CarLinkDexContract.KEY_CONNECT_STATE,
                                    CarLinkDexContract.SESSION_STATE_CONNECTED_READY);
                            param.setResult(state);
                        }
                    });
                } else if ("getCurrentSession".equals(name) || "getActiveCarDevice".equals(name)) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Bundle session = syncSessionFromPsAssistant();
                            Object existing = param.getResult();
                            if (existing != null) {
                                populateSessionFields(existing, session);
                            } else {
                                Object fabricated = fabricateSessionInstance(m.getReturnType(), session);
                                if (fabricated != null) {
                                    param.setResult(fabricated);
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    private Object fabricateSessionInstance(Class<?> sessionType, Bundle sessionData) {
        if (sessionType == null || sessionType == Object.class || sessionType.isInterface()) {
            return null;
        }
        try {
            for (Constructor<?> c : sessionType.getDeclaredConstructors()) {
                c.setAccessible(true);
                if (c.getParameterTypes().length == 0) {
                    Object instance = c.newInstance();
                    populateSessionFields(instance, sessionData);
                    return instance;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void populateSessionFields(Object sessionObj, Bundle sessionData) {
        if (sessionObj == null || sessionData == null) return;
        String vin = sessionData.getString(CarLinkDexContract.KEY_VIN, "LPSVSEDEENL000001");
        boolean locked = sessionData.getBoolean(CarLinkDexContract.KEY_LOCKED, true);
        boolean climate = sessionData.getBoolean(CarLinkDexContract.KEY_CLIMATE_ACTIVE, false);
        boolean windowClosed = sessionData.getBoolean(CarLinkDexContract.KEY_WINDOW_CLOSED, true);

        for (Field f : sessionObj.getClass().getDeclaredFields()) {
            try {
                f.setAccessible(true);
                String name = f.getName().toLowerCase();
                Class<?> type = f.getType();
                if (name.contains("vin") && type == String.class) {
                    f.set(sessionObj, vin);
                } else if ((name.contains("pkg") || name.contains("package")) && type == String.class) {
                    f.set(sessionObj, CarLinkDexContract.HOST_PACKAGE);
                } else if ((name.contains("brand") || name.contains("name")) && type == String.class) {
                    f.set(sessionObj, "Polestar");
                } else if (name.contains("lock") && (type == boolean.class || type == Boolean.class)) {
                    f.set(sessionObj, locked);
                } else if ((name.contains("climate") || name.contains("ac")) && (type == boolean.class || type == Boolean.class)) {
                    f.set(sessionObj, climate);
                } else if (name.contains("window") && (type == boolean.class || type == Boolean.class)) {
                    f.set(sessionObj, windowClosed);
                } else if (name.contains("connect") && (type == int.class || type == Integer.class)) {
                    f.set(sessionObj, CarLinkDexContract.SESSION_STATE_CONNECTED_READY);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    // =========================================================================
    // 模块五 (路径 B)：拦截四个 CAR 控制事件并安全转发至 PsAssistant 主 APK
    // =========================================================================

    private void hookFourCarControlEvents(XC_LoadPackage.LoadPackageParam lpparam) {
        String[] dispatcherClasses = new String[]{
                "com.oppo.carlink.control.CarControlDispatcher",
                "com.oplus.carlink.control.CarControlDispatcher",
                "com.coloros.sceneservice.car.CarControlService",
                "com.coloros.sceneservice.car.CarCommandManager"
        };

        for (String clsName : dispatcherClasses) {
            Class<?> cls = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader);
            if (cls == null) continue;

            for (Method m : cls.getDeclaredMethods()) {
                String name = m.getName();
                if (name.contains("dispatch") || name.contains("sendCar")
                        || name.contains("executeCommand") || name.contains("performCarAction")) {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Bundle routeBundle = extractCarCommandFromArgs(m.getName(), param.args);
                            if (routeBundle == null) return;

                            routeCarEventToPsAssistant(routeBundle);

                            Class<?> retType = m.getReturnType();
                            if (retType == boolean.class || retType == Boolean.class) {
                                param.setResult(true);
                            } else if (retType == int.class || retType == Integer.class) {
                                param.setResult(0);
                            } else if (retType == void.class) {
                                param.setResult(null);
                            }
                        }
                    });
                }
            }
        }
    }

    private Bundle extractCarCommandFromArgs(String methodName, Object[] args) {
        Bundle bundle = new Bundle();
        int eventId = -1;
        boolean targetState = true;
        String sdkCmdName = null;

        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof Integer && eventId == -1) {
                    eventId = normalizeRawEventCode((Integer) arg);
                } else if (arg instanceof Boolean) {
                    targetState = (Boolean) arg;
                } else if (arg instanceof String && sdkCmdName == null) {
                    sdkCmdName = (String) arg;
                } else if (arg instanceof Bundle) {
                    bundle.putAll((Bundle) arg);
                } else if (arg instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) arg;
                    if (map.containsKey("action")) {
                        sdkCmdName = String.valueOf(map.get("action"));
                    }
                }
            }
        }

        if (eventId == -1 && sdkCmdName == null) {
            sdkCmdName = methodName;
        }

        if (eventId != -1) {
            bundle.putInt(CarLinkDexContract.KEY_EVENT_ID, eventId);
        }
        if (sdkCmdName != null) {
            bundle.putString(CarLinkDexContract.KEY_SDK_CMD_NAME, sdkCmdName);
        }
        bundle.putBoolean(CarLinkDexContract.KEY_ACTION_TARGET, targetState);
        bundle.putString(CarLinkDexContract.KEY_REQUEST_ID, UUID.randomUUID().toString());
        return bundle;
    }

    private int normalizeRawEventCode(int raw) {
        switch (raw) {
            case 1:
            case CarLinkDexContract.CAR_EVENT_LOCK_UNLOCK:
                return CarLinkDexContract.CAR_EVENT_LOCK_UNLOCK;
            case 2:
            case CarLinkDexContract.CAR_EVENT_CLIMATE:
                return CarLinkDexContract.CAR_EVENT_CLIMATE;
            case 3:
            case CarLinkDexContract.CAR_EVENT_WINDOW:
                return CarLinkDexContract.CAR_EVENT_WINDOW;
            case 4:
            case CarLinkDexContract.CAR_EVENT_FIND_CAR:
                return CarLinkDexContract.CAR_EVENT_FIND_CAR;
            default:
                return -1;
        }
    }

    private void routeCarEventToPsAssistant(Bundle commandExtras) {
        ResultReceiver resultReceiver = new ResultReceiver(new Handler(Looper.getMainLooper())) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultData != null) {
                    cachedSession = resultData;
                    XposedBridge.log(TAG + "Polestar command callback: code=" + resultCode
                            + ", msg=" + resultData.getString(CarLinkDexContract.KEY_EXEC_MESSAGE));
                }
            }
        };
        commandExtras.putParcelable(CarLinkDexContract.KEY_RESULT_RECEIVER, resultReceiver);

        // 1. 优先通过 PsAssistantBridgeProvider.call() 同步投递
        Bundle ack = callPsAssistantBridge(CarLinkDexContract.METHOD_DISPATCH_COMMAND, commandExtras);
        if (ack != null && ack.getBoolean(CarLinkDexContract.KEY_EXEC_SUCCESS, false)) {
            return;
        }

        // 2. 若 Provider 被后台冻结，使用显式广播唤醒 OppoCarLinkInitReceiver 兜底投递
        try {
            Context context = AndroidAppHelper.currentApplication();
            if (context != null) {
                Intent intent = new Intent(CarLinkDexContract.ACTION_PSA_DIRECT_COMMAND);
                intent.setPackage(CarLinkDexContract.HOST_PACKAGE);
                intent.putExtras(commandExtras);
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                context.sendBroadcast(intent);
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + "Fallback broadcast routing failed: " + t.getMessage());
        }
    }

    private Bundle syncSessionFromPsAssistant() {
        Bundle session = callPsAssistantBridge(CarLinkDexContract.METHOD_GET_SESSION, null);
        if (session != null) {
            cachedSession = session;
        }
        return cachedSession;
    }

    private Bundle callPsAssistantBridge(String method, Bundle extras) {
        try {
            Context context = AndroidAppHelper.currentApplication();
            if (context == null) return null;
            return context.getContentResolver().call(
                    CarLinkDexContract.BRIDGE_URI,
                    method,
                    null,
                    extras
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + "Bridge IPC call failed (" + method + "): " + t.getMessage());
            return null;
        }
    }
}

