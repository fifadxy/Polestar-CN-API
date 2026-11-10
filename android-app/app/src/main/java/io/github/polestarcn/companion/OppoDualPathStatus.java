package io.github.polestarcn.companion;

import android.content.Context;
import android.os.Bundle;

/** Independent, observable official SDK authorization and local vehicle bridge.
 * Never infer official credentials, partner entitlement or a vehicle-command
 * success from a projected local card/binding session. */
final class OppoDualPathStatus {
  private OppoDualPathStatus() {}

  static Bundle summarize(boolean sdkPackaged, boolean officialConfigValid,
      boolean officialAuthorized, boolean mock, Bundle localSession) {
    Bundle out=new Bundle();
    boolean official= sdkPackaged && officialConfigValid && officialAuthorized && !mock;
    out.putString("officialPath",mock?"MOCK":official?"AUTHORIZED":
        !sdkPackaged?"SDK_NOT_PACKAGED":!officialConfigValid?"CONFIG_REQUIRED":"AUTH_PENDING");
    out.putBoolean("officialAuthorized",official);
    out.putBoolean("localVisible",localSession!=null&&localSession.getBoolean("present",false));
    out.putBoolean("localActive",localSession!=null&&localSession.getBoolean("active",false));
    out.putBoolean("localCommandReady",localSession!=null&&localSession.getBoolean("commandReady",false));
    out.putBoolean("cachedOnly",localSession!=null&&localSession.getBoolean("cacheOnly",false));
    out.putString("localLifecycle",localSession==null?"DISCONNECTED":
        localSession.getString("lifecycle","DISCONNECTED"));
    return out;
  }

  static Bundle snapshot(Context context) {
    OppoCarLinkConfig config=OppoCarLinkConfig.load(context);
    Bundle local=VirtualVehicleSessionManager.status(context,VehicleCoordinator.get(context));
    return summarize(BuildConfig.OPPO_SDK_PACKAGED,config.missing().isEmpty(),
        OppoCarLinkRuntime.get(context).authorized(),BuildConfig.OPPO_SDK_MOCK,local);
  }

  static String readable(Context context) {
    Bundle state=snapshot(context);
    return "双路径诊断（严格区分官方授权与本地桥接）"
        +"\nOPPO官方SDK="+state.getString("officialPath")
        +" · 已授权="+state.getBoolean("officialAuthorized")
        +"\nPsAssistant本地会话="+state.getString("localLifecycle")
        +" · 可展示="+state.getBoolean("localVisible")
        +" · 缓存展示="+state.getBoolean("cachedOnly")
        +" · 可安全发起指令="+state.getBoolean("localCommandReady")
        +"\n提示：本地卡片绑定/指令入队不代表OPPO官方授权或车辆执行成功。";
  }
}
