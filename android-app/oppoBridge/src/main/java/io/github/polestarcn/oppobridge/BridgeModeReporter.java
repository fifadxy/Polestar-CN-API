package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.os.Bundle;

/** Passive, per-process confirmation of actual LSPosed mode at hook installation. */
final class BridgeModeReporter {
  private BridgeModeReporter(){}

  static void report(OppoBridgeModule module,String pkg,String process,String mode){
    report(module,pkg,process,mode,0);
  }
  private static void report(OppoBridgeModule module,String pkg,String process,String mode,int attempt){
    try {
      Application app=(Application)Class.forName("android.app.ActivityThread")
          .getMethod("currentApplication").invoke(null);
      if(app==null){
        if(attempt<3)new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            ()->report(module,pkg,process,mode,attempt+1),1000L);
        return;
      }
      Bundle b=new Bundle();b.putString("packageName",pkg);b.putString("process",process);
      b.putString("mode",BridgeSettings.validMode(mode));
      b.putString("moduleVersion",BridgeBuild.VERSION_NAME);
      b.putLong("seenAt",System.currentTimeMillis());
      BridgeClient.call(app,"bridge-mode-report",b);
      module.info("bridge_mode_active package="+pkg+" process="+process+" mode="+mode);
    }catch(Throwable ignored){ /* Never interfere with a ColorOS host's startup. */ }
  }
}
