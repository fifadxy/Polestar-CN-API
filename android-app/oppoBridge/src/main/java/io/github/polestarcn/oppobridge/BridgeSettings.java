package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

/** Small non-secret settings contract shared with the LSPosed host processes. */
final class BridgeSettings {
  static final String AUTHORITY = "io.github.polestarcn.oppobridge.settings";
  static final Uri URI = Uri.parse("content://" + AUTHORITY);
  static final String METHOD_SNAPSHOT = "snapshot";
  static final String METHOD_UPDATE = "update";
  static final String PREFS = "bridge-settings-v2";
  static final String MASTER = "masterEnabled";
  static final String MY_DEVICES = "myDevicesEnabled";
  static final String ASSISTANT = "assistantScreenEnabled";
  static final String WATCH = "watchEnabled";
  static final String DIRECT = "directControlEnabled";
  static final String CARLINK_PROBE = "carLinkProbeEnabled";
  private static final String[] BOOLEAN_KEYS = {
      MASTER, MY_DEVICES, ASSISTANT, WATCH, DIRECT, CARLINK_PROBE
  };

  private BridgeSettings() {}

  static Bundle localSnapshot(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    Bundle out = defaults();
    for (String key : BOOLEAN_KEYS) out.putBoolean(key, prefs.getBoolean(key, true));
    out.putString("moduleVersion", BridgeBuild.VERSION_NAME);
    out.putInt("settingsVersion", 2);
    return out;
  }

  static Bundle snapshot(Context context) {
    if (BridgeIdentity.STANDALONE_PACKAGE.equals(context.getPackageName()))
      return localSnapshot(context);
    try {
      Bundle value = context.getContentResolver().call(URI, METHOD_SNAPSHOT, null, null);
      return value == null ? defaults() : value;
    } catch (RuntimeException unavailable) {
      // Preserve beta28 behaviour when the standalone settings provider is not installed yet.
      return defaults();
    }
  }

  static void updateLocal(Context context, String key, boolean value) {
    if (!isBooleanKey(key)) throw new IllegalArgumentException("Unknown bridge setting");
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(key, value).apply();
    context.getContentResolver().notifyChange(URI, null);
  }

  static void enableRecommendedDefaults(Context context) {
    SharedPreferences.Editor edit = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
    for (String key : BOOLEAN_KEYS) edit.putBoolean(key, true);
    edit.apply();
    context.getContentResolver().notifyChange(URI, null);
  }

  static boolean enabledForCurrentHost(String packageName) {
    Application app = currentApplication();
    if (app == null) return true;
    Bundle settings = snapshot(app);
    if (!settings.getBoolean(MASTER, true)) return false;
    if ("com.heytap.mydevices".equals(packageName)
        || "com.oplus.smartengine".equals(packageName)
        || "com.oplus.cardservice".equals(packageName)
        || "com.nearme.instant.platform".equals(packageName))
      return settings.getBoolean(MY_DEVICES, true);
    if ("com.coloros.assistantscreen".equals(packageName)
        || "com.oplus.assistantscreen".equals(packageName)
        || "com.oplus.pantanal.ums".equals(packageName)
        || "com.heytap.speechassist".equals(packageName)
        || "com.oplus.ocar".equals(packageName))
      return settings.getBoolean(ASSISTANT, true);
    if ("com.finshell.wallet".equals(packageName)
        || "com.heytap.health.wallet".equals(packageName)
        || "com.oplus.cardigitalkey".equals(packageName))
      return settings.getBoolean(WATCH, true);
    if ("com.heytap.opluscarlink".equals(packageName))
      return settings.getBoolean(CARLINK_PROBE, true);
    return true;
  }

  static boolean directControlForCurrentHost() {
    Application app = currentApplication();
    return app == null || snapshot(app).getBoolean(DIRECT, true);
  }

  static String describe(Context context) {
    Bundle value = snapshot(context);
    return "module=" + BridgeBuild.VERSION_NAME
        + "\nmaster=" + value.getBoolean(MASTER, true)
        + "\nmyDevices=" + value.getBoolean(MY_DEVICES, true)
        + "\nassistantScreen=" + value.getBoolean(ASSISTANT, true)
        + "\nwatch=" + value.getBoolean(WATCH, true)
        + "\ndirectControl=" + value.getBoolean(DIRECT, true)
        + "\ncarLinkProbe=" + value.getBoolean(CARLINK_PROBE, true) + '\n';
  }

  private static Bundle defaults() {
    Bundle out = new Bundle();
    for (String key : BOOLEAN_KEYS) out.putBoolean(key, true);
    out.putString("moduleVersion", BridgeBuild.VERSION_NAME);
    out.putInt("settingsVersion", 2);
    return out;
  }

  private static boolean isBooleanKey(String key) {
    for (String candidate : BOOLEAN_KEYS) if (candidate.equals(key)) return true;
    return false;
  }

  private static Application currentApplication() {
    try {
      return (Application) Class.forName("android.app.ActivityThread")
          .getMethod("currentApplication").invoke(null);
    } catch (Throwable ignored) {
      return null;
    }
  }
}
