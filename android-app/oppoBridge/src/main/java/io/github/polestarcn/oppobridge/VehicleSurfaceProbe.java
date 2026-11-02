package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Read-only class/component fingerprint for OPPO CarLink and Wallet vehicle surfaces. */
final class VehicleSurfaceProbe {
  private static final Set<String> HOSTS=new LinkedHashSet<>();
  static {
    HOSTS.add("com.heytap.opluscarlink");
    HOSTS.add("com.finshell.wallet");
    HOSTS.add("com.heytap.health.wallet");
    HOSTS.add("com.oplus.cardigitalkey");
  }
  private static final String[] CARLINK_CLASSES={
      "com.oplus.ocs.carlink.CarLink",
      "com.oplus.ocs.carlink.CarLinkManager",
      "com.oplus.ocs.carlink.BluetoothKey","com.oplus.ocs.carlink.BluetoothKey$Builder",
      "com.oplus.ocs.carlink.key.ICarKeyCallback",
      "com.oplus.ocs.carlink.control.CarControl",
      "com.oplus.ocs.carlink.control.ICarControlCallback",
      "com.oplus.ocs.carlink.model.CarInfo",
      "com.oplus.ocs.carlink.model.CarStatus"
  };

  private VehicleSurfaceProbe() {}

  static void install(OppoBridgeModule module,String pkg,String process,ClassLoader loader) {
    if(!HOSTS.contains(pkg))return;
    attempt(module,pkg,process,loader,0);
  }

  private static void attempt(OppoBridgeModule module,String pkg,String process,ClassLoader loader,int retry) {
    Application app=currentApplication();
    if(app==null) {
      if(retry<5)new Handler(Looper.getMainLooper()).postDelayed(
          ()->attempt(module,pkg,process,loader,retry+1),700L);
      return;
    }
    try {
      PackageInfo info=app.getPackageManager().getPackageInfo(pkg,
          PackageManager.GET_ACTIVITIES|PackageManager.GET_SERVICES|PackageManager.GET_PROVIDERS);
      Bundle in=new Bundle();in.putString("packageName",pkg);in.putString("process",process==null?"":process);
      in.putBoolean("ready",true);in.putString("reason","");
      in.putString("versionName",info.versionName==null?"":info.versionName);
      in.putLong("versionCode",Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode);
      in.putString("moduleVersion","1.12.0");
      in.putString("classes",probeClasses(pkg,loader));
      in.putString("components",components(info));
      BridgeClient.call(app,"vehicle-surface-report",in);
      module.info("vehicle_surface_probe package="+pkg+" classes="+in.getString("classes","") );
    } catch(Throwable failure) {
      try {
        Bundle in=new Bundle();in.putString("packageName",pkg);in.putString("process",process==null?"":process);
        in.putBoolean("ready",false);in.putString("reason",failure.getClass().getSimpleName());
        in.putString("moduleVersion","1.12.0");BridgeClient.call(app,"vehicle-surface-report",in);
      } catch(Throwable ignored) {}
      module.warn("vehicle_surface_probe package="+pkg+" failed="+failure.getClass().getSimpleName());
    }
  }

  private static String probeClasses(String pkg,ClassLoader loader) {
    if(!"com.heytap.opluscarlink".equals(pkg))return "";
    ArrayList<String> found=new ArrayList<>();
    for(String name:CARLINK_CLASSES)try {Class.forName(name,false,loader);found.add(name);}catch(Throwable ignored) {}
    return String.join("|",found);
  }

  private static String components(PackageInfo info) {
    ArrayList<String> out=new ArrayList<>();
    if(info.activities!=null)for(ActivityInfo x:info.activities)add(out,"A",x.name);
    if(info.services!=null)for(ServiceInfo x:info.services)add(out,"S",x.name);
    if(info.providers!=null)for(ProviderInfo x:info.providers)add(out,"P",x.name);
    return String.join("|",out);
  }

  private static void add(ArrayList<String> out,String kind,String name) {
    if(name==null||out.size()>=48)return;String lower=name.toLowerCase(Locale.ROOT);
    if(lower.contains("car")||lower.contains("key")||lower.contains("vehicle")||lower.contains("wallet"))
      out.add(kind+":"+name.substring(0,Math.min(name.length(),180)));
  }

  private static Application currentApplication() {
    try{return (Application)Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);}
    catch(Throwable ignored){return null;}
  }
}