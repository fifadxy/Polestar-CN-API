package io.github.polestarcn.oppobridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Consolidated standalone LSPosed settings, test and export surface. */
public final class BridgeDiagnosticsActivity extends Activity {
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private TextView status;
  private Switch master;
  private Switch myDevices;
  private Switch assistant;
  private Switch watch;
  private Switch direct;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle("PsAssistant OPPO Bridge " + BridgeBuild.VERSION_NAME);

    LinearLayout column = new LinearLayout(this);
    column.setOrientation(LinearLayout.VERTICAL);
    int p = dp(18);
    column.setPadding(p, p, p, p);

    TextView title = new TextView(this);
    title.setText("OPPO系统车辆桥接");
    title.setTextSize(24);
    title.setPadding(0, 0, 0, dp(8));
    column.addView(title);

    TextView description = new TextView(this);
    description.setText("beta31 TestLab · Virtual Session + CAR Dispatcher + PsAssistant协议网关\n模拟测试不发车；真实车控只由主APK在已登录会话中执行。");
    description.setTextSize(14);
    description.setPadding(0, 0, 0, dp(12));
    column.addView(description);

    Bundle settings = BridgeSettings.localSnapshot(this);
    master = settingSwitch(column, "启用OPPO系统车辆桥接", BridgeSettings.MASTER, settings);

    TextView scope = new TextView(this);
    scope.setText("适配范围");
    scope.setTextSize(18);
    scope.setPadding(0, dp(12), 0, dp(4));
    column.addView(scope);
    myDevices = settingSwitch(column, "设备空间 / MyDevices", BridgeSettings.MY_DEVICES, settings);
    assistant = settingSwitch(column, "负一屏 / AssistantScreen", BridgeSettings.ASSISTANT, settings);
    watch = settingSwitch(column, "Watch X2 / 钱包宿主探测", BridgeSettings.WATCH, settings);
    direct = settingSwitch(column, "允许4×2直接控制", BridgeSettings.DIRECT, settings);

    Button oneClick = actionButton("一键诊断 + 模拟测试 + 导出");
    oneClick.setOnClickListener(v -> runOneClick());
    column.addView(oneClick);

    Button test = actionButton("一键检测全部方案（只读、不发车）");
    test.setOnClickListener(v -> runAllTests());
    column.addView(test);

    Button simulation = actionButton("模拟完整链路（不发车）");
    simulation.setOnClickListener(v -> runSimulation());
    column.addView(simulation);

    Button repair = actionButton("一键修复并重载宿主");
    repair.setOnClickListener(v -> confirmRepair());
    column.addView(repair);

    Button export = actionButton("诊断与导出");
    export.setOnClickListener(v -> showExportMenu());
    column.addView(export);

    status = new TextView(this);
    status.setTextSize(15);
    status.setTextIsSelectable(true);
    status.setPadding(0, dp(14), 0, dp(24));
    status.setText(BridgeTestOrchestrator.quickStatus(this));
    column.addView(status);

