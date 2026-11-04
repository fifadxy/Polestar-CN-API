package io.github.polestarcn.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

/** Sanitized observations from OPPO vehicle/Wallet hosts. No account, VIN, BLE address or key data. */
final class OppoVehicleSurfaceDiagnostics {
  private static final String PREF="oppo-vehicle-surface-diagnostics";
  private static final String[] HOSTS={
      "com.heytap.opluscarlink","com.finshell.wallet","com.heytap.health.wallet","com.oplus.cardigitalkey"
  };
  private OppoVehicleSurfaceDiagnostics() {}

  static Bundle record(Context context,Bundle in) {
    if(in==null)throw new IllegalArgumentException("Missing vehicle-surface report");
    String pkg=safePackage(in.getString("packageName",""));
    if(pkg.isEmpty())throw new IllegalArgumentException("Unsupported vehicle-surface host");
    String prefix=prefix(pkg)+"_";
    SharedPreferences.Editor e=context.getSharedPreferences(PREF,0).edit();
    e.putLong(prefix+"seenAt",System.currentTimeMillis());
    e.putBoolean(prefix+"ready",in.getBoolean("ready",false));
    e.putLong(prefix+"versionCode",Math.max(-1L,in.getLong("versionCode",-1L)));
    for(String key:new String[]{"versionName","process","moduleVersion","classes","components","reason"})
      e.putString(prefix+key,bounded(in.getString(key,""),key.equals("components")?2600:1200));
    e.apply();
    return status(context);
  }

  static Bundle status(Context context) {
    SharedPreferences p=context.getSharedPreferences(PREF,0);Bundle out=new Bundle();
    for(String pkg:HOSTS) {
      String prefix=prefix(pkg)+"_";
      out.putString(prefix+"packageName",pkg);
      out.putLong(prefix+"seenAt",p.getLong(prefix+"seenAt",0L));
      out.putBoolean(prefix+"ready",p.getBoolean(prefix+"ready",false));
      out.putLong(prefix+"versionCode",p.getLong(prefix+"versionCode",-1L));
      for(String key:new String[]{"versionName","process","moduleVersion","classes","components","reason"})
        out.putString(prefix+key,p.getString(prefix+key,""));
    }
    return out;
  }

  static String prefix(String pkg) {
    if("com.heytap.opluscarlink".equals(pkg))return "carlink";
    if("com.finshell.wallet".equals(pkg))return "wallet";
    if("com.heytap.health.wallet".equals(pkg))return "healthWallet";
    if("com.oplus.cardigitalkey".equals(pkg))return "digitalKey";
    return "unknown";
  }

  private static String safePackage(String pkg) {
    for(String host:HOSTS)if(host.equals(pkg))return host;return "";
  }
  private static String bounded(String value,int limit) {
    if(value==null)return "";String clean=value.replace('\n',' ').replace('\r',' ').trim();
    return clean.substring(0,Math.min(clean.length(),limit));
  }
}