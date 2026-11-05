package io.github.polestarcn.companion;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

/**
 * OPPO Watch Indicator compatibility lab, OFF by default. It does not connect to
 * the watch, send a car command, poll the vehicle or obtain OEM permissions.
 *
 * The official OPPO API is defined for notifications POSTED BY WATCH APPS.
 * A phone notification alone is NOT evidence of a real watch-face indicator.
 */
public final class OppoWatchIndicatorLab {
  public static final String PREF_ENABLED="oppo_watch_indicator_lab";
  public static final String PREF_RESIDENT="oppo_watch_indicator_resident";
  public static final String PREF_CATEGORY="oppo_watch_test_category";
  public static final String EXTRA_INDICATOR="show_heytap_indicator";
  public static final String CHANNEL="psassistant_oppo_watch_lab_v1";
  public static final int NOTIFICATION_ID=43044;
  public static final long TEST_WINDOW_MS=90_000L;
  private OppoWatchIndicatorLab(){}

  public static boolean enabled(Context c){return AppPreferences.visible(c,PREF_ENABLED,false);}
  public static boolean residentEnabled(Context c){return enabled(c)&&AppPreferences.visible(c,PREF_RESIDENT,false);}

  /** Only supported OPPO testing categories are selectable. No OEM status is implied. */
  public static String category(Context c){
    return "workout".equals(AppPreferences.get(c,PREF_CATEGORY,"navigation"))?"workout":"navigation";
  }
  public static boolean notificationsAllowed(Context c){
    NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
    if(n==null||!n.areNotificationsEnabled())return false;
    return Build.VERSION.SDK_INT<33||c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
  }
  private static NotificationManager manager(Context c){
    return (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
  }
  private static String tr(Context c,String zh,String en){return AppPreferences.tr(c,zh,en);}
  private static void channel(Context c){
    NotificationManager n=manager(c);if(n==null)return;
    NotificationChannel channel=new NotificationChannel(CHANNEL,
        tr(c,"OPPO Watch · Indicator 实验通知","OPPO Watch · Indicator lab"),
        NotificationManager.IMPORTANCE_LOW);
    channel.setSound(null,null);channel.enableVibration(false);channel.setShowBadge(false);
    n.createNotificationChannel(channel);
  }
  private static PendingIntent openApp(Context c){
    Intent open=new Intent(c,MainActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
    return PendingIntent.getActivity(c,NOTIFICATION_ID,open,
        PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  }
  /** Package visible for Robolectric assertions; is a one-shot synthetic category lab. */
  static Notification buildTest(Context c,String category){
    String chosen="workout".equals(category)?"workout":"navigation";
    Bundle extras=new Bundle();extras.putBoolean(EXTRA_INDICATOR,true);
    String scene="workout".equals(chosen)
        ?tr(c,"运动类别模拟","Workout-category simulation")
        :tr(c,"导航类别模拟","Navigation-category simulation");
    return new Notification.Builder(c,CHANNEL)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(tr(c,"OPPO Watch Indicator · 实验测试","OPPO Watch Indicator · lab test"))
        .setContentText(scene+tr(c," · 90 秒后自动结束"," · auto-cancels after 90 sec"))
        .setContentIntent(openApp(c))
        .setOnlyAlertOnce(true)
        .setShowWhen(false)
        .setVisibility(Notification.VISIBILITY_PRIVATE)
        .setCategory(chosen)
        .setOngoing(true)
        .setTimeoutAfter(TEST_WINDOW_MS)
        .addExtras(extras)
        .build();
  }
  public static String startTest(Context c){
    if(!enabled(c))return tr(c,"先在设置中启用 OPPO Watch 测试开关","Enable the watch lab switch first");
    if(!notificationsAllowed(c))return tr(c,"手机通知权限未开启","Phone notification permission disabled");
    channel(c);
    NotificationManager n=manager(c);
    if(n==null)return tr(c,"通知服务不可用","Notification service unavailable");
    NotificationChannel ch=n.getNotificationChannel(CHANNEL);
    if(ch==null||ch.getImportance()==NotificationManager.IMPORTANCE_NONE)
      return tr(c,"实验通知通道被禁用","Lab notification channel is blocked");
    n.notify(NOTIFICATION_ID,buildTest(c,category(c)));
    AppPreferences.set(c,"oppo_watch_last_test",String.valueOf(System.currentTimeMillis()));
    return tr(c,"已发送 90 秒手机侧 Indicator 模拟通知。本版本仅集成手机端，不能据此认定手表表盘已经显示。",
        "90-second phone-side test notification posted. This APK does not install a watch app and does not prove a watch-face indicator.");
  }
  public static void cancel(Context c){
    NotificationManager n=manager(c);if(n!=null)n.cancel(NOTIFICATION_ID);
  }
  /** Attach the vendor hint to a GENUINE service notification. Do not falsely label car status as navigation/workout. */
  public static Notification.Builder decorateResident(Context c,Notification.Builder builder){
    if(residentEnabled(c)){
      Bundle b=new Bundle();b.putBoolean(EXTRA_INDICATOR,true);
      builder.addExtras(b);
    }
    return builder;
  }
  public static String diagnostics(Context c){
    NotificationManager n=manager(c);
    String cat="none",tag="not-posted";boolean ongoing=false,extra=false;
    if(n!=null)for(StatusBarNotification event:n.getActiveNotifications()){
      if(event.getId()!=NOTIFICATION_ID||!c.getPackageName().equals(event.getPackageName()))continue;
      Notification notification=event.getNotification();cat=notification.category;
      ongoing=(notification.flags&Notification.FLAG_ONGOING_EVENT)!=0;
      extra=notification.extras.getBoolean(EXTRA_INDICATOR,false);
      tag="posted";break;
    }
    NotificationChannel ch=n==null?null:n.getNotificationChannel(CHANNEL);
    return tr(c,"实验开关：","Lab enabled: ")+enabled(c)
        +"\n"+tr(c,"手机通知许可：","Phone notification access: ")+notificationsAllowed(c)
        +"\n"+tr(c,"手机实验通知：","Phone lab notification: ")+tag
        +"\ncategory="+cat+"  ongoing="+ongoing+"  "+EXTRA_INDICATOR+"="+extra
        +"\n"+tr(c,"通道重要级别：","Channel importance: ")+(ch==null?"not-created":ch.getImportance())
        +"\n"+tr(c,"常驻通知附加标记：","Resident hint flag: ")+residentEnabled(c)
        +"\n"+tr(c,"手表实际接收：未检测（仅手机端集成）","Watch received: unverified (phone-side integration only)");
  }
}
