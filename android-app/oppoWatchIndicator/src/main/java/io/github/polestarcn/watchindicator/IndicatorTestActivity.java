package io.github.polestarcn.watchindicator;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Self-contained on-watch verification of OPPO's documented Indicator notification. */
public final class IndicatorTestActivity extends Activity {
  static final String CHANNEL="psassistant_watch_indicator_lab_v1";
  static final String EXTRA_INDICATOR="show_heytap_indicator";
  static final long WINDOW_MS=90_000L;
  static final int ID=44;
  private TextView status;

  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
    LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
    body.setGravity(Gravity.CENTER);body.setPadding(30,30,30,30);body.setBackgroundColor(Color.BLACK);
    TextView heading=label("PsAssistant",20);body.addView(heading);
    TextView sub=label("OPPO Indicator · 手表端测试",13);body.addView(sub);
    addButton(body,"导航 Indicator · 90秒",()->send("navigation"));
    addButton(body,"运动 Indicator · 90秒",()->send("workout"));
    addButton(body,"取消测试",this::stop);
    status=label("未开始 · 不连接手机或车辆",12);body.addView(status);
    scroll.addView(body);setContentView(scroll);
  }
  private TextView label(String text,int sp){
    TextView tv=new TextView(this);tv.setText(text);tv.setTextColor(Color.WHITE);tv.setTextSize(sp);
    tv.setGravity(Gravity.CENTER);tv.setPadding(4,8,4,12);return tv;
  }
  private void addButton(LinearLayout parent,String label,Runnable callback){
    Button button=new Button(this);button.setText(label);button.setAllCaps(false);
    parent.addView(button,new LinearLayout.LayoutParams(-1,-2));button.setOnClickListener(v->callback.run());
  }
  static Notification create(Activity activity,String category){
    String value="workout".equals(category)?"workout":"navigation";
    Bundle props=new Bundle();props.putBoolean(EXTRA_INDICATOR,true);
    PendingIntent open=PendingIntent.getActivity(activity,ID,new Intent(activity,IndicatorTestActivity.class),
      PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    return new Notification.Builder(activity,CHANNEL)
      .setSmallIcon(R.drawable.ic_indicator)
      .setContentTitle("PsAssistant · OPPO Indicator 测试")
      .setContentText("workout".equals(value)?"运动类别模拟，90秒结束":"导航类别模拟，90秒结束")
      .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
      .setShowWhen(false).setCategory(value).addExtras(props)
      .setVisibility(Notification.VISIBILITY_PRIVATE).setTimeoutAfter(WINDOW_MS).build();
  }
  private void send(String category){
    NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    if(manager==null){status.setText("通知服务不可用");return;}
    if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
      requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);
      status.setText("请授权后再次点击测试");return;
    }
    NotificationChannel channel=new NotificationChannel(CHANNEL,"PsAssistant · 手表指示器测试",NotificationManager.IMPORTANCE_LOW);
    channel.setSound(null,null);channel.enableVibration(false);manager.createNotificationChannel(channel);
    if(!manager.areNotificationsEnabled()||manager.getNotificationChannel(CHANNEL).getImportance()==NotificationManager.IMPORTANCE_NONE){
      status.setText("通知权限/通道已关闭");return;
    }
    manager.notify(ID,create(this,category));
    status.setText("已发出 " + category + " 实验通知。返回表盘检查图标；90秒后自动结束。不发送车辆指令。");
  }
  private void stop(){
    NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    if(manager!=null)manager.cancel(ID);
    status.setText("测试已取消");
  }
}
