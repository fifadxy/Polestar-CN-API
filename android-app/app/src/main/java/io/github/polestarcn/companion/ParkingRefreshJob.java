package io.github.polestarcn.companion;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
/** Optional parking-only c3Lbs background poll; never opens phone GPS or full vehicle reads. */
public final class ParkingRefreshJob extends JobService {
 private static final int ID=91;
 private boolean stopped;
 static void schedule(Context c){
   JobScheduler jobs=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
   if(jobs==null)return;
   if(!RefreshPolicy.mapBackground(c)){jobs.cancel(ID);return;}
   int period=Math.max(60,RefreshPolicy.map(c));
   JobInfo current=jobs.getPendingJob(ID);
   if(current!=null&&current.getIntervalMillis()==period*60_000L)return;
   jobs.schedule(new JobInfo.Builder(ID,new ComponentName(c,ParkingRefreshJob.class))
       .setPersisted(true).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
       .setRequiresBatteryNotLow(true).setPeriodic(period*60_000L).build());
 }
 @Override public boolean onStartJob(JobParameters params){
   if(!FirstUseConsent.accepted(this)||!RefreshPolicy.mapBackground(this))return false;
   VehicleCoordinator m=VehicleCoordinator.get(this);
   if(!m.hasSession()||m.selected==null)return false;
   stopped=false;m.readLastParkedLocation(false,(point,error)->{if(!stopped)jobFinished(params,false);});
   return true;
 }
 @Override public boolean onStopJob(JobParameters params){stopped=true;return false;}
}
