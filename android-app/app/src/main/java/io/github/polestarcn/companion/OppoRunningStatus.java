package io.github.polestarcn.companion;

import android.content.Context;
import android.os.Bundle;

/** Displays an observed running timer, never asserts that the car has stopped. */
public final class OppoRunningStatus {
  private OppoRunningStatus(){}
  public static void write(Bundle out,Context c,VehicleSnapshot s){
    boolean active=AppPreferences.visible(c,"oppo_dynamic_status",true);
    out.putBoolean("oppoDynamicStatusEnabled",active);
    out.putString("oppoDynamicLabel",active?label(c,s,System.currentTimeMillis()):"");
  }
  public static String label(Context c,VehicleSnapshot state,long now){
    if(state==null||state.isDemo)return "";
    boolean en=AppPreferences.english(c);
    CampSession camp=CampModeManager.session(c);
    if(camp!=null&&CampModeManager.active(c)){
      VehicleCoordinator car=VehicleCoordinator.get(c);
      if(car.selected!=null&&camp.vin.equals(car.selected.vin)){
        long minutes=Math.max(0,(camp.endsAt-now+59999L)/60000L);
        return (en?"Camp":"托管")+minutes+"m";
      }
    }
    if("开启".equals(state.climate)){
      Integer left=ClimateRuntime.remainingMinutes(state,now);
      if(left==null){
        long remaining=ClimateRuntime.estimatedRemaining(c,AppPreferences.get(c,"vehicle",""),now);
        if(remaining>0)left=(int)((remaining+59999L)/60000L);
      }
      return (en?"Climate":"空调")+(left!=null&&left>0?left+"m":en?" running":"运行中");
    }
    if("开启".equals(state.air)){
      Long left=AirRuntime.remaining(c,state,AppPreferences.get(c,"vehicle",""),now);
      return (en?"Air":"净化")+(left!=null&&left>0?(left+59999L)/60000L+"m":en?" running":"运行中");
    }
    return "";
  }
}
