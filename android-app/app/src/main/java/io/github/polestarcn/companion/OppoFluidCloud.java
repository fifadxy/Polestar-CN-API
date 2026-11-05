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
import android.os.SystemClock;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ColorOS Fluid Cloud / Android live-update compatibility layer. OFF by default.
 *
 * Genuine OPPO Pantanal IntelligentIntent requires approved service IDs and a
 * registered business; this class NEVER pretends to send that private payload.
 * "v1" is an ordinary compatible notification, "v2" requests Android 16
 * promoted ongoing for qualifying user-started activities, if the OS supports it.
 * SystemUI and OPPO, not this app, decide whether to surface a capsule.
 * Synthetic tests never call the vehicle API or VehicleCoordinator.command().
 */
public final class OppoFluidCloud {
  private OppoFluidCloud(){}
  public static final String ACTION_EXTRA="oppo_fluid_action";
  public static final String SOURCE_EXTRA="oppo_fluid_source_scene";
  public static final String TEST_EXTRA="oppo_fluid_test_action";
  public static final String CH_EVENTS="psa_oppo_fluid_events_v1";
  public static final String CH_ACTIVE="psa_oppo_fluid_active_v1";
  public static final String CH_ALERT="psa_oppo_fluid_alerts_v1";
  private static final int COLOR=0xffff6b00;
  private static final int ID_BASE=47000,TEST_ID_BASE=48000;
  private static final String STORE="psa_fluid_events_v1";
  private static final String[] ACTIONS={"lock","unlock","climate-start","climate-stop","air-start","air-stop",
      "camp-stop","trunk-open","trunk-close","windows-open","windows-close","honk","flash","honk-flash"};
  private static String tr(Context c,String zh,String en){return AppPreferences.tr(c,zh,en);}
  private static android.content.SharedPreferences store(Context c){return c.getSharedPreferences(STORE,Context.MODE_PRIVATE);}
  public static boolean allowedAction(String action){return Arrays.asList(ACTIONS).contains(action);}
  public static boolean actionBelongs(String scene,String action){
    if(!allowedAction(action))return false;
    switch(scene){
      case "lock":return "unlock".equals(action);
      case "unlock":return "lock".equals(action);
      case "climate":return action.startsWith("climate-");
      case "air":return action.startsWith("air-");
      case "camp":return "camp-stop".equals(action);
      case "trunk":return action.startsWith("trunk-");
      case "windows":return action.startsWith("windows-");
      case "find":return action.equals("honk")||action.equals("flash")||action.equals("honk-flash");
      case "status":return action.equals("lock")||action.equals("unlock")||action.startsWith("climate-");
      default:return false;
    }
  }
  public static boolean canNotify(Context c){
    NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
    return n!=null&&n.areNotificationsEnabled()&&(Build.VERSION.SDK_INT<33||
        c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED);
  }
  public static int itemId(String scene,boolean test){for(int i=0;i<OppoFluidPolicy.SCENES.length;i++)if(scene.equals(OppoFluidPolicy.SCENES[i]))
      return (test?TEST_ID_BASE:ID_BASE)+i;return -1;}
  private static NotificationManager manager(Context c){return (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);}
  static void channels(Context c){NotificationManager n=manager(c);if(n==null)return;
    createChannel(n,CH_EVENTS,tr(c,"OPPO 车控事件","OPPO vehicle events"),NotificationManager.IMPORTANCE_DEFAULT);
    createChannel(n,CH_ACTIVE,tr(c,"OPPO 运行中活动","OPPO active tasks"),NotificationManager.IMPORTANCE_LOW);
    createChannel(n,CH_ALERT,tr(c,"OPPO 车辆异常","OPPO vehicle alerts"),NotificationManager.IMPORTANCE_DEFAULT);
  }
  private static void createChannel(NotificationManager n,String id,String label,int importance){
    NotificationChannel ch=new NotificationChannel(id,label,importance);
    ch.setSound(null,null);ch.enableVibration(false);ch.setShowBadge(false);n.createNotificationChannel(ch);
  }
  private static PendingIntent open(Context c,int request,String action,String scene,boolean demo){
    Intent i=new Intent(c,demo?OppoSystemCompatActivity.class:MainActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
    if(action!=null&&!action.isEmpty()){
      i.putExtra(demo?TEST_EXTRA:ACTION_EXTRA,action);
      if(!demo)i.putExtra(SOURCE_EXTRA,scene);
      i.setAction("io.github.polestarcn.companion.FLUID."+(demo?"DEMO.":"CONTROL.")+action);
    }
    return PendingIntent.getActivity(c,request,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  }
  private static boolean requestedPromoted(Context c,String path,boolean ongoing){
    return ongoing&&Build.VERSION.SDK_INT>=36&&!("v1".equals(path));
  }
  /** Android 16 APIs must be reflected because the main project compiles against SDK 35. */
  private static void applyPromoted(Notification.Builder b,OppoFluidScene s,String path){
    if(!requestedPromoted(null,path,s.ongoing))return;
    try{
      Method request=Notification.Builder.class.getMethod("setRequestPromotedOngoing",boolean.class);
      request.invoke(b,true);
      Method chip=Notification.Builder.class.getMethod("setShortCriticalText",String.class);
      chip.invoke(b,s.chip.length()>7?s.chip.substring(0,7):s.chip);
    }catch(ReflectiveOperationException|RuntimeException ignored){/* fallback to ordinary notification */}
  }
  public static Notification build(Context c,OppoFluidScene s,String style,int level,String path,boolean allowActions){
    String channel=s.id.equals("fault")?CH_ALERT:s.ongoing?CH_ACTIVE:CH_EVENTS;
    int notificationId=itemId(s.id,s.synthetic);
    String title=s.title;
    String detail=(s.synthetic?tr(c,"【模拟】","[TEST] "):"")+s.detail;
    String capsule="simple".equals(style)?s.chip:title;
    Notification.Builder b=new Notification.Builder(c,channel).setSmallIcon(R.drawable.ic_notification)
        .setColor(COLOR).setColorized(false).setContentTitle(title).setContentText(detail)
        .setContentIntent(open(c,notificationId,null,s.id,s.synthetic))
        .setCategory(Notification.CATEGORY_SERVICE).setOngoing(s.ongoing)
        .setOnlyAlertOnce(s.ongoing).setShowWhen(false).setVisibility(Notification.VISIBILITY_PRIVATE);
    if(!"simple".equals(style)||level==2||requestedPromoted(c,path,s.ongoing))
      b.setStyle(new Notification.BigTextStyle().bigText(detail+"\n"+tr(c,"一级：","Primary: ")+capsule));
    if(s.endsAt>System.currentTimeMillis()+1000&&s.ongoing&&!s.synthetic){
      // Android ticks down locally; no repeated polling or CPU wake-up is required.
      b.setUsesChronometer(true).setChronometerCountDown(true).setWhen(s.endsAt).setShowWhen(true);
    }
    if(s.synthetic)b.setTimeoutAfter(90_000L);
    else if(!s.ongoing&&!s.id.equals("fault")){b.setTimeoutAfter(OppoFluidPolicy.lockSeconds(c)*1000L);}
    if(level==2&&allowActions){
      for(String action:new String[]{s.firstAction,s.secondAction})if(action!=null&&actionBelongs(s.id,action)){
        String label=label(c,action)+(s.synthetic?tr(c,"（模拟）"," (preview)"):"");
        PendingIntent pi=open(c,notificationId*100+Math.abs(action.hashCode()%100),action,s.id,s.synthetic);
        b.addAction(android.R.drawable.ic_menu_send,label,pi);
      }
    }
    applyPromoted(b,s,path);
    return b.build();
  }
  private static String label(Context c,String action){switch(action){
    case "lock":return tr(c,"锁车","Lock");case "unlock":return tr(c,"解锁","Unlock");
    case "climate-start":return tr(c,"开空调","Climate on");case "climate-stop":return tr(c,"关空调","Climate off");
    case "air-start":return tr(c,"开净化","Purify on");case "air-stop":return tr(c,"关净化","Purify off");
    case "camp-stop":return tr(c,"停止托管","Stop camping");
    case "trunk-open":return tr(c,"开尾箱","Open trunk");case "trunk-close":return tr(c,"关尾箱","Close trunk");
    case "windows-open":return tr(c,"开窗","Open windows");case "windows-close":return tr(c,"关窗","Close windows");
    case "honk":return tr(c,"鸣笛","Honk");case "flash":return tr(c,"闪灯","Flash");
    default:return action;}}
  private static String safeChip(Context c,String zh,String en){return AppPreferences.english(c)?en:zh;}
  /** Static synthetic scenes cover every supported button and data card with no vehicle/network access. */
  public static OppoFluidScene demo(Context c,String scene){
    final long end=System.currentTimeMillis()+7*60_000L;
    switch(scene){
      case "lock":return new OppoFluidScene(scene,tr(c,"Polestar · 已锁定","Polestar · Locked"),tr(c,"车门上锁结果模拟","Simulated vehicle lock result"),safeChip(c,"已锁","LOCK"),false,0,"unlock",null,true);
      case "unlock":return new OppoFluidScene(scene,tr(c,"Polestar · 已解锁","Polestar · Unlocked"),tr(c,"车门解锁结果模拟","Simulated vehicle unlock result"),safeChip(c,"解锁","OPEN"),false,0,"lock",null,true);
      case "climate":return new OppoFluidScene(scene,tr(c,"空调运行中","Climate running"),tr(c,"剩余 07:00 · 车内 23°C","07:00 left · Cabin 23°C"),safeChip(c,"空调7m","AC 7m"),true,end,"climate-stop","climate-start",true);
      case "air":return new OppoFluidScene(scene,tr(c,"空气净化中","Air purification"),tr(c,"剩余 04:00 · AQI 38","04:00 left · AQI 38"),safeChip(c,"净化4m","AIR 4m"),true,end,"air-stop","air-start",true);
      case "camp":return new OppoFluidScene(scene,tr(c,"露营托管中","Camping active"),tr(c,"剩余 2小时 · 模拟状态","2h left · simulated"),safeChip(c,"托管2h","CAMP"),true,end,"camp-stop",null,true);
      case "charging":return new OppoFluidScene(scene,tr(c,"车辆充电中","Vehicle charging"),tr(c,"65% · 续航 290 km · 模拟数据","65% · 290 km · simulated"),safeChip(c,"充电65%","65%"),true,end,null,null,true);
      case "fault":return new OppoFluidScene(scene,tr(c,"车辆异常 · 2 项","Vehicle warnings · 2"),tr(c,"左前轮胎压偏低 · 12V 蓄电池异常","Front left tyre low · 12V warning"),safeChip(c,"异常2项","2 WARN"),false,0,null,null,true);
      case "trunk":return new OppoFluidScene(scene,tr(c,"尾箱控制","Tailgate"),tr(c,"模拟尾箱状态 · 未执行真实操作","Simulated tailgate · no command sent"),safeChip(c,"尾箱","TRUNK"),false,0,"trunk-open","trunk-close",true);
      case "windows":return new OppoFluidScene(scene,tr(c,"车窗控制","Windows"),tr(c,"模拟车窗状态 · 未执行真实操作","Simulated windows · no command sent"),safeChip(c,"车窗","WINDOW"),false,0,"windows-open","windows-close",true);
      case "find":return new OppoFluidScene(scene,tr(c,"寻车功能","Find vehicle"),tr(c,"模拟鸣笛、闪灯 · 无真实指令","Simulated horn and flash · no actual command"),safeChip(c,"寻车","FIND"),false,0,"honk","flash",true);
      default:return new OppoFluidScene("status",tr(c,"Polestar · 车辆状态","Polestar · Status"),
          tr(c,"65% · 290 km · 车辆状态模拟","65% · 290 km · simulated"),safeChip(c,"车况","STATUS"),false,0,"lock","climate-start",true);
    }
  }
  public static String test(Context c,String scene){
    if(!OppoFluidPolicy.allowed(scene))return tr(c,"无效测试项目","Unknown test case");
    if(!OppoFluidPolicy.enabled(c))return tr(c,"请先开启 OPPO 系统兼容总开关","Enable OPPO integration lab first");
    if(!canNotify(c))return tr(c,"请先允许应用通知权限","Please allow notifications first");
    channels(c);NotificationManager n=manager(c);if(n==null)return tr(c,"通知服务不可用","Notification service unavailable");
    OppoFluidScene sample=demo(c,scene);
    n.notify(itemId(scene,true),build(c,sample,OppoFluidPolicy.style(c),OppoFluidPolicy.level(c),OppoFluidPolicy.path(c),true));
    store(c).edit().putLong("last_test_time",System.currentTimeMillis()).putString("last_test_scene",scene).apply();
    return tr(c,"已发送模拟通知：","Simulated notification posted: ")+scene;
  }
  public static String testAll(Context c){
    if(!OppoFluidPolicy.enabled(c)||!canNotify(c))return tr(c,"请开启实验总开关并授权通知","Enable lab and grant notification permission");
    int posted=0;for(String scene:OppoFluidPolicy.SCENES)if(test(c,scene).contains(scene))posted++;
    return tr(c,"已测试 ","Posted ")+posted+tr(c," 项模拟通知，不会发送任何真实车辆指令。"," synthetic notifications. No car commands were sent.");
  }
  public static int validateMatrix(Context c){int count=0;
    for(String scene:OppoFluidPolicy.SCENES)for(String path:new String[]{"v1","v2"})
      for(String style:new String[]{"simple","full"})for(int level:new int[]{1,2}){
        Notification n=build(c,demo(c,scene),style,level,path,true);
        if(n.contentIntent==null||n.getSmallIcon()==null)throw new IllegalStateException("Notification missing required fields: "+scene);
        if((n.flags&Notification.FLAG_ONGOING_EVENT)!=0&&!demo(c,scene).ongoing)throw new IllegalStateException("wrong ongoing flag");
        count++;
      }
    return count;
  }
  public static void cancelRealScene(Context c,String scene){if(!OppoFluidPolicy.allowed(scene))return;NotificationManager n=manager(c);if(n!=null)n.cancel(itemId(scene,false));store(c).edit().remove("active_sig_"+scene).apply();}
  public static void cancelTests(Context c){NotificationManager n=manager(c);if(n!=null)for(String scene:OppoFluidPolicy.SCENES)n.cancel(itemId(scene,true));}
  public static void cancelAll(Context c){NotificationManager n=manager(c);if(n!=null){
    for(String scene:OppoFluidPolicy.SCENES){n.cancel(itemId(scene,true));n.cancel(itemId(scene,false));}
    n.cancel(47099);
  }store(c).edit().clear().apply();}
  public static void onVerifiedCommand(Context c,String action,OperationState result){
    // "server accepted" is not vehicle execution. Only confirmed live readback qualifies.
    if(result!=OperationState.VEHICLE_CONFIRMED||!OppoFluidPolicy.enabled(c)||!canNotify(c))return;
    // A user-started, readback-confirmed activity is eligible to request Android live promotion.
    if("climate-start".equals(action)||"air-start".equals(action))
      store(c).edit().putLong("self_started_"+(action.startsWith("climate")?"climate":"air"),System.currentTimeMillis()).apply();
    if("climate-stop".equals(action)||"air-stop".equals(action))
      store(c).edit().remove("self_started_"+(action.startsWith("climate")?"climate":"air")).apply();
    String scene=null;
    if("lock".equals(action))scene="lock";else if("unlock".equals(action))scene="unlock";
    else if(action.startsWith("trunk-"))scene="trunk";else if(action.startsWith("windows-"))scene="windows";
    if(scene==null||!OppoFluidPolicy.notify(c,scene))return;
    OppoFluidScene d=demo(c,scene);
    String title=tr(c,"车辆操作已确认","Vehicle command confirmed");
    String state=label(c,action);
    OppoFluidScene live=new OppoFluidScene(scene,title,state+tr(c," · 车端状态核实成功"," · vehicle readback confirmed"),
      d.chip,false,0,null,null,false);
    channels(c);manager(c).notify(itemId(scene,false),build(c,live,OppoFluidPolicy.style(c),1,"v1",false));
    store(c).edit().putLong("last_command_at",System.currentTimeMillis()).putString("last_command",action).apply();
  }
  /** Read-only status update; never calls the car API or a privileged OEM service. */
  public static void onLiveSnapshot(Context c,VehicleSnapshot s){
    if(!OppoFluidPolicy.enabled(c)||s==null||s.isDemo||!"LIVE".equals(s.source)||!canNotify(c))return;
    String sourceId=Integer.toHexString(s.id==null?0:s.id.hashCode());
    android.content.SharedPreferences state=store(c);
    String previousId=state.getString("last_source_id","");
    if(!previousId.isEmpty()&&!previousId.equals(sourceId)){
      // Never carry a vehicle-specific cloud action, cached fault, or caption into another VIN.
      for(String scene:OppoFluidPolicy.SCENES)cancelRealScene(c,scene);
      NotificationManager manager=manager(c);if(manager!=null)manager.cancel(47099);
    }
    android.content.SharedPreferences.Editor mark=state.edit();
    mark.putLong("last_live_snapshot",System.currentTimeMillis());
    mark.putString("last_source_id",sourceId);mark.apply();
    channels(c);onIssues(c,s);syncOngoing(c,s,true);
  }
  private static void onIssues(Context c,VehicleSnapshot s){
    List<String> issues=OppoVehicleIssues.collect(c,s);String fingerprint=OppoVehicleIssues.detail(issues,issues.size());
    String vehicleKey="issues_"+Integer.toHexString(s.id==null?0:s.id.hashCode());
    android.content.SharedPreferences p=store(c);String before=p.getString(vehicleKey,"");
    long now=System.currentTimeMillis(),last=p.getLong(vehicleKey+"_time",0);
    if(issues.isEmpty()){
      if(!before.isEmpty()){manager(c).cancel(itemId("fault",false));p.edit().remove(vehicleKey).remove(vehicleKey+"_time").apply();}
      return;
    }
    if(!OppoFluidPolicy.notify(c,"fault"))return;
    if(before.equals(fingerprint)&&now-last<OppoFluidPolicy.warningCooldown(c)*60_000L)return;
    String text=OppoVehicleIssues.detail(issues,5);
    OppoFluidScene scene=new OppoFluidScene("fault",tr(c,"车辆需要检查 · ","Vehicle warning · ")+issues.size(),
        text,tr(c,"异常"+issues.size(),"WARN"),false,0,null,null,false);
    manager(c).notify(itemId("fault",false),build(c,scene,OppoFluidPolicy.style(c),1,"v1",false));
    p.edit().putString(vehicleKey,fingerprint).putLong(vehicleKey+"_time",now).apply();
  }
  public static void updateFromCache(Context c){
    if(!OppoFluidPolicy.enabled(c)||!canNotify(c))return;
    long saved=store(c).getLong("last_live_snapshot",0),now=System.currentTimeMillis();
    if(saved<=0||saved>now||now-saved>4*60*60_000L){
      // No current live evidence. Never create fresh activity from an old or imported cache.
      return;
    }
    VehicleSnapshot s=VehicleSnapshotStore.loadChina(c);
    if(s==null||!Integer.toHexString(s.id==null?0:s.id.hashCode()).equals(store(c).getString("last_source_id","")))return;
    syncOngoing(c,s,false);
  }
  public static void syncCamp(Context c){
    if(!OppoFluidPolicy.enabled(c)||!canNotify(c))return;channels(c);
    VehicleSnapshot s=VehicleSnapshotStore.loadChina(c);
    if(s!=null)syncCampScene(c,s);
  }
  private static void syncOngoing(Context c,VehicleSnapshot s,boolean genuine){
    String vehicle=AppPreferences.get(c,"vehicle","");long now=System.currentTimeMillis();
    if("开启".equals(s.climate)){
      Integer remaining=ClimateRuntime.remainingMinutes(s,now);
      if(remaining==null){long local=ClimateRuntime.estimatedRemaining(c,vehicle,now);if(local>0)remaining=(int)((local+59999)/60000);}
      boolean valid=remaining!=null&&remaining>0;
      long ends=valid?now+remaining*60_000L:0;
      showActive(c,"climate",true,valid?tr(c,"空调剩余 ","Climate left ")+remaining+"m":tr(c,"空调运行 · 时间待核实","Climate running · time unverified"),
          valid?tr(c,"空调"+remaining+"m","AC "+remaining+"m"):tr(c,"空调","AC"),ends,"climate-stop","climate-start");
    }else showActive(c,"climate",false,"","",0,null,null);
    if("开启".equals(s.air)){
      Long left=AirRuntime.remaining(c,s,vehicle,now);int minutes=left==null?-1:(int)((left+59999)/60_000L);
      showActive(c,"air",true,minutes>0?tr(c,"净化剩余 ","Purification left ")+minutes+"m":tr(c,"净化运行 · 时间待核实","Purification active · time unverified"),
          minutes>0?tr(c,"净化"+minutes+"m","AIR "+minutes+"m"):tr(c,"净化","AIR"),
          left!=null&&left>0?now+left:0,"air-stop","air-start");
    }else showActive(c,"air",false,"","",0,null,null);
    if(ChargingVisibility.shouldShow(s))showActive(c,"charging",true,
        VehicleSnapshot.metric(s.batteryPercent,"%")+tr(c," · 续航 "," · range ")+VehicleSnapshot.metric(s.rangeKm,"km"),
        VehicleSnapshot.metric(s.batteryPercent,"%"),0,null,null);
    else showActive(c,"charging",false,"","",0,null,null);
    syncCampScene(c,s);
    if(OppoFluidPolicy.notify(c,"status")&&genuine){
      OppoFluidScene status=new OppoFluidScene("status",tr(c,"Polestar 车况","Polestar status"),
          VehicleSnapshot.metric(s.batteryPercent,"%")+" · "+VehicleSnapshot.metric(s.rangeKm,"km"),tr(c,"车况","STATUS"),false,0,
          "已上锁".equals(s.lock)||"已锁".equals(s.lock)?"unlock":"lock",
          "开启".equals(s.climate)?"climate-stop":"climate-start",false);
      maybeNotify(c,status);
    }
  }
  private static void syncCampScene(Context c,VehicleSnapshot s){
    if(CampModeManager.active(c)){
      CampSession camp=CampModeManager.session(c);long now=System.currentTimeMillis();
      long end=camp==null?0:camp.endsAt;int minutes=end<=now?0:(int)((end-now+59999)/60000);
      String detail=minutes>0?tr(c,"本地托管 · 剩余 ","Managed locally · ")+minutes+"m":
          tr(c,"本地计时到期，车辆停止状态待核实","Local timer ended; remote stop unverified");
      showActive(c,"camp",true,detail,minutes>0?tr(c,"托管"+minutes+"m","CAMP "+minutes+"m"):tr(c,"待核实","VERIFY"),end>now?end:0,"camp-stop",null);
    }else{
      showActive(c,"camp",false,"","",0,null,null);
      if(CampModeManager.pendingStop(c)&&OppoFluidPolicy.notify(c,"fault")){
        OppoFluidScene alert=new OppoFluidScene("fault",tr(c,"托管结束待核实","Camping stop unconfirmed"),
            tr(c,"本地已停止，远程空调/净化是否关闭仍待确认","Local task stopped; remote HVAC remains unverified"),
            tr(c,"待核实","VERIFY"),false,0,null,null,false);
        // Dedicated ID: do not overwrite the real tyre/12V/vehicle-issue notification.
        manager(c).notify(47099,build(c,alert,OppoFluidPolicy.style(c),1,"v1",false));
      }else manager(c).cancel(47099);
    }
  }
  private static void showActive(Context c,String id,boolean active,String detail,String chip,long end,String first,String second){
    if(!active||!OppoFluidPolicy.notify(c,id)){
      manager(c).cancel(itemId(id,false));store(c).edit().remove("active_sig_"+id).apply();return;
    }
    String title;
    switch(id){case "climate":title=tr(c,"空调运行中","Climate active");break;
      case "air":title=tr(c,"空气净化中","Air purification");break;
      case "camp":title=tr(c,"露营托管中","Camping active");break;
      default:title=tr(c,"车辆充电中","Vehicle charging");}
    OppoFluidScene scene=new OppoFluidScene(id,title,detail,chip,true,end,first,second,false);
    maybeNotify(c,scene);
  }
  private static void maybeNotify(Context c,OppoFluidScene scene){
    if(!OppoFluidPolicy.notify(c,scene.id))return;
    String path="v1";
    if(OppoFluidPolicy.cloud(c,scene.id)){
      if(scene.synthetic)path=OppoFluidPolicy.path(c);
      else if("camp".equals(scene.id)&&CampModeManager.active(c))path=OppoFluidPolicy.path(c);
      else if("climate".equals(scene.id)||"air".equals(scene.id)){
        long started=store(c).getLong("self_started_"+scene.id,0),now=System.currentTimeMillis();
        if(started>0&&now>=started&&now-started<120*60_000L)path=OppoFluidPolicy.path(c);
      }
    }
    String signature=scene.title+"|"+scene.detail+"|"+scene.firstAction+"|"+scene.secondAction+"|"+
        OppoFluidPolicy.level(c)+"|"+OppoFluidPolicy.style(c)+"|"+path+"|"+OppoFluidPolicy.action(c,scene.id);
    String key="active_sig_"+scene.id;
    android.content.SharedPreferences p=store(c);
    if(signature.equals(p.getString(key,"")))return;
    Notification n=build(c,scene,OppoFluidPolicy.style(c),OppoFluidPolicy.level(c),
        path,OppoFluidPolicy.action(c,scene.id));
    manager(c).notify(itemId(scene.id,false),n);
    p.edit().putString(key,signature).apply();
  }
  public static String diagnose(Context c){
    boolean live=Build.VERSION.SDK_INT>=36;
    String capability=tr(c,"不可判定（Android < 16）","Unavailable (Android < 16)");
    if(live){
      try{Method method=NotificationManager.class.getMethod("canPostPromotedNotifications");
        capability=String.valueOf(method.invoke(manager(c)));}
      catch(Exception e){capability=tr(c,"系统 API 不可用","API unavailable");}
    }
    return "OPPO FluidCloud Lab / Beta45\n"+
        "master="+OppoFluidPolicy.enabled(c)+" / notification permission="+canNotify(c)+
        "\ncompatibility path="+OppoFluidPolicy.path(c)+" / style="+OppoFluidPolicy.style(c)+
        " / level="+OppoFluidPolicy.level(c)+" / actions="+OppoFluidPolicy.actions(c)+
        "\nAndroid="+Build.VERSION.SDK_INT+" / Android 16 promoted="+capability+
        "\nOPPO Pantanal official registration="+tr(c,"未配置（需要平台审批及 serviceId）","Not registered (requires approved serviceId)")+
        "\nlast synthetic test="+store(c).getString("last_test_scene","none")+
        "\n"+tr(c,"仅验证通知提交/构造；系统胶囊是否呈现必须真机观察。","Notification construction only; real capsule display requires device observation.");
  }
}
