package io.github.polestarcn.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

/** Last read-only AssistantScreen -> Pantanal Shelf service observation. */
final class OppoShelfDiagnostics {
  private static final String PREF = "oppo-shelf-diagnostics";

  private OppoShelfDiagnostics() {}

  static Bundle record(Context context, Bundle in) {
    if (in == null) throw new IllegalArgumentException("Missing Shelf report");
    SharedPreferences.Editor e = context.getSharedPreferences(PREF, 0).edit();
    e.putLong("seenAt", System.currentTimeMillis());
    e.putBoolean("ready", in.getBoolean("ready", false));
    e.putInt("rowCount", Math.max(0, in.getInt("rowCount", 0)));
    e.putString("reason", bounded(in.getString("reason", ""), 160));
    e.putString("hostVersion", bounded(in.getString("hostVersion", ""), 80));
    e.putString("moduleVersion", bounded(in.getString("moduleVersion", ""), 40));
    e.putString("columns", bounded(in.getString("columns", ""), 1200));
    e.putString("candidates", bounded(in.getString("candidates", ""), 2400));
    e.apply();
    return status(context);
  }

  static Bundle status(Context context) {
    SharedPreferences p = context.getSharedPreferences(PREF, 0);
    Bundle out = new Bundle();
    out.putLong("seenAt", p.getLong("seenAt", 0L));
    out.putBoolean("ready", p.getBoolean("ready", false));
    out.putInt("rowCount", p.getInt("rowCount", 0));
    for (String key : new String[]{"reason", "hostVersion", "moduleVersion", "columns", "candidates"})
      out.putString(key, p.getString(key, ""));
    return out;
  }

  private static String bounded(String value, int limit) {
    if (value == null) return "";
    return value.substring(0, Math.min(value.length(), limit));
  }
}
