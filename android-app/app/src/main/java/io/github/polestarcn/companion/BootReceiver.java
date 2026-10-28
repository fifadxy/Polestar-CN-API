package io.github.polestarcn.companion;

import android.content.*;

public final class BootReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent i) {
    if (Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())
        || Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction())) {
      AppIcons.sync(c);
      try { CampModeManager.schedule(c); }
      catch (RuntimeException e) { DiagnosticLog.add("Camping restart not scheduled"); }
      ResidentService.scheduleUpdates(c);
      if (AppPreferences.enabled(c))
        try {
          ResidentService.apply(c);
        } catch (RuntimeException e) {
          DiagnosticLog.add("Background restart restricted by Android");
        }
    }
  }
}
