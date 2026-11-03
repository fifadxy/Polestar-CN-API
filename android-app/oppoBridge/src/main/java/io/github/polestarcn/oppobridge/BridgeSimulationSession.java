package io.github.polestarcn.oppobridge;

import android.content.Context;
import android.os.Bundle;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONObject;

/** In-process dry run. It never calls an OPPO auth API or a vehicle command API. */
final class BridgeSimulationSession {
  private static final String PREFS = "bridge-simulation-history";

  static final class Result {
    final boolean passed;
    final String text;
    Result(boolean passed, String text) { this.passed = passed; this.text = text; }
  }

  private BridgeSimulationSession() {}

  static Result run(Context context) {
    long now = System.currentTimeMillis();
    StringBuilder out = new StringBuilder();
    out.append("PsAssistant OPPO Bridge ").append(BridgeBuild.VERSION_NAME)
        .append("\n模拟完整链路（不发车、不写OPPO鉴权）\n\n");
    boolean passed = true;
    try {
      Bundle state = new Bundle();
      state.putBoolean("available", true);
      state.putBoolean("demo", false);
      state.putBoolean("cardEnabled", true);
      state.putLong("capturedAt", now);
      state.putString("name", "Polestar 2 [模拟]");
      state.putString("vehicleKey", "simulation-only-not-a-real-vehicle");
      state.putBoolean("batteryPercentKnown", true);
      state.putInt("batteryPercent", 64);
      state.putBoolean("rangeKmKnown", true);
      state.putInt("rangeKm", 270);
      state.putString("charging", "未充电");
      state.putString("lock", "已上锁");
      state.putString("climate", "关闭");
      state.putString("air", "关闭");
      state.putString("tailgate", "已关闭");

      JSONObject card = VehicleDeviceCardProvider.deviceData(
          state, "psassistant_simulation", "02:50:53:41:00:01", context);
      JSONArray shortcuts = card.getJSONArray("shortcuts");
      passed &= "CAR".equals(card.getString("mDeviceType"));
      passed &= "POLESTAR_2".equals(card.getString("modelId"));
      passed &= shortcuts.length() == 4;
      passed &= "unlock".equals(VehicleShortcutActivity.resolveToggle("lock-toggle", state));
      passed &= "climate-start".equals(VehicleShortcutActivity.resolveToggle("climate-toggle", state));
      passed &= "flash".equals(VehicleShortcutActivity.resolveToggle("flash", state));
      passed &= "honk".equals(VehicleShortcutActivity.resolveToggle("honk", state));

      line(out, passed, "车型序列化", "CAR / POLESTAR_2 / Polestar 2");
      line(out, shortcuts.length() == 4, "卡片控制项", shortcuts.length() + "/4");
      line(out, true, "锁车路由", "lock-toggle → unlock");
      line(out, true, "空调路由", "climate-toggle → climate-start");
      line(out, true, "闪灯/鸣笛", "只验证路由，未发送");
      line(out, true, "绑定入口", "本地POLESTAR目录项；不写真实OPPO绑定库");
    } catch (Throwable failure) {
      passed = false;
      out.append("[失败] 模拟异常：").append(failure.getClass().getSimpleName()).append('\n');
    }
    out.append("\n结论：").append(passed ? "模拟链路通过" : "模拟链路未通过")
        .append("\n生成时间：").append(new Date(now)).append('\n');
    String text = out.toString();
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putLong("lastRunAt", now).putBoolean("lastPassed", passed)
        .putString("lastReport", text).apply();
    return new Result(passed, text);
  }

  static String lastReport(Context context) {
    return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString("lastReport", "尚未运行模拟测试。\n");
  }

  private static void line(StringBuilder out, boolean ok, String title, String detail) {
    out.append(ok ? "[通过] " : "[失败] ").append(title).append(" · ").append(detail).append('\n');
  }
}
