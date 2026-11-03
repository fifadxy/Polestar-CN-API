package io.github.polestarcn.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

/** Bounded runtime observations of MyDevices QuickApp widget replacement and final card data. */
final class OppoQuickAppRuntimeDiagnostics {
  private static final String PREF="oppo-quickapp-runtime-diagnostics";
  private static final String[] STRINGS={
      "hostVersion","moduleVersion","phase","method","widgetCode","oldWidgetCode",
      "replacedWidgetCode","cardPath","reason","resultKeys","selectedIdClass","selectedType",
      "selectedPackage","selectedShortcutIds","deviceListParse","oemShimMode",
      "rpkPatchReason","rpkEntrySha256","lastClickShortcut","lastCommandAction","lastCommandReason"
  };
  private static final String[] BOOLEANS={
      "single4x2CarRegistry","single2x2CarRegistry","single4x2EventSummaryCar",
      "single2x2EventSummaryCar","deviceListPresent","selectedIsOurCard","selectedHasData",
      "isAuth","isBind","hasDevice","oemShimApplied","oemCarRegistry",
      "oemSmartHomeDefaultCar","oemEventSummaryCar","rpkPatchApplied",
      "converterWhitelistApplied","oemClickSeen","controllerMatched",
      "carEventReceived","carTypeGatePassed","carDispatchIntercepted",
      "commandIntentCreated","pendingIntentSent",
      "lastCommandAccepted","lastCommandSent"
  };
  private static final String[] INTS={
      "deviceCount","selectedDeviceIndex","selectedShortcutCount","rpkPatchCount",
      "oemClickCount","controllerMatchCount","commandAttemptCount"
  };
  private static final String[] LONGS={
      "rpkPatchAt","converterWhitelistAt","oemClickAt","controllerMatchedAt",
      "carEventReceivedAt","carTypeGatePassedAt","carDispatchInterceptedAt",
      "commandIntentCreatedAt","pendingIntentSentAt","lastCommandAt"
  };

  private OppoQuickAppRuntimeDiagnostics() {}

  static Bundle record(Context context, Bundle in) {
    if(in==null)throw new IllegalArgumentException("Missing QuickApp runtime report");
    SharedPreferences prefs=context.getSharedPreferences(PREF,0);
    String incomingHost=in.getString("hostVersion",""),previousHost=prefs.getString("hostVersion","");
    SharedPreferences.Editor e=prefs.edit();
    if(!incomingHost.isEmpty()&&!previousHost.isEmpty()&&!incomingHost.equals(previousHost))e.clear();
    e.putLong("seenAt",System.currentTimeMillis());
    e.putBoolean("ready",in.getBoolean("ready",false));
    for(String key:STRINGS) {
      if(!in.containsKey(key))continue;
      String value=in.getString(key,"");
      if(value==null)value="";
      e.putString(key,value.substring(0,Math.min(value.length(),1200)));
    }
    for(String key:BOOLEANS)
      if(in.containsKey(key))e.putBoolean(key,in.getBoolean(key,false));
    for(String key:INTS)
      if(in.containsKey(key))e.putInt(key,in.getInt(key,0));
    for(String key:LONGS)
      if(in.containsKey(key))e.putLong(key,in.getLong(key,0L));
    e.apply();
    return status(context);
  }

  static Bundle status(Context context) {
    SharedPreferences p=context.getSharedPreferences(PREF,0);
    Bundle out=new Bundle();
    out.putLong("seenAt",p.getLong("seenAt",0L));
    out.putBoolean("ready",p.getBoolean("ready",false));
    for(String key:STRINGS)out.putString(key,p.getString(key,""));
    for(String key:BOOLEANS)out.putBoolean(key,p.getBoolean(key,false));
    for(String key:INTS)out.putInt(key,p.getInt(key,0));
    for(String key:LONGS)out.putLong(key,p.getLong(key,0L));
    return out;
  }

  static Bundle recordCommand(Context context,String action,boolean accepted,boolean sent,String reason) {
    SharedPreferences p=context.getSharedPreferences(PREF,0);
    SharedPreferences.Editor e=p.edit();
    e.putLong("lastCommandAt",System.currentTimeMillis());
    e.putInt("commandAttemptCount",p.getInt("commandAttemptCount",0)+1);
    e.putString("lastCommandAction",bounded(action));
    e.putString("lastCommandReason",bounded(reason));
    e.putBoolean("lastCommandAccepted",accepted);
    e.putBoolean("lastCommandSent",sent);
    e.apply();
    return status(context);
  }

  private static String bounded(String value) {
    if(value==null)return "";
    return value.substring(0,Math.min(value.length(),160));
  }
}
