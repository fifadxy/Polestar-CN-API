package io.github.polestarcn.companion;
import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
/** A user-defined *display* format; the vehicle-reported timestamp is never replaced. */
final class WidgetTimestampFormat {
  static final String KEY="widget_timestamp_format";
  static final String DEFAULT="MM-dd HH:mm";
  static final String[] PRESETS={"MM-dd HH:mm","MM/dd HH:mm","MM-dd","HH:mm","yyyy-MM-dd HH:mm"};
  private WidgetTimestampFormat(){}
  static boolean valid(String value){
    return value!=null&&value.length()>=3&&value.length()<=30
        &&value.matches("(?:yyyy|yy|MM|M|dd|d|HH|H|mm|m|[-/.: 年月日 ])+")
        &&value.matches(".*[yMdH].*");
  }
  static String pattern(Context c){String candidate=AppPreferences.get(c,KEY,DEFAULT);return valid(candidate)?candidate:DEFAULT;}
  static String format(Context c,long timestamp){
    if(timestamp<=0)return "--";
    try{return new SimpleDateFormat(pattern(c),Locale.getDefault()).format(new Date(timestamp));}
    catch(IllegalArgumentException e){return new SimpleDateFormat(DEFAULT,Locale.getDefault()).format(new Date(timestamp));}
  }
  static String text(Context c,long timestamp,boolean compact){
    String date=format(c,timestamp);
    return compact?date:AppPreferences.widgetTr(c,"更新 ","Upd ")+date;
  }
  static String compactText(String raw){return raw.replaceFirst("^(更新\\s*|Upd\\s*)","");}
}
