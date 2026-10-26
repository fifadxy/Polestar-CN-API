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
      default: return false;
    }
  }
}
