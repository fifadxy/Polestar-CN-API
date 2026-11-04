package io.github.polestarcn.companion;

import android.content.Context;
import android.graphics.Color;
import java.util.Locale;

/** Global widget text palette across Classic, VoLike, Polestar VI and PSeu. */
public final class WidgetColors {
  public static final String[] KEYS={"system","light","dark","vi_orange","nordic_teal","stone_gray","custom"};
  private WidgetColors(){}
  public static int border(Context c){return WidgetAppearance.border(c);}
  public static int borderDrawable(Context c,boolean on){switch(AppPreferences.get(c,"widget_border","white")){case "orange":return on?R.drawable.widget_border_orange_on:R.drawable.widget_border_orange_clear;case "black":return on?R.drawable.widget_border_black_on:R.drawable.widget_border_black_clear;default:return on?R.drawable.widget_border_white_on:R.drawable.widget_border_white_clear;}}
  public static boolean explicit(Context c) { return !"system".equals(AppPreferences.get(c,"widget_ink","system")); }
  public static int ink(Context c) {
    String choice=AppPreferences.get(c,"widget_ink","system");
    int base;
    switch(choice){
      case "light": base=Color.WHITE;break;
      case "dark": base=Color.rgb(20,22,24);break;
      case "vi_orange": base=PsaDesign.ORANGE;break;
      case "nordic_teal": base=Color.rgb(103,172,160);break;
      case "stone_gray": base=Color.rgb(137,144,150);break;
      case "custom": {
        String hex=AppPreferences.get(c,"widget_ink_custom","#FF6B00");
        try{base=Color.parseColor(hex);}catch(IllegalArgumentException ignored){base=PsaDesign.ORANGE;}break;
      }
      default:base=PsaDesign.ink(AppPreferences.dark(c));
    }
    return WidgetAppearance.alpha(base,WidgetAppearance.percent(c,"widget_text_opacity",100));
  }
  public static int secondary(Context c,int primary) {
    if(!explicit(c))return WidgetAppearance.alpha(PsaDesign.muted(AppPreferences.dark(c)),WidgetAppearance.percent(c,"widget_text_opacity",100));
    return WidgetAppearance.alpha(Color.argb(218,Color.red(primary),Color.green(primary),Color.blue(primary)),WidgetAppearance.percent(c,"widget_text_opacity",100));
  }
  public static boolean validHex(String text){return text!=null&&text.toUpperCase(Locale.ROOT).matches("#[0-9A-F]{6}");}
  public static String label(Context c,String key) {
    switch(key){
      case "light":return AppPreferences.tr(c,"白色","White");
      case "dark":return AppPreferences.tr(c,"黑色","Black");
      case "vi_orange":return AppPreferences.tr(c,"VI 橙色","VI orange");
      case "nordic_teal":return AppPreferences.tr(c,"北欧青绿","Nordic teal");
      case "stone_gray":return AppPreferences.tr(c,"石墨灰","Stone grey");
      case "custom":return AppPreferences.tr(c,"自定义 HEX 颜色","Custom HEX color");
      default:return AppPreferences.tr(c,"跟随系统深浅色","Follow system appearance");
    }
  }
}
