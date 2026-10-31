package io.github.polestarcn.oppobridge;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
/** Diagnostic observation only. No account, token or VIN crosses this channel. */
final class HostReporter {
  static void report(String pkg,String process,String phase,boolean ready,String reason) {
    if(!"com.heytap.mydevices".equals(pkg)||!(pkg.equals(process)||(pkg+":cards").equals(process)))return;
    attempt(process,phase,ready,reason,0);
  }
  private static void attempt(String process,String phase,boolean ready,String reason,int attempts) {
    try {
      Application app=(Application)Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
      if(app==null) {
        if(attempts<3)new Handler(Looper.getMainLooper()).postDelayed(()->attempt(process,phase,ready,reason,attempts+1),1000);
        return;
      }
      Bundle in=new Bundle();in.putString("process",process);in.putString("phase",phase);
      in.putBoolean("ready",ready);in.putString("reason",reason);in.putString("moduleVersion","0.7.0");
      in.putString("hostVersion",app.getPackageManager().getPackageInfo("com.heytap.mydevices",0).versionName);
      // No legacy runtime fallback: reporting must never block OEM startup on an old client.
      app.getContentResolver().call(BridgeClient.URI,"host-report",null,in);
    } catch(Throwable ignored) { /* Diagnostics cannot interfere with host startup. */ }
  }
}
