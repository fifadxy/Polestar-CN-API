package io.github.polestarcn.companion;

import android.content.Context;

/**
 * A local estimate, not a vehicle-reported timer. The China C3 readback currently provides
 * a climate state but no authenticated remaining-duration field.
 */
public final class ClimateRuntime {
  private static final String KEY = "climate_runtime_v1";
  private static final long ESTIMATED_DURATION_MS = 30L * 60L * 1000L;
  private ClimateRuntime() {}
  public static void started(Context c, String vehicleHash, long now) {
    c.getSharedPreferences(KEY, 0).edit().putString("vehicle", vehicleHash)
        .putLong("started", now).apply();
  }
  public static void stopped(Context c) {
    c.getSharedPreferences(KEY, 0).edit().clear().apply();
  }
  public static long estimatedRemaining(Context c, String vehicleHash, long now) {
    android.content.SharedPreferences p = c.getSharedPreferences(KEY, 0);
    if (!vehicleHash.equals(p.getString("vehicle", ""))) return -1L;
    long start = p.getLong("started", 0);
    if (start <= 0 || now < start) return -1L;
    long remaining = ESTIMATED_DURATION_MS - (now - start);
    return remaining > 0 ? remaining : -1L;
  }
  public static String label(Context c, VehicleSnapshot s, String vehicleHash) {
    boolean en = AppPreferences.english(c);
    if (s == null) return en ? "Unknown" : "未知";
    if ("处理中".equals(s.climate)) return en ? "Running · verifying" : "运行中 · 待确认";
    if (!"开启".equals(s.climate)) return AppPreferences.status(c, s.climate);
    long remain = estimatedRemaining(c, vehicleHash == null ? "" : vehicleHash,
        System.currentTimeMillis());
    if (remain < 0) return en ? "Running · time unknown" : "运行中 · 剩余时间未知";
    long minutes = (remain + 59999L) / 60000L;
    return en ? "Running · ~" + minutes + " min left" : "运行中 · 预计剩余" + minutes + "分钟";
  }
  public static String widgetLabel(Context c, VehicleSnapshot s, String vehicleHash) {
    String state = label(c, s, vehicleHash);
    if (AppPreferences.widgetEnglish(c) == AppPreferences.english(c)) return state;
    if (s == null) return AppPreferences.widgetTr(c, "未知", "Unknown");
    if ("处理中".equals(s.climate)) return AppPreferences.widgetTr(c, "运行中 · 待确认", "Running · verifying");
    if (!"开启".equals(s.climate)) return AppPreferences.widgetStatus(c, s.climate);
    long remain = estimatedRemaining(c, vehicleHash == null ? "" : vehicleHash,
        System.currentTimeMillis());
    if (remain < 0) return AppPreferences.widgetTr(c, "运行中", "Running");
    long minutes = (remain + 59999L) / 60000L;
    return AppPreferences.widgetTr(c, "预计" + minutes + "分钟", "~" + minutes + " min");
  }
}

