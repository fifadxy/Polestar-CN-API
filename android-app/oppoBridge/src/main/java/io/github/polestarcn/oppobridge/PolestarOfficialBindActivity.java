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
    note.setText("该入口把真实 PsAssistant 登录/车辆状态映射到 ColorOS 车辆 Provider，并在兼容固件中注入 Polestar 品牌行。"
        +"登录且已发现真实车辆后默认自动映射，不再要求额外本地确认；手动关闭后会保持关闭。"
        +"不会写 SceneService 数据库，不会伪造蓝牙数字钥匙或跳过车辆认证。");
    note.setTextSize(14f);
    note.setPadding(0,p/2,0,p/2);
    root.addView(note);

    status=new TextView(this);
    status.setTextSize(15f);
    root.addView(status);

    enable=new Button(this);
    enable.setText("启用 Polestar 系统车辆入口");
    enable.setAllCaps(false);
    enable.setOnClickListener(v->setBound(true));
    root.addView(enable);

    disable=new Button(this);
    disable.setText("关闭 Polestar 系统车辆入口");
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
      Bundle system=BridgeClient.call(this,"system-vehicle-status",null);
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
      out.append("Polestar 系统入口：").append(state.getBoolean("enabled")?"已映射":"未映射").append('\n');
      out.append("额外本地验证：").append(state.getBoolean("extraProviderVerificationRequired",true)?"需要":"不需要").append('\n');
      out.append("Provider 目录：").append(system.getBoolean("providerCatalogContainsPolestar",false)?"已出现 Polestar":"尚未出现 Polestar").append('\n');
      out.append("AssistantScreen Hook：").append(system.getBoolean("assistantHookSeen",false)?"已观察到":"未观察到").append(" · ").append(system.getString("assistantVersion","")).append('\n');
      out.append("CarLink Hook：").append(system.getBoolean("carLinkHostHookSeen",false)?"已观察到":"未观察到").append(" · ").append(system.getString("carLinkVersion","")).append("\n\n");
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
          ?"已启用。请重启负一屏/CarLink 进程，然后重新进入“我的车辆”。"
          :"已关闭 Polestar 系统车辆入口。");
      status.postDelayed(this::refresh,500L);
    } catch(Throwable failure) {
      status.setText("操作失败："+failure.getClass().getSimpleName());
    }
  }
}