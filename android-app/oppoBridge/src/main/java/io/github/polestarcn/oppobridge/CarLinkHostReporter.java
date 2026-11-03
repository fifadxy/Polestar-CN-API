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
  private static void attempt(OppoBridgeModule module,String process,String phase,boolean ready,String reason,ClassLoader loader,int retry){Application app=currentApplication();if(app==null){if(retry<5)new Handler(Looper.getMainLooper()).postDelayed(()->attempt(module,process,phase,ready,reason,loader,retry+1),700L);return;}try{android.content.pm.PackageInfo info=app.getPackageManager().getPackageInfo(HOST,0);Bundle in=new Bundle();in.putString("process",process==null?"":process);in.putString("phase",phase==null?"":phase);in.putBoolean("ready",ready);in.putString("reason",reason==null?"":reason);in.putString("hostVersion",info.versionName==null?"":info.versionName);in.putLong("versionCode",Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode);in.putString("moduleVersion",BridgeBuild.VERSION_NAME);in.putString("classes",classes(loader));in.putString("contract",contract(loader));BridgeClient.call(app,"carlink-host-report",in);module.info("carlink_host_report phase="+phase+" ready="+ready+" process="+process);}catch(Throwable failure){module.warn("carlink_host_report failed="+failure.getClass().getSimpleName());}}
  private static String classes(ClassLoader loader){ArrayList<String> found=new ArrayList<>();for(String name:CLASSES)try{Class.forName(name,false,loader);found.add(name);}catch(Throwable ignored){}return String.join("|",found);}
  private static String contract(ClassLoader loader){
    ArrayList<String> found=new ArrayList<>();
    inspect(loader,"com.oplus.ocs.carlink.control.CarControl",new String[]{
        "registerCallback","auth","subscribe","accountExpired","accountUnbind",
        "sendCarInfo","sendCarInfoList","sendCarStatus","sendCarStatusList",
        "sendControlMessage","sendCarInstruction","sendSwitchCarResult",
        "supportCarControl","hasCarBinding","checkCarBind"},found);
    inspect(loader,"com.oplus.ocs.carlink.control.ICarControlCallback",new String[]{
        "onComplete","inquireCarInfo","inquireCarStatus","onControl","onAuthResult",
        "onSubscribeResult","onUnbindResult","onVerify","onSwitchCar",
        "onShowCard","onHideCard","activateCar"},found);
    return String.join("|",found);
  }
  private static void inspect(ClassLoader loader,String className,String[] wanted,ArrayList<String> out){
    try{
      Class<?> type=Class.forName(className,false,loader);
      java.util.HashSet<String> names=new java.util.HashSet<>();
      for(java.lang.reflect.Method method:type.getDeclaredMethods())names.add(method.getName());
      for(String name:wanted)if(names.contains(name))out.add(type.getSimpleName()+"."+name);
    }catch(Throwable ignored){}
  }
  private static Application currentApplication(){try{return (Application)Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);}catch(Throwable ignored){return null;}}
}
