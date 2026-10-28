package io.github.polestarcn.companion;
import android.content.*;
/** Display parameters only. Never creates requests or changes vehicle settings. */
public final class EffectSettings {
 public static final String[] STYLES={"c1a","c1d","c1+a","c1+d","c1+c"};
 public final boolean master,charge,air,absorb,fade;public final int style,particles,lines,strength;public final float chargeSpeed,airSpeed;
 public EffectSettings(Context c){migrate(c);master=AppPreferences.visible(c,"light_effects",true);charge=AppPreferences.visible(c,"fx_charge",true);air=AppPreferences.visible(c,"fx_air",true);absorb="absorb".equals(AppPreferences.get(c,"fx_direction","up"));fade=AppPreferences.visible(c,"fx_fade",true);int s=java.util.Arrays.asList(STYLES).indexOf(AppPreferences.get(c,"fx_style","c1a"));style=Math.max(0,s);particles=number(c,"fx_particles",60,20,150);lines=number(c,"fx_lines",36,12,120);strength=number(c,"fx_strength",60,20,100);chargeSpeed=number(c,"fx_charge_speed",100,50,200)/100f;airSpeed=number(c,"fx_air_speed",100,50,200)/100f;}
 private EffectSettings(EffectSettings p,int s,boolean direction){master=p.master;charge=p.charge;air=p.air;absorb=direction;fade=p.fade;style=Math.max(0,Math.min(4,s));particles=p.particles;lines=p.lines;strength=p.strength;chargeSpeed=p.chargeSpeed;airSpeed=p.airSpeed;}
 EffectSettings previewCharge(int s,boolean direction){return new EffectSettings(this,s,direction);}
 private static void migrate(Context c){SharedPreferences p=c.getSharedPreferences("preferences",0);if(p.getBoolean("fx_legacy_migrated",false))return;SharedPreferences.Editor e=p.edit().putBoolean("fx_legacy_migrated",true);
  String[][] keys={{"effect_enabled","fx_charge"},{"effect_enabled","fx_air"},{"effect_speed","fx_charge_speed"},{"effect_speed","fx_air_speed"},{"effect_particles","fx_particles"},{"effect_wind_strength","fx_strength"}};
  for(String[] key:keys)if(p.contains(key[0])&&!p.contains(key[1]))e.putString(key[1],p.getString(key[0],""));
  if(p.contains("effect_charge_style")&&!p.contains("fx_style"))e.putString("fx_style",p.getString("effect_charge_style","C1a").toLowerCase(java.util.Locale.ROOT));
  if(p.contains("effect_charge_direction")&&!p.contains("fx_direction"))e.putString("fx_direction","absorb".equals(p.getString("effect_charge_direction","rise"))?"absorb":"up");
  if(p.contains("effect_wind_count")&&!p.contains("fx_lines"))try{e.putString("fx_lines",""+(Integer.parseInt(p.getString("effect_wind_count","12"))*3));}catch(Exception ignored){}
  e.apply();
 }
 static int number(Context c,String key,int fallback,int low,int high){try{return Math.max(low,Math.min(high,Integer.parseInt(AppPreferences.get(c,key,""+fallback))));}catch(Exception e){return fallback;}}
 public static void defaults(Context c){android.content.SharedPreferences.Editor e=c.getSharedPreferences("preferences",0).edit();for(String key:new String[]{"light_effects","all_good_shimmer","charging_animation","page_transition","fx_charge","fx_air","fx_direction","fx_fade","fx_style","fx_particles","fx_lines","fx_strength","fx_charge_speed","fx_air_speed"})e.remove(key);e.apply();}
}
