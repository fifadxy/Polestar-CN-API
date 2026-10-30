package io.github.polestarcn.oppobridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Direct diagnostic/control entry; usable before MyDevices discovers a card. */
public final class BridgeDiagnosticsActivity extends Activity {
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private TextView status;
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    LinearLayout column = new LinearLayout(this);
    column.setOrientation(LinearLayout.VERTICAL);
    int p = (int) (18 * getResources().getDisplayMetrics().density);
    column.setPadding(p, p, p, p);
    Button panel = new Button(this);
    panel.setText("打开车辆控制面板");
    panel.setOnClickListener(v -> startActivity(new Intent(this, VehicleCardDetailActivity.class)));
    column.addView(panel);
    Button refresh = new Button(this);
    refresh.setText("重新检测桥接");
    refresh.setOnClickListener(v -> diagnose());
    column.addView(refresh);
    status = new TextView(this);
    status.setTextSize(15);
    status.setTextIsSelectable(true);
    column.addView(status);
    ScrollView scroll = new ScrollView(this);
    scroll.addView(column);
    setContentView(scroll);
    diagnose();
  }

  private void diagnose() {
    status.setText("正在检测 PsAssistant 桥接…");
    worker.execute(() -> {
      StringBuilder out = new StringBuilder("PsAssistant OPPO Bridge 0.4.0\n\n");
      try {
        Bundle caps = BridgeClient.capabilities(this);
        int protocol = caps.getInt("protocolVersion", -1);
        boolean ready = protocol == 1 && caps.getBoolean("state", false);
        out.append("卡片开关: ").append(caps.getBoolean("cardEnabled", true) ? "开启" : "关闭，请在主程序设置启用").append('\n');
        out.append("桥接连接: ").append(ready ? "可用" : "不可用").append('\n');
        out.append("协议: ").append(protocol).append('\n');
        if (!ready) out.append("原因: ").append(caps.getString("reason", "invalid-capabilities")).append('\n');
        out.append("命令: ").append(Arrays.toString(caps.getStringArray("commands"))).append("\n\n");
        if (ready) {
          Bundle state = BridgeClient.snapshot(this);
          out.append("车辆数据: ").append(state.getBoolean("available", false) ? "可用" : "不可用").append('\n');
          if (state.getBoolean("available", false)) {
            out.append("车辆: ").append(state.getString("name", "--")).append('\n');
            out.append("SOC: ").append(state.getBoolean("batteryPercentKnown") ? state.getInt("batteryPercent") + "%" : "--").append('\n');
            out.append("门锁: ").append(state.getString("lock", "--")).append('\n');
            out.append("空调: ").append(state.getString("climate", "--")).append('\n');
          } else out.append("原因: ").append(state.getString("reason", "请先登录并刷新车辆")).append('\n');
        }
      } catch (Throwable failure) {
        out.append("检测失败: ").append(failure.getClass().getSimpleName()).append(": ").append(failure.getMessage()).append('\n');
      }
      out.append("\n安装路径\n新版 PsAssistant: 使用内置桥接接口。\n旧版 PsAssistant: 启用 API 102 模块，勾选 PsAssistant 与我的设备，重启目标应用。运行时桥接要求 Android 14 或更高版本。\n");
      out.append("\n已实现: 桥接、直接车控面板、MyDevices 注入代码。\n待真机验证: 原生卡片发现及车控结果。\n小布、负一屏、流体云、Car+ 目前仅探测，未实现功能接入。\n\n文字可长按选择复制。");
      runOnUiThread(() -> {
        if (!isFinishing() && !isDestroyed()) status.setText(out.toString());
      });
    });
  }

  @Override protected void onDestroy() {
    worker.shutdownNow();
    super.onDestroy();
  }
}
