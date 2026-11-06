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
  private String lastTimerSignature="";
  private String timerSignature(){
    VehicleSnapshot s=VehicleSnapshotStore.loadChina(this);
    String vehicle=AppPreferences.get(this,"vehicle","");
    return (s==null?"--":AirRuntime.label(this,s,vehicle))+"|"
        +ClimateRuntime.remainingMinutes(s,System.currentTimeMillis())+"|"
        +CampModeManager.remainingCaption(this);
  }
  private final Runnable timerTick=()->{
    if(!running)return;
    String signature=timerSignature();
    boolean changed=!signature.equals(lastTimerSignature);
    boolean redrawEachMinute="minute".equals(AppPreferences.get(this,"refresh_local_redraw","event"));
    if(changed||redrawEachMinute){
      NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
      nm.notify(ID,notification(this));WidgetRenderer.refreshAll(this);
      if(OppoDeviceIntegration.enabled(this))OppoCardSummarySettings.changed(this);
    }
    lastTimerSignature=signature;OppoFluidCloud.updateFromCache(this);scheduleTimerTick();
  };
  private void scheduleTimerTick(){timerHandler.removeCallbacks(timerTick);VehicleSnapshot v=VehicleSnapshotStore.loadChina(this);String vehicle=AppPreferences.get(this,"vehicle","");long now=System.currentTimeMillis();Long air=AirRuntime.remaining(this,v,vehicle,now);Integer climate=ClimateRuntime.remainingMinutes(v,now);boolean ticking=CampModeManager.active(this)||(air!=null&&air>0)||(climate!=null&&climate>0)||(v!=null&&"开启".equals(v.climate)&&climate==null&&ClimateRuntime.estimatedRemaining(this,vehicle,now)>0);if(ticking)timerHandler.postDelayed(timerTick,60_000L);}

  public static void apply(Context c) {
    if (AppPreferences.enabled(c)) {
      c.startForegroundService(new Intent(c, ResidentService.class));
    } else c.stopService(new Intent(c, ResidentService.class));
    scheduleUpdates(c);
  }

  public static void scheduleUpdates(Context c) {
    JobScheduler jobs = (JobScheduler) c.getSystemService(JOB_SCHEDULER_SERVICE);
    int minutes=RefreshPolicy.background(c,AppPreferences.enabled(c),WidgetRenderer.hasWidgets(c));
    ParkingRefreshJob.schedule(c);
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
    OppoFluidCloud.updateFromCache(c);
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
    boolean camping=CampModeManager.active(c),pending=CampModeManager.pendingStop(c);
    String title=v==null?tr(c,"PsAssistant · 未连接","PsAssistant · Offline")
        :VehicleSnapshot.metric(v.batteryPercent,"%")+" · "+tr(c,"续航 ","Range ")+VehicleSnapshot.metric(v.rangeKm,"km");
    String climate=v==null?tr(c,"未知","Unknown"):
        "开启".equals(v.climate)?tr(c,"运行中","Running"):"关闭".equals(v.climate)?tr(c,"关闭","Off"):tr(c,"待确认","Unconfirmed");
    String vehicle=AppPreferences.get(c,"vehicle","");
    StringBuilder detail=new StringBuilder(tr(c,"空调 ","Climate ")).append(climate);
    if(v!=null&&"开启".equals(v.climate)){
      Integer remaining=ClimateRuntime.remainingMinutes(v,System.currentTimeMillis());
      if(remaining==null){long estimate=ClimateRuntime.estimatedRemaining(c,vehicle,System.currentTimeMillis());
        if(estimate>0)remaining=(int)((estimate+59999L)/60000L);}
      if(remaining!=null&&remaining>0)detail.append(tr(c," · 剩余 "," · left ")).append(remaining).append("m");
    }
    if(camping){
      CampSession session=CampModeManager.session(c);
      if(session!=null){long remaining=Math.max(0,session.endsAt-System.currentTimeMillis());
        detail.append(tr(c," · 托管剩余 "," · camping left ")).append((remaining+59999L)/60000L).append("m");}
    }
    if(pending)detail.append(tr(c," · 提示：托管停止待核实"," · Notice: camping stop unconfirmed"));
    else if(v!=null&&VehicleCondition.problem(v,true))
      detail.append(tr(c," · 提示：请检查车辆"," · Notice: check vehicle"));
    String timestamps=tr(c,"数据更新 ","Data updated ")+(v==null?"—":time(v.timestampMillis));
    PendingIntent open =
        PendingIntent.getActivity(
            c,
            0,
            new Intent(c, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    return OppoWatchIndicatorLab.decorateResident(c,new Notification.Builder(c, channel)
        .setSmallIcon(AppIcons.notificationSmall(c))
        .setContentTitle(title)
        .setContentText(detail.toString() + " · " + timestamps)
        .setStyle(new Notification.BigTextStyle().bigText(detail.toString() + "\n" + timestamps))
        .setContentIntent(open)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setShowWhen(false)
        .setCategory(Notification.CATEGORY_SERVICE)
        .setVisibility(Notification.VISIBILITY_PRIVATE))
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

