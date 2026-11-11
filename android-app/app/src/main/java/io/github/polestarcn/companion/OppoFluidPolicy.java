package io.github.polestarcn.companion;

import android.content.Context;

/** Local vehicle notification and ColorOS Fluid Cloud compatibility preferences. */
public final class OppoFluidPolicy {
  public static final String MASTER="oppo_compat_fluid_enabled";
  public static final String PATH="oppo_compat_fluid_path";
  public static final String STYLE="oppo_compat_fluid_style";
  public static final String LEVEL="oppo_compat_fluid_level";
  public static final String ACTIONS="oppo_compat_fluid_actions";
  public static final String LOCK_SECONDS="oppo_compat_lock_seconds";
  public static final String WARNING_COOLDOWN="oppo_compat_issue_cooldown";
  public static final String[] SCENES={"lock","unlock","climate","air","camp","charging","fault","status","trunk","windows","find"};
  private OppoFluidPolicy(){}
  public static boolean enabled(Context c){return AppPreferences.visible(c,MASTER,true);}
  public static String path(Context c){String x=AppPreferences.get(c,PATH,"auto");return "v1".equals(x)||"v2".equals(x)?x:"auto";}
  public static String style(Context c){return "full".equals(AppPreferences.get(c,STYLE,"simple"))?"full":"simple";}
  public static int level(Context c){return "2".equals(AppPreferences.get(c,LEVEL,"1"))?2:1;}
  public static boolean actions(Context c){return enabled(c)&&level(c)==2&&AppPreferences.visible(c,ACTIONS,false);}
  public static boolean allowed(String scene){for(String x:SCENES)if(x.equals(scene))return true;return false;}
  public static boolean notify(Context c,String scene){return enabled(c)&&allowed(scene)&&AppPreferences.visible(c,"oppo_compat_notify_"+scene,defaultEnabled(scene));}
  public static boolean cloud(Context c,String scene){return enabled(c)&&allowed(scene)&&AppPreferences.visible(c,"oppo_compat_cloud_"+scene,defaultEnabled(scene));}
  public static boolean action(Context c,String scene){return actions(c)&&allowed(scene)&&AppPreferences.visible(c,"oppo_compat_action_"+scene,false);}
  public static boolean defaultEnabled(String scene){return "lock".equals(scene)||"unlock".equals(scene)||"climate".equals(scene)||"air".equals(scene)||"camp".equals(scene)||"charging".equals(scene)||"fault".equals(scene);}
  public static int lockSeconds(Context c){return ranged(c,LOCK_SECONDS,10,5,30);}
  public static int warningCooldown(Context c){return ranged(c,WARNING_COOLDOWN,60,15,1440);}
  private static int ranged(Context c,String key,int fallback,int min,int max){try{return Math.max(min,Math.min(max,Integer.parseInt(AppPreferences.get(c,key,""+fallback))));}catch(Exception ignored){return fallback;}}
  public static void reset(Context c){android.content.SharedPreferences.Editor edit=c.getSharedPreferences("preferences",0).edit();
    for(String key:new String[]{MASTER,PATH,STYLE,LEVEL,ACTIONS,LOCK_SECONDS,WARNING_COOLDOWN})edit.remove(key);
    for(String scene:SCENES){edit.remove("oppo_compat_notify_"+scene);edit.remove("oppo_compat_cloud_"+scene);edit.remove("oppo_compat_action_"+scene);}edit.apply();}
}
