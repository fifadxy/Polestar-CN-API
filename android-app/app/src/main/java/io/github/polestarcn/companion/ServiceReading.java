package io.github.polestarcn.companion;
import android.content.Context;
/** Remaining service values always come from vehicle Health; no local record substitution. */
public final class ServiceReading {
  public static final int REFERENCE_DAYS=365,REFERENCE_KM=20_000;
  private ServiceReading(){}
  public static Integer days(Context c,VehicleSnapshot v){return v==null?null:v.health.serviceDaysLeft;}
  public static Integer km(Context c,VehicleSnapshot v){return v==null?null:v.health.serviceKmLeft;}
  public static Integer percent(Integer left,int cycle){return left==null||cycle<=0?null:(int)Math.max(0,Math.min(100,Math.round(left*100.0/cycle)));}
  public static Integer timePercent(Context c,VehicleSnapshot v){return percent(days(c,v),REFERENCE_DAYS);}
  public static Integer distancePercent(Context c,VehicleSnapshot v){return percent(km(c,v),REFERENCE_KM);}
  public static String number(Integer value,String unit){return value==null?"--":value+unit;}
}
