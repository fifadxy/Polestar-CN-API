package io.github.polestarcn.oppobridge;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class VehicleCardDetailActivity extends Activity {
  private TextView state, feedback;
  private final java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
  private final java.util.List<Button> buttons = new java.util.ArrayList<>();
  private boolean busy;
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

    feedback = new TextView(this);feedback.setTextIsSelectable(true);root.addView(feedback);
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
    buttons.add(b);
    b.setOnClickListener(v -> {
      if (busy) return;
      busy=true;for(Button button:buttons) button.setEnabled(false);
      feedback.setText("正在提交请求…");
      worker.execute(() -> {
      String message;
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
        message = "__restore_card".equals(action) ? "设备卡已恢复" : result.getBoolean("accepted",false) ? "请求已受理，等待车辆回读确认" : "请求未受理: " + result.getString("reason","unknown");
      } catch (Throwable t) {
        message = "请求失败: " + t.getClass().getSimpleName();
      }
      final String resultMessage=message;
      runOnUiThread(() -> {if(isFinishing()||isDestroyed())return;
        feedback.setText(resultMessage);busy=false;for(Button button:buttons)button.setEnabled(true);
        root.postDelayed(this::refresh,900);
      });
      });
    });
    root.addView(b);
  }

  private void refresh() {
    if(isFinishing()||isDestroyed())return;
    worker.execute(this::readSnapshot);
  }

  private void showState(String text) {
    runOnUiThread(() -> {if(!isFinishing()&&!isDestroyed())state.setText(text);});
  }

  private void readSnapshot() {
    try {
      Bundle s = BridgeClient.snapshot(this);
      if (!s.getBoolean("available", false)) {
        showState("车辆不可用: "+s.getString("reason","请先登录并选中车辆"));
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
      showState(out.toString());
    } catch (Throwable t) {
      showState("Bridge 读取失败: " + t.getClass().getSimpleName());
    }
  }
  @Override protected void onDestroy() {
    worker.shutdownNow();
    super.onDestroy();
  }
}
