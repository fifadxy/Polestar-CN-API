package io.github.polestarcn.companion;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.*;

/** Launcher aliases are cosmetic only. MainActivity is never disabled or killed. */
public final class AppIcons {
  public static final String[] STYLES={"outline","red","skid","classic"};
  public static final String[] MODES={"auto","day","night"};
  private AppIcons(){}
  private static String valid(String value,String[] values){return Arrays.asList(values).contains(value)?value:values[0];}
  public static String style(Context c){return valid(AppPreferences.get(c,"app_icon_style","outline"),STYLES);}
  public static String mode(Context c){return valid(AppPreferences.get(c,"app_icon_mode","auto"),MODES);}
  public static String label(Context c,String style){return style.equals("classic")?AppPreferences.tr(c,"确认稿 · 黑白车辆","Confirmed · classic black/white car"):style.equals("red")?AppPreferences.tr(c,"红色车辆","Red car"):style.equals("skid")?AppPreferences.tr(c,"防滑图形","Skid symbol"):AppPreferences.tr(c,"车辆轮廓","Car outline");}
  static ComponentName component(Context c,String style,String mode){return new ComponentName(c.getPackageName(),"io.github.polestarcn.companion.Launcher"+cap(style)+cap(mode));}
  private static String cap(String s){return Character.toUpperCase(s.charAt(0))+s.substring(1);}
  public static int small(Context c){return small(style(c));}
  public static int notificationSmall(Context c){String style=AppPreferences.get(c,"notification_icon_style","follow");return small(style.equals("follow")?style(c):valid(style,STYLES));}
  public static int small(String style){return style.equals("classic")?R.drawable.ic_status_classic: style.equals("red")?R.drawable.ic_status_red:style.equals("skid")?R.drawable.ic_status_skid:R.drawable.ic_status_outline;}
  public static int launcher(String style,String mode){
    if(style.equals("classic"))return mode.equals("day")?R.mipmap.ic_launcher_classic_day:mode.equals("night")?R.mipmap.ic_launcher_classic_night:R.mipmap.ic_launcher_classic_auto;
    if(style.equals("red"))return mode.equals("day")?R.mipmap.ic_launcher_red_day:mode.equals("night")?R.mipmap.ic_launcher_red_night:R.mipmap.ic_launcher_red_auto;
    if(style.equals("skid"))return mode.equals("day")?R.mipmap.ic_launcher_skid_day:mode.equals("night")?R.mipmap.ic_launcher_skid_night:R.mipmap.ic_launcher_skid_auto;
    return mode.equals("day")?R.mipmap.ic_launcher_outline_day:mode.equals("night")?R.mipmap.ic_launcher_outline_night:R.mipmap.ic_launcher_outline_auto;
  }
  public static boolean select(Context c,String style,String mode){
    if(!Arrays.asList(STYLES).contains(style)||!Arrays.asList(MODES).contains(mode))return false;
    if(!apply(c,style,mode)){apply(c,style(c),mode(c));return false;}
    AppPreferences.set(c,"app_icon_style",style);AppPreferences.set(c,"app_icon_mode",mode);
    ResidentService.update(c);WidgetRenderer.refreshAll(c);return true;
  }
  public static void sync(Context c){apply(c,style(c),mode(c));}
  private static boolean apply(Context c,String style,String mode){
    PackageManager pm=c.getPackageManager();ComponentName selected=component(c,style,mode);
    try{
      List<PackageManager.ComponentEnabledSetting> batch=Build.VERSION.SDK_INT>=33?new ArrayList<>():null;
      // Enable first on older Android so an interrupted change never removes the last launcher.
      if(Build.VERSION.SDK_INT<33&&pm.getComponentEnabledSetting(selected)!=PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
        pm.setComponentEnabledSetting(selected,PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);
      for(String s:STYLES)for(String m:MODES){ComponentName name=component(c,s,m);int wanted=name.equals(selected)?PackageManager.COMPONENT_ENABLED_STATE_ENABLED:PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        if(pm.getComponentEnabledSetting(name)==wanted)continue;
        if(Build.VERSION.SDK_INT>=33)batch.add(new PackageManager.ComponentEnabledSetting(name,wanted,PackageManager.DONT_KILL_APP));
        else pm.setComponentEnabledSetting(name,wanted,PackageManager.DONT_KILL_APP);
      }
      if(Build.VERSION.SDK_INT>=33&&!batch.isEmpty())pm.setComponentEnabledSettings(batch);
      return true;
    }catch(RuntimeException error){DiagnosticLog.add("Launcher icon switch unavailable");return false;}
  }
}
