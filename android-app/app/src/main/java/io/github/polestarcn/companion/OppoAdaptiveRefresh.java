package io.github.polestarcn.companion;

import android.content.Context;
import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** OPPO read eligibility only. Never bypasses authorization, command readback or Android Doze. */
public final class OppoAdaptiveRefresh {
  private static final String PREF="oppo-adaptive-refresh-v1";
  private OppoAdaptiveRefresh(){}
  public static boolean enabled(Context c){
    return AppPreferences.visible(c,"oppo_dynamic_refresh",true);
  }
  static int minute(Context c,String key,int def,int... allowed){
    return RefreshPolicy.minutes(c,key,def,allowed);
  }
  public static int regularMinutes(Context c,boolean front){
    return front?minute(c,"oppo_refresh_foreground_minutes",10,2,5,10,15,30)
        :minute(c,"oppo_refresh_background_minutes",30,15,30,60,120);
  }
  public static int activeMinutes(Context c,boolean front){
    return front?minute(c,"oppo_active_foreground_minutes",1,1,2,5,10)
        :minute(c,"oppo_active_background_minutes",10,10,15,30,60);
  }
  public static int lockBoostMinutes(Context c){
    return minute(c,"oppo_lock_boost_minutes",3,1,3,5,10);
  }
  public static int lockBoostSeconds(Context c){
    return minute(c,"oppo_lock_boost_seconds",30,30,60,120);
  }
  public static int bridgeCooldownSeconds(Context c,String vin,long now){
    int normal=minute(c,"oppo_refresh_cooldown_seconds",60,30,60,120,300);
    if(enabled(c)&&lockBoostActive(c,vin,now))return Math.min(normal,lockBoostSeconds(c));
    return normal;
  }
  private static String key(String vin){
    if(vin==null||vin.isEmpty())return "";
    try{
      byte[] hash=MessageDigest.getInstance("SHA-256").digest(vin.getBytes(StandardCharsets.UTF_8));
      StringBuilder text=new StringBuilder("boost-");
      for(int i=0;i<12;i++)text.append(String.format(Locale.US,"%02x",hash[i]&0xff));
      return text.toString();
    }catch(Exception e){return "";}
  }
  public static void confirmedAction(Context c,String vin,String action,long now){
    if(vin==null||action==null||!enabled(c))return;
    boolean lock="lock".equals(action)||"unlock".equals(action);
    boolean active=action.startsWith("climate-")||action.startsWith("air-");
    if(!lock&&!active)return;
    String key=key(vin);if(key.isEmpty())return;
    int mins=lock?lockBoostMinutes(c):2;
    c.getSharedPreferences(PREF,0).edit().putLong(key+(lock?"-lock":"-active"),now+mins*60_000L).apply();
  }
  public static boolean lockBoostActive(Context c,String vin,long now){
    String key=key(vin);if(key.isEmpty()||!enabled(c))return false;
    long until=c.getSharedPreferences(PREF,0).getLong(key+"-lock",0L);
    return now>0&&until>=now&&until-now<=10*60_000L;
  }
  static boolean featureBoostActive(Context c,String vin,long now){
    String key=key(vin);if(key.isEmpty()||!enabled(c))return false;
    long until=c.getSharedPreferences(PREF,0).getLong(key+"-active",0L);
    return now>0&&until>=now&&until-now<=2*60_000L;
  }
  public static boolean running(Context c,VehicleSnapshot s){
    return CampModeManager.active(c)
        ||s!=null&&("开启".equals(s.climate)||"开启".equals(s.air)
        ||ChargingVisibility.shouldShow(s));
  }
  /** Milliseconds, allowing sub-minute foreground reads only during a bounded lock boost. */
  /** Running HVAC/air/camp: 1 min foreground / 10 min requested background.
   * Charging alone: 3 min foreground / 20 min requested background.
   * Existing commands and Android Doze policy remain independent.
   */
  public static long ttl(Context c,RefreshDomain domain,boolean front,VehicleSnapshot state,
      String vin,long configuredMs,long now){
    if(!enabled(c))return configuredMs;
    boolean climate=state!=null&&"开启".equals(state.climate);
    boolean air=state!=null&&"开启".equals(state.air);
    boolean camp=CampModeManager.active(c);
    boolean charging=state!=null&&ChargingVisibility.shouldShow(state);
    boolean highPriority=climate||air||camp;
    long active=activeMinutes(c,front)*60_000L;
    long charge=(front?3L:20L)*60_000L;
    long requested=0;
    switch(domain){
      case CORE:
        if(lockBoostActive(c,vin,now))requested=(front?lockBoostSeconds(c):60)*1000L;
        else if(highPriority||featureBoostActive(c,vin,now))requested=active;
        else if(charging)requested=charge;
        else if(OppoDeviceIntegration.enabled(c))requested=regularMinutes(c,front)*60_000L;
        break;
      case CLIMATE:
        if(highPriority||featureBoostActive(c,vin,now))requested=active;
        break;
      case AIR:
        if(air||camp)requested=active;
        break;
      case CHARGING:
        if(charging)requested=highPriority?active:charge;
        break;
      default:break;
    }
    if(requested<=0)return configuredMs;
    return configuredMs<=0?requested:Math.min(configuredMs,requested);
  }
  public static int backgroundMinutes(Context c,VehicleSnapshot state,int ordinary){
    if(!enabled(c))return ordinary;
    boolean highPriority=CampModeManager.active(c)
        ||state!=null&&("开启".equals(state.climate)||"开启".equals(state.air));
    boolean charging=state!=null&&ChargingVisibility.shouldShow(state);
    int wanted;
    if(highPriority)wanted=activeMinutes(c,false);
    else if(charging)wanted=20;
    else if(OppoDeviceIntegration.enabled(c))wanted=regularMinutes(c,false);
    else return ordinary;
    return ordinary<=0?wanted:Math.min(ordinary,wanted);
  }
}
