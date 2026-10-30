package io.github.polestarcn.companion;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;

/** User opt-in shared by the integrated Provider and external bridge clients. */
public final class OppoDeviceIntegration {
  static final String PREFS = "oppo-device-integration";
  static final String KEY = "enabled";
  public static final String PROVIDER_CLASS = "io.github.polestarcn.oppobridge.VehicleDeviceCardProvider";
  public static final Uri CARD_URI = Uri.parse("content://io.github.polestarcn.companion.devices/device_info_table_new");
  private OppoDeviceIntegration() {}
  public static boolean enabled(Context context) {
    return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false);
  }
  public static void setEnabled(Context context, boolean enabled) {
    SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    boolean previous = enabled(context);
    if (!preferences.edit().putBoolean(KEY, enabled).commit())
      throw new IllegalStateException("Cannot save OPPO integration preference");
    try {
      // Notify bound hosts before disabling; bound providers also check the gate.
      notifyHosts(context);
      context.getPackageManager().setComponentEnabledSetting(
          new ComponentName(context.getPackageName(), PROVIDER_CLASS),
          enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
          PackageManager.DONT_KILL_APP);
      if (enabled) context.getSharedPreferences("vehicle-card", Context.MODE_PRIVATE).edit().remove("hidden").apply();
      notifyHosts(context);
    } catch (RuntimeException failure) {
      preferences.edit().putBoolean(KEY, previous).commit();
      throw failure;
    }
  }
  private static void notifyHosts(Context context) {
    try { context.getContentResolver().notifyChange(CARD_URI, null); } catch (RuntimeException ignored) {}
    try { context.getContentResolver().notifyChange(PsAssistantBridgeProvider.URI, null); } catch (RuntimeException ignored) {}
  }
}
