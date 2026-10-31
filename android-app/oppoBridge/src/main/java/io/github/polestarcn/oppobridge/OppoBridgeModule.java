package io.github.polestarcn.oppobridge;

import android.util.Log;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class OppoBridgeModule extends XposedModule {
  private static final String TAG = "PsOppoBridge";
  private volatile String processName = "unknown";

  @Override public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
    processName = param.getProcessName();
    log(Log.INFO, TAG,
        "module_loaded process=" + processName
            + " systemServer=" + param.isSystemServer()
            + " api=" + getApiVersion()
            + " framework=" + getFrameworkName() + "/" + getFrameworkVersion());
  }

  @Override public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
    String pkg = param.getPackageName();
    String process = processName;
    if (process == null || !(process.equals(pkg) || process.startsWith(pkg + ":"))) return;
    try {
      if (RuntimeBridgeClient.PSASSISTANT.equals(pkg)) {
        if (RuntimeBridgeClient.PSASSISTANT.equals(process)) {
          PsAssistantRuntimeHook.install(this, param.getClassLoader());
        }
        return;
      }
      HostProbe.install(this, pkg, param.getClassLoader());
      if ("com.heytap.mydevices".equals(pkg)) {
        if ("com.heytap.mydevices".equals(process)) {
          MyDevicesVehicleHook.installMain(this, param.getClassLoader());
          try { NativeHomeCardHook.install(this, param.getClassLoader()); }
          catch (Throwable unavailable) { warn("native_home_card unavailable="+unavailable.getClass().getSimpleName()); }
          HostReporter.report(pkg,process,"hooks-installed",true,"");
        } else if ("com.heytap.mydevices:cards".equals(process)) {
          MyDevicesVehicleHook.installCardProcess(this, param.getClassLoader());
          HostReporter.report(pkg,process,"hooks-installed",true,"");
        }
      }
    } catch (Throwable t) {
      HostReporter.report(pkg,process,"install-failed",false,t.getClass().getSimpleName());
      log(Log.ERROR, TAG, "bridge_install_failed package=" + pkg + " process=" + process, t);
    }
  }

  void info(String message) { log(Log.INFO, TAG, message); }
  void warn(String message) { log(Log.WARN, TAG, message); }
}
