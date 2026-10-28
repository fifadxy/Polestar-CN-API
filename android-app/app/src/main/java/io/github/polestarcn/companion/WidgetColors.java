package io.github.polestarcn.companion;

import android.content.Context;
import android.graphics.Color;
import java.util.Locale;

/** Global widget text palette across Classic, VoLike, Polestar VI and PSeu. */
public final class WidgetColors {
  public static final String[] KEYS={"system","light","dark","vi_orange","nordic_teal","stone_gray","custom"};
  private WidgetColors(){}
  public static boolean explicit(Context c) { return !"system".equals(AppPreferences.get(c,"widget_ink","system")); }
  public static int ink(Context c) {
    String choice=AppPreferences.get(c,"widget_ink","system");
    switch(choice){
      case "light": return Color.WHITE;
      case "dark": return Color.rgb(20,22,24);
      case "vi_orange": return PsaDesign.ORANGE;
      case "nordic_teal": return Color.rgb(103,172,160);
      case "stone_gray": return Color.rgb(137,144,150);
      case "custom": {
        String hex=AppPreferences.get(c,"widget_ink_custom","#FF6B00");
        try{return Color.parseColor(hex);}catch(IllegalArgumentException ignored){return PsaDesign.ORANGE;}
      }
      default:return PsaDesign.ink(AppPreferences.dark(c));
    }
  }
  public static int secondary(Context c,int primary) {
    if(!explicit(c))return PsaDesign.muted(AppPreferences.dark(c));
    return Color.argb(218,Color.red(primary),Color.green(primary),Color.blue(primary));
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
