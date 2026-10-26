package io.github.polestarcn.companion;

import io.github.polestarcn.companion.api.ProtoWire;
import java.util.Map;
import java.util.Locale;

/** Battery payload fields verified against the original reference APK. */
public final class ChargingSnapshot {
  public final Integer connection, type, powerStatus, minutesToFull;
  public final Double powerWatts, currentAmps;
  public ChargingSnapshot(Integer connection, Integer type, Integer powerStatus,
      Integer minutesToFull, Double powerWatts, Double currentAmps) {
    this.connection = connection; this.type = type; this.powerStatus = powerStatus;
    this.minutesToFull = minutesToFull != null && minutesToFull >= 0 ? minutesToFull : null;
    this.powerWatts = valid(powerWatts); this.currentAmps = valid(currentAmps);
  }
  private static Double valid(Double v) { return v != null && Double.isFinite(v) ? v : null; }
  public static ChargingSnapshot unknown() { return new ChargingSnapshot(null, null, null, null, null, null); }
  public static ChargingSnapshot fromData(Map<Integer,Object> battery) {
    return new ChargingSnapshot(ProtoWire.integer(battery, 6), ProtoWire.integer(battery, 17),
        ProtoWire.integer(battery, 26), ProtoWire.integer(battery, 5), ProtoWire.decimal(battery, 10),
        ProtoWire.decimal(battery, 11));
  }
  private static String enumLabel(Integer value, boolean en, String[] zh, String[] english) {
    return value == null || value < 0 || value >= zh.length ? en ? "Unknown" : "未知" : (en ? english : zh)[value];
  }
  public String connection(boolean en) { return enumLabel(connection, en,
      new String[]{"未知", "已连接", "未连接", "连接故障"},
      new String[]{"Unknown", "Connected", "Disconnected", "Connection fault"}); }
  public String type(boolean en) { return enumLabel(type, en,
      new String[]{"未知", "未充电", "交流", "直流", "无线"},
      new String[]{"Unknown", "Not charging", "AC", "DC", "Wireless"}); }
  public String powerStatus(boolean en) { return enumLabel(powerStatus, en,
      new String[]{"未知", "无电源", "初始化中", "可用", "正在供电", "故障"},
      new String[]{"Unknown", "No power", "Initializing", "Available", "Supplying power", "Fault"}); }
  public String power(boolean unit) { return MileageSnapshot.decimalText(powerWatts == null ? null : powerWatts / 1000.0, unit ? " kW" : ""); }
  public String current(boolean unit) { return MileageSnapshot.decimalText(currentAmps, unit ? " A" : ""); }
  public String timeToFull(boolean en) {
    if (minutesToFull == null) return "--";
    int hours = minutesToFull / 60, minutes = minutesToFull % 60;
    return hours == 0 ? minutes + (en ? " min" : " 分钟")
        : hours + (en ? " h" : " 小时") + (minutes == 0 ? "" : " " + minutes + (en ? " min" : " 分钟"));
  }
}
