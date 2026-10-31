package io.github.polestarcn.oppobridge;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Native content inside our MyDevices home card. No cloud request on bind/show. */
final class NativeVehicleHomeView extends LinearLayout {
  private final TextView summary, feedback;
  private final Button[] controls=new Button[4];
  private final String[] actions=new String[4];
  private final Button refresh;
  private final AtomicBoolean reading=new AtomicBoolean();
  private ExecutorService worker;
  private boolean active,busy,dirty;
  private Bundle snapshot=new Bundle();
  private int generation;
  private final ContentObserver observer=new ContentObserver(new Handler(Looper.getMainLooper())) {
    @Override public void onChange(boolean selfChange){readCache();}
  };
  NativeVehicleHomeView(Context context) {
    super(context);setOrientation(VERTICAL);int pad=dp(8);setPadding(pad,pad,pad,pad);
    boolean dark=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
    setBackgroundColor(dark?Color.rgb(25,27,29):Color.rgb(247,247,247));
    int ink=dark?Color.WHITE:Color.rgb(25,27,29);
    summary=new TextView(context,null,0);summary.setMinimumHeight(0);summary.setMinHeight(0);summary.setPadding(0,0,0,0);summary.setTextSize(12);summary.setIncludeFontPadding(false);summary.setTextColor(ink);addView(summary);
    for(int rowIndex=0;rowIndex<2;rowIndex++) {
      LinearLayout row=new LinearLayout(context);addView(row,new LayoutParams(-1,-2));
      for(int col=0;col<2;col++) {
        final int index=rowIndex*2+col;
        Button b=new Button(context,null,0);controls[index]=b;b.setTextSize(12);b.setAllCaps(false);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(40));b.setMinimumHeight(dp(40));b.setPadding(dp(3),0,dp(3),0);
        android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(dark?Color.rgb(48,51,54):Color.rgb(229,231,232));bg.setCornerRadius(dp(6));b.setBackground(bg);b.setIncludeFontPadding(false);b.setSingleLine(true);b.setTextColor(ink);
        LayoutParams params=new LayoutParams(0,-2,1);params.setMargins(dp(2),0,dp(2),0);row.addView(b,params);
        b.setOnClickListener(v->submit(actions[index],b.getText().toString()));
      }
    }
    refresh=new Button(context,null,0);refresh.setText("刷新车况");refresh.setTextSize(12);refresh.setMinHeight(dp(28));refresh.setMinimumHeight(dp(28));refresh.setPadding(0,0,0,0);refresh.setBackgroundColor(Color.TRANSPARENT);refresh.setTextColor(ink);refresh.setIncludeFontPadding(false);refresh.setAllCaps(false);refresh.setOnClickListener(v->submit(null,"刷新车况"));addView(refresh);
    feedback=new TextView(context,null,0);feedback.setMinimumHeight(0);feedback.setMinHeight(0);feedback.setPadding(0,0,0,0);feedback.setIncludeFontPadding(false);feedback.setTextSize(10);feedback.setTextColor(ink);addView(feedback);bindSnapshot(new Bundle());
  }
  static boolean isOurCard(String pkg,String type,String id) {
    return ("io.github.polestarcn.oppobridge".equals(pkg)||"io.github.polestarcn.companion".equals(pkg))
        &&"CAR".equals(type)&&id!=null&&id.startsWith("psassistant_");
  }
  private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
  @Override protected void onAttachedToWindow(){
    super.onAttachedToWindow();active=true;generation++;worker=Executors.newSingleThreadExecutor();
    try{getContext().getContentResolver().registerContentObserver(BridgeClient.URI,true,observer);}catch(RuntimeException ignored){}
    readCache();
  }
  @Override protected void onDetachedFromWindow(){
    active=false;generation++;busy=false;reading.set(false);
    try{getContext().getContentResolver().unregisterContentObserver(observer);}catch(RuntimeException ignored){}
    if(worker!=null)worker.shutdownNow();worker=null;super.onDetachedFromWindow();
  }
  private void readCache(){
    if(!active||worker==null)return;
    dirty=true;if(!reading.compareAndSet(false,true))return;dirty=false;final int session=generation;
    worker.execute(()->{
      Bundle data;
      try{data=BridgeClient.snapshot(getContext());}catch(RuntimeException e){data=new Bundle();}
      final Bundle result=data;
      post(()->{if(!active||session!=generation)return;reading.set(false);bindSnapshot(result);if(dirty)readCache();});
    });
  }
  void bindSnapshot(Bundle data){
    snapshot=new Bundle(data);
    String battery=data.getBoolean("batteryPercentKnown")?data.getInt("batteryPercent")+"%":"—";
    String range=data.getBoolean("rangeKmKnown")?data.getInt("rangeKm")+" km":"—";
    long time=data.getLong("capturedAt",0);
    String stamp=time>0?android.text.format.DateFormat.format("MM-dd HH:mm",time).toString():"时间未知";
    summary.setText("Polestar · "+battery+" · "+range+"\n缓存 "+stamp);
    boolean locked="已上锁".equals(data.getString("lock"))||"已锁".equals(data.getString("lock"));
    boolean climate="开启".equals(data.getString("climate"));
    boolean air="开启".equals(data.getString("air"));
    boolean trunk="已打开".equals(data.getString("tailgate"))||"未关严".equals(data.getString("tailgate"));
    actions[0]=locked?"unlock":"lock";controls[0].setText(locked?"解锁":"上锁");
    actions[1]=climate?"climate-stop":"climate-start";controls[1].setText(climate?"关闭空调":"开启空调");
    actions[2]=air?"air-stop":"air-start";controls[2].setText(air?"关闭净化":"空气净化");
    actions[3]=trunk?"trunk-close":"trunk-open";controls[3].setText(trunk?"关闭尾门":"开启尾门");
    boolean enabled=data.getBoolean("available")&&!data.getBoolean("demo")&&data.getBoolean("cardEnabled",true);
    for(Button b:controls)b.setEnabled(enabled&&!busy);
    refresh.setEnabled(data.getBoolean("cardEnabled",true)&&!busy);
    if(enabled&&feedback.getText().toString().startsWith("请在 PsAssistant"))feedback.setText("");
    if(!enabled)feedback.setText("请在 PsAssistant 登录并刷新车况，或检查集成开关");
  }
  private void submit(String action,String label){
    if(!active||busy||worker==null)return;
    if("unlock".equals(action)||"trunk-open".equals(action)||"trunk-close".equals(action)) {
      new android.app.AlertDialog.Builder(getContext()).setTitle(label).setNegativeButton("取消",null)
          .setPositiveButton("确认",(d,w)->dispatch(action)).show();
    }else dispatch(action);
  }
  private void dispatch(String action){
    if(!active||busy||worker==null)return;
    busy=true;bindSnapshot(snapshot);feedback.setText("正在提交…");final int session=generation;
    worker.execute(()->{
      String message;
      try{
        Bundle r=action==null?BridgeClient.refresh(getContext()):BridgeClient.command(getContext(),action);
        message=r.getBoolean("accepted")?"请求已受理，等待车况回读":"未受理："+r.getString("reason","unknown");
      }catch(RuntimeException e){message="桥接暂不可用，请检查 PsAssistant";}
      final String result=message;
      post(()->{if(!active||session!=generation)return;busy=false;bindSnapshot(snapshot);feedback.setText(result);readCache();});
    });
  }
}
