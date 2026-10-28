package io.github.polestarcn.companion;
import android.content.Context;
/** Appearance-only animation configuration; does not issue commands or infer vehicle operation from a tap. */
public final class VehicleEffectSettings {
  public enum Mode { NONE, CHARGING, COOLING, HEATING, VENTILATION }
  private VehicleEffectSettings() {}
  public static int intValue(Context c,String key,int fallback,int min,int max) {
    try {return Math.max(min,Math.min(max,Integer.parseInt(AppPreferences.get(c,key,""+fallback))));}
    catch(Exception e){return fallback;}
  }
  public static float speed(Context c){return intValue(c,"effect_speed",100,50,200)/100f;}
  public static int count(Context c){return intValue(c,"effect_particles",60,20,150);}
  public static int airflow(Context c){return intValue(c,"effect_wind_count",12,3,30);}
  public static int strength(Context c){return intValue(c,"effect_wind_strength",65,20,100);}
  public static String style(Context c){String style=AppPreferences.get(c,"effect_charge_style","C1a");
    for(String allowed:new String[]{"C1a","C1d","C1+a","C1+d","C1+c"})if(style.equals(allowed))return style;
    return "C1a";
  }
  public static boolean absorption(Context c){return "absorb".equals(AppPreferences.get(c,"effect_charge_direction","rise"));}
  public static Mode vehicle(Context c,VehicleSnapshot s){
    if(!AppPreferences.visible(c,"effect_enabled",true)||s==null||s.isDemo||!"LIVE".equals(s.source))return Mode.NONE;
    if(ChargingVisibility.shouldShow(s))return Mode.CHARGING;
    if("开启".equals(s.air))return Mode.VENTILATION;
    if("开启".equals(s.climate)){
      // China's current telemetry reports on/off without trustworthy cooling/heating mode.
      // User picks a visual preference; this is not a claim that HVAC confirmed heat/cool.
      return "heat".equals(AppPreferences.get(c,"effect_climate_visual","cool"))?Mode.HEATING:Mode.COOLING;
    }
    return Mode.NONE;
  }
}
