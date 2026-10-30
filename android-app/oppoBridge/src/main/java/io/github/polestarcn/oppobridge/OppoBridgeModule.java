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
      HostProbe.install(this, pkg, param.getClassLoader());
    } catch (Throwable t) {
      log(Log.ERROR, TAG, "probe_install_failed package=" + pkg, t);
    }
  }

  void info(String message) { log(Log.INFO, TAG, message); }
  void warn(String message) { log(Log.WARN, TAG, message); }
}
