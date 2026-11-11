package io.github.polestarcn.companion;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import org.json.JSONObject;

/**
 * Read-only diagnostic snapshot for a rooted ColorOS device.
 * Only fixed, explicitly allowlisted fields are collected. Never exports VIN,
 * app credentials, session, installed APKs, unfiltered logcat, or vehicle data.
 */
final class OppoBridgeAutoDiagnostic {
  private static final String PREF="psa-lsposed-diagnostic-v1210";
  private static final long RECENT_MS=10L*60_000L;
  private OppoBridgeAutoDiagnostic(){}

  static boolean recent(long seen,long now){
    return seen>0&&seen<=now&&now-seen<=RECENT_MS;
  }
  static String analysis(boolean root,boolean integration,boolean hostSeen,
      boolean quickSeen,boolean localReady,boolean carAuthorized){
    if(!root)return "ROOT_NOT_GRANTED：检查 Magisk 授权及 su 可执行性；不尝试修改系统权限。";
    if(!integration)return "BRIDGE_DISABLED：在 PsAssistant 系统接入页启用设备接入，然后重新检查。";
    if(!hostSeen)return "NO_LIVE_HOOK：核对 LSPosed 中模块是否启用、作用域是否包含 MyDevices/AssistantScreen/CarLink；用确认按钮重启宿主后重新进入卡片。";
    if(!quickSeen)return "QUICKAPP_NO_CALLBACK：宿主已报告 Hook，但没有近期 MyDevices RPK/卡片回报。核查宿主版本、RPK 结构匹配与 New/Stable 模式。";
    if(!localReady)return "SESSION_NOT_READY：确认 PsAssistant 中国区登录、选车及刷新，检查 Provider 只读授权，不要伪造官方绑定。";
    if(!carAuthorized)return "LOCAL_ONLY：本地桥接可用，但 OPPO 官方 SDK 授权未获真实回调；不能将本地绑定标为 OEM 认证成功。";
    return "READY_FOR_DEVICE_TEST：仅表明模块、宿主回报与本地桥接条件具备；请通过明确授权的操作验证实车回读。";
  }
  private static JSONObject host(Context c,String pkg,long now){
    JSONObject o=new JSONObject();
    try {
      o.put("package",pkg);
      PackageInfo info=c.getPackageManager().getPackageInfo(pkg,0);
      o.put("visibility","visible").put("version",info.versionName==null?"":info.versionName);
    }catch(Exception notVisible){o.put("visibility","not-visible-or-not-installed");}
    return o;
  }
  static String capture(Context c,String root){
    try {
      long now=System.currentTimeMillis();
      Bundle settings=io.github.polestarcn.oppobridge.BridgeSettings.snapshot(c);
      Bundle hosts=OppoHostDiagnostics.status(c);
      Bundle catalog=OppoCarLinkHostDiagnostics.status(c);
      Bundle quick=OppoQuickAppRuntimeDiagnostics.status(c);
      Bundle modes=OppoBridgeModeDiagnostics.status(c);
      Bundle local=VirtualVehicleSessionManager.status(c,VehicleCoordinator.get(c));
      Bundle official=OppoDualPathStatus.snapshot(c);
      boolean myDevices=recent(hosts.getLong("main_seenAt",0),now)
          ||recent(hosts.getLong("cards_seenAt",0),now);
      boolean assistant=recent(OppoAssistantHostDiagnostics.status(c).getLong("seenAt",0),now);
      boolean carlink=recent(catalog.getLong("seenAt",0),now);
      boolean anyHost=myDevices||assistant||carlink;
      boolean quickActive=recent(quick.getLong("seenAt",0),now);
      boolean integration=OppoDeviceIntegration.enabled(c);
      boolean canCommand=local.getBoolean("commandReady",false);
      boolean approved=official.getBoolean("officialAuthorized",false);
      JSONObject o=new JSONObject()
          .put("schema","psa-lsposed-diagnostic-v1210")
          .put("timestamp",now)
          .put("appVersion",BuildConfig.VERSION_NAME)
          .put("bridgeVersion",settings.getString("moduleVersion","unknown"))
          .put("androidApi",android.os.Build.VERSION.SDK_INT)
          .put("root",root)
          .put("moduleEnabled",settings.getBoolean("masterEnabled",false))
          .put("selectedMode",settings.getString("compatibilityMode","unknown"))
          .put("rpkMode",settings.getString("rpkCompatibilityMode","unknown"))
          .put("integrationEnabled",integration)
          .put("signedIn",VehicleCoordinator.get(c).hasSession())
          .put("selectedVehicle",VehicleCoordinator.get(c).selected!=null)
          .put("localSessionPresent",local.getBoolean("present",false))
          .put("localSessionCommandReady",canCommand)
          .put("oppoOfficialAuthorized",approved)
          .put("myDevicesHookRecent",myDevices)
          .put("assistantScreenHookRecent",assistant)
          .put("carlinkHookRecent",carlink)
          .put("quickAppObservedRecent",quickActive)
          .put("quickAppRpkApplied",quick.getBoolean("rpkPatchApplied",false))
          .put("quickAppDispatchSeen",quick.getBoolean("carDispatchIntercepted",false))
          .put("quickAppLastCommandAccepted",quick.getBoolean("lastCommandAccepted",false))
          .put("quickAppLastCommandSent",quick.getBoolean("lastCommandSent",false))
          .put("myDevicesHookPhase",hosts.getString("main_phase",""))
          .put("cardProcessHookPhase",hosts.getString("cards_phase",""))
          .put("latestHostModeCount",modes.size()/2)
          .put("analysis",analysis("ROOT_GRANTED".equals(root),integration,anyHost,quickActive,canCommand,approved))
          .put("note","Hook observed != active now; command accepted != vehicle readback; no OEM authorization is simulated.");
      org.json.JSONArray packages=new org.json.JSONArray();
      for(String pkg:io.github.polestarcn.oppobridge.BridgeHostRestarter.targets())
        packages.put(host(c,pkg,now));
      o.put("hostPackages",packages);
      return o.toString(2);
    }catch(Exception error){
      return "{\"schema\":\"psa-lsposed-diagnostic-v1210\",\"error\":\""
          +error.getClass().getSimpleName()+"\"}";
    }
  }
  static String run(Context c){
    String result=capture(c,io.github.polestarcn.oppobridge.BridgeHostRestarter.rootStatus());
    c.getSharedPreferences(PREF,0).edit().putString("last",result).apply();
    return result;
  }
  static String export(Context c){
    String last=c.getSharedPreferences(PREF,0).getString("last","");
    return last.isEmpty()?capture(c,"NOT_PROBED"):last;
  }
}
