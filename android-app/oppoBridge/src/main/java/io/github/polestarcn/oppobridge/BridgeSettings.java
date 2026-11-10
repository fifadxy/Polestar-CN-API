package io.github.polestarcn.oppobridge;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

/** Small non-secret settings contract shared with the LSPosed host processes. */
public final class BridgeSettings {
  static final String AUTHORITY = "io.github.polestarcn.oppobridge.settings";
  static final String INTEGRATED_AUTHORITY = "io.github.polestarcn.companion.bridge-settings";
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
  public static final String MODE="compatibilityMode", RPK_MODE="rpkCompatibilityMode";
  public static final String MODE_STABLE="stable", MODE_NEW="new", MODE_MOCK="mock";
  public static final String RPK_EXACT="exact", RPK_FEATURE="feature";
  private static final String[] BOOLEAN_KEYS = {
      MASTER, MY_DEVICES, ASSISTANT, WATCH, DIRECT, CARLINK_PROBE
  };

  private BridgeSettings() {}

  static Bundle localSnapshot(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    Bundle out = defaults();
    for (String key : BOOLEAN_KEYS) out.putBoolean(key, prefs.getBoolean(key, true));
    String selectedMode=validMode(prefs.getString(MODE,MODE_STABLE));
    out.putString(MODE,selectedMode);
    // New mode uses verified structural feature detection by default.
    // Stable keeps exact SHA matching; explicit user choices always prevail.
    String defaultRpk=MODE_NEW.equals(selectedMode)?RPK_FEATURE:RPK_EXACT;
    out.putString(RPK_MODE,validRpkMode(prefs.getString(RPK_MODE,defaultRpk)));
    out.putString("moduleVersion", BridgeBuild.VERSION_NAME);
    out.putInt("settingsVersion", 3);
    return out;
  }

  static Bundle snapshot(Context context) {
    if (BridgeIdentity.isBridgePackage(context.getPackageName()))
      return localSnapshot(context);
    try {
      Uri uri = settingsUri(context);
      Bundle value = context.getContentResolver().call(uri, METHOD_SNAPSHOT, null, null);
      return value == null ? defaults() : value;
    } catch (RuntimeException unavailable) {
      // Preserve beta28 behaviour when the standalone settings provider is not installed yet.
      return defaults();
    }
  }

  public static String validMode(String value){
    if(MODE_NEW.equals(value)||MODE_MOCK.equals(value))return value;
    return MODE_STABLE;
  }
  public static String validRpkMode(String value){
    return RPK_FEATURE.equals(value)?RPK_FEATURE:RPK_EXACT;
  }
  public static String mode(Context context){
    return validMode(snapshot(context).getString(MODE,MODE_STABLE));
  }
  static String modeForCurrentHost(){
    Application app=currentApplication();return app==null?MODE_STABLE:mode(app);
  }
  static String rpkModeForCurrentHost(){
    Application app=currentApplication();
    return app==null?RPK_EXACT:validRpkMode(snapshot(app).getString(RPK_MODE,RPK_EXACT));
  }
  static void updateLocalMode(Context context,String key,String value){
    if(!MODE.equals(key)&&!RPK_MODE.equals(key))
      throw new IllegalArgumentException("Unknown mode key");
    String mode=MODE.equals(key)?validMode(value):validRpkMode(value);
    context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(key,mode).apply();
    context.getContentResolver().notifyChange(settingsUri(context),null);
  }
  static void updateLocal(Context context, String key, boolean value) {
    if (!isBooleanKey(key)) throw new IllegalArgumentException("Unknown bridge setting");
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(key, value).apply();
    context.getContentResolver().notifyChange(settingsUri(context), null);
  }

  public static void setUnified(Context context,boolean enabled) {
    SharedPreferences.Editor edit=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit();
    for(String key:BOOLEAN_KEYS)edit.putBoolean(key,enabled);
    edit.apply();context.getContentResolver().notifyChange(settingsUri(context),null);
  }

  static void enableRecommendedDefaults(Context context) {
    SharedPreferences.Editor edit = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
    for (String key : BOOLEAN_KEYS) edit.putBoolean(key, true);
    edit.apply();
    context.getContentResolver().notifyChange(settingsUri(context), null);
  }

  static Uri settingsUri(Context context) {
    if (BridgeIdentity.INTEGRATED_PACKAGE.equals(context.getPackageName()))
      return Uri.parse("content://" + INTEGRATED_AUTHORITY);
    try {
      if (context.getPackageManager().resolveContentProvider(INTEGRATED_AUTHORITY, 0) != null)
        return Uri.parse("content://" + INTEGRATED_AUTHORITY);
    } catch (RuntimeException ignored) {}
    return URI;
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
        || "com.oplus.ocar".equals(packageName)
        || "com.oplus.sceneservice".equals(packageName)
        || "com.coloros.sceneservice".equals(packageName))
      return settings.getBoolean(ASSISTANT, true);
    if ("com.finshell.wallet".equals(packageName)
        || "com.heytap.health.wallet".equals(packageName)
        || "com.oplus.cardigitalkey".equals(packageName))
      return settings.getBoolean(WATCH, true);
    if (BridgeIdentity.isCarLinkHost(packageName))
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
        + "\ncarLinkProbe=" + value.getBoolean(CARLINK_PROBE, true)
        + "\nmode=" + value.getString(MODE,MODE_STABLE)
        + "\nrpk=" + value.getString(RPK_MODE,RPK_EXACT)+'\n';
  }

  private static Bundle defaults() {
    Bundle out = new Bundle();
    for (String key : BOOLEAN_KEYS) out.putBoolean(key, true);
    out.putString(MODE,MODE_STABLE);
    out.putString(RPK_MODE,RPK_EXACT);
    out.putString("moduleVersion", BridgeBuild.VERSION_NAME);
    out.putInt("settingsVersion", 3);
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
