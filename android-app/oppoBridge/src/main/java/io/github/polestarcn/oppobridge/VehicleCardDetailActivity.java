package io.github.polestarcn.oppobridge;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class VehicleCardDetailActivity extends Activity {
  private TextView state;
  private LinearLayout root;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ScrollView scroll = new ScrollView(this);
    root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    int p = (int) (16 * getResources().getDisplayMetrics().density);
    root.setPadding(p, p, p, p);
    scroll.addView(root);

    TextView title = new TextView(this);
    title.setText("Polestar · PsAssistant");
    title.setTextSize(22);
    root.addView(title);

    state = new TextView(this);
    state.setTextSize(15);
    root.addView(state);

    addButton("刷新车辆", null);
    addButton("上锁", "lock");
    addButton("解锁", "unlock");
    addButton("开启空调", "climate-start");
    addButton("关闭空调", "climate-stop");
    addButton("开启空气净化", "air-start");
    addButton("关闭空气净化", "air-stop");
    addButton("开启尾门", "trunk-open");
    addButton("关闭尾门", "trunk-close");
    addButton("恢复设备卡", "__restore_card");

    setContentView(scroll);
    refresh();
  }

  private void addButton(String label, String action) {
    Button b = new Button(this);
    b.setText(label);
    b.setAllCaps(false);
    b.setOnClickListener(v -> {
      try {
        Bundle result;
        if ("__restore_card".equals(action)) {
          getSharedPreferences("vehicle-card", MODE_PRIVATE).edit().remove("hidden").apply();
          getContentResolver().notifyChange(VehicleDeviceCardProvider.URI, null);
          result = Bundle.EMPTY;
        } else if (action == null) {
          result = BridgeClient.refresh(this);
        } else {
          result = BridgeClient.command(this, action);
        }
        state.setText("请求结果: " + result + "\n\n" + state.getText());
      } catch (Throwable t) {
        state.setText("请求失败: " + t + "\n\n" + state.getText());
      }
      root.postDelayed(this::refresh, 900);
    });
    root.addView(b);
  }

  private void refresh() {
    try {
      Bundle s = BridgeClient.snapshot(this);
      if (!s.getBoolean("available", false)) {
        state.setText("\nPsAssistant 未提供可用车辆。请先登录并选中车辆。\n");
        return;
      }
      StringBuilder out = new StringBuilder("\n");
      out.append("车辆: ").append(s.getString("name", "--")).append('\n');
      out.append("SOC: ").append(s.getBoolean("batteryPercentKnown") ? s.getInt("batteryPercent") + "%" : "--").append('\n');
      out.append("续航: ").append(s.getBoolean("rangeKmKnown") ? s.getInt("rangeKm") + " km" : "--").append('\n');
      out.append("里程: ").append(s.getBoolean("odometerKmKnown") ? s.getInt("odometerKm") + " km" : "--").append('\n');
      out.append("锁车: ").append(s.getString("lock", "--")).append('\n');
      out.append("尾门: ").append(s.getString("tailgate", "--")).append('\n');
      out.append("空调: ").append(s.getString("climate", "--")).append('\n');
      out.append("净化: ").append(s.getString("air", "--")).append('\n');
      out.append("充电: ").append(s.getString("charging", "--")).append('\n');
      state.setText(out.toString());
    } catch (Throwable t) {
      state.setText("\nBridge 读取失败: " + t + "\n");
    }
  }
}
