package io.github.polestarcn.oppobridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Local cache viewer. Only an explicit refresh or control requests vehicle work. */
public final class VehicleCardDetailActivity extends Activity {
  private TextView state, feedback;
  private final java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
  private final java.util.List<Button> buttons = new java.util.ArrayList<>();
  private final java.util.concurrent.atomic.AtomicBoolean reading = new java.util.concurrent.atomic.AtomicBoolean();
  private final java.util.concurrent.atomic.AtomicBoolean dirty = new java.util.concurrent.atomic.AtomicBoolean();
  private boolean busy, observing;
  private LinearLayout root;
  private final ContentObserver observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
    @Override public void onChange(boolean selfChange) { refresh(); }
  };

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ScrollView scroll = new ScrollView(this);
    scroll.setBackgroundColor(Color.rgb(15,17,19));
    root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    int p = dp(20);
    root.setPadding(p,p,p,p);
    scroll.addView(root);
    TextView title = text("Polestar · 车辆控制",24,Color.WHITE);
    root.addView(title);
    root.addView(text("设备卡连接表示桥接可用，不代表车辆实时在线",13,Color.LTGRAY));
    state = text("读取本地缓存…",18,Color.WHITE);
    state.setPadding(0,dp(24),0,dp(20));root.addView(state);
    feedback = text("打开页面只读取缓存；点击刷新才请求车辆数据",14,Color.rgb(230,205,120));
    feedback.setTextIsSelectable(true);root.addView(feedback);
    addRow("上锁","lock","解锁","unlock");
    addRow("开启空调","climate-start","关闭空调","climate-stop");
    addRow("开启净化","air-start","关闭净化","air-stop");
    addRow("开启尾门","trunk-open","关闭尾门","trunk-close");
    addButton(root,"手动刷新车辆",null,false);
    addButton(root,"恢复设备卡","__restore_card",false);
    setContentView(scroll);
  }

  private int dp(int value) { return (int)(value*getResources().getDisplayMetrics().density+0.5f); }
  private TextView text(String value,int size,int color) {
    TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(color);return view;
  }
  private void addRow(String left,String leftAction,String right,String rightAction) {
    LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
    root.addView(row);addButton(row,left,leftAction,true);addButton(row,right,rightAction,true);
  }
  private void addButton(LinearLayout parent,String label,String action,boolean half) {
    Button b=new Button(this);b.setText(label);b.setTextSize(16);b.setAllCaps(false);
    b.setTextColor(Color.WHITE);b.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(48,52,56)));
    b.setMinHeight(dp(56));
    LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(half?0:LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT,half?1:0);
    params.setMargins(dp(2),dp(5),dp(2),dp(5));parent.addView(b,params);buttons.add(b);
    b.setOnClickListener(v->{
      if(busy)return;
      if("unlock".equals(action)||"trunk-open".equals(action)||"trunk-close".equals(action)) {
        new AlertDialog.Builder(this).setTitle(label).setMessage("确认车辆周围安全后执行。请求受理不代表车辆已完成操作。")
            .setNegativeButton("取消",null).setPositiveButton("确认",(dialog,which)->submit(action)).show();
      } else submit(action);
    });
  }
  private void submit(String action) {
    if(busy||isFinishing()||isDestroyed())return;
    busy=true;for(Button b:buttons)b.setEnabled(false);feedback.setText("正在提交请求…");
    worker.execute(()->{
      String message;
      try {
        if("__restore_card".equals(action)) {
          getSharedPreferences("vehicle-card",MODE_PRIVATE).edit().remove("hidden").apply();
          getContentResolver().notifyChange(VehicleDeviceCardProvider.URI,null);message="设备卡已恢复";
        } else {
          Bundle result=action==null?BridgeClient.refresh(this):BridgeClient.command(this,action);
          message=result.getBoolean("accepted",false)?"请求已受理，等待车辆回读；请核对缓存时间和状态":"请求未受理："+result.getString("reason","unknown");
        }
      } catch(RuntimeException failure) {message="请求失败："+failure.getClass().getSimpleName();}
      final String resultMessage=message;
      runOnUiThread(()->{if(isFinishing()||isDestroyed())return;
        feedback.setText(resultMessage);busy=false;for(Button b:buttons)b.setEnabled(true);refresh();
      });
    });
  }
  private void refresh() {
    if(isFinishing()||isDestroyed()||worker.isShutdown())return;
    dirty.set(true);
    if(!reading.compareAndSet(false,true))return;
    dirty.set(false);
    worker.execute(()->{
      String display;
      try {display=formatSnapshot(BridgeClient.snapshot(this));}
      catch(RuntimeException failure) {display="缓存读取失败："+failure.getClass().getSimpleName();}
      final String value=display;
      runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())state.setText(value);});
      reading.set(false);
      if(dirty.get())runOnUiThread(this::refresh);
    });
  }
  static String formatSnapshot(Bundle s) {
    if(!s.getBoolean("available",false))return "车辆不可用："+s.getString("reason","请先登录并选中车辆");
    long at=s.getLong("capturedAt",0L);
    String time=at>0?new java.text.SimpleDateFormat("MM-dd HH:mm:ss",java.util.Locale.getDefault()).format(new java.util.Date(at)):"时间未知";
    return s.getString("name","Polestar")+"\n\n"+
        (s.getBoolean("batteryPercentKnown")?s.getInt("batteryPercent")+"%":"--")+"   ·   "+
        (s.getBoolean("rangeKmKnown")?s.getInt("rangeKm")+" km":"--")+"\n\n"+
        "门锁  "+s.getString("lock","--")+"     空调  "+s.getString("climate","--")+"\n"+
        "尾门  "+s.getString("tailgate","--")+"     净化  "+s.getString("air","--")+"\n"+
        "充电  "+s.getString("charging","--")+"\n"+
        "里程  "+(s.getBoolean("odometerKmKnown")?s.getInt("odometerKm")+" km":"--")+"\n\n缓存更新  "+time;
  }
  @Override protected void onStart() {
    super.onStart();
    try {getContentResolver().registerContentObserver(BridgeClient.URI,false,observer);observing=true;}
    catch(RuntimeException unavailable) {observing=false;}
    refresh();
  }
  @Override protected void onStop() {
    if(observing){getContentResolver().unregisterContentObserver(observer);observing=false;}
    super.onStop();
  }
  @Override protected void onDestroy() {worker.shutdownNow();super.onDestroy();}
}
