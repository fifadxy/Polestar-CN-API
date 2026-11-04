package io.github.polestarcn.bridge;
/** PsAssistant local IPC v1. Event IDs are local, not undocumented OEM Binder transaction IDs. */
public final class CarLinkBridgeContract {
 private CarLinkBridgeContract(){}
 public static final int VERSION=1,LOCK=1001,CLIMATE=1002,WINDOW=1003,FIND=1004;
 public static final String AUTHORITY="io.github.polestarcn.companion.bridge";
 public static final String BIND_PATH="auth/checkCarBind/hasCarBinding";
 public static final String SESSION="getVirtualSession",SNAPSHOT="getVehicleSnapshot",DISPATCH="dispatchCarCommand";
 public static final String EVENT="car_event_id",COMMAND="sdk_cmd_name",TARGET="action_target_state",REQUEST="request_id",RECEIVER="result_receiver";
 public static final String SERVICE_ACTION="io.github.polestarcn.companion.action.CARLINK_MESSENGER_V1",COMMAND_ACTION="io.github.polestarcn.companion.action.CARLINK_COMMAND";
 public static boolean bindMethod(String method){return BIND_PATH.equals(method)||"checkCarBind".equals(method)||"hasCarBinding".equals(method);}
 public static boolean readMethod(String method){return bindMethod(method)||SESSION.equals(method)||SNAPSHOT.equals(method);}
}
