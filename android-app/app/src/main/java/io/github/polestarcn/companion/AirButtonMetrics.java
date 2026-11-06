package io.github.polestarcn.companion;
import android.content.Context;
import java.util.Locale;
/** Optional PM2.5/AQI readouts below purification. No synthetic air-quality values. */
public final class AirButtonMetrics {
  public static final String KEY="air_button_metrics";
  private AirButtonMetrics(){}
  /** Keep both readings on one UI line without changing sensor values or preferences. */
  public static String singleLine(String value){return value==null?"":value.replace("\n"," · ");}
  public static String mode(Context c){
    String mode=AppPreferences.get(c,KEY,"both");
    return mode.equals("pm25")||mode.equals("aqi")||mode.equals("off")?mode:"both";
  }
  public static String value(Context c,VehicleSnapshot s,boolean compact){
    String mode=mode(c);if("off".equals(mode))return "";
    Double pm=s==null?null:s.environment.pm25UgM3;
    Integer aqi=s==null?null:s.environment.aqi;
    String particulate=pm==null?"--":String.format(Locale.getDefault(),pm%1==0?"%.0f":"%.1f",pm);
    String quality=aqi==null?"--":Integer.toString(aqi);
    if("pm25".equals(mode))return "PM2.5 "+particulate;
    if("aqi".equals(mode))return "AQI "+quality;
    return "PM2.5 "+particulate+(compact?"\n":" · ")+"AQI "+quality;
  }
}