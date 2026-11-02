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
  private final Button refresh,more;
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
    LinearLayout footer=new LinearLayout(context);addView(footer,new LayoutParams(-1,-2));
    refresh=new Button(context,null,0);refresh.setText("刷新");styleFooterButton(refresh,ink);refresh.setOnClickListener(v->submit(null,"刷新车况"));
    more=new Button(context,null,0);more.setText("更多控制");styleFooterButton(more,ink);more.setOnClickListener(v->openDetails());
    footer.addView(refresh,new LayoutParams(0,-2,1));footer.addView(more,new LayoutParams(0,-2,1));
    feedback=new TextView(context,null,0);feedback.setMinimumHeight(0);feedback.setMinHeight(0);feedback.setPadding(0,0,0,0);feedback.setIncludeFontPadding(false);feedback.setTextSize(10);feedback.setTextColor(ink);addView(feedback);bindSnapshot(new Bundle());
  }
  private void styleFooterButton(Button button,int ink){
    button.setTextSize(12);button.setMinHeight(dp(28));button.setMinimumHeight(dp(28));
    button.setPadding(0,0,0,0);button.setBackgroundColor(Color.TRANSPARENT);
    button.setTextColor(ink);button.setIncludeFontPadding(false);button.setAllCaps(false);
  }
  private void openDetails(){
    try{
      android.content.Intent intent=new android.content.Intent(getContext(),VehicleCardDetailActivity.class)
          .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
      getContext().startActivity(intent);
    }catch(RuntimeException failure){feedback.setText("无法打开完整控制面板");}
  }
  static boolean isOurCard(String pkg,String type,String id) {
    // The provider-generated psassistant_* id is unique to this bridge and survives MyDevices
    // package/type normalization. Treat it as the primary identity so a transformed CAR type
    // cannot silently fall back to the generic "View device" card.
    if(id!=null&&id.startsWith("psassistant_"))return true;
    return ("io.github.polestarcn.oppobridge".equals(pkg)||"io.github.polestarcn.companion".equals(pkg))
        &&type!=null&&"CAR".equalsIgnoreCase(type);
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
    String lock=data.getString("lock"),climateValue=data.getString("climate");
    String airValue=data.getString("air"),tailgate=data.getString("tailgate");
    boolean locked=stateIs(lock,"已上锁","已锁","LOCKED");
    boolean climate=stateIs(climateValue,"开启","运行中","ON","ACTIVE","RUNNING");
    boolean air=stateIs(airValue,"开启","运行中","ON","ACTIVE","RUNNING");
    boolean trunk=stateIs(tailgate,"已打开","未关严","OPEN","AJAR");
    boolean[] known={
      stateIs(lock,"已上锁","已锁","LOCKED","未锁","已解锁","未上锁","UNLOCKED"),
      stateIs(climateValue,"开启","运行中","ON","ACTIVE","RUNNING","关闭","未运行","OFF","INACTIVE","STOPPED"),
      stateIs(airValue,"开启","运行中","ON","ACTIVE","RUNNING","关闭","未运行","OFF","INACTIVE","STOPPED"),
      stateIs(tailgate,"已打开","未关严","OPEN","AJAR","已关闭","关闭","CLOSED")
    };
    actions[0]=locked?"unlock":"lock";controls[0].setText(locked?"解锁":"上锁");
    actions[1]=climate?"climate-stop":"climate-start";controls[1].setText(climate?"关闭空调":"开启空调");
    actions[2]=air?"air-stop":"air-start";controls[2].setText(air?"关闭净化":"净化");
    actions[3]=trunk?"trunk-close":"trunk-open";controls[3].setText(trunk?"关闭尾门":"开启尾门");
    boolean fresh=VehicleShortcutActivity.fresh(data.getLong("capturedAt",0L),System.currentTimeMillis());
    boolean enabled=data.getBoolean("available")&&!data.getBoolean("demo")
        &&data.getBoolean("cardEnabled",true)&&fresh;
    String[] unknownLabels={"车锁未知","空调未知","净化未知","尾门未知"};
    for(int i=0;i<controls.length;i++) {
      controls[i].setEnabled(enabled&&known[i]&&!busy);
      if(!known[i]) {actions[i]=null;controls[i].setText(unknownLabels[i]);}
    }
    refresh.setEnabled(data.getBoolean("cardEnabled",true)&&!busy);
    more.setEnabled(data.getBoolean("cardEnabled",true)&&!busy);
    String previous=feedback.getText().toString();
    if(enabled&&(previous.startsWith("请在 PsAssistant")||previous.startsWith("车况缓存已过期")))feedback.setText("");
    if(!data.getBoolean("available")||data.getBoolean("demo")||!data.getBoolean("cardEnabled",true))
      feedback.setText("请在 PsAssistant 登录并刷新车况，或检查集成开关");
    else if(!fresh)feedback.setText("车况缓存已过期，请先刷新");
  }
  private static boolean stateIs(String observed,String... known) {
    if(observed==null)return false;
    String value=observed.trim().toUpperCase(java.util.Locale.ROOT);
    for(String item:known)if(value.equals(item.toUpperCase(java.util.Locale.ROOT)))return true;
    return false;
  }
  private void submit(String action,String label){
    if(!active||busy||worker==null)return;
    if(action!=null&&!VehicleShortcutActivity.fresh(snapshot.getLong("capturedAt",0L),System.currentTimeMillis())) {
      bindSnapshot(snapshot);
      feedback.setText("车况缓存已过期，请先刷新");
      return;
    }
    dispatch(action); // Main app applies the selected per-action verification exactly once.
  }
  private void dispatch(String action){
    if(!active||busy||worker==null)return;
    busy=true;bindSnapshot(snapshot);feedback.setText("正在提交…");final int session=generation;
    final long before=snapshot.getLong("capturedAt",0L);
    worker.execute(()->{
      String message;
      Bundle latest=null;
      try{
        Bundle r=action==null?BridgeClient.refresh(getContext()):BridgeClient.command(getContext(),action);
        if(!r.getBoolean("accepted")) {
          message="未受理："+r.getString("reason","unknown");
        } else if(action==null) {
          message="刷新请求已受理，等待新车况";
        } else if(!VehicleCommandConfirmation.supports(action)) {
          message="请求已受理；该指令无可读回状态，请现场确认";
        } else {
          message="请求已受理，等待实车回读…";
          long deadline=android.os.SystemClock.elapsedRealtime()+32000L;
          while(active&&session==generation&&android.os.SystemClock.elapsedRealtime()<deadline) {
            try {Thread.sleep(1400L);} catch(InterruptedException interrupted) {Thread.currentThread().interrupt();break;}
            try {
              Bundle candidate=BridgeClient.snapshot(getContext());
              long captured=candidate.getLong("capturedAt",0L);
              if(captured>before&&VehicleCommandConfirmation.matches(candidate,action)) {
                latest=candidate;
                message=VehicleCommandConfirmation.successText(action);
                break;
              }
            } catch(RuntimeException ignored) {}
          }
          if(latest==null&&message.startsWith("请求已受理"))message="车辆未在 32 秒内确认，请稍后刷新核对";
        }
      }catch(RuntimeException e){message="桥接暂不可用，请检查 PsAssistant";}
      final String result=message;
      final Bundle confirmed=latest;
      post(()->{if(!active||session!=generation)return;busy=false;if(confirmed!=null)bindSnapshot(confirmed);else bindSnapshot(snapshot);feedback.setText(result);readCache();});
    });
  }
}
