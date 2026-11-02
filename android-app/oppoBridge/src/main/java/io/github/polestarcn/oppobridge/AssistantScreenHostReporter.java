package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/** Reports that the current Bridge generation actually loaded into AssistantScreen. */
final class AssistantScreenHostReporter {
  private static final String HOST="com.coloros.assistantscreen";
  private AssistantScreenHostReporter() {}

  static void report(OppoBridgeModule module,String phase,boolean ready,String reason) {
    attempt(module,phase,ready,reason,0);
  }

  private static void attempt(OppoBridgeModule module,String phase,boolean ready,String reason,int retry) {
    Application app=currentApplication();
    if(app==null) {
      if(retry<5)new Handler(Looper.getMainLooper()).postDelayed(
          ()->attempt(module,phase,ready,reason,retry+1),700L);
      return;
    }
    try {
      Bundle in=new Bundle();
      in.putString("phase",phase);
      in.putBoolean("ready",ready);
      in.putString("reason",reason==null?"":reason);
      in.putString("hostVersion",app.getPackageManager().getPackageInfo(HOST,0).versionName);
      in.putString("moduleVersion","1.11.0");
      BridgeClient.call(app,"assistant-host-report",in);
      module.info("assistant_host_report phase="+phase+" ready="+ready);
    } catch(Throwable failure) {
      module.warn("assistant_host_report failed="+failure.getClass().getSimpleName());
    }
  }

  private static Application currentApplication() {
    try {
      return (Application)Class.forName("android.app.ActivityThread")
          .getMethod("currentApplication").invoke(null);
    } catch(Throwable ignored) { return null; }
  }
}