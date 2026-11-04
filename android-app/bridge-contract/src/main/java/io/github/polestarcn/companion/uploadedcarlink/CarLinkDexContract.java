package io.github.polestarcn.companion.uploadedcarlink;

import android.net.Uri;

/**
 * PsAssistant 主程序与 OPPO CarLink / SceneService / LSPosed 模组的统一协议契约
 * 同时覆盖：
 * - 路径 A (原生 platform_sdk URI / Service / Broadcast 协议)
 * - 路径 B (LSPosed 虚拟车辆 Session 与 ContentProvider.call 桥接协议)
 */
public final class CarLinkDexContract {

    private CarLinkDexContract() {}

    // 宿主与目标包名定义
    public static final String HOST_PACKAGE = "io.github.polestarcn.companion";
    public static final String PERMISSION_OPPO_SAFE_IOT = "com.oppo.permission.safe.IOT";

    // ContentProvider Authority 与标准路径 (兼容路径 A 的 URI 查询与路径 B 的 call 桥接)
    public static final String AUTHORITY = HOST_PACKAGE + ".bridge";
    public static final Uri BRIDGE_URI = Uri.parse("content://" + AUTHORITY);
    public static final String PATH_CHECK_CAR_BIND = "auth/checkCarBind/hasCarBinding";
    public static final Uri CHECK_CAR_BIND_URI = Uri.withAppendedPath(BRIDGE_URI, PATH_CHECK_CAR_BIND);

    // 路径 A：CarAppDemo (platform_sdk / car_control) 唤醒 Action 与 Service 契约
    public static final String ACTION_OPPO_CAR_CONTROL_SERVICE = "com.oppo.carlink.action.CAR_CONTROL_SERVICE";
    public static final String ACTION_OPLUS_CAR_CONTROL_SERVICE = "com.oplus.carlink.action.CAR_CONTROL_SERVICE";
    public static final String ACTION_PSA_DIRECT_COMMAND = HOST_PACKAGE + ".action.CARLINK_COMMAND";
    public static final String ACTION_PSA_STATE_CHANGED = HOST_PACKAGE + ".action.VEHICLE_STATE_CHANGED";

    // 路径 B：ContentProvider.call() 方法名定义
    public static final String METHOD_CHECK_BIND = "auth/checkCarBind/hasCarBinding";
    public static final String METHOD_CHECK_BIND_SHORT = "checkCarBind";
    public static final String METHOD_GET_SESSION = "getVirtualSession";
    public static final String METHOD_GET_SNAPSHOT = "getVehicleSnapshot";
    public static final String METHOD_DISPATCH_COMMAND = "dispatchCarCommand";

    // 四个核心 CAR 控制事件常量 (统一内部事件码)
    public static final int CAR_EVENT_LOCK_UNLOCK = 1001; // 1. 车门解闭锁
    public static final int CAR_EVENT_CLIMATE     = 1002; // 2. 空调启停/温控
    public static final int CAR_EVENT_WINDOW      = 1003; // 3. 车窗升降/透气
    public static final int CAR_EVENT_FIND_CAR    = 1004; // 4. 寻车闪灯鸣笛

    // 路径 A：platform_sdk 字符串指令名兼容常量
    public static final String SDK_CMD_LOCK = "lock";
    public static final String SDK_CMD_UNLOCK = "unlock";
    public static final String SDK_CMD_CLIMATE_ON = "climate_on";
    public static final String SDK_CMD_CLIMATE_OFF = "climate_off";
    public static final String SDK_CMD_WINDOW_CLOSE = "window_close";
    public static final String SDK_CMD_WINDOW_VENT = "window_vent";
    public static final String SDK_CMD_FIND_CAR = "find_car";

    // Bundle 传输字段 Key
    public static final String KEY_HAS_BINDING = "hasCarBinding";
    public static final String KEY_HAS_BINDING_SNAKE = "has_car_binding";
    public static final String KEY_VIN = "vehicle_vin";
    public static final String KEY_BRAND_NAME = "brand_name";
    public static final String KEY_VEHICLE_NAME = "vehicle_name";
    public static final String KEY_CONNECT_STATE = "connect_state";
    public static final String KEY_CAPABILITY_MASK = "capability_mask";

    // 车况快照字段 Key
    public static final String KEY_LOCKED = "is_locked";
    public static final String KEY_CLIMATE_ACTIVE = "is_climate_active";
    public static final String KEY_WINDOW_CLOSED = "is_window_closed";
    public static final String KEY_SOC_PERCENT = "soc_percent";
    public static final String KEY_RANGE_KM = "range_km";
    public static final String KEY_CABIN_TEMP = "cabin_temp";
    public static final String KEY_UPDATED_AT = "updated_at";

    // 控车指令请求与回调 Key
    public static final String KEY_EVENT_ID = "car_event_id";
    public static final String KEY_SDK_CMD_NAME = "sdk_cmd_name";
    public static final String KEY_ACTION_TARGET = "action_target_state";
    public static final String KEY_TARGET_TEMP = "target_temp";
    public static final String KEY_REQUEST_ID = "request_id";
    public static final String KEY_RESULT_RECEIVER = "result_receiver";
    public static final String KEY_EXEC_SUCCESS = "exec_success";
    public static final String KEY_EXEC_CODE = "exec_code";
    public static final String KEY_EXEC_MESSAGE = "exec_message";

    // 虚拟会话状态常量
    public static final int SESSION_STATE_DISCONNECTED = 0;
    public static final int SESSION_STATE_CONNECTING = 1;
    public static final int SESSION_STATE_CONNECTED_READY = 2;

    // 极星默认支持能力掩码 (Bit 0: 解闭锁, Bit 1: 空调, Bit 2: 车窗, Bit 3: 寻车)
    public static final int DEFAULT_POLESTAR_CAPABILITY_MASK = 0x0F;
}

