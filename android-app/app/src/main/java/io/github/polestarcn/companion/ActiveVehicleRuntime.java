package io.github.polestarcn.companion;

import android.content.Context;
import java.util.Locale;

/** Actual reported running states, never inferred from queued commands. */
final class ActiveVehicleRuntime {
  private ActiveVehicleRuntime(){}
  static boolean climate(String value){
    if(value==null)return false;
    String v=value.trim().toLowerCase(Locale.ROOT);
    return v.equals("开启")||v.equals("运行中")||v.equals("制冷")||v.equals("制热")
        ||v.equals("cooling")||v.equals("heating")||v.equals("running")
        ||v.equals("on")||v.equals("active");
  }
  static boolean air(String value){
    if(value==null)return false;
    String v=value.trim().toLowerCase(Locale.ROOT);
    return v.equals("开启")||v.equals("运行中")||v.equals("净化中")
        ||v.equals("purifying")||v.equals("running")||v.equals("on")||v.equals("active");
  }
  static boolean charging(VehicleSnapshot snapshot){
    return snapshot!=null&&ChargingVisibility.shouldShow(snapshot);
  }
  static boolean working(Context c,VehicleSnapshot s){
    return (s!=null&&(climate(s.climate)||air(s.air)||charging(s)))||CampModeManager.active(c);
  }
}
