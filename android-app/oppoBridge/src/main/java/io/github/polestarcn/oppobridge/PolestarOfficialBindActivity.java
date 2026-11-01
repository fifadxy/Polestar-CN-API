package io.github.polestarcn.oppobridge;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * User-facing opt-in for the experimental official ColorOS "My Vehicle" bridge.
 *
 * <p>This does not write OPPO account databases. Enabling only allows the exact-build
 * AssistantScreen hook to present POLESTAR as locally bound when PsAssistant already has a valid
 * authorized vehicle session.</p>
 */
public final class PolestarOfficialBindActivity extends Activity {
  private TextView status;
  private Button enable,disable;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ScrollView scroll=new ScrollView(this);
    LinearLayout root=new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    int p=(int)(18*getResources().getDisplayMetrics().density);
    root.setPadding(p,p,p,p);
    scroll.addView(root);

    TextView title=new TextView(this);
    title.setText("Polestar · ColorOS 我的车辆");
    title.setTextSize(22f);
    root.addView(title);

    TextView note=new TextView(this);
    note.setText("实验桥接只修改 AssistantScreen 当前进程看到的品牌/绑定状态；"
        +"不会写 OPPO SceneService 数据库，也不会绕过 Polestar 登录。");
    note.setTextSize(14f);
    note.setPadding(0,p/2,0,p/2);
    root.addView(note);

    status=new TextView(this);
    status.setTextSize(15f);
    root.addView(status);

    enable=new Button(this);
    enable.setText("绑定到 ColorOS（实验）");
    enable.setAllCaps(false);
    enable.setOnClickListener(v->setBound(true));
    root.addView(enable);

    disable=new Button(this);
    disable.setText("取消 ColorOS 实验绑定");
    disable.setAllCaps(false);
    disable.setOnClickListener(v->setBound(false));
    root.addView(disable);

    Button control=new Button(this);
    control.setText("打开完整车辆控制");
    control.setAllCaps(false);
    control.setOnClickListener(v->startActivity(new android.content.Intent(this,VehicleCardDetailActivity.class)));
    root.addView(control);

    setContentView(scroll);
    refresh();
  }

  private void refresh() {
    try {
      Bundle state=BridgeClient.call(this,"oppo-official-bind-status",null);
      Bundle vehicle=BridgeClient.snapshot(this);
      StringBuilder out=new StringBuilder("\n");
      out.append("PsAssistant 登录：").append(state.getBoolean("signedIn")?"已登录":"未登录").append('\n');
      out.append("车辆：").append(state.getBoolean("vehicleAvailable")?"可用":"未刷新").append('\n');
      if(vehicle.getBoolean("available",false)) {
        out.append("车况：");
        if(vehicle.getBoolean("batteryPercentKnown"))out.append(vehicle.getInt("batteryPercent")).append("%");
        if(vehicle.getBoolean("rangeKmKnown"))out.append(" · ").append(vehicle.getInt("rangeKm")).append(" km");
        out.append('\n');
      }
      out.append("ColorOS 实验绑定：").append(state.getBoolean("enabled")?"已启用":"未启用").append("\n\n");
      status.setText(out.toString());
      enable.setEnabled(!state.getBoolean("enabled")&&state.getBoolean("signedIn")
          &&state.getBoolean("vehicleAvailable"));
      disable.setEnabled(state.getBoolean("requested"));
    } catch(Throwable failure) {
      status.setText("\nBridge 不可用："+failure.getClass().getSimpleName()+"\n");
      enable.setEnabled(false);disable.setEnabled(false);
    }
  }

  private void setBound(boolean bound) {
    try {
      Bundle in=new Bundle();in.putBoolean("enabled",bound);
      Bundle result=BridgeClient.call(this,"oppo-official-bind-set",in);
      if(!result.getBoolean("accepted",false))
        status.setText("未启用："+result.getString("reason","unknown"));
      else status.setText(bound
          ?"已启用。请返回负一屏/“我的车辆”并重新进入。"
          :"已取消实验绑定。");
      status.postDelayed(this::refresh,500L);
    } catch(Throwable failure) {
      status.setText("操作失败："+failure.getClass().getSimpleName());
    }
  }
}
