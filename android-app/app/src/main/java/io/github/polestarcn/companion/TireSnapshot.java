package io.github.polestarcn.companion;

import io.github.polestarcn.companion.api.ProtoWire;
import java.util.Map;

/** China C3 HealthService tyre state. Order: front-left, front-right, rear-left, rear-right. */
public final class TireSnapshot {
  public static final int UNKNOWN = -1, OK = 1, VERY_LOW = 2, LOW = 3, HIGH = 4;
  private final Double[] kpa;
  private final int[] warnings;
  public final long reportedAt;

  public TireSnapshot(Double[] kpa, int[] warnings, long reportedAt) {
    this.kpa = new Double[4];
    this.warnings = new int[] {UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN};
    if (kpa != null) System.arraycopy(kpa, 0, this.kpa, 0, Math.min(4, kpa.length));
    if (warnings != null) System.arraycopy(warnings, 0, this.warnings, 0, Math.min(4, warnings.length));
    this.reportedAt = reportedAt;
  }

  public static TireSnapshot unknown() { return new TireSnapshot(null, null, 0); }

  public static TireSnapshot fromData(Map<Integer, Object> data) {
    int[] warningFields = {9, 10, 11, 12};
    int[] pressureFields = {39, 40, 41, 42};
    Double[] pressure = new Double[4];
    int[] warning = new int[4];
    for (int i = 0; i < 4; i++) {
      Integer w = ProtoWire.integer(data, warningFields[i]);
      warning[i] = w == null ? UNKNOWN : w;
      Double p = ProtoWire.decimal(data, pressureFields[i]);
      pressure[i] = p != null && p > 0 && Double.isFinite(p) ? p : null;
    }
    return new TireSnapshot(pressure, warning, ProtoWire.timestampMillis(data, 1));
  }

  public Double pressureKpa(int position) {
    return position < 0 || position >= 4 ? null : kpa[position];
  }

  public int warning(int position) {
    return position < 0 || position >= 4 ? UNKNOWN : warnings[position];
  }

  public boolean hasAnyReport() {
    for (int i = 0; i < 4; i++) if (kpa[i] != null || warnings[i] != UNKNOWN) return true;
    return false;
  }

  public boolean hasProblem() {
    for (int value : warnings) if (value == VERY_LOW || value == LOW || value == HIGH) return true;
    return false;
  }

  public boolean allGood() {
    if (!hasAnyReport() || hasProblem()) return false;
    boolean explicit = false;
    for (int value : warnings) if (value == OK) explicit = true;
    if (explicit) return true;
    for (Double value : kpa) if (value != null) return true;
    return false;
  }

  public String summary(boolean english) {
    if (hasProblem()) return english ? "CHECK !" : "检查 !";
    if (allGood()) return "ALL GOOD";
    return english ? "UNKNOWN" : "未知";
  }

  public String label(int position, boolean english) {
    int w = warning(position);
    String status;
    switch (w) {
      case OK: status = english ? "OK" : "正常"; break;
      case VERY_LOW: status = english ? "Very low" : "严重偏低"; break;
      case LOW: status = english ? "Low" : "偏低"; break;
      case HIGH: status = english ? "High" : "偏高"; break;
      default: status = english ? "Unknown" : "未知";
    }
    Double p = pressureKpa(position);
    if (p == null) return status;
    return Math.round(p) + " kPa · " + status;
  }

  public Double[] pressures() { return kpa.clone(); }
  public int[] warnings() { return warnings.clone(); }
}
