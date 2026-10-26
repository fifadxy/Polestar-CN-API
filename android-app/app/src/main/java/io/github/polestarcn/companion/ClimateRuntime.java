package io.github.polestarcn.companion;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

/**
 * Climate runtime presentation.
 *
 * Reference APK DEX verification:
 * ParkingClimatization field 2 = state, field 3 = remaining runtime minutes,
 * field 7 = cabin temperature, field 8 = target temperature.
 * Vehicle-reported remaining minutes always take precedence. The local 30-minute timer is only a
 * fallback after this app has confirmed a climate-start command.
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

  public static Integer vehicleRemainingMinutes(VehicleSnapshot s) {
    if (s == null || !"开启".equals(s.climate)) return null;
    Integer minutes = s.environment == null ? null : s.environment.climateRuntimeMinutes;
    return minutes != null && minutes > 0 ? minutes : null;
  }

  public static long estimatedRemaining(Context c, String vehicleHash, long now) {
    android.content.SharedPreferences p = c.getSharedPreferences(KEY, 0);
    if (!vehicleHash.equals(p.getString("vehicle", ""))) return -1L;
    long start = p.getLong("started", 0);
    if (start <= 0 || now < start) return -1L;
    long remaining = ESTIMATED_DURATION_MS - (now - start);
    return remaining > 0 ? remaining : -1L;
  }

  private static Integer fallbackMinutes(Context c, String vehicleHash) {
    long remaining = estimatedRemaining(
        c, vehicleHash == null ? "" : vehicleHash, System.currentTimeMillis());
    return remaining < 0 ? null : (int) ((remaining + 59999L) / 60000L);
  }

  public static String label(Context c, VehicleSnapshot s, String vehicleHash) {
    boolean en = AppPreferences.english(c);
    if (s == null) return en ? "Unknown" : "未知";
    if ("处理中".equals(s.climate))
      return en ? "Running · verifying" : "运行中 · 待确认";
    if (!"开启".equals(s.climate)) return AppPreferences.status(c, s.climate);

    Integer vehicle = vehicleRemainingMinutes(s);
    if (vehicle != null)
      return en ? "Running · " + formatTime(vehicle, true) + " left"
          : "运行中 · 剩余" + formatTime(vehicle, false);

    Integer estimate = fallbackMinutes(c, vehicleHash);
    if (estimate != null)
      return en ? "Running · ~" + formatTime(estimate, true) + " left"
          : "运行中 · 估算剩余" + formatTime(estimate, false);

    return en ? "Running · remaining time unavailable" : "运行中 · 剩余时间暂无";
  }

  public static String detail(Context c, VehicleSnapshot s, String vehicleHash) {
    if (s == null) return "--";
    boolean en = AppPreferences.english(c);
    List<String> parts = new ArrayList<>();
    if (s.environment.temperatureC != null)
      parts.add((en ? "Cabin " : "车内 ")
          + EnvironmentSnapshot.temperatureText(s.environment.temperatureC, true));
    if (s.environment.targetTemperatureC != null)
      parts.add((en ? "Target " : "设定 ")
          + EnvironmentSnapshot.temperatureText(s.environment.targetTemperatureC, true));
    if ("开启".equals(s.climate)) {
      Integer vehicle = vehicleRemainingMinutes(s);
      if (vehicle != null) parts.add((en ? "Left " : "剩余 ") + formatTime(vehicle, en));
      else {
        Integer estimate = fallbackMinutes(c, vehicleHash);
        parts.add(estimate == null
            ? (en ? "Time unavailable" : "剩余时间暂无")
            : (en ? "~" : "估算 ") + formatTime(estimate, en));
      }
    }
    if (parts.isEmpty()) return en ? "Temperature unavailable" : "温度暂无";
    return android.text.TextUtils.join(" · ", parts).toString();
  }

  private static String formatTime(int minutes, boolean english) {
    int safe = Math.max(0, minutes);
    if (safe < 60) return safe + (english ? " min" : "分钟");
    int hours = safe / 60, remain = safe % 60;
    if (remain == 0) return hours + (english ? " h" : "小时");
    return english ? hours + " h " + remain + " min" : hours + "小时" + remain + "分钟";
  }

  public static String widgetLabel(Context c, VehicleSnapshot s, String vehicleHash) {
    if (s == null) return AppPreferences.widgetTr(c, "未知", "Unknown");
    if ("处理中".equals(s.climate))
      return AppPreferences.widgetTr(c, "运行中 · 待确认", "Running · verifying");
    if (!"开启".equals(s.climate)) return AppPreferences.widgetStatus(c, s.climate);

    Integer vehicle = vehicleRemainingMinutes(s);
    if (vehicle != null)
      return AppPreferences.widgetTr(c, "剩余" + formatTime(vehicle, false),
          formatTime(vehicle, true) + " left");

    Integer estimate = fallbackMinutes(c, vehicleHash);
    if (estimate != null)
      return AppPreferences.widgetTr(c, "估算" + formatTime(estimate, false),
          "~" + formatTime(estimate, true));

    return AppPreferences.widgetTr(c, "运行中", "Running");
  }
}
