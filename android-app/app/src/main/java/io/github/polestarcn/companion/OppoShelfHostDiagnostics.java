package io.github.polestarcn.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

/** Sanitized host observations for AssistantScreen/Pantanal Shelf card surfaces. */
final class OppoShelfHostDiagnostics {
  private static final String PREF = "oppo-shelf-host-diagnostics";
  private OppoShelfHostDiagnostics() {}

  static Bundle record(Context context, Bundle in) {
    if (in == null) throw new IllegalArgumentException("Missing Shelf host report");
    String pkg = in.getString("packageName", "");
    String prefix;
    if ("com.oplus.pantanal.ums".equals(pkg)) prefix = "pantanal";
    else if ("com.coloros.assistantscreen".equals(pkg)) prefix = "assistant";
    else if ("com.oplus.assistantscreen".equals(pkg)) prefix = "altassistant";
    else if ("com.oplus.smartengine".equals(pkg)) prefix = "smartengine";
    else if ("com.oplus.cardservice".equals(pkg)) prefix = "cardservice";
    else throw new IllegalArgumentException("Unsupported Shelf host");
    SharedPreferences.Editor e = context.getSharedPreferences(PREF, 0).edit();
    e.putLong(prefix + "_seenAt", System.currentTimeMillis());
    e.putBoolean(prefix + "_ready", in.getBoolean("ready", false));
    e.putLong(prefix + "_versionCode", Math.max(-1L, in.getLong("versionCode", -1L)));
    for (String key : new String[]{"versionName", "process", "moduleVersion", "components", "reason"})
      e.putString(prefix + "_" + key, bounded(in.getString(key, ""), key.equals("components") ? 3600 : 300));
    e.apply();
    return status(context);
  }

  static Bundle status(Context context) {
    SharedPreferences p = context.getSharedPreferences(PREF, 0);
    Bundle out = new Bundle();
    for (String prefix : new String[]{"pantanal", "assistant", "altassistant", "smartengine", "cardservice"}) {
      out.putLong(prefix + "_seenAt", p.getLong(prefix + "_seenAt", 0L));
      out.putBoolean(prefix + "_ready", p.getBoolean(prefix + "_ready", false));
      out.putLong(prefix + "_versionCode", p.getLong(prefix + "_versionCode", -1L));
      for (String key : new String[]{"versionName", "process", "moduleVersion", "components", "reason"})
        out.putString(prefix + "_" + key, p.getString(prefix + "_" + key, ""));
    }
    return out;
  }

  private static String bounded(String value, int limit) {
    if (value == null) return "";
    String flat = value.replace('\n', ' ').replace('\r', ' ').trim();
    return flat.substring(0, Math.min(flat.length(), limit));
  }
}