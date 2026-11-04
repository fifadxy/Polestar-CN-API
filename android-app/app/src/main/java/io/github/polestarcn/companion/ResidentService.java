package io.github.polestarcn.companion;

import android.app.*;
import android.app.job.*;
import android.content.*;
import android.os.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Optional foreground presence. Timer presentation reads cache and does not wake the car. */
public final class ResidentService extends Service {
  private static final String CHANNEL = "vehicle_status_v1";
  private static final int ID = 41;
  private static boolean running;
  private static ResidentService active;
  private final Handler timerHandler=new Handler(Looper.getMainLooper());
  private final Runnable timerTick=()->{if(!running)return;NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);nm.notify(ID,notification(this));WidgetRenderer.refreshAll(this);scheduleTimerTick();};
  private void scheduleTimerTick(){timerHandler.removeCallbacks(timerTick);VehicleSnapshot v=VehicleSnapshotStore.loadChina(this);String vehicle=AppPreferences.get(this,"vehicle","");long now=System.currentTimeMillis();Long air=AirRuntime.remaining(this,v,vehicle,now);Integer climate=ClimateRuntime.remainingMinutes(v,now);boolean ticking=(air!=null&&air>0)||(climate!=null&&climate>0)||(v!=null&&"开启".equals(v.climate)&&climate==null&&ClimateRuntime.estimatedRemaining(this,vehicle,now)>0);if(ticking)timerHandler.postDelayed(timerTick,60_000L);}

  public static void apply(Context c) {
    if (AppPreferences.enabled(c)) {
      c.startForegroundService(new Intent(c, ResidentService.class));
    } else c.stopService(new Intent(c, ResidentService.class));
    scheduleUpdates(c);
  }

  public static void scheduleUpdates(Context c) {
    JobScheduler jobs = (JobScheduler) c.getSystemService(JOB_SCHEDULER_SERVICE);
    int minutes=RefreshPolicy.background(c,AppPreferences.enabled(c),WidgetRenderer.hasWidgets(c));
    if (minutes>0) {
      JobInfo pending=jobs==null?null:jobs.getPendingJob(41);
      int network=AppPreferences.visible(c,"refresh_wifi_only",false)?JobInfo.NETWORK_TYPE_UNMETERED:JobInfo.NETWORK_TYPE_ANY;
      boolean plugged=AppPreferences.visible(c,"refresh_only_charging",false);
      if (jobs != null && (pending==null||pending.getIntervalMillis()!=minutes*60_000L||pending.getNetworkType()!=network||pending.isRequireCharging()!=plugged))
        jobs.schedule(
            new JobInfo.Builder(41, new ComponentName(c, VehicleRefreshJob.class))
                .setPersisted(true)
                .setRequiredNetworkType(network)
                .setRequiresCharging(plugged)
                .setRequiresBatteryNotLow(true)
                .setPeriodic(minutes * 60_000L, 5 * 60_000L)
                .build());
    } else {
      if (jobs != null) jobs.cancel(41);
    }
  }

  public static void update(Context c) {
    if (running) {
      NotificationManager nm = (NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);
      nm.notify(ID, notification(c));
      ResidentService service=active;if(service!=null)service.timerHandler.post(service::scheduleTimerTick);
    }
  }

  private static String time(long t) {
    return t <= 0 ? "—" : new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(t));
  }

  private static String tr(Context c, String zh, String en) {
    return AppPreferences.tr(c, zh, en);
  }

  private static Notification notification(Context c) {
    NotificationManager nm = (NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);
    // Separate channels: importance is immutable after creation. OEM/user settings remain authoritative.
    boolean showIcon=AppPreferences.visible(c,"notification_status_icon",true);String channel=showIcon?"vehicle_status_icon_v2":"vehicle_status_quiet_v2";
    NotificationChannel ch =
        new NotificationChannel(
            channel, showIcon?tr(c,"车辆状态 · 状态栏图标","Vehicle status · status icon"):tr(c,"车辆状态 · 静默通知","Vehicle status · silent"), showIcon?NotificationManager.IMPORTANCE_LOW:NotificationManager.IMPORTANCE_MIN);
    ch.setSound(null, null);
    ch.enableVibration(false);
    ch.setShowBadge(false);
    nm.createNotificationChannel(ch);
    VehicleSnapshot v = VehicleSnapshotStore.loadChina(c);
    String title =
        v == null
            ? tr(c, "PsAssistant · 未连接", "PsAssistant · Offline")
            : VehicleSnapshot.metric(v.batteryPercent, "%")
                + " · "
                + AppPreferences.status(c, v.charging);
    String detail =
        v == null
            ? tr(c, "点击连接车辆", "Tap to connect")
            : tr(c, "空调 ", "Climate ")
                + ClimateRuntime.label(c, v, AppPreferences.get(c, "vehicle", ""))
                + " · "
                + tr(c, "净化 ", "Air ")
                + AirRuntime.label(c, v, AppPreferences.get(c,"vehicle",""))
                + " · " + tr(c,"门锁 ","Lock ") + AppPreferences.status(c,v.lock)
                + " · " + tr(c,"尾门 ","Trunk ") + AppPreferences.status(c,v.tailgate)
                + " · " + tr(c,"车窗 ","Windows ") + AppPreferences.status(c,v.windows);
    String timestamps = tr(c, "数据更新 ", "Data updated ")
        + (v == null ? "—" : time(v.timestampMillis));
    PendingIntent open =
        PendingIntent.getActivity(
            c,
            0,
            new Intent(c, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    return new Notification.Builder(c, channel)
        .setSmallIcon(AppIcons.notificationSmall(c))
        .setContentTitle(title)
        .setContentText(detail + " · " + timestamps)
        .setStyle(new Notification.BigTextStyle().bigText(detail + "\n" + timestamps))
        .setContentIntent(open)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setShowWhen(false)
        .setCategory(Notification.CATEGORY_SERVICE)
        .setVisibility(Notification.VISIBILITY_PRIVATE)
        .build();
  }

  @Override
  public int onStartCommand(Intent i, int flags, int startId) {
    if (!AppPreferences.enabled(this)) {
      stopSelf();
      return START_NOT_STICKY;
    }
    startForeground(ID, notification(this));
    running = true;
    active=this;scheduleTimerTick();
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    running = false;
    if(active==this)active=null;timerHandler.removeCallbacks(timerTick);
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent i) {
    return null;
  }
}

