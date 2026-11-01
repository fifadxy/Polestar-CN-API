package io.github.polestarcn.oppobridge;

import android.os.Bundle;
import java.util.Locale;

/**
 * Maps only PsAssistant-owned IDs and the four exact MyDevices 17.4.15 dormant CAR IDs.
 * OEM toggle IDs are resolved from the current PsAssistant snapshot rather than guessing
 * the meaning of the QuickApp valueInt/valueBoolean payload.
 */
final class CardShortcutCommand {
  private CardShortcutCommand() {}

  static String fromId(String id) {
    return fromId(id, null);
  }

  static String fromId(String id, Bundle state) {
    if (id == null) return null;
    switch (id) {
      case "ps_lock": return "lock";
      case "ps_unlock": return "unlock";
      case "ps_climate-start": return "climate-start";
      case "ps_climate-stop": return "climate-stop";
      case "ps_trunk-open": return "trunk-open";
      case "ps_trunk-close": return "trunk-close";
      case "ps_air-start": return "air-start";
      case "ps_air-stop": return "air-stop";

      // Exact dormant CAR shortcut IDs found in MyDevices 17.4.15 RPK.
      case "shanguang": return "flash";
      case "mingdi": return "honk";
      case "chesuo": return lockToggle(state);
      case "kongtiao": return climateToggle(state);
      default: return null;
    }
  }

  static boolean isOemCarId(String id) {
    return "kongtiao".equals(id) || "chesuo".equals(id)
        || "shanguang".equals(id) || "mingdi".equals(id);
  }

  private static String lockToggle(Bundle state) {
    String value = value(state, "lock");
    if (contains(value, "已上锁", "已锁", "LOCKED")) return "unlock";
    if (contains(value, "已解锁", "未上锁", "UNLOCKED")) return "lock";
    return null;
  }

  private static String climateToggle(Bundle state) {
    String value = value(state, "climate");
    if (contains(value, "开启", "运行中", "ON", "ACTIVE", "RUNNING")) return "climate-stop";
    if (contains(value, "关闭", "OFF", "INACTIVE", "STOPPED")) return "climate-start";
    return null;
  }

  private static String value(Bundle state, String key) {
    return state == null ? "" : String.valueOf(state.getString(key, ""));
  }

  private static boolean contains(String value, String... expected) {
    String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    for (String item : expected)
      if (normalized.contains(item.toUpperCase(Locale.ROOT))) return true;
    return false;
  }
}
