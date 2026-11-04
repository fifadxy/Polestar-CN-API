package io.github.polestarcn.companion;

import io.github.polestarcn.companion.api.ProtoWire;
import java.util.Map;

/** Reference APK climate/air mapping, with independent source timestamps. */
public final class EnvironmentSnapshot {
  public final String climate, air;
  public final Double temperatureC, targetTemperatureC, pm25UgM3;
  public final Integer aqi, climateRuntimeMinutes, airRuntimeMinutes;
  public final long climateReportedAt, airReportedAt;
  public EnvironmentSnapshot(String climate, String air, Double temperatureC, Double targetTemperatureC,
      Double pm25UgM3, Integer aqi, Integer climateRuntimeMinutes, Integer airRuntimeMinutes,
      long climateReportedAt, long airReportedAt) {
    this.climate = climate; this.air = air;
    this.temperatureC = temperature(temperatureC); this.targetTemperatureC = temperature(targetTemperatureC);
    this.pm25UgM3 = pm25UgM3 != null && Double.isFinite(pm25UgM3) && pm25UgM3 >= 0 ? pm25UgM3 : null;
    this.aqi = nonnegative(aqi); this.climateRuntimeMinutes = nonnegative(climateRuntimeMinutes);
    this.airRuntimeMinutes = nonnegative(airRuntimeMinutes);
    this.climateReportedAt = Math.max(0, climateReportedAt); this.airReportedAt = Math.max(0, airReportedAt);
  }
  private static Integer nonnegative(Integer v) { return v != null && v >= 0 ? v : null; }
  private static Double temperature(Double v) { return v != null && Double.isFinite(v) && v >= -100 && v <= 100 ? v : null; }
  public static EnvironmentSnapshot unknown() {
    return new EnvironmentSnapshot("未知", "未知", null, null, null, null, null, null, 0, 0);
  }
  public static EnvironmentSnapshot fromData(Map<Integer,Object> climate, Map<Integer,Object> air) {
    return new EnvironmentSnapshot(
        ProtoWire.enumText(climate, 2, "未知", "开启", "关闭", "处理中"),
        ProtoWire.enumText(air, 6, "未知", "开启", "关闭", "处理中"),
        ProtoWire.decimal(climate, 7), ProtoWire.decimal(climate, 8), ProtoWire.decimal(air, 10),
        ProtoWire.integer(air, 9), ProtoWire.integer(climate, 3), ProtoWire.integer(air, 11),
        ProtoWire.timestampMillis(climate, 1), ProtoWire.timestampMillis(air, 1));
  }
  public static String temperatureText(Double value, boolean unit) {
    return value == null ? "--" : String.format(java.util.Locale.getDefault(), "%.1f", value) + (unit ? " °C" : "");
  }
}
