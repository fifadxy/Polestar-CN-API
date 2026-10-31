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
      StringBuilder out = new StringBuilder("PsAssistant OPPO Bridge 0.9.0\n\n");
      try {
        Bundle host=BridgeClient.call(this,"host-status",null);
        for(String prefix:new String[]{"main_","cards_"}) {
          long seen=host.getLong(prefix+"seenAt",0);
          out.append(prefix.equals("main_")?"设备空间主进程: ":"设备卡进程: ");
          if(seen==0)out.append("未收到 Hook 回报；不代表主程序接口失效\n");
          else out.append("上次回报 ").append(new java.util.Date(seen)).append(" · ").append(host.getString(prefix+"phase","")).append(" · ").append(host.getString(prefix+"reason","")).append(" · 设备空间 ").append(host.getString(prefix+"hostVersion","")).append(" · 模块 ").append(host.getString(prefix+"moduleVersion","")).append('\n');
        }
        out.append("回报是历史观测，不代表当前仍已启用或车辆已响应。\n\n");
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
      out.append("\n已实现: 桥接、直接车控面板、MyDevices 17.4.15 原生首页车辆控件。\nQuickApp: 保留 OEM 数据，不修改白名单；0.9.0 会报告 converter/RPK 双重过滤状态。\n待真机验证: 原生首页控件实际渲染及车辆读回。\n负一屏静态 car setting 已确认属于车载蓝牙场景，不等同于“我的车辆”车企远控；Pantanal/动态服务仍在研究。\n小布、Watch 长续航、官方车企绑定尚未宣称完成。\n\n文字可长按选择复制。");
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
