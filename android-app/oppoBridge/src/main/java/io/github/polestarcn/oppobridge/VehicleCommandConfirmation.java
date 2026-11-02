package io.github.polestarcn.oppobridge;

import android.os.Bundle;
import java.util.Locale;

/** Maps bridge commands to observable vehicle cache states for truthful confirmation. */
final class VehicleCommandConfirmation {
  private VehicleCommandConfirmation() {}

  static boolean supports(String action) {
    return "lock".equals(action) || "unlock".equals(action)
        || "climate-start".equals(action) || "climate-stop".equals(action)
        || "air-start".equals(action) || "air-stop".equals(action)
        || "trunk-open".equals(action) || "trunk-close".equals(action)
        || "windows-open".equals(action) || "windows-close".equals(action);
  }

  static boolean matches(Bundle state, String action) {
    if (state == null || !state.getBoolean("available", false)) return false;
    if ("lock".equals(action)) return any(state.getString("lock"), "已上锁", "已锁", "LOCKED");
    if ("unlock".equals(action)) return any(state.getString("lock"), "已解锁", "未上锁", "UNLOCKED");
    if ("climate-start".equals(action)) return on(state.getString("climate"));
    if ("climate-stop".equals(action)) return off(state.getString("climate"));
    if ("air-start".equals(action)) return on(state.getString("air"));
    if ("air-stop".equals(action)) return off(state.getString("air"));
    if ("trunk-open".equals(action)) return any(state.getString("tailgate"), "已打开", "未关严", "OPEN");
    if ("trunk-close".equals(action)) return any(state.getString("tailgate"), "已关闭", "关闭", "CLOSED");
    if ("windows-open".equals(action)) return any(state.getString("windows"), "已打开", "打开", "OPEN");
    if ("windows-close".equals(action)) return any(state.getString("windows"), "已关闭", "关闭", "CLOSED");
    return false;
  }

  static String successText(String action) {
    if ("lock".equals(action)) return "车辆已确认：已上锁";
    if ("unlock".equals(action)) return "车辆已确认：已解锁";
    if ("climate-start".equals(action)) return "车辆已确认：空调已开启";
    if ("climate-stop".equals(action)) return "车辆已确认：空调已关闭";
    if ("air-start".equals(action)) return "车辆已确认：净化已开启";
    if ("air-stop".equals(action)) return "车辆已确认：净化已关闭";
    if ("trunk-open".equals(action)) return "车辆已确认：尾门已打开";
    if ("trunk-close".equals(action)) return "车辆已确认：尾门已关闭";
    if ("windows-open".equals(action)) return "车辆已确认：车窗已打开";
    if ("windows-close".equals(action)) return "车辆已确认：车窗已关闭";
    return "车辆状态已确认";
  }

  private static boolean on(String value) {
    return any(value, "开启", "运行中", "ON", "ACTIVE", "RUNNING");
  }

  private static boolean off(String value) {
    return any(value, "关闭", "OFF", "INACTIVE", "STOPPED");
  }

  private static boolean any(String value, String... expected) {
    if (value == null) return false;
    String normalized = value.trim().toUpperCase(Locale.ROOT);
    for (String item : expected) {
      if (normalized.contains(item.toUpperCase(Locale.ROOT))) return true;
    }
    return false;
  }
}
