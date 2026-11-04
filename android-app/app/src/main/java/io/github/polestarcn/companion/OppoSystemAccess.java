package io.github.polestarcn.companion;

import android.content.Context;
import android.os.Bundle;
import android.content.SharedPreferences;

/** One live-device integration setting and non-secret, independently observed test results. */
final class OppoSystemAccess {
  private static final String PREF="oppo-system-access-v3";
  private OppoSystemAccess(){}
  static void setEnabled(Context c,boolean on){
    OppoDeviceIntegration.setEnabled(c,on);
    io.github.polestarcn.oppobridge.BridgeSettings.setUnified(c,on);
    OppoOemCarExperimentState.set(c,on);
    VirtualVehicleSessionManager.setEnabled(c,VehicleCoordinator.get(c),on);
    OppoOfficialVehicleBindState.set(c,VehicleCoordinator.get(c),on);
    if(on)OppoDeviceIntegration.restoreCard(c);
  }
  static Bundle recordPanel(Context c,Bundle input){
    if(input==null)throw new IllegalArgumentException("Missing panel observation");
    c.getSharedPreferences(PREF,0).edit().putLong("panelAt",System.currentTimeMillis())
      .putBoolean("panelReady",input.getBoolean("ready",false))
      .putString("panelModule",input.getString("moduleVersion",""))
      .putString("panelReason",input.getString("reason","")).apply();return observations(c);
  }
  static void commandEvent(Context c,String action,long issued,String state){
    c.getSharedPreferences(PREF,0).edit().putString("commandAction",action).putLong("commandIssuedAt",issued)
      .putLong("commandEventAt",System.currentTimeMillis()).putString("commandState",state).apply();
  }
  static Bundle observations(Context c){
    SharedPreferences p=c.getSharedPreferences(PREF,0);Bundle b=new Bundle();
    for(String key:new String[]{"panelModule","panelReason","commandAction","commandState"})b.putString(key,p.getString(key,""));
    for(String key:new String[]{"panelAt","commandIssuedAt","commandEventAt"})b.putLong(key,p.getLong(key,0));
    b.putBoolean("panelReady",p.getBoolean("panelReady",false));return b;
  }
  static String summary(Context c){
    VehicleCoordinator m=VehicleCoordinator.get(c);Bundle session=VirtualVehicleSessionManager.status(c,m);
    Bundle route=OppoOfficialCarLink.status(c),quick=OppoQuickAppRuntimeDiagnostics.status(c),observed=observations(c);
    return "PsAssistant "+BuildConfig.VERSION_NAME+" / "+BuildConfig.VERSION_CODE+" · Bridge 2.0.0\n"
      +"运行通道："+(BuildConfig.OPPO_SDK_MOCK?"MOCK_TESTLAB":"真实极星中国区接口")
      +"\n系统接入："+OppoDeviceIntegration.enabled(c)+" · 登录="+m.hasSession()+" · 选车="+(m.selected!=null)
      +"\n车辆会话="+session.getString("lifecycle","")+" · 可控="+session.getBoolean("commandReady",false)
      +"\n设备卡="+OppoProviderDiagnostics.inspect(c).toString()
      +"\n负一屏事件时间="+quick.getLong("carEventReceivedAt",0)+" · 受理="+quick.getBoolean("lastCommandAccepted",false)
      +" · 原因="+quick.getString("lastCommandReason",quick.getString("reason",""))
      +"\nCarLink Hook="+OppoCarLinkHostDiagnostics.status(c).getLong("seenAt",0)
      +"\nCarLink 本机面板="+observed.getBoolean("panelReady",false)+" · 时间="+observed.getLong("panelAt",0)
      +" · 模块="+observed.getString("panelModule","")+" · 原因="+observed.getString("panelReason","")
      +"\nIOT 系统权限="+route.getBoolean("bindActivityPermissionGranted",false)
      +" · 权限名="+route.getString("bindActivityPermission","")
      +"\nRoot 入口使用 root 身份启动；不表示主程序获得 IOT 权限。"
      +"\n最近真实命令="+observed.getString("commandAction","")+" · 结果="+observed.getString("commandState","")
      +" · 时间="+observed.getLong("commandEventAt",0)
      +"\n仅 VEHICLE_CONFIRMED 表示可读状态已确认；鸣笛/闪灯需现场确认。"
      +"\n官方 OCS："+(BuildConfig.OPPO_SDK_PACKAGED?"SDK 已打包；授权需真实回调":"当前为本机桥接构建，未包含官方 SDK 实现")
      +"\n手表/数字钥匙：尚未完成实机适配。页面不自动刷新车辆。";
  }
}
