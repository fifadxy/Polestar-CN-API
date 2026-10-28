package io.github.polestarcn.companion;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.RemoteViews;
import java.util.*;

/** Shared status presentation and selectable widget actions. Every write still opens biometric UI. */
public final class WidgetButtons {
 public static final String[] CHOICES={"lock","lock-fixed","unlock-fixed","climate","climate-start","climate-stop","air","windows","trunk","find","location","status","refresh","charging"};
 private WidgetButtons(){}
 public static String key(WidgetRenderer.Size size){return "widget_buttons_"+size.name();}
 public static List<String> read(Context c,WidgetRenderer.Size size){
  String fallback=size==WidgetRenderer.Size.EXTRA_WIDE?"lock,climate,air,trunk,windows":VoLikeWidgetRenderer.PSEU.equals(VoLikeWidgetRenderer.style(c))&&"controls".equals(AppPreferences.get(c,"pseu_type","battery"))?"lock-fixed,unlock-fixed,climate-start,climate-stop":!VoLikeWidgetRenderer.CLASSIC.equals(VoLikeWidgetRenderer.style(c))&&size!=WidgetRenderer.Size.WIDE&&size!=WidgetRenderer.Size.MINI?"lock,climate,charging,location":"lock,climate";
  String raw=AppPreferences.get(c,key(size),"");if(raw.isEmpty())raw=fallback;LinkedHashSet<String> out=new LinkedHashSet<>();
  for(String v:raw.split(","))if(Arrays.asList(CHOICES).contains(v)||v.equals("climate-start")||v.equals("climate-stop"))out.add(v);return new ArrayList<>(out);
 }
 public static void save(Context c,WidgetRenderer.Size size,List<String> actions){AppPreferences.set(c,key(size),actions.isEmpty()?"none":String.join(",",actions));}
 public static String label(Context c,String id){boolean en=AppPreferences.widgetEnglish(c);switch(id){
  case "lock":return en?"Door lock":"门锁";case "lock-fixed":return en?"Lock":"上锁";case "unlock-fixed":return en?"Unlock":"解锁";
  case "climate":return en?"Climate":"空调";case "climate-start":return en?"Start climate":"开空调";case "climate-stop":return en?"Stop climate":"关空调";
  case "air":return en?"Air":"净化";case "windows":return en?"Windows":"车窗";case "trunk":return en?"Trunk":"尾箱";case "find":return en?"Find car":"寻车";
  case "location":return en?"Location":"位置";case "status":return en?"Status":"车况";case "refresh":return en?"Refresh":"刷新";default:return en?"Charging":"充电";}}
 public static int icon(String id,VehicleSnapshot s){switch(id){
  case "lock":return s==null||ControlAction.next("lock",s)==null?R.drawable.ic_warning:"未锁".equals(s.lock)?R.drawable.ic_unlock:R.drawable.ic_lock;
  case "lock-fixed":return R.drawable.ic_lock;case "unlock-fixed":return R.drawable.ic_unlock;
  case "climate":return s!=null&&"关闭".equals(s.climate)?R.drawable.ic_climate_off:R.drawable.ic_climate;
  case "climate-stop":return R.drawable.ic_climate_off;case "climate-start":return R.drawable.ic_climate;
  case "air":return R.drawable.ic_air;case "windows":return s!=null&&ControlAction.next("windows",s)!=null&&"windows-open".equals(ControlAction.next("windows",s))?R.drawable.ic_window_close:R.drawable.ic_window;
  case "trunk":return s!=null&&("已打开".equals(s.tailgate)||"未关严".equals(s.tailgate))?R.drawable.ic_unlock:R.drawable.ic_lock;
  case "find":return R.drawable.ic_horn;case "location":return R.drawable.ic_location;case "status":return R.drawable.ic_nav_vehicle_data_ref;case "refresh":return R.drawable.ic_refresh;default:return R.drawable.ic_charge;}}
 public static String state(Context c,String id,VehicleSnapshot s){if(s==null)return "--";switch(id){case "lock":case "lock-fixed":case "unlock-fixed":return AppPreferences.widgetStatus(c,s.lock);case "climate":case "climate-start":case "climate-stop":return AppPreferences.widgetStatus(c,s.climate);case "air":return AppPreferences.widgetStatus(c,s.air);case "trunk":return AppPreferences.widgetStatus(c,s.tailgate);case "windows":return AppPreferences.widgetStatus(c,s.windows);case "charging":return AppPreferences.widgetStatus(c,s.charging);case "status":return VehicleCondition.summary(s,AppPreferences.widgetEnglish(c),AppPreferences.visible(c,"show_tire_status",true));default:return "";}}
 private static boolean active(String id,VehicleSnapshot s){if(s==null)return false;switch(id){case "lock":case "unlock-fixed":return "未锁".equals(s.lock);case "lock-fixed":return "已锁".equals(s.lock)||"已上锁".equals(s.lock);case "climate":case "climate-start":return "开启".equals(s.climate);case "climate-stop":return "关闭".equals(s.climate);case "air":return "开启".equals(s.air);case "trunk":return "已打开".equals(s.tailgate)||"未关严".equals(s.tailgate);case "windows":return "windows-close".equals(ControlAction.next("windows",s));case "charging":return ChargingVisibility.shouldShow(s);case "status":return VehicleCondition.problem(s,true);default:return false;}}
 public static String intentAction(String id){switch(id){case "lock":return "lock-choice";case "climate":return "climate-choice";case "lock-fixed":return "widget-lock";case "unlock-fixed":return "widget-unlock";case "climate-start":return "widget-climate-start";case "climate-stop":return "widget-climate-stop";case "charging":return "charging";case "location":case "refresh":case "status":return id;default:return "control-"+id;}}
 public static void bind(RemoteViews v,Context c,int widget,WidgetRenderer.Size size,VehicleSnapshot s,int ink,int limit){
  List<String> chosen=read(c,size);int count=Math.min(limit,chosen.size());int[] roots={R.id.widget_action_lock,R.id.widget_action_climate,R.id.volike_widget_charge,R.id.volike_widget_location,R.id.widget_action_extra4,R.id.widget_action_extra5};
  int[] wrappers={R.id.volike_widget_lock,R.id.volike_widget_climate,R.id.volike_widget_charge,R.id.volike_widget_location,R.id.widget_action_extra4,R.id.widget_action_extra5};
  int[] icons={R.id.widget_lock_icon,R.id.widget_climate_icon,R.id.volike_widget_charge_icon,R.id.volike_widget_location_icon,R.id.widget_extra4_icon,R.id.widget_extra5_icon};
  int[] labels={R.id.widget_lock_label,R.id.widget_climate_label,R.id.widget_extra2_label,R.id.widget_extra3_label,R.id.widget_extra4_label,R.id.widget_extra5_label};
  v.setViewVisibility(R.id.widget_action_row,count==0?View.GONE:View.VISIBLE);
  for(int i=0;i<roots.length;i++){v.setViewVisibility(wrappers[i],i<count?View.VISIBLE:View.GONE);if(i>=count)continue;String id=chosen.get(i);boolean on=active(id,s);int color=on?Color.BLACK:ink;
   v.setInt(roots[i],"setBackgroundResource",on?R.drawable.widget_action_on:R.drawable.widget_action_clear);v.setImageViewResource(icons[i],icon(id,s));v.setInt(icons[i],"setBackgroundResource",0);v.setInt(icons[i],"setColorFilter",color);
   String name=label(c,id),state=state(c,id,s);String visible=(id.equals("lock")||id.equals("climate")||id.equals("air"))?state:name;
   android.text.SpannableString text=new android.text.SpannableString(visible);if(AppPreferences.visible(c,"widget_bold",false))text.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),0,text.length(),0);
   v.setTextViewText(labels[i],text);v.setTextColor(labels[i],color);v.setTextViewTextSize(labels[i],android.util.TypedValue.COMPLEX_UNIT_SP,9*UiTypography.widgetScale(c));
   v.setContentDescription(roots[i],name+" · "+state+AppPreferences.widgetTr(c," · 点击操作，远控需生物验证"," · Tap to act; remote control requires biometrics"));
   v.setOnClickPendingIntent(roots[i],WidgetRenderer.pending(c,widget,size,intentAction(id)));v.setOnClickPendingIntent(wrappers[i],WidgetRenderer.pending(c,widget,size,intentAction(id)));
  }
 }
}
