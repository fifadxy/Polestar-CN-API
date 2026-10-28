package io.github.polestarcn.companion;
import android.content.Context;
/** Experimental app limits, not a declaration of the vehicle's accepted range. No command retries. */
public final class ClimateTarget {
 public static final int MIN=18,MAX=28,DEFAULT=22;
 private ClimateTarget(){}
 public static boolean enabled(Context c){return AppPreferences.visible(c,"climate_custom_target",false);}
 public static int selected(Context c){try{int value=Integer.parseInt(AppPreferences.get(c,"climate_target_c","22"));return value>=MIN&&value<=MAX?value:DEFAULT;}catch(NumberFormatException e){return DEFAULT;}}
 public static float request(Context c){return enabled(c)?selected(c):DEFAULT;}
 public static boolean confirmed(VehicleSnapshot state,float wanted,long issuedAt){return state!=null&&!state.isDemo&&"LIVE".equals(state.source)&&"开启".equals(state.climate)&&state.environment.targetTemperatureC!=null&&state.environment.climateReportedAt>=issuedAt&&state.environment.climateReportedAt<=System.currentTimeMillis()+300_000L&&Math.abs(state.environment.targetTemperatureC-wanted)<.05;}
 public static void validate(float value){if(!Float.isFinite(value)||value<MIN||value>MAX)throw new IllegalArgumentException("Experimental climate target outside app limits");}
}
