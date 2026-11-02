package io.github.polestarcn.companion;
import android.content.Context;
import android.graphics.Typeface;
/** Display-only feedback settings; none of these preferences affect remote control validation. */
public final class ButtonFeedback {
 private ButtonFeedback(){}
 public static String mode(Context c,String control){String v=AppPreferences.get(c,"feedback_"+control,"state");return v.equals("hidden")||v.equals("state")||v.equals("temperature")||v.equals("remaining")||v.equals("air")||v.equals("climate")?v:"state";}
 public static int sp(Context c){try{return Math.max(8,Math.min(14,Integer.parseInt(AppPreferences.get(c,"feedback_sp","10"))));}catch(NumberFormatException e){return 10;}}
 public static Typeface typeface(Context c){return AppPreferences.visible(c,"feedback_bold",false)?PsaDesign.display():PsaDesign.regular();}
 public static String value(Context c,String control,VehicleSnapshot s,String state){
  if(UiDisplayMode.minimal(c)&&!showMinimal(c,control,s,state))return "";

  if(!"hidden".equals(mode(c,control))&&pending(state))return state;
  if("camp".equals(control)&&!"hidden".equals(mode(c,control))&& (CampModeManager.active(c)||"STOPPING".equals(CampModeManager.phase(c)))) return CampModeManager.remainingCaption(c);
  if("camp".equals(control)&&"state".equals(mode(c,control)))return ControlStatusText.value(c,"camp",s,state);
  switch(mode(c,control)){
   case "hidden":return "";
   case "climate":return ControlStatusText.value(c,control,s,state);
   case "temperature":return DisplayMetric.TEMPERATURE.value(c,s,true,AppPreferences.english(c));
   case "remaining":return s!=null&&"开启".equals(s.climate)?ClimateRuntime.shortLabel(c,s,AppPreferences.get(c,"vehicle",""),AppPreferences.english(c)):"--";
   case "air":return "AQI "+DisplayMetric.AQI.value(c,s,false,AppPreferences.english(c));
   default:return ControlStatusText.value(c,control,s,state);
  }
 }
 private static boolean pending(String state){return "发送中".equals(state)||"Sending".equals(state)||"待确认".equals(state)||"Verifying".equals(state);}
 static boolean showMinimal(Context c,String control,VehicleSnapshot s,String pending){
  if(pending(pending))return true;
  if("camp".equals(control))return CampModeManager.active(c)||"STOPPING".equals(CampModeManager.phase(c));
  if(s==null)return false;
  String raw="climate".equals(control)?s.climate:"air".equals(control)?s.air:"lock".equals(control)?s.lock:"trunk".equals(control)?s.tailgate:"windows".equals(control)?s.windows:"";
  return "开启".equals(raw)||"未锁".equals(raw)||"已打开".equals(raw)||"未关严".equals(raw)||raw.contains("个开启");
 }
}