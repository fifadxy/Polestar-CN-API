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
import java.util.Locale;

/** Read-only fingerprint of OPPO Shelf/Pantanal plugin-card hosts. */
final class ShelfCardHostProbe {
  private ShelfCardHostProbe() {}

  static void install(OppoBridgeModule module,String pkg,String process) {
    if(!"com.coloros.assistantscreen".equals(pkg)&&!"com.oplus.assistantscreen".equals(pkg)
        &&!"com.oplus.pantanal.ums".equals(pkg)&&!"com.oplus.smartengine".equals(pkg)
        &&!"com.oplus.cardservice".equals(pkg))return;
    attempt(module,pkg,process,0);
  }

  private static void attempt(OppoBridgeModule module,String pkg,String process,int retry) {
    Application app=currentApplication();
    if(app==null) {
      if(retry<5)new Handler(Looper.getMainLooper()).postDelayed(
          ()->attempt(module,pkg,process,retry+1),700L);
      return;
    }
    try {
      PackageInfo info=app.getPackageManager().getPackageInfo(pkg,
          PackageManager.GET_ACTIVITIES|PackageManager.GET_SERVICES|PackageManager.GET_PROVIDERS);
      Bundle in=new Bundle();
      in.putString("packageName",pkg);
      in.putString("process",process==null?"":process);
      in.putBoolean("ready",true);
      in.putString("reason","");
      in.putString("versionName",info.versionName==null?"":info.versionName);
      in.putLong("versionCode",Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode);
      in.putString("moduleVersion","1.11.0");
      in.putString("components",components(info));
      BridgeClient.call(app,"shelf-host-report",in);
      module.info("shelf_card_host package="+pkg+" components="+in.getString("components",""));
    } catch(Throwable failure) {
      try {
        Bundle in=new Bundle();in.putString("packageName",pkg);in.putString("process",process==null?"":process);
        in.putBoolean("ready",false);in.putString("reason",failure.getClass().getSimpleName());
        in.putString("moduleVersion","1.11.0");BridgeClient.call(app,"shelf-host-report",in);
      } catch(Throwable ignored) {}
      module.warn("shelf_card_host package="+pkg+" failed="+failure.getClass().getSimpleName());
    }
  }

  private static String components(PackageInfo info) {
    ArrayList<String> out=new ArrayList<>();
    if(info.activities!=null)for(ActivityInfo x:info.activities)add(out,"A",x.name);
    if(info.services!=null)for(ServiceInfo x:info.services)add(out,"S",x.name);
    if(info.providers!=null)for(ProviderInfo x:info.providers)add(out,"P",x.name);
    return String.join("|",out);
  }

  private static void add(ArrayList<String> out,String kind,String name) {
    if(name==null||out.size()>=64)return;
    String lower=name.toLowerCase(Locale.ROOT);
    if(lower.contains("shelf")||lower.contains("pantanal")||lower.contains("seedling")
        ||lower.contains("card")||lower.contains("plugin")||lower.contains("vehicle")
        ||lower.contains("car"))
      out.add(kind+":"+name.substring(0,Math.min(name.length(),180)));
  }

  private static Application currentApplication() {
    try{return (Application)Class.forName("android.app.ActivityThread")
        .getMethod("currentApplication").invoke(null);}
    catch(Throwable ignored){return null;}
  }
}