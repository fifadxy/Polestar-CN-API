package io.github.polestarcn.companion;

import android.content.Context;

/** UI-only visibility rules for aggregated vehicle health. */
public final class VehicleHealthUi {
  private VehicleHealthUi() {}

  public static boolean includeTire(Context c) {
    return AppPreferences.visible(c, "experimental_tire", false);
  }

  public static boolean hasProblem(Context c, VehicleHealthSnapshot h) {
    if (h == null) return false;
    return h.lowVoltageBattery == VehicleHealthSnapshot.WARNING
        || h.washerFluid == VehicleHealthSnapshot.WARNING
        || (includeTire(c) && h.tirePressure == VehicleHealthSnapshot.WARNING);
  }

  public static boolean allGood(Context c, VehicleHealthSnapshot h) {
    if (h == null) return false;
    if (h.lowVoltageBattery != VehicleHealthSnapshot.OK
        || h.washerFluid != VehicleHealthSnapshot.OK) return false;
    return !includeTire(c) || h.tirePressure == VehicleHealthSnapshot.OK;
  }

  public static boolean hasAnyReport(Context c, VehicleHealthSnapshot h) {
    if (h == null) return false;
    return h.lowVoltageBattery != VehicleHealthSnapshot.UNKNOWN
        || h.washerFluid != VehicleHealthSnapshot.UNKNOWN
        || (includeTire(c) && h.tirePressure != VehicleHealthSnapshot.UNKNOWN);
  }

  public static int issueCount(Context c, VehicleHealthSnapshot h) {
    if (h == null) return 0;
    int n = 0;
    if (h.lowVoltageBattery == VehicleHealthSnapshot.WARNING) n++;
    if (h.washerFluid == VehicleHealthSnapshot.WARNING) n++;
    if (includeTire(c) && h.tirePressure == VehicleHealthSnapshot.WARNING) n++;
    return n;
  }

  public static String summary(Context c, VehicleHealthSnapshot h, boolean english) {
    if (hasProblem(c, h)) {
      int count = issueCount(c, h);
      return english ? "Needs attention · " + count : "需检查 · " + count + "项";
    }
    if (allGood(c, h)) return "ALL GOOD";
    return hasAnyReport(c, h)
        ? (english ? "Partially reported" : "部分已上报")
        : (english ? "Unavailable" : "未上报");
  }
}
