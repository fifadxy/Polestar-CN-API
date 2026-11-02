package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;

/** Read-only proof that the module loaded in the official OPPO CarLink host. */
final class CarLinkHostReporter {
  private static final String HOST="com.heytap.opluscarlink";
  private static final String[] CLASSES={
      "com.oplus.ocs.carlink.CarLink","com.oplus.ocs.carlink.CarLinkManager",
      "com.oplus.ocs.carlink.CarLinkUnit","com.oplus.ocs.carlink.CarLinkUnitClient",
      "com.oplus.ocs.carlink.BluetoothKey","com.oplus.ocs.carlink.BluetoothKey$Builder","com.oplus.ocs.carlink.key.ICarKeyCallback",
      "com.oplus.ocs.carlink.control.CarControl","com.oplus.ocs.carlink.control.ICarControlCallback"
  };
  private CarLinkHostReporter() {}
  static void report(OppoBridgeModule module,String process,String phase,boolean ready,String reason,ClassLoader loader){attempt(module,process,phase,ready,reason,loader,0);}
  private static void attempt(OppoBridgeModule module,String process,String phase,boolean ready,String reason,ClassLoader loader,int retry){Application app=currentApplication();if(app==null){if(retry<5)new Handler(Looper.getMainLooper()).postDelayed(()->attempt(module,process,phase,ready,reason,loader,retry+1),700L);return;}try{android.content.pm.PackageInfo info=app.getPackageManager().getPackageInfo(HOST,0);Bundle in=new Bundle();in.putString("process",process==null?"":process);in.putString("phase",phase==null?"":phase);in.putBoolean("ready",ready);in.putString("reason",reason==null?"":reason);in.putString("hostVersion",info.versionName==null?"":info.versionName);in.putLong("versionCode",Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode);in.putString("moduleVersion","1.11.0");in.putString("classes",classes(loader));BridgeClient.call(app,"carlink-host-report",in);module.info("carlink_host_report phase="+phase+" ready="+ready+" process="+process);}catch(Throwable failure){module.warn("carlink_host_report failed="+failure.getClass().getSimpleName());}}
  private static String classes(ClassLoader loader){ArrayList<String> found=new ArrayList<>();for(String name:CLASSES)try{Class.forName(name,false,loader);found.add(name);}catch(Throwable ignored){}return String.join("|",found);}
  private static Application currentApplication(){try{return (Application)Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);}catch(Throwable ignored){return null;}}
}