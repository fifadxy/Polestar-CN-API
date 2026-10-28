package io.github.polestarcn.companion;
import android.content.Context;
import android.graphics.Typeface;
/** Display-only feedback settings; none of these preferences affect remote control validation. */
public final class ButtonFeedback {
 private ButtonFeedback(){}
 public static String mode(Context c,String control){String v=AppPreferences.get(c,"feedback_"+control,"climate".equals(control)?"climate":"state");return v.equals("hidden")||v.equals("state")||v.equals("temperature")||v.equals("remaining")||v.equals("air")||v.equals("climate")?v:"state";}
 public static int sp(Context c){try{return Math.max(8,Math.min(14,Integer.parseInt(AppPreferences.get(c,"feedback_sp","10"))));}catch(NumberFormatException e){return 10;}}
 public static Typeface typeface(Context c){return AppPreferences.visible(c,"feedback_bold",false)?PsaDesign.display():PsaDesign.regular();}
 public static String value(Context c,String control,VehicleSnapshot s,String state){
  switch(mode(c,control)){
   case "hidden":return "";
   case "climate":{
    String running=s==null?"--":"开启".equals(s.climate)?AppPreferences.tr(c,"运行中","Running"):AppPreferences.status(c,s.climate);
    String time=s!=null&&"开启".equals(s.climate)?ClimateRuntime.shortLabel(c,s,AppPreferences.get(c,"vehicle",""),AppPreferences.english(c)):"";
    if(time.equals(running))time="";
    String target=EnvironmentSnapshot.temperatureText(s==null?null:s.environment.targetTemperatureC,false).replaceAll("\\.0$","")+"°";
    return running+(time.isEmpty()?"":" "+time)+" · "+target;
   }
   case "temperature":return DisplayMetric.TEMPERATURE.value(c,s,true,AppPreferences.english(c));
   case "remaining":return s!=null&&"开启".equals(s.climate)?ClimateRuntime.shortLabel(c,s,AppPreferences.get(c,"vehicle",""),AppPreferences.english(c)):"--";
   case "air":return "AQI "+DisplayMetric.AQI.value(c,s,false,AppPreferences.english(c));
   default:return state;
  }
 }
}
