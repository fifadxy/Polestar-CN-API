package io.github.polestarcn.companion;

import android.content.Context;
import android.content.SharedPreferences;

/** Display-only purification timer. A locally confirmed start runs for five minutes. */
public final class AirRuntime {
  private static final String PREF="air-runtime-v1";
  private static final long DURATION=300_000L;
  private AirRuntime() {}
  public static void started(Context c,String vehicle,long now){
    c.getSharedPreferences(PREF,0).edit().putString("vehicle",vehicle).putLong("started",now).apply();
  }
  public static void stopped(Context c){c.getSharedPreferences(PREF,0).edit().clear().apply();}
  static Long remaining(Context c,VehicleSnapshot s,String vehicle,long now){
    if(s==null||!"开启".equals(s.air))return null;
    SharedPreferences prefs=c.getSharedPreferences(PREF,0);
    long at=prefs.getLong("started",0);
    if(vehicle!=null&&vehicle.equals(prefs.getString("vehicle",""))&&at>0&&now>=at)
      return Math.max(0,DURATION-(now-at));
    Integer minutes=s.environment.airRuntimeMinutes;
    long reported=s.environment.airReportedAt;
    if(minutes==null||reported<=0||reported>now)return null;
    return Math.max(0,Math.min(DURATION,minutes*60_000L)-(now-reported));
  }
  public static String label(Context c,VehicleSnapshot s,String vehicle){
    if(s==null)return "--";
    if("关闭".equals(s.air))return AppPreferences.tr(c,"关闭","Off");
    if(!"开启".equals(s.air))return AppPreferences.status(c,s.air);
    Long left=remaining(c,s,vehicle,System.currentTimeMillis());
    if(left==null)return AppPreferences.tr(c,"开启","On");
    if(left==0)return AppPreferences.tr(c,"待确认","Verify");
    long seconds=(left+999)/1000;
    return String.format(java.util.Locale.ROOT,"%d:%02d",seconds/60,seconds%60);
  }
}
