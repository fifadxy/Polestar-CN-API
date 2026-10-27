package io.github.polestarcn.companion;
import android.content.Context;
/** Vehicle remaining values take precedence; percentages are relative to user-configured cycles. */
public final class ServiceReading {
  private ServiceReading(){}
  public static Integer days(Context c,VehicleSnapshot v){
    if(v!=null&&v.health.serviceDaysLeft!=null)return v.health.serviceDaysLeft;
    MaintenanceSchedule.Status local=MaintenanceSchedule.status(c,AppPreferences.get(c,"vehicle",""),v);
    return local.configured?(int)Math.max(Integer.MIN_VALUE,local.remainingDays()):null;
  }
  public static Integer km(Context c,VehicleSnapshot v){
    if(v!=null&&v.health.serviceKmLeft!=null)return v.health.serviceKmLeft;
    MaintenanceSchedule.Status local=MaintenanceSchedule.status(c,AppPreferences.get(c,"vehicle",""),v);
    return local.configured?local.remainingKm():null;
  }
  public static Integer percent(Integer left,int cycle){return left==null||cycle<=0?null:(int)Math.max(0,Math.min(100,Math.round(left*100.0/cycle)));}
  public static Integer timePercent(Context c,VehicleSnapshot v){return percent(days(c,v),MaintenanceSchedule.intervalDays(c));}
  public static Integer distancePercent(Context c,VehicleSnapshot v){return percent(km(c,v),MaintenanceSchedule.intervalKm(c));}
  public static String number(Integer value,String unit){return value==null?"--":value+unit;}
}
