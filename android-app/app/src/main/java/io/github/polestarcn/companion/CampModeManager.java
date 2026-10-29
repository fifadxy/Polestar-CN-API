package io.github.polestarcn.companion;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import io.github.polestarcn.companion.auth.SecureCampStore;

public final class CampModeManager {
  private static final String PREF = "camp_status";
  private static final int PERIODIC = 72, IMMEDIATE = 73, EXPIRY = 74;
  private static final long RETRY_DELAY_MS=15*60_000L;
  private CampModeManager() {}
  public static CampSession session(Context c) { return SecureCampStore.load(c); }
  public static String phase(Context c) { return c.getSharedPreferences(PREF, 0).getString("phase", "STOPPED"); }
  public static boolean active(Context c) { return "ACTIVE".equals(phase(c)) && session(c) != null; }
  /** A local stop must remain possible while offline. Remote HVAC status is tracked separately. */
  public static boolean pendingStop(Context c) {
    return c.getSharedPreferences(PREF,0).getBoolean("remote_stop_pending",false) && session(c)!=null;
  }
  public static int stopAttemptMask(Context c){return c.getSharedPreferences(PREF,0).getInt("stop_attempt_mask",0);}
  public static synchronized void markStopAttempted(Context c,String id,int bit){
    if(!current(c,id)||!pendingStop(c))return;
    int existing=stopAttemptMask(c);c.getSharedPreferences(PREF,0).edit()
      .putInt("stop_attempt_mask",existing|bit).putString("result","REMOTE_STOP_VERIFY").commit();
  }
  public static synchronized void markPendingStopStatus(Context c,String id,String status){
    if(!current(c,id)||!pendingStop(c))return;
    c.getSharedPreferences(PREF,0).edit().putString("result",status)
      .putLong("checked_at",System.currentTimeMillis())
      .putLong("next_recovery_at",System.currentTimeMillis()+RETRY_DELAY_MS).commit();
  }
  public static synchronized void completeRemoteStop(Context c,String id){
    if(!current(c,id)||!pendingStop(c))return;
    c.getSharedPreferences(PREF,0).edit().putBoolean("remote_stop_pending",false)
      .putString("result","REMOTE_STOP_CONFIRMED").putLong("checked_at",System.currentTimeMillis()).commit();
    cancelTasks(c);
  }
  public static boolean needsRecovery(Context c){return active(c)||pendingStop(c);}
  public static void retryNetworkRecovery(Context c){
    if(needsRecovery(c))try{schedule(c);}catch(RuntimeException e){DiagnosticLog.add("Camping network recovery reschedule failed");}
  }
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
  private static synchronized boolean requestStop(Context c,String reason) {
    CampSession s=session(c);
    if(s==null||(!active(c)&&!"STOPPING".equals(phase(c))))return false;
    // Stop local scheduling synchronously. Network and login errors can NEVER block this operation.
    c.getSharedPreferences(PREF,0).edit()
      .putString("phase","STOPPED").putString("stop_reason",reason)
      .putString("result","LOCAL_STOPPED_REMOTE_PENDING")
      .putBoolean("remote_stop_pending",true).putInt("stop_attempt_mask",0)
      .putLong("next_recovery_at",0).commit();
    cancelTasks(c);
    try{schedule(c);}catch(RuntimeException e){DiagnosticLog.add("Camping local stop persisted; network task pending");}
    ResidentService.update(c);
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
  public static void finishCheck(Context c,String id) {
    if(!current(c,id))return;
    if(pendingStop(c)){try{schedule(c);}catch(RuntimeException e){DiagnosticLog.add("Camping pending stop recovery not scheduled");}}
    else if("STOPPED".equals(phase(c)))cancelTasks(c);
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
    if (s == null || !(active(c) || "STOPPING".equals(phase(c)) || pendingStop(c))) return;
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
    long next=pendingStop(c)?c.getSharedPreferences(PREF,0).getLong("next_recovery_at",0):0;
    if (jobs.schedule(new JobInfo.Builder(IMMEDIATE, service).setPersisted(true)
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setMinimumLatency(Math.max(0,next-System.currentTimeMillis())).build()) != JobScheduler.RESULT_SUCCESS)
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
      case "WAITING_FOR_NETWORK": zh = "网络不可用 · 已暂停托管写入，等待连接恢复"; en = "Offline · remote commands paused until connected"; break;
      case "LOGIN_REQUIRED": zh = "登录会话需要重新验证 · 托管已暂停"; en = "Session needs sign-in · camping paused"; break;
      case "LOCAL_STOPPED_REMOTE_PENDING": zh = "本地托管已停止 · 等待确认远程空调状态"; en = "Local camping stopped · remote HVAC not yet verified"; break;
      case "REMOTE_STOP_VERIFY": zh = "本地已停止 · 正在核实车端是否停止"; en = "Locally stopped · verifying vehicle shutdown"; break;
      case "REMOTE_STOP_UNCONFIRMED": zh = "本地已停止 · 远程空调停止未确认"; en = "Locally stopped · remote HVAC stop unconfirmed"; break;
      case "REMOTE_STOP_CONFIRMED": zh = "托管已结束 · 车辆空调状态已确认"; en = "Camping ended · vehicle state confirmed"; break;
      default:
        boolean low = raw.startsWith("LOW_BATTERY"), expired = raw.startsWith("EXPIRED");
        zh = low ? "低于电量下限，已结束托管" : expired ? "已到设定时间，托管结束" : "托管已停止";
        en = low ? "Stopped: below battery limit" : expired ? "Camping duration ended" : "Camping stopped";
        if (raw.endsWith("STOP_UNCONFIRMED")) { zh += " · 停止状态未确认"; en += " · stop unconfirmed"; }
    }
    return AppPreferences.tr(c, zh, en);
  }
  /** Short, bounded display-only countdown based on the scheduled end time. */
  public static String remainingCaption(Context c){
    CampSession s=session(c);
    if(s==null||(!active(c)&&!"STOPPING".equals(phase(c))))return compactStatus(c);
    long left=Math.max(0,s.endsAt-System.currentTimeMillis());
    long min=(left+59999L)/60000L;
    long hours=min/60,mins=min%60;
    return hours>0 ? (AppPreferences.english(c)?"":"剩余 ")+hours+"h"+String.format(java.util.Locale.ROOT,"%02d",mins)+"m"
        : (AppPreferences.english(c)?"":"剩余 ")+mins+"m";
  }
  public static long checkedAt(Context c) { return c.getSharedPreferences(PREF, 0).getLong("checked_at", 0); }
  /** Short button caption; the full diagnostic remains in status() and the camp details. */
  public static String compactStatus(Context c) {
    String result=c.getSharedPreferences(PREF,0).getString("result","STOPPED");
    if("STOPPING".equals(phase(c)))return AppPreferences.tr(c,"停止中","Stopping");
    if(pendingStop(c))return AppPreferences.tr(c,"本地已停止 · 待核实","Stopped · verify remote");
    if(result.contains("UNCONFIRMED"))return AppPreferences.tr(c,"待确认","Check");
    if(active(c))return "RUNNING".equals(result)?AppPreferences.tr(c,"运行中","Active"):AppPreferences.tr(c,"待检查","Queued");
    if(result.startsWith("LOW_BATTERY"))return AppPreferences.tr(c,"低电停止","Low SoC");
    if(result.startsWith("EXPIRED"))return AppPreferences.tr(c,"已结束","Ended");
    return AppPreferences.tr(c,"未开启","Off");
  }
}
