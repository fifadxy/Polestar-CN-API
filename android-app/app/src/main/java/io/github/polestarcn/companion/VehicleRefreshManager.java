package io.github.polestarcn.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.EnumSet;

/**
 * One ordinary telemetry coordinator for Activity, JobScheduler, widgets and ColorOS.
 * Remote-control verification and camping deliberately bypass this 2-second queue.
 * Callbacks always execute on the main thread. Per-VIN timestamps use wall time and
 * are rejected if the clock changes; no raw VIN or token enters the preferences file.
 */
public final class VehicleRefreshManager {
 private static VehicleRefreshManager instance;
 public static synchronized VehicleRefreshManager get(Context c){
   if(instance==null)instance=new VehicleRefreshManager(c.getApplicationContext());
   return instance;
 }
 private final Context context;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final SharedPreferences cache;
 private EnumSet<RefreshDomain> pending=EnumSet.noneOf(RefreshDomain.class);
 private final ArrayList<Runnable> waiting=new ArrayList<>();
 private boolean inFlight,foreground;
 private EnumSet<RefreshDomain> activeScopes=EnumSet.noneOf(RefreshDomain.class);
 private long queuedAt,blockedSince,lastFailedAt;
 private String queuedVehicle="";
 private final Runnable dispatch=this::dispatch;
 private VehicleRefreshManager(Context context){
   this.context=context;
   cache=context.getSharedPreferences("psa-refresh-beta43",Context.MODE_PRIVATE);
 }
 public void setForeground(boolean visible){foreground=visible;}
 public boolean foreground(){return foreground;}
 private static String hashed(String vin){
   if(vin==null||vin.isEmpty())return "";
   try{
     byte[] digest=MessageDigest.getInstance("SHA-256").digest(vin.getBytes(StandardCharsets.UTF_8));
     StringBuilder out=new StringBuilder();
     for(int i=0;i<12;i++)out.append(String.format(java.util.Locale.US,"%02x",digest[i]&255));
     return out.toString();
   }catch(Exception unlikely){return Integer.toHexString(vin.hashCode());}
 }
 private String token(String vin,RefreshDomain group){return "v-"+hashed(vin)+"-"+group.name();}
 private long readAt(String vin,RefreshDomain group){
   long at=cache.getLong(token(vin,group),0),now=System.currentTimeMillis();
   return at>0&&at<=now+60_000L?Math.min(at,now):0;
 }
 private void record(String key){cache.edit().putLong(key,cache.getLong(key,0)+1).apply();}
 public synchronized void onFullRead(String vin){
   if(vin==null||vin.isEmpty())return;
   SharedPreferences.Editor edit=cache.edit();long now=System.currentTimeMillis();
   for(RefreshDomain group:RefreshDomain.values())edit.putLong(token(vin,group),now);
   edit.apply();
 }
 private synchronized void onScopesRead(String vin,EnumSet<RefreshDomain> scopes){
   if(vin==null||scopes.isEmpty())return;
   SharedPreferences.Editor edit=cache.edit();long now=System.currentTimeMillis();
   for(RefreshDomain group:scopes)edit.putLong(token(vin,group),now);
   if(scopes.contains(RefreshDomain.HEALTH))edit.putLong(token(vin,RefreshDomain.MAINTENANCE),now);
   if(scopes.contains(RefreshDomain.MAINTENANCE))edit.putLong(token(vin,RefreshDomain.HEALTH),now);
   edit.apply();
 }
 private boolean due(String vin,RefreshDomain group,boolean front,VehicleSnapshot snapshot){
   int period=RefreshPolicy.activeMinutes(context,group,front,snapshot);
   if(period<=0)return false;
   return !RefreshPlanner.fresh(readAt(vin,group),System.currentTimeMillis(),period*60_000L);
 }
 private boolean airExpirationDue(String vin,VehicleSnapshot s){
   if(s==null||!"开启".equals(s.air))return false;
   long expiry=0;
   android.content.SharedPreferences local=context.getSharedPreferences("air-runtime-v1",0);
   String identity=AppPreferences.get(context,"vehicle","");
   if(identity.equals(local.getString("vehicle",""))){
     long started=local.getLong("started",0);if(started>0)expiry=started+300_000L;
   }
   if(expiry==0&&s.environment.airRuntimeMinutes!=null&&s.environment.airReportedAt>0)
     expiry=s.environment.airReportedAt+s.environment.airRuntimeMinutes*60_000L;
   long now=System.currentTimeMillis();
   return expiry>0&&expiry<=now&&now-expiry<24*60*60_000L&&readAt(vin,RefreshDomain.AIR)<expiry;
 }
 private EnumSet<RefreshDomain> requested(boolean front,boolean idle){
   VehicleCoordinator car=VehicleCoordinator.get(context);
   EnumSet<RefreshDomain> set=EnumSet.noneOf(RefreshDomain.class);
   if(car.selected==null)return set;
   String vin=car.selected.vin;
   if(idle){
     int interval=RefreshPolicy.minutes(context,"refresh_idle_minutes",120,0,60,120,240);
     if(interval>0&&!RefreshPlanner.fresh(readAt(vin,RefreshDomain.CORE),System.currentTimeMillis(),interval*60_000L))
       set.add(RefreshDomain.CORE);
     return set;
   }
   for(RefreshDomain d:RefreshDomain.values())if(due(vin,d,front,car.snapshot))set.add(d);
   if(front&&airExpirationDue(vin,car.snapshot))set.add(RefreshDomain.AIR);
   // These fields are on the same Health RPC; read it once when either expires.
   if(set.contains(RefreshDomain.HEALTH)||set.contains(RefreshDomain.MAINTENANCE)){
     set.add(RefreshDomain.HEALTH);set.add(RefreshDomain.MAINTENANCE);
   }
   return set;
 }
 public void requestForeground(){request(true,false,null);}
 public void requestBackground(Runnable done){
   boolean consumer=AppPreferences.enabled(context)||WidgetRenderer.hasWidgets(context)
        ||OppoDeviceIntegration.enabled(context)||AppPreferences.visible(context,"app_background_enabled",false);
   request(false,!consumer,done);
 }
 public void requestResume(){
   VehicleCoordinator car=VehicleCoordinator.get(context);
   if(!foreground||car.selected==null||!car.hasSession())return;
   int minutes=RefreshPolicy.resumeThreshold(context);
   if(minutes==0)return;
   // Each group's TTL is checked independently: a recently refreshed battery does
   // not suppress a due 2-minute climate check after returning to the foreground.
   request(true,false,null);
 }
 public void requestBridge(Runnable done){request(false,false,done);}
 private void request(boolean front,boolean idle,Runnable done){
   if(Looper.myLooper()!=Looper.getMainLooper()){
     main.post(()->request(front,idle,done));return;
   }
   VehicleCoordinator car=VehicleCoordinator.get(context);
   if(!VehiclePowerPolicy.automaticReads(context)||!FirstUseConsent.accepted(context)
       ||!car.hasSession()||car.selected==null||car.selected.vin==null){
     if(done!=null)done.run();return;
   }
   EnumSet<RefreshDomain> needs=requested(front,idle);
   String vin=car.selected.vin;
   if(inFlight&&queuedVehicle.equals(vin)){
     needs.removeAll(activeScopes);
     if(needs.isEmpty()){
       record("merged");if(done!=null)waiting.add(done);return;
     }
   }
   if(needs.isEmpty()){record("cacheHits");if(done!=null)done.run();return;}
   if(!queuedVehicle.isEmpty()&&!queuedVehicle.equals(vin)){
     pending.clear();for(Runnable callback:waiting)callback.run();waiting.clear();
   }
   queuedVehicle=vin;
   if(!pending.isEmpty()||inFlight)record("merged");
   pending.addAll(needs);if(done!=null)waiting.add(done);
   if(inFlight)return;
   main.removeCallbacks(dispatch);
   if(queuedAt==0)queuedAt=SystemClock.elapsedRealtime();
   main.postDelayed(dispatch,RefreshPolicy.mergeSeconds(context)*1000L);
 }
 private void dispatch(){
   VehicleCoordinator car=VehicleCoordinator.get(context);
   if(!VehiclePowerPolicy.automaticReads(context)||!car.hasSession()||car.selected==null
        ||!queuedVehicle.equals(car.selected.vin)){
     drainCallbacks();return;
   }
   if(inFlight)return;
   if(car.busy||car.commandPending){
     if(blockedSince==0)blockedSince=SystemClock.elapsedRealtime();
     if(SystemClock.elapsedRealtime()-blockedSince>45_000L){drainCallbacks();return;}
     main.postDelayed(dispatch,2000L);return;
   }
   if(pending.isEmpty()){drainCallbacks();return;}
   if(lastFailedAt>0&&SystemClock.elapsedRealtime()-lastFailedAt<30_000L){
     record("backoff");drainCallbacks();return;
   }
   final EnumSet<RefreshDomain> scope=EnumSet.copyOf(pending);
   final String vin=queuedVehicle;
   pending.clear();inFlight=true;activeScopes=EnumSet.copyOf(scope);blockedSince=0;
   record("networkBatches");
   car.refreshScoped(scope,()->{
     inFlight=false;activeScopes.clear();
     if(car.selected==null||!vin.equals(car.selected.vin)){
       // A logout/vehicle switch invalidates an old scope result; don't backoff the new VIN.
       record("cancelled");
     }else if(car.lastVehicleReadOk){
       onScopesRead(vin,scope);
     }else {record("failures");lastFailedAt=SystemClock.elapsedRealtime();}
     if(!pending.isEmpty()){main.postDelayed(dispatch,RefreshPolicy.mergeSeconds(context)*1000L);}
     else drainCallbacks();
     ResidentService.scheduleUpdates(context);
   });
 }
 private void drainCallbacks(){
   pending.clear();queuedAt=0;queuedVehicle="";blockedSince=0;
   main.removeCallbacks(dispatch);
   ArrayList<Runnable> done=new ArrayList<>(waiting);waiting.clear();
   for(Runnable cb:done)try{cb.run();}catch(RuntimeException ignored){}
 }
 public long nextForegroundDelay(){
   if(!VehiclePowerPolicy.automaticReads(context))return -1;
   VehicleCoordinator car=VehicleCoordinator.get(context);
   if(car.selected==null||!car.hasSession())return 60_000L;
   long now=System.currentTimeMillis(),earliest=30*60_000L;
   for(RefreshDomain group:RefreshDomain.values()){
     int n=RefreshPolicy.activeMinutes(context,group,true,car.snapshot);
     if(n<=0)continue;
     long last=readAt(car.selected.vin,group);
     long due=last==0?1000L:Math.max(1000L,n*60_000L-(now-last));
     earliest=Math.min(earliest,due);
   }
   return Math.max(1000L,earliest);
 }
 public String diagnostics(){
   return "Beta43 cloud batches="+cache.getLong("networkBatches",0)
       +" · merged="+cache.getLong("merged",0)
       +" · cacheHits="+cache.getLong("cacheHits",0)
       +" · failures="+cache.getLong("failures",0)
       +" · backoff="+cache.getLong("backoff",0);
 }
}
