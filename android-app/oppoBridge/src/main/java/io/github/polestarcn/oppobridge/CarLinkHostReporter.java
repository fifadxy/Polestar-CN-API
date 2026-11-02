package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/** Read-only proof that the module actually loaded in the official OPPO CarLink host. */
final class CarLinkHostReporter {
  private static final String HOST="com.heytap.opluscarlink";
  private CarLinkHostReporter() {}

  static void report(OppoBridgeModule module,String process,String phase,boolean ready,String reason) {
    attempt(module,process,phase,ready,reason,0);
  }

  private static void attempt(OppoBridgeModule module,String process,String phase,
      boolean ready,String reason,int retry) {
    Application app=currentApplication();
    if(app==null) {
      if(retry<5)new Handler(Looper.getMainLooper()).postDelayed(
          ()->attempt(module,process,phase,ready,reason,retry+1),700L);
      return;
    }
    try {
      Bundle in=new Bundle();
      in.putString("process",process==null?"":process);
      in.putString("phase",phase==null?"":phase);
      in.putBoolean("ready",ready);
      in.putString("reason",reason==null?"":reason);
      in.putString("hostVersion",app.getPackageManager().getPackageInfo(HOST,0).versionName);
      in.putString("moduleVersion","1.5.0");
      BridgeClient.call(app,"carlink-host-report",in);
      module.info("carlink_host_report phase="+phase+" ready="+ready+" process="+process);
    } catch(Throwable failure) {
      module.warn("carlink_host_report failed="+failure.getClass().getSimpleName());
    }
  }

  private static Application currentApplication() {
    try {
      return (Application)Class.forName("android.app.ActivityThread")
          .getMethod("currentApplication").invoke(null);
    } catch(Throwable ignored) { return null; }
  }
}
