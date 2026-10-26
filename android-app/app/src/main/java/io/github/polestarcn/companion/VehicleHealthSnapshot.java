package io.github.polestarcn.companion;

import io.github.polestarcn.companion.api.ProtoWire;
import java.util.Map;

/**
 * Warning-only C3 Health snapshot.
 *
 * Global Hisingen source mapping:
 * field 13 = washer-fluid warning, field 38 = low-voltage/12 V warning,
 * fields 9-12 = tyre/iTPMS warnings. These mappings are source-verified globally; China
 * execution remains vehicle-verification dependent.
 */
public final class VehicleHealthSnapshot {
  public static final int UNKNOWN = -1;
  public static final int OK = 1;
  public static final int WARNING = 2;

  public final int lowVoltageBattery;
  public final int washerFluid;
  public final int tirePressure;
  public final long reportedAt;

  public VehicleHealthSnapshot(
      int lowVoltageBattery, int washerFluid, int tirePressure, long reportedAt) {
    this.lowVoltageBattery = normalize(lowVoltageBattery);
    this.washerFluid = normalize(washerFluid);
    this.tirePressure = normalize(tirePressure);
    this.reportedAt = Math.max(0L, reportedAt);
  }

  public static VehicleHealthSnapshot unknown() {
    return new VehicleHealthSnapshot(UNKNOWN, UNKNOWN, UNKNOWN, 0L);
  }

  public static VehicleHealthSnapshot fromData(
      Map<Integer, Object> health, TireSnapshot tires) {
    Integer washerRaw = ProtoWire.integer(health, 13);
    Integer lowVoltageRaw = ProtoWire.integer(health, 38);
    int washer = washerRaw == null ? UNKNOWN : washerRaw > 1 ? WARNING : OK;
    int lowVoltage =
        lowVoltageRaw == null ? UNKNOWN : lowVoltageRaw == 2 ? WARNING : OK;
    int tyre =
        tires == null || !tires.hasAnyReport()
            ? UNKNOWN
            : tires.hasProblem() ? WARNING : tires.allGood() ? OK : UNKNOWN;
    return new VehicleHealthSnapshot(
        lowVoltage, washer, tyre, ProtoWire.timestampMillis(health, 1));
  }

  private static int normalize(int value) {
    return value == OK || value == WARNING ? value : UNKNOWN;
  }

  public boolean hasAnyReport() {
    return lowVoltageBattery != UNKNOWN || washerFluid != UNKNOWN || tirePressure != UNKNOWN;
  }

  public boolean hasProblem() {
    return lowVoltageBattery == WARNING || washerFluid == WARNING || tirePressure == WARNING;
  }

  public boolean allGood() {
    return lowVoltageBattery == OK && washerFluid == OK && tirePressure == OK;
  }

  public int issueCount() {
    int n = 0;
    if (lowVoltageBattery == WARNING) n++;
    if (washerFluid == WARNING) n++;
    if (tirePressure == WARNING) n++;
    return n;
  }

  public String summary(boolean english) {
    if (hasProblem()) {
      int count = issueCount();
      return english ? "Needs attention · " + count : "需检查 · " + count + "项";
    }
    if (allGood()) return english ? "ALL GOOD" : "正常";
    return hasAnyReport() ? (english ? "Partially reported" : "部分已上报")
        : (english ? "Unavailable" : "未上报");
  }

  public static String label(int state, boolean english) {
    if (state == OK) return english ? "Normal" : "正常";
    if (state == WARNING) return english ? "Check" : "需检查";
    return english ? "Unavailable" : "未上报";
  }
}
