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
  // All scenario states are in-memory only. Never forward them to a real vehicle coordinator.
  private String chargePhase="auto",dataPhase="live",verifyResult="confirmed",event="";
  private boolean refreshing;
  private final Map<String,String> mockCommands=new LinkedHashMap<>();
  private String tr(String zh,String en){return AppPreferences.tr(this,zh,en);}
  private int dp(int x){return (int)(getResources().getDisplayMetrics().density*x+.5f);}
  private EffectStateController.Mode mode(){
    if(!"live".equals(dataPhase))return EffectStateController.Mode.OFF;
    if("active".equals(chargePhase)||"auto".equals(chargePhase)&&Boolean.TRUE.equals(toggles.get("charge")))
      return EffectStateController.Mode.CHARGING;
    if(Boolean.TRUE.equals(toggles.get("cool")))return EffectStateController.Mode.COOLING;
    if(Boolean.TRUE.equals(toggles.get("heat")))return EffectStateController.Mode.HEATING;
    if(Boolean.TRUE.equals(toggles.get("air")))return EffectStateController.Mode.PURIFICATION;
    if(Boolean.TRUE.equals(toggles.get("vent")))return EffectStateController.Mode.VENTILATION;
    return EffectStateController.Mode.OFF;
  }
  private String enabled(String key) {
    return Boolean.TRUE.equals(toggles.get(key))?tr("开启","On"):tr("关闭","Off");
  }
  private void update(){
    hero.setPreviewMode(mode());
    StringBuilder line=new StringBuilder(tr("【离线模拟】没有任何真实车辆请求","[OFFLINE SIMULATION] No vehicle requests"));
    line.append("\nSOC ").append(soc).append("% · ")
        .append(tr("动效 ","Visual effect ")).append(mode());
    line.append("\n").append(tr("充电阶段 ","Charging phase ")).append(chargePhase)
        .append(tr(" · 数据状态 "," · Telemetry ")).append(dataPhase);
    line.append("\n").append(tr("刷新 ","Refresh ")).append(refreshing?tr("进行中","In progress"):tr("空闲","Idle"))
        .append(tr(" · 预设指令结果 "," · Command outcome ")).append(verifyResult);
    line.append("\n").append(tr("车门 ","Lock ")).append(enabled("lock"))
        .append(tr(" · 尾箱 "," · Trunk ")).append(enabled("trunk"))
        .append(tr(" · 车窗 "," · Windows ")).append(enabled("windows"));
    line.append("\n").append(tr("制冷 ","Cooling ")).append(enabled("cool"))
        .append(tr(" · 制热 "," · Heating ")).append(enabled("heat"))
        .append(tr(" · 通风 "," · Vent ")).append(enabled("vent"))
        .append(tr(" · 净化 "," · Air ")).append(enabled("air"));
    line.append("\n").append(tr("露营 ","Camp ")).append(enabled("camp"))
        .append(" · All Good ").append(enabled("allgood"))
        .append(tr(" · 保养告警 "," · Service ")).append(enabled("maintenance"))
        .append(tr(" · 停车位置 "," · Parking ")).append(enabled("location"));
    if(!mockCommands.isEmpty()){
      line.append("\n").append(tr("独立指令：","Independent commands: "));
      for(Map.Entry<String,String> entry:mockCommands.entrySet()){
        line.append(entry.getKey()).append(" ").append(entry.getValue()).append("  ");
      }
    }
    if(!event.isEmpty())line.append("\n").append(event);
    summary.setText(line.toString());
  }
  /** Instant scenario selector; never saves or modifies the real vehicle session. */
  private void choice(LinearLayout page,String heading,String[] labels,String[] values,
      java.util.function.Consumer<String> changed){
    TextView caption=new TextView(this);caption.setText(tr(heading,heading));caption.setTextSize(13);
    caption.setTextColor(PsaDesign.ink(AppPreferences.dark(this)));
    caption.setPadding(dp(8),dp(8),dp(8),dp(2));page.addView(caption);
    Spinner selector=new Spinner(this);
    ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labels){
      @Override public android.view.View getView(int pos,android.view.View view,android.view.ViewGroup parent){
        android.view.View result=super.getView(pos,view,parent);
        if(result instanceof TextView)((TextView)result).setTextColor(PsaDesign.ink(AppPreferences.dark(VehicleSimulationActivity.this)));
        return result;
      }
    };
    selector.setAdapter(adapter);
    selector.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
      public void onNothingSelected(android.widget.AdapterView<?> parent){}
      public void onItemSelected(android.widget.AdapterView<?> parent,android.view.View v,int position,long id){
        changed.accept(values[position]);update();
      }
    });
    page.addView(selector);
  }
  /** Commands are independent by type and synthetic; no backend call, even for "success". */
  private void simulate(String family){
    if(mockCommands.containsKey(family)){
      event=tr("【模拟】同类指令仍在确认中，未重复发送","[SIMULATION] Same family pending; duplicate suppressed");
      update();return;
    }
    mockCommands.put(family,tr("发送中","Sending"));event="";update();
    main.postDelayed(()->{
      if(!mockCommands.containsKey(family)||isFinishing())return;
      mockCommands.put(family,tr("待确认","Verifying"));update();
    },350);
    main.postDelayed(()->{
      if(!mockCommands.containsKey(family)||isFinishing())return;
      mockCommands.remove(family);
      event=tr("【模拟】","[SIMULATION] ")+family+" · "+verifyResult
          +tr("（仅本地结果，未发送任何指令）"," (local outcome only; no vehicle write)");
      update();
    },1900);
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
    hero=new VehicleHeroView(this,true);page.addView(hero,new LinearLayout.LayoutParams(-1,dp(225)));
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
    choice(page,tr("模拟充电阶段","Charging scenario"),
      new String[]{tr("跟随充电开关","Follow charging switch"),tr("准备连接","Preparing"),
        tr("正在充电","Charging"),tr("暂停充电","Paused"),tr("充电完成","Complete"),tr("充电故障","Fault")},
      new String[]{"auto","preparing","active","paused","complete","fault"},v->chargePhase=v);
    choice(page,tr("模拟数据来源","Data freshness scenario"),
      new String[]{tr("实时数据","Live"),tr("缓存数据","Cached"),tr("已过期","Stale"),tr("离线/未知","Offline / unknown")},
      new String[]{"live","cached","stale","offline"},v->dataPhase=v);
    choice(page,tr("模拟回读结果","Command readback outcome"),
      new String[]{tr("车辆确认成功","Confirmed"),tr("结果未确认","Unconfirmed"),tr("车辆拒绝/失败","Failed"),tr("等待超时","Timeout")},
      new String[]{"confirmed","unconfirmed","failed","timeout"},v->verifyResult=v);
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
    for(String family:new String[]{"lock","climate","trunk","windows","air","signal"}){
      Button operation=new Button(this);
      operation.setText(tr("模拟发送独立指令 · ","Simulate independent command · ")+family);
      operation.setOnClickListener(v->simulate(family));
      page.addView(operation);
    }
    Button horn=new Button(this);horn.setText(tr("模拟鸣笛／闪灯（无车辆请求）","Simulate honk/flash (no request)"));
    horn.setOnClickListener(v->simulate("signal"));
    page.addView(horn);
    Button refresh=new Button(this);refresh.setText(tr("模拟刷新与指令等待（可继续操作其他开关）","Simulate refresh/verification without blocking controls"));
    refresh.setOnClickListener(v->{
      refreshing=true;event=tr("【模拟】独立刷新中，其他指令仍可使用","[SIMULATION] Refresh independent of other commands");
      update();
      main.postDelayed(()->{refreshing=false;event=tr("【模拟】状态刷新完成","[SIMULATION] Refresh complete");update();},1600);
    });
    page.addView(refresh);
    update();
  }
  @Override protected void onResume(){super.onResume();AllGoodTextView.host(findViewById(android.R.id.content),true);}
  @Override protected void onPause(){AllGoodTextView.host(findViewById(android.R.id.content),false);super.onPause();}
  @Override protected void onDestroy(){main.removeCallbacksAndMessages(null);super.onDestroy();}
}
