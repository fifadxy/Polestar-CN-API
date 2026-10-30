package io.github.polestarcn.companion;

import android.content.Context;
import android.content.res.Configuration;

/** Independent per-skin application type scale; Android system accessibility scale still applies. */
public final class UiTypography {
  private UiTypography() {}
  public static String key(Context c){return "font_percent_"+UiSkin.get(c);}
  public static int percent(Context c,String key){
    try{return Math.max(80,Math.min(120,Integer.parseInt(AppPreferences.get(c,key,"100"))));}
    catch(NumberFormatException e){return 100;}
  }
  public static Context wrap(Context base){
    Configuration config=new Configuration(base.getResources().getConfiguration());
    config.fontScale*=percent(base,key(base))/100f;
    return base.createConfigurationContext(config);
  }
  public static void applyTree(android.view.View v){if(v==null)return;if(v instanceof android.widget.TextView&&AppPreferences.visible(v.getContext(),"ui_all_bold",false)){android.widget.TextView t=(android.widget.TextView)v;t.setTypeface(t.getTypeface(),android.graphics.Typeface.BOLD);}ConditionStyle.symbol(v);if(v instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)v).getChildCount();i++)applyTree(((android.view.ViewGroup)v).getChildAt(i));}
  public static float widgetScale(Context c){return percent(c,"widget_font_percent")/100f;}
}
