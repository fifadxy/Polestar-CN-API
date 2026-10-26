package io.github.polestarcn.companion;

import android.app.*;
import android.app.job.*;
import android.content.*;
import android.os.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Optional foreground presence. No wake lock, timer loop, or remote writes. */
public final class ResidentService extends Service {
  private static final String CHANNEL = "vehicle_status_v1";
  private static final int ID = 41;
  private static boolean running;

  public static void apply(Context c) {
    if (AppPreferences.enabled(c)) {
      c.startForegroundService(new Intent(c, ResidentService.class));
    } else c.stopService(new Intent(c, ResidentService.class));
    scheduleUpdates(c);
  }

  public static void scheduleUpdates(Context c) {
    JobScheduler jobs = (JobScheduler) c.getSystemService(JOB_SCHEDULER_SERVICE);
    if (AppPreferences.enabled(c) || WidgetRenderer.hasWidgets(c)) {
      if (jobs != null && jobs.getPendingJob(41) == null)
        jobs.schedule(
            new JobInfo.Builder(41, new ComponentName(c, VehicleRefreshJob.class))
                .setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(30 * 60 * 1000L, 5 * 60 * 1000L)
                .build());
    } else {
      if (jobs != null) jobs.cancel(41);
    }
  }

  public static void update(Context c) {
    if (running) {
      NotificationManager nm = (NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);
      nm.notify(ID, notification(c));
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
    // LOW is silent and keeps the status icon; MIN may hide that icon on Android.
    NotificationChannel ch =
        new NotificationChannel(
            CHANNEL, tr(c, "车辆状态", "Vehicle status"), NotificationManager.IMPORTANCE_LOW);
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
                + AppPreferences.status(c, v.air);
    String timestamps = tr(c, "数据更新 ", "Data updated ")
        + (v == null ? "—" : time(v.timestampMillis));
    PendingIntent open =
        PendingIntent.getActivity(
            c,
            0,
            new Intent(c, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    return new Notification.Builder(c, CHANNEL)
        .setSmallIcon(R.drawable.ic_status)
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
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    running = false;
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent i) {
    return null;
  }
}

