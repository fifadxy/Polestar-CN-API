package io.github.polestarcn.companion;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import io.github.polestarcn.companion.auth.SecureCampStore;

public final class CampModeManager {
  private static final String PREF = "camp_status";
  private static final int PERIODIC = 72, IMMEDIATE = 73, EXPIRY = 74;
  private CampModeManager() {}
  public static CampSession session(Context c) { return SecureCampStore.load(c); }
  public static String phase(Context c) { return c.getSharedPreferences(PREF, 0).getString("phase", "STOPPED"); }
  public static boolean active(Context c) { return "ACTIVE".equals(phase(c)) && session(c) != null; }
  public static boolean current(Context c, String id) {
    CampSession s = session(c); return s != null && s.id.equals(id);
  }
  public static int interval(Context c) { return number(c, "camp_interval", 31, 20, 60); }
  public static int minimum(Context c) { return number(c, "camp_minimum", 10, 5, 90); }
  private static int number(Context c, String key, int fallback, int min, int max) {
    try { return Math.max(min, Math.min(max, Integer.parseInt(AppPreferences.get(c, key, "" + fallback)))); }
    catch (NumberFormatException ignored) { return fallback; }
  }
  public static synchronized void start(Context c, CampSession s) throws Exception {
    if (active(c) || "STOPPING".equals(phase(c))) throw new IllegalStateException("Camping already active");
    SecureCampStore.save(c, s);
    c.getSharedPreferences(PREF, 0).edit().putString("phase", "ACTIVE")
        .putString("result", "QUEUED").putLong("checked_at", 0).commit();
    try { schedule(c); }
    catch (RuntimeException e) { deactivate(c, s.id, "SCHEDULE_FAILED"); finishCheck(c, s.id); throw e; }
  }
  public static synchronized boolean requestStop(Context c) {
    return requestStop(c, "MANUAL");
  }
  private static synchronized boolean requestStop(Context c, String reason) {
    CampSession s = session(c);
    if (s == null || !active(c)) return false;
    c.getSharedPreferences(PREF, 0).edit().putString("phase", "STOPPING").putString("stop_reason", reason).commit();
    cancelTasks(c);
    try { schedule(c); } catch (RuntimeException e) { DiagnosticLog.add("Camping stop task not scheduled"); }
    return true;
  }
  public static synchronized boolean protectBattery(Context c, String vin, VehicleSnapshot state) {
    CampSession s = session(c);
    return s != null && s.vin.equals(vin) && state != null && !state.isDemo && "LIVE".equals(state.source)
        && state.batteryPercent != null && state.batteryPercent >= 0 && state.batteryPercent < s.minimumBattery
        && requestStop(c, "LOW_BATTERY");
  }
  public static String stopReason(Context c) { return c.getSharedPreferences(PREF, 0).getString("stop_reason", "MANUAL"); }
  public static synchronized void deactivate(Context c, String id, String reason) {
    if (!current(c, id)) return;
    c.getSharedPreferences(PREF, 0).edit().putString("phase", "STOPPED").putString("result", reason).commit();
  }
  public static void finishCheck(Context c, String id) {
    if (current(c, id) && "STOPPED".equals(phase(c))) cancelTasks(c);
  }
  public static void report(Context c, String id, String result) {
    if (!current(c, id)) return;
    c.getSharedPreferences(PREF, 0).edit().putString("result", result)
        .putLong("checked_at", System.currentTimeMillis()).apply();
    ResidentService.update(c);
  }
  public static void clear(Context c) {
    cancelTasks(c);
    c.getSharedPreferences(PREF, 0).edit().clear().commit();
    SecureCampStore.clear(c);
  }
  private static JobScheduler jobs(Context c) { return (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE); }
  private static void cancelTasks(Context c) {
    JobScheduler jobs = jobs(c);
    if (jobs != null) { jobs.cancel(PERIODIC); jobs.cancel(IMMEDIATE); jobs.cancel(EXPIRY); }
  }
  public static void schedule(Context c) {
    CampSession s = session(c);
    if (s == null || !(active(c) || "STOPPING".equals(phase(c)))) return;
    JobScheduler jobs = jobs(c);
    if (jobs == null) throw new IllegalStateException("Background scheduler unavailable");
    ComponentName service = new ComponentName(c, CampModeJob.class);
    if (active(c)) {
      long interval = s.intervalMinutes * 60_000L;
      int periodic = jobs.schedule(new JobInfo.Builder(PERIODIC, service).setPersisted(true)
          .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(interval, 5 * 60_000L).build());
      int end = jobs.schedule(new JobInfo.Builder(EXPIRY, service).setPersisted(true)
          .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
          .setMinimumLatency(Math.max(0, s.endsAt - System.currentTimeMillis())).build());
      if (periodic != JobScheduler.RESULT_SUCCESS || end != JobScheduler.RESULT_SUCCESS)
        throw new IllegalStateException("Background task was not scheduled");
    }
    if (jobs.schedule(new JobInfo.Builder(IMMEDIATE, service).setPersisted(true)
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setMinimumLatency(0).build()) != JobScheduler.RESULT_SUCCESS)
      throw new IllegalStateException("Camping check was not scheduled");
  }
  public static String status(Context c) {
    String raw = c.getSharedPreferences(PREF, 0).getString("result", "STOPPED");
    String zh, en;
    switch (raw) {
      case "QUEUED": zh = "等待首次检查"; en = "First check queued"; break;
      case "RUNNING": zh = "空调托管中"; en = "Climate maintained"; break;
      case "WAITING_FOR_BATTERY": zh = "电量数据未知或过期，暂停启动"; en = "Waiting for current battery data"; break;
      case "STATE_UNCONFIRMED": zh = "车辆状态待确认，本次未启动"; en = "State unconfirmed; no start sent"; break;
      case "COMMAND_UNCONFIRMED": zh = "指令已发送，车辆状态未确认"; en = "Sent; vehicle state unconfirmed"; break;
      case "CHECK_UNAVAILABLE": zh = "本次检查不可用，等待下次检查"; en = "Check unavailable; waiting for next check"; break;
      default:
        boolean low = raw.startsWith("LOW_BATTERY"), expired = raw.startsWith("EXPIRED");
        zh = low ? "低于电量下限，已结束托管" : expired ? "已到设定时间，托管结束" : "托管已停止";
        en = low ? "Stopped: below battery limit" : expired ? "Camping duration ended" : "Camping stopped";
        if (raw.endsWith("STOP_UNCONFIRMED")) { zh += " · 停止状态未确认"; en += " · stop unconfirmed"; }
    }
    return AppPreferences.tr(c, zh, en);
  }
  public static long checkedAt(Context c) { return c.getSharedPreferences(PREF, 0).getLong("checked_at", 0); }
}
