package io.github.polestarcn.companion;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.widget.*;
import java.util.LinkedHashMap;
import java.util.Map;
/** Strictly offline: no VehicleCoordinator instance, API, token, vehicle VIN, or write transport. */
public final class VehicleSimulationActivity extends Activity {
  private final Map<String,Boolean> toggles=new LinkedHashMap<>();
  private final Handler main=new Handler(Looper.getMainLooper());
  private VehicleHeroView hero;private TextView summary;private int soc=72;
  private String tr(String zh,String en){return AppPreferences.tr(this,zh,en);}
  private int dp(int x){return (int)(getResources().getDisplayMetrics().density*x+.5f);}
  private VehicleEffectSettings.Mode mode(){
    if(Boolean.TRUE.equals(toggles.get("charge")))return VehicleEffectSettings.Mode.CHARGING;
    if(Boolean.TRUE.equals(toggles.get("cool")))return VehicleEffectSettings.Mode.COOLING;
    if(Boolean.TRUE.equals(toggles.get("heat")))return VehicleEffectSettings.Mode.HEATING;
    if(Boolean.TRUE.equals(toggles.get("vent"))||Boolean.TRUE.equals(toggles.get("air")))return VehicleEffectSettings.Mode.VENTILATION;
    return VehicleEffectSettings.Mode.NONE;
  }
  private void update(){
    hero.preview(mode());
    summary.setText(tr("【模拟数据】绝不发送真实车辆指令","[SIMULATION] No real vehicle commands")
        +"\nSOC "+soc+"%  ·  "+tr("当前视觉效果 ","Active visual effect ")+mode()
        +"\n"+tr("车门 ","Doors ")+(Boolean.TRUE.equals(toggles.get("lock"))?tr("上锁","Locked"):tr("解锁","Unlocked"))
        +"  ·  "+tr("尾箱 ","Trunk ")+(Boolean.TRUE.equals(toggles.get("trunk"))?tr("开启","Open"):tr("关闭","Closed"))
        +"  ·  "+tr("车窗 ","Windows ")+(Boolean.TRUE.equals(toggles.get("windows"))?tr("开启","Open"):tr("关闭","Closed")));
  }
  private void addToggle(LinearLayout parent,String key,String zh,String en){
    Switch control=new Switch(this);control.setText(tr(zh,en));control.setTextSize(15);
    control.setTextColor(PsaDesign.ink(AppPreferences.dark(this)));control.setPadding(dp(8),dp(10),dp(8),dp(10));
    control.setChecked(Boolean.TRUE.equals(toggles.get(key)));
    control.setOnCheckedChangeListener((button,on)->{
      if((key.equals("cool")||key.equals("heat"))&&on){
        String other=key.equals("cool")?"heat":"cool";
        toggles.put(other,false);
        for(int i=0;i<parent.getChildCount();i++){android.view.View item=parent.getChildAt(i);
          if(item instanceof Switch&&other.equals(item.getTag()))((Switch)item).setChecked(false);}
      }
      toggles.put(key,on);update();
    });
    control.setTag(key);parent.addView(control);
  }
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    ScrollView scroll=new ScrollView(this);LinearLayout page=new LinearLayout(this);
    page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(14),dp(14),dp(14),dp(30));
    page.setBackgroundColor(PsaDesign.canvas(AppPreferences.dark(this)));scroll.addView(page);setContentView(scroll);
    Button back=new Button(this);back.setText(tr("退出离线测试模式","Exit offline test mode"));back.setOnClickListener(v->finish());page.addView(back);
    TextView title=new TextView(this);title.setText(tr("全功能测试模式 · 不连接车辆","Full scenario simulator · offline"));title.setTextSize(20);
    title.setTextColor(PsaDesign.ink(AppPreferences.dark(this)));page.addView(title);
    TextView note=new TextView(this);note.setText(tr("此页面所有开关仅修改本地模拟画面，无登录、API、远控或真实车辆写入。","All switches modify this local simulation only. No login, API calls, remote commands, or vehicle writes."));
    note.setTextSize(12);note.setTextColor(PsaDesign.muted(AppPreferences.dark(this)));page.addView(note);
    hero=new VehicleHeroView(this);page.addView(hero,new LinearLayout.LayoutParams(-1,dp(225)));
    summary=new TextView(this);summary.setTextSize(15);summary.setPadding(dp(6),dp(6),dp(6),dp(9));
    summary.setTextColor(PsaDesign.ink(AppPreferences.dark(this)));page.addView(summary);
    TextView battery=new TextView(this);battery.setText(tr("模拟电量 · SOC","Simulated battery SOC"));battery.setTextSize(14);
    battery.setTextColor(PsaDesign.ink(AppPreferences.dark(this)));page.addView(battery);
    SeekBar seek=new SeekBar(this);seek.setMax(100);seek.setProgress(soc);
    seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
      public void onProgressChanged(SeekBar b,int p,boolean user){soc=p;update();}
      public void onStartTrackingTouch(SeekBar b){}
      public void onStopTrackingTouch(SeekBar b){}
    });page.addView(seek);
    addToggle(page,"charge","充电中","Charging");
    addToggle(page,"cool","空调制冷","Climate cooling");
    addToggle(page,"heat","空调制热","Climate heating");
    addToggle(page,"vent","车辆通风","Ventilation");
    addToggle(page,"air","空气净化","Air purification");
    addToggle(page,"lock","车门上锁","Doors locked");
    addToggle(page,"trunk","尾箱开启","Trunk open");
    addToggle(page,"windows","车窗开启","Windows open");
    addToggle(page,"camp","露营托管","Camping mode");
    addToggle(page,"allgood","All Good · 健康状态","All Good status");
    addToggle(page,"maintenance","保养提醒","Service warning");
    addToggle(page,"location","停车位置可用","Parking location available");
    Button horn=new Button(this);horn.setText(tr("模拟鸣笛／闪灯（无车辆请求）","Simulate honk/flash (no request)"));
    horn.setOnClickListener(v->{summary.setText(tr("【模拟】鸣笛／闪灯指令已发送 · 无实车操作","[SIMULATION] Honk/flash sent locally only"));main.postDelayed(this::update,900);});
    page.addView(horn);
    Button refresh=new Button(this);refresh.setText(tr("模拟刷新与指令等待（可继续操作其他开关）","Simulate refresh/verification without blocking controls"));
    refresh.setOnClickListener(v->{summary.setText(tr("【模拟】刷新中 · 0%，其他开关仍可使用","[SIMULATION] Refreshing · 0%; other controls remain usable"));main.postDelayed(this::update,1000);});
    page.addView(refresh);
    update();
  }
  @Override protected void onDestroy(){main.removeCallbacksAndMessages(null);super.onDestroy();}
}
