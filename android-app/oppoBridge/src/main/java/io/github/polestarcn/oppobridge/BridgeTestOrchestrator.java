package io.github.polestarcn.oppobridge;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/** Runs all read-only compatibility paths in one pass. No vehicle command is sent. */
final class BridgeTestOrchestrator {
  private static final String PREFS = "bridge-test-history";

  static final class Result {
    final String text;
    final int passed;
    final int pending;
    final int failed;
    Result(String text, int passed, int pending, int failed) {
      this.text = text; this.passed = passed; this.pending = pending; this.failed = failed;
    }
  }

  private BridgeTestOrchestrator() {}

  static Result run(Context context) {
    Counter counter = new Counter();
    StringBuilder out = new StringBuilder("PsAssistant OPPO Bridge ")
        .append(BridgeBuild.VERSION_NAME).append("\n一键检测全部方案（只读，不发车）\n\n");
    Bundle settings = BridgeSettings.localSnapshot(context);
    stage(out, counter, settings.getBoolean(BridgeSettings.MASTER, true) ? 1 : -1,
        "模块总开关", settings.getBoolean(BridgeSettings.MASTER, true) ? "已开启" : "已关闭");
    boolean root = rootAvailable();
    stage(out, counter, root ? 1 : 0, "Root环境",
        root ? "su可用" : "未授权；不影响只读诊断，无法自动重启宿主");

    PackageManager pm = context.getPackageManager();
    packageStage(out, counter, pm, "io.github.polestarcn.companion", "PsAssistant主程序");
    packageStage(out, counter, pm, "com.heytap.mydevices", "设备空间/MyDevices");
    packageStage(out, counter, pm, "com.coloros.assistantscreen", "负一屏/AssistantScreen");
    packageStage(out, counter, pm, "com.heytap.opluscarlink", "OPPO CarLink（只读探测）");

    Bundle caps = safeCall(context, "capabilities");
    boolean bridge = caps.getInt("protocolVersion", -1) == 1 && caps.getBoolean("state", false);
    stage(out, counter, bridge ? 1 : -1, "PsAssistant本机桥接",
        bridge ? "协议1可用" : caps.getString("reason", "主程序未响应"));

    Bundle snapshot = safeCall(context, "snapshot");
    boolean realState = snapshot.getBoolean("available", false) && !snapshot.getBoolean("demo", false);
    stage(out, counter, realState ? 1 : 0, "真实车辆只读状态",
        realState ? snapshot.getString("name", "Polestar") : snapshot.getString("reason", "尚未登录/刷新"));

    Bundle host = safeCall(context, "host-status");
    boolean mainHook = recent(host.getLong("main_seenAt", 0L));
    boolean cardsHook = recent(host.getLong("cards_seenAt", 0L));
    boolean mainCurrent = BridgeBuild.VERSION_NAME.equals(host.getString("main_moduleVersion", ""));
    boolean cardsCurrent = BridgeBuild.VERSION_NAME.equals(host.getString("cards_moduleVersion", ""));
    stage(out, counter, mainHook && mainCurrent ? 1 : (mainHook ? -1 : 0), "MyDevices主进程Hook",
        hostDetail(host, "main_"));
    stage(out, counter, cardsHook && cardsCurrent ? 1 : (cardsHook ? -1 : 0), "MyDevices卡片进程Hook",
        hostDetail(host, "cards_"));

    QuickAppCarCapability.Result route = QuickAppCarCapability.detect(context);
    stage(out, counter, route.routeSafe() ? 1 : -1, "17.4.15精确RPK门禁",
        route.mode() + " · " + route.reason);
    Bundle quick = safeCall(context, "quickapp-runtime-status");
    boolean exact = quick.getBoolean("rpkPatchApplied", false)
        && quick.getBoolean("converterWhitelistApplied", false);
    stage(out, counter, exact ? 1 : 0, "4×2直接控制运行链路",
        "RPK=" + quick.getBoolean("rpkPatchApplied", false)
            + " Converter=" + quick.getBoolean("converterWhitelistApplied", false)
            + " shortcuts=" + quick.getInt("selectedShortcutCount", 0));

    Bundle assistant = safeCall(context, "assistant-host-status");
    boolean assistantRecent = recent(assistant.getLong("seenAt", 0L));
    boolean assistantCurrent = BridgeBuild.VERSION_NAME.equals(assistant.getString("moduleVersion", ""));
    stage(out, counter, assistantRecent && assistantCurrent ? 1 : (assistantRecent ? -1 : 0),
        "负一屏Hook", assistant.getString("phase", "尚无回报") + " · module="
            + assistant.getString("moduleVersion", ""));
    Bundle catalog = safeCall(context, "account-bind-status");
    String rows = catalog.getString("providerCatalog", "");
    stage(out, counter, rows.contains("POLESTAR") ? 1 : 0, "车辆绑定入口Polestar",
        rows.contains("POLESTAR") ? "本地目录已观测到POLESTAR" : "打开车辆绑定目录后重新检测");

    int providerRows = providerRows(context);
    stage(out, counter, providerRows > 0 ? 1 : (realState ? -1 : 0), "设备卡Provider",
        providerRows > 0 ? "row-ready" : "暂无可发布车辆行");

    Bundle surfaces = safeCall(context, "vehicle-surface-status");
    boolean watchObserved = surfaces.getLong("wallet_seenAt", 0L) > 0
        || surfaces.getLong("healthWallet_seenAt", 0L) > 0
        || surfaces.getLong("digitalKey_seenAt", 0L) > 0;
    stage(out, counter, watchObserved ? 1 : 0, "Watch/钱包宿主探测",
        watchObserved ? "已观察到宿主" : "等待手表或钱包首次调用");

    out.append("\n汇总：通过 ").append(counter.passed).append(" · 待处理 ")
        .append(counter.pending).append(" · 失败 ").append(counter.failed)
        .append("\n未执行锁车、空调、闪灯或鸣笛。\n生成时间：")
        .append(new Date()).append('\n');
    String report = out.toString();
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putLong("lastRunAt", System.currentTimeMillis()).putString("lastReport", report)
        .putInt("passed", counter.passed).putInt("pending", counter.pending)
        .putInt("failed", counter.failed).apply();
    return new Result(report, counter.passed, counter.pending, counter.failed);
  }

  static String lastReport(Context context) {
    return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString("lastReport", "尚未运行一键检测。\n");
  }

  static String quickStatus(Context context) {
    Bundle settings = BridgeSettings.localSnapshot(context);
    Bundle caps = safeCall(context, "capabilities");
    Bundle state = safeCall(context, "snapshot");
    return "PsAssistant OPPO Bridge " + BridgeBuild.VERSION_NAME
        + "\n工作模式：本机原生兼容链路"
        + "\n模块总开关：" + (settings.getBoolean(BridgeSettings.MASTER, true) ? "开启" : "关闭")
        + "\nPsAssistant桥接：" + (caps.getBoolean("state", false) ? "可用" : "未连接")
        + "\n车辆状态：" + (state.getBoolean("available", false)
            ? state.getString("name", "Polestar") : "尚未读取")
        + "\n说明：模拟测试不会写入OPPO真实鉴权或发送车辆命令。";
  }

  private static void packageStage(StringBuilder out, Counter counter, PackageManager pm,
      String packageName, String title) {
    try {
      PackageInfo info = pm.getPackageInfo(packageName, 0);
      stage(out, counter, 1, title, info.versionName == null ? "已安装" : info.versionName);
    } catch (PackageManager.NameNotFoundException missing) {
      stage(out, counter, 0, title, "未安装/当前不可见");
    }
  }

  private static Bundle safeCall(Context context, String method) {
    try { return BridgeClient.call(context, method, null); }
    catch (Throwable ignored) { return Bundle.EMPTY; }
  }

  private static int providerRows(Context context) {
    try (Cursor cursor = context.getContentResolver().query(
        BridgeIdentity.deviceUri(context), null, null, null, null)) {
      return cursor == null ? 0 : cursor.getCount();
    } catch (Throwable ignored) { return 0; }
  }

  private static boolean recent(long timestamp) {
    long now = System.currentTimeMillis();
    return timestamp > 0 && timestamp <= now + 30_000L && now - timestamp <= 10 * 60_000L;
  }

  private static String hostDetail(Bundle value, String prefix) {
    long seen = value.getLong(prefix + "seenAt", 0L);
    if (seen <= 0) return "尚无回报";
    return value.getString(prefix + "phase", "") + " · host="
        + value.getString(prefix + "hostVersion", "") + " · module="
        + value.getString(prefix + "moduleVersion", "");
  }

  private static boolean rootAvailable() {
    Process process = null;
    try {
      process = new ProcessBuilder("su", "-c", "id -u").redirectErrorStream(true).start();
      if (!process.waitFor(3, TimeUnit.SECONDS)) { process.destroyForcibly(); return false; }
      String line;
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(
          process.getInputStream(), StandardCharsets.UTF_8))) { line = reader.readLine(); }
      return process.exitValue() == 0 && "0".equals(line == null ? "" : line.trim());
    } catch (Throwable unavailable) {
      if (process != null) process.destroyForcibly();
      return false;
    }
  }

  private static void stage(StringBuilder out, Counter counter, int state,
      String title, String detail) {
    if (state > 0) { out.append("[通过] "); counter.passed++; }
    else if (state == 0) { out.append("[待处理] "); counter.pending++; }
    else { out.append("[失败] "); counter.failed++; }
    out.append(title).append(" · ").append(detail == null ? "" : detail).append('\n');
  }

  private static final class Counter { int passed; int pending; int failed; }
}