    ScrollView scroll = new ScrollView(this);
    scroll.addView(column);
    setContentView(scroll);
    if(getIntent()!=null&&getIntent().getBooleanExtra("autoOneClick",false))runOneClick();
  }

  private Switch settingSwitch(LinearLayout parent, String label, String key, Bundle settings) {
    Switch view = new Switch(this);
    view.setText(label);
    view.setChecked(settings.getBoolean(key, true));
    view.setPadding(0, dp(2), 0, dp(2));
    view.setOnCheckedChangeListener((button, checked) -> {
      BridgeSettings.updateLocal(this, key, checked);
      if (status != null) status.setText("设置已保存。执行“一键修复并重载宿主”后，新设置会在目标进程重新加载。\n\n"
          + BridgeTestOrchestrator.quickStatus(this));
    });
    parent.addView(view);
    return view;
  }

  private Button actionButton(String label) {
    Button button = new Button(this);
    button.setText(label);
    button.setAllCaps(false);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    lp.topMargin = dp(8);
    button.setLayoutParams(lp);
    return button;
  }

  private void runOneClick() {
    status.setText("正在执行 beta31 TestLab 一键诊断：只读检测 → 模拟链路 → 轻量导出……不会发送车辆命令。");
    worker.execute(() -> {
      try {
        BridgeTestOrchestrator.Result test = BridgeTestOrchestrator.run(this);
        BridgeSimulationSession.Result simulation = BridgeSimulationSession.run(this);
        File file = OppoResearchExport.create(this);
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".oppoexports", file);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("application/zip");share.putExtra(Intent.EXTRA_STREAM, uri);share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        String summary = "beta31 TestLab 一键诊断完成\n通过 " + test.passed + " · 待处理 " + test.pending + " · 失败 " + test.failed
            + "\n模拟链路：" + (simulation.passed ? "通过" : "未通过") + "\n导出：" + file.getName() + " · " + file.length()/1024 + " KB\n\n" + test.text;
        runOnUiThread(() -> {if(isFinishing()||isDestroyed())return;status.setText(summary);startActivity(Intent.createChooser(share,"分享 beta31 TestLab 诊断包"));});
      } catch(Throwable failure) {
        showText("一键诊断失败："+failure.getClass().getSimpleName()+": "+failure.getMessage());
      }
    });
  }

  private void runAllTests() {
    status.setText("正在执行全部只读检测……不会发送车辆命令。");
    worker.execute(() -> {
      BridgeTestOrchestrator.Result result = BridgeTestOrchestrator.run(this);
      showText(result.text);
    });
  }

  private void runSimulation() {
    status.setText("正在运行模块内部模拟链路……");
    worker.execute(() -> showText(BridgeSimulationSession.run(this).text));
  }

  private void confirmRepair() {
    new AlertDialog.Builder(this)
        .setTitle("一键修复并重载宿主")
        .setMessage("将恢复模块推荐设置、启用本机Provider与4×2实验，并使用Root停止设备空间、负一屏、SmartEngine和CarLink进程。不会修改OPPO账号、真实鉴权或车辆数据。")
        .setNegativeButton("取消", null)
        .setPositiveButton("继续", (dialog, which) -> repairAndReload())
        .show();
  }

  private void repairAndReload() {
    status.setText("正在修复模块设置并重载OPPO宿主……");
    worker.execute(() -> {
      StringBuilder out = new StringBuilder();
      BridgeSettings.enableRecommendedDefaults(this);
      getSharedPreferences("vehicle-card", MODE_PRIVATE).edit().remove("hidden").apply();
      getContentResolver().notifyChange(BridgeIdentity.deviceUri(this), null);
      try {
        Bundle input = new Bundle();
        input.putBoolean("enabled", true);
        Bundle result = BridgeClient.call(this, "oppo-oem-car-set", input);
        out.append(result.getBoolean("accepted", false)
            ? "[通过] 主程序4×2本机路由已启用\n"
            : "[待处理] 主程序未接受4×2设置：" + result.getString("reason", "未连接") + "\n");
        Bundle session = BridgeClient.call(this,"virtual-session-set",input);
        out.append(session.getBoolean("accepted",false)
            ? "[通过] Virtual Vehicle Session 已启用\n"
            : "[待处理] Session未就绪："+session.getString("reason","未连接")+"\n");
      } catch (Throwable failure) {
        out.append("[待处理] 主程序设置不可用：").append(failure.getClass().getSimpleName()).append('\n');
      }
      out.append("[通过] 模块总开关和全部作用面已恢复\n")
          .append("[通过] Provider隐藏状态已清除\n");
      Process process = null;
      try {
        process = new ProcessBuilder("su", "-c",
            "am force-stop com.heytap.mydevices; "
                + "am force-stop com.coloros.assistantscreen; "
                + "am force-stop com.oplus.assistantscreen; "
                + "am force-stop com.oplus.smartengine; "
                + "am force-stop com.oplus.cardservice; "
                + "am force-stop com.nearme.instant.platform; "
                + "am force-stop com.heytap.opluscarlink")
            .redirectErrorStream(true).start();
        boolean done = process.waitFor(10, TimeUnit.SECONDS);
        if (!done) process.destroyForcibly();
        out.append(done && process.exitValue() == 0
            ? "[通过] OPPO宿主进程已停止；请重新打开设备空间和负一屏\n"
            : "[待处理] Root重载未完成，请手动强停相关应用\n");
      } catch (Throwable failure) {
        if (process != null) process.destroyForcibly();
        out.append("[待处理] Root不可用，请手动重启宿主\n");
      }
      runOnUiThread(() -> {
        setAllSwitchesChecked();
        showText(out.toString());
      });
    });
  }

  private void setAllSwitchesChecked() {
    master.setChecked(true);
    myDevices.setChecked(true);
    assistant.setChecked(true);
    watch.setChecked(true);
    direct.setChecked(true);
  }

  private void showExportMenu() {
    new AlertDialog.Builder(this)
        .setTitle("诊断与导出")
        .setItems(new String[]{"复制统一文字摘要", "导出轻量ZIP（小于10MB）"}, (dialog, which) -> {
          if (which == 0) copySummary(); else exportBundle();
        })
        .setNegativeButton("取消", null)
        .show();
  }

  private void copySummary() {
    status.setText("正在生成统一文字摘要……");
    worker.execute(() -> {
      String text = OppoResearchExport.copySummary(this);
      runOnUiThread(() -> {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("PsAssistant OPPO诊断", text));
        status.setText("统一摘要已复制，可直接粘贴给ChatGPT。\n字符数：" + text.length() + "\n\n" + text);
      });
    });
  }

  private void exportBundle() {
    status.setText("正在生成轻量资料包……不会导出账号、Token、VIN、AUTH_CODE或原始数据库。");
    worker.execute(() -> {
      try {
        File file = OppoResearchExport.create(this);
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".oppoexports", file);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("application/zip");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        runOnUiThread(() -> {
          if (isFinishing() || isDestroyed()) return;
          status.setText("轻量资料包已生成：" + file.getName() + "\n大小："
              + file.length() / 1024 + " KB（硬限制小于10MB）");
          startActivity(Intent.createChooser(share, "分享OPPO统一诊断包"));
        });
      } catch (Throwable failure) {
        showText("资料包生成失败：" + failure.getClass().getSimpleName() + ": " + failure.getMessage());
      }
    });
  }

  private void showText(String text) {
    runOnUiThread(() -> {
      if (!isFinishing() && !isDestroyed()) status.setText(text);
    });
  }

  private int dp(int value) {
    return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
  }

  @Override protected void onDestroy() {
    worker.shutdownNow();
    super.onDestroy();
  }
}
