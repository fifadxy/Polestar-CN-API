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
    return OppoDeviceIntegration.enabled(c)&&AppPreferences.visible(c,"oppo_dynamic_refresh",true);
  }
  static int minute(Context c,String key,int def,int... allowed){
    return RefreshPolicy.minutes(c,key,def,allowed);
  }
  public static int regularMinutes(Context c,boolean front){
    return front?minute(c,"oppo_refresh_foreground_minutes",10,2,5,10,15,30)
        :minute(c,"oppo_refresh_background_minutes",30,15,30,60,120);
  }
  public static int activeMinutes(Context c,boolean front){
    return front?minute(c,"oppo_active_foreground_minutes",2,1,2,5,10)
        :minute(c,"oppo_active_background_minutes",15,15,30,60);
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
    return s!=null&&("开启".equals(s.climate)||"开启".equals(s.air)||CampModeManager.active(c));
  }
  /** Milliseconds, allowing sub-minute foreground reads only during a bounded lock boost. */
  public static long ttl(Context c,RefreshDomain domain,boolean front,VehicleSnapshot state,
      String vin,long configuredMs,long now){
    if(!enabled(c))return configuredMs;
    long period=configuredMs;
    if(domain==RefreshDomain.CORE){
      long requested=regularMinutes(c,front)*60_000L;
      if(lockBoostActive(c,vin,now))requested=(front?lockBoostSeconds(c):60)*1000L;
      else if(running(c,state)||featureBoostActive(c,vin,now))
        requested=activeMinutes(c,front)*60_000L;
      period=period<=0?requested:Math.min(period,requested);
    }else if(domain==RefreshDomain.CLIMATE && state!=null&&"开启".equals(state.climate)){
      long requested=activeMinutes(c,front)*60_000L;
      period=period<=0?requested:Math.min(period,requested);
    }else if(domain==RefreshDomain.AIR && state!=null&&"开启".equals(state.air)){
      long requested=activeMinutes(c,front)*60_000L;
      period=period<=0?requested:Math.min(period,requested);
    }else if(domain==RefreshDomain.CLIMATE&&featureBoostActive(c,vin,now)){
      long requested=activeMinutes(c,front)*60_000L;
      period=period<=0?requested:Math.min(period,requested);
    }
    return period;
  }
  public static int backgroundMinutes(Context c,VehicleSnapshot state,int ordinary){
    if(!OppoDeviceIntegration.enabled(c))return ordinary;
    int wanted=(enabled(c)&&running(c,state))?activeMinutes(c,false):regularMinutes(c,false);
    return ordinary<=0?wanted:Math.min(ordinary,wanted);
  }
}
