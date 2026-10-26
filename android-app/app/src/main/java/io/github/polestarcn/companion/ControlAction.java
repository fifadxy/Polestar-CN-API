package io.github.polestarcn.companion;

/** A direct toggle requires a known vehicle state. Unknown/processing is never guessed. */
public final class ControlAction {
  private ControlAction() {}
  public static String next(String control, VehicleSnapshot state) {
    if (state == null) return null;
    switch (control) {
      case "lock": return "未锁".equals(state.lock) ? "lock"
          : ("已上锁".equals(state.lock) || "已锁".equals(state.lock)) ? "unlock" : null;
      case "trunk": return "已关闭".equals(state.tailgate) ? "trunk-open"
          : ("已打开".equals(state.tailgate) || "未关严".equals(state.tailgate)) ? "trunk-close" : null;
      case "climate": return "开启".equals(state.climate) ? "climate-stop"
          : "关闭".equals(state.climate) ? "climate-start" : null;
      case "air": return "开启".equals(state.air) ? "air-stop"
          : "关闭".equals(state.air) ? "air-start" : null;
      case "windows": return allWindows(state, 2) ? "windows-open"
          : anyWindowOpen(state) ? "windows-close" : null;
      default: return null;
    }
  }
  public static boolean reached(String action, VehicleSnapshot state) {
    if (state == null || state.isDemo || !"LIVE".equals(state.source)) return false;
    switch (action) {
      case "lock": return "已上锁".equals(state.lock) || "已锁".equals(state.lock);
      case "unlock": return "未锁".equals(state.lock);
      case "trunk-open": return "已打开".equals(state.tailgate);
      case "trunk-close": return "已关闭".equals(state.tailgate);
      case "climate-start": return "开启".equals(state.climate);
      case "climate-stop": return "关闭".equals(state.climate);
      case "air-start": return "开启".equals(state.air);
      case "air-stop": return "关闭".equals(state.air);
      case "windows-open": return allWindows(state, 1);
      case "windows-close": return allWindows(state, 2);
      default: return false;
    }
  }

  private static boolean allWindows(VehicleSnapshot state, int target) {
    if (state == null || state.exterior == null) return false;
    for (int i = 0; i < 4; i++) if (state.exterior.state(true, i) != target) return false;
    return true;
  }

  private static boolean anyWindowOpen(VehicleSnapshot state) {
    if (state == null || state.exterior == null) return false;
    boolean known = false;
    for (int i = 0; i < 4; i++) {
      int value = state.exterior.state(true, i);
      if (value == 1 || value == 3) return true;
      if (value == 2) known = true;
    }
    return false;
  }
}
