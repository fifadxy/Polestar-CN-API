package io.github.polestarcn.companion.uploadedcarlink;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.util.Log;

/**
 * 双模跨进程数据与控制入口：
 * 1. 支持路径 A：OPPO CarLink / SceneService 直接通过 URI (auth/checkCarBind/hasCarBinding) 查询绑定状态与车辆信息
 * 2. 支持路径 B：LSPosed 模块通过 ContentResolver.call() 获取虚拟车辆 Session 并投递 4 个 CAR 控制事件
 */
public class PsAssistantBridgeProvider extends ContentProvider {

    private static final String TAG = "PsAssistantBridge";

    private static final int MATCH_ROOT = 1;
    private static final int MATCH_CHECK_CAR_BIND = 2;
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(CarLinkDexContract.AUTHORITY, null, MATCH_ROOT);
        URI_MATCHER.addURI(CarLinkDexContract.AUTHORITY, CarLinkDexContract.PATH_CHECK_CAR_BIND, MATCH_CHECK_CAR_BIND);
        URI_MATCHER.addURI(CarLinkDexContract.AUTHORITY, "auth/checkCarBind/*", MATCH_CHECK_CAR_BIND);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (getContext() == null || method == null) {
            return null;
        }
        Log.d(TAG, "Bridge call: method=" + method + ", arg=" + arg + ", uid=" + Binder.getCallingUid());
        VirtualVehicleSessionManager sessionMgr = VirtualVehicleSessionManager.getInstance(getContext());

        if (CarLinkDexContract.METHOD_CHECK_BIND.equals(method)
                || CarLinkDexContract.METHOD_CHECK_BIND_SHORT.equals(method)
                || "hasCarBinding".equals(method)) {
            boolean bound = sessionMgr.hasActiveBinding();
            Bundle res = new Bundle();
            res.putBoolean(CarLinkDexContract.KEY_HAS_BINDING, bound);
            res.putBoolean(CarLinkDexContract.KEY_HAS_BINDING_SNAKE, bound);
            res.putInt("code", 0);
            res.putString(CarLinkDexContract.KEY_VIN, sessionMgr.getCurrentVin());
            return res;
        }

        if (CarLinkDexContract.METHOD_GET_SESSION.equals(method)) {
            return sessionMgr.buildSessionBundle();
        }

        if (CarLinkDexContract.METHOD_GET_SNAPSHOT.equals(method)) {
            return sessionMgr.buildSnapshotBundle();
        }

        if (CarLinkDexContract.METHOD_DISPATCH_COMMAND.equals(method)) {
            Bundle safeExtras = extras != null ? extras : new Bundle();
            if (arg != null && !safeExtras.containsKey(CarLinkDexContract.KEY_SDK_CMD_NAME)) {
                safeExtras.putString(CarLinkDexContract.KEY_SDK_CMD_NAME, arg);
            }
            int eventId = safeExtras.getInt(CarLinkDexContract.KEY_EVENT_ID, -1);
            VehicleProtocolGateway.getInstance(getContext()).executeCarEventAsync(eventId, safeExtras);

            Bundle ack = new Bundle();
            ack.putBoolean(CarLinkDexContract.KEY_EXEC_SUCCESS, true);
            ack.putInt(CarLinkDexContract.KEY_EXEC_CODE, 0);
            ack.putString(CarLinkDexContract.KEY_EXEC_MESSAGE, "DISPATCHED");
            return ack;
        }

        // 兼容路径 A：如果 CarLink 直接将方法名作为控车指令名调用 call()
        OppoCarLinkCommandMap.MappedAction directAction =
                OppoCarLinkCommandMap.resolveFromSdkCommandName(method, extras);
        if (directAction != OppoCarLinkCommandMap.MappedAction.UNKNOWN) {
            Bundle safeExtras = extras != null ? new Bundle(extras) : new Bundle();
            safeExtras.putString(CarLinkDexContract.KEY_SDK_CMD_NAME, method);
            safeExtras.putBoolean(CarLinkDexContract.KEY_ACTION_TARGET, directAction.targetState);
            VehicleProtocolGateway.getInstance(getContext())
                    .executeCarEventAsync(directAction.eventId, safeExtras);

            Bundle ack = new Bundle();
            ack.putBoolean(CarLinkDexContract.KEY_EXEC_SUCCESS, true);
            ack.putInt(CarLinkDexContract.KEY_EXEC_CODE, 0);
            return ack;
        }

        return sessionMgr.buildSessionBundle();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (getContext() == null) return null;
        VirtualVehicleSessionManager sessionMgr = VirtualVehicleSessionManager.getInstance(getContext());
        Bundle session = sessionMgr.buildSessionBundle();
        boolean bound = session.getBoolean(CarLinkDexContract.KEY_HAS_BINDING, true);

        // 路径 A：专门响应 auth/checkCarBind/hasCarBinding 的 Cursor 查询
        if (URI_MATCHER.match(uri) == MATCH_CHECK_CAR_BIND
                || (uri != null && uri.getPath() != null && uri.getPath().contains("checkCarBind"))) {
            MatrixCursor bindCursor = new MatrixCursor(new String[]{
                    "hasCarBinding", "has_car_binding", "is_bound", "vin", "brand", "vehicle_name", "code"
            });
            bindCursor.addRow(new Object[]{
                    bound ? 1 : 0,
                    bound ? 1 : 0,
                    bound ? 1 : 0,
                    session.getString(CarLinkDexContract.KEY_VIN),
                    "Polestar",
                    session.getString(CarLinkDexContract.KEY_VEHICLE_NAME),
                    0
            });
            return bindCursor;
        }

        // 通用车辆状态与白名单能力集 Cursor
        MatrixCursor cursor = new MatrixCursor(new String[]{
                "package_name", "brand_name", "vehicle_name", "vin",
                "hasCarBinding", "has_binding", "connect_state", "capability_mask",
                "is_locked", "is_climate_active", "is_window_closed", "soc", "range_km"
        });
        cursor.addRow(new Object[]{
                CarLinkDexContract.HOST_PACKAGE,
                "Polestar",
                session.getString(CarLinkDexContract.KEY_VEHICLE_NAME),
                session.getString(CarLinkDexContract.KEY_VIN),
                bound ? 1 : 0,
                bound ? 1 : 0,
                session.getInt(CarLinkDexContract.KEY_CONNECT_STATE),
                session.getInt(CarLinkDexContract.KEY_CAPABILITY_MASK),
                session.getBoolean(CarLinkDexContract.KEY_LOCKED) ? 1 : 0,
                session.getBoolean(CarLinkDexContract.KEY_CLIMATE_ACTIVE) ? 1 : 0,
                session.getBoolean(CarLinkDexContract.KEY_WINDOW_CLOSED) ? 1 : 0,
                session.getInt(CarLinkDexContract.KEY_SOC_PERCENT),
                session.getInt(CarLinkDexContract.KEY_RANGE_KM)
        });
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.item/psa_carlink_bridge";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) { return null; }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}

