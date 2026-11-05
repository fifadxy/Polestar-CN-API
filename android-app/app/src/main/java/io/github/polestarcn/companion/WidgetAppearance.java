package io.github.polestarcn.companion;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import java.util.Locale;

/** Independent and backwards-compatible widget ink, outer border and action styling. */
public final class WidgetAppearance {
  private WidgetAppearance() {}
  public static final String[] PALETTE={"system","white","black","vi_orange","nordic_teal","stone_gray","custom"};

  public static int percent(Context c,String key,int fallback){
    try { return Math.max(0,Math.min(100,Integer.parseInt(AppPreferences.get(c,key,""+fallback)))); }
    catch(NumberFormatException ignored){return fallback;}
  }
  public static int width(Context c,String key,int fallback){
    try { return Math.max(0,Math.min(6,Integer.parseInt(AppPreferences.get(c,key,""+fallback)))); }
    catch(NumberFormatException ignored){return fallback;}
  }
  static int alpha(int color,int opacity){
    return Color.argb(Math.round(Color.alpha(color)*opacity/100f),Color.red(color),Color.green(color),Color.blue(color));
  }
  static int palette(Context c,String key,String customKey,String fallback,int system){
    String selected=AppPreferences.get(c,key,fallback);
    switch(selected){
      case "light":case "white":return Color.WHITE;
      case "dark":case "black":return Color.rgb(20,22,24);
      case "orange":case "vi_orange":return PsaDesign.ORANGE;
      case "nordic_teal":return Color.rgb(103,172,160);
      case "stone_gray":return Color.rgb(137,144,150);
      case "custom":
        try{return Color.parseColor(AppPreferences.get(c,customKey,"#FF6B00"));}
        catch(IllegalArgumentException ignored){return system;}
      default:return system;
    }
  }
  public static int text(Context c){
    return alpha(palette(c,"widget_ink","widget_ink_custom","system",PsaDesign.ink(AppPreferences.dark(c))),
      percent(c,"widget_text_opacity",100));
  }
  public static int border(Context c){
    return alpha(palette(c,"widget_border","widget_border_custom","white",Color.WHITE),
      percent(c,"widget_border_opacity",100));
  }
  public static int button(Context c){
    return alpha(palette(c,"widget_button_color","widget_button_custom","vi_orange",PsaDesign.ORANGE),
      percent(c,"widget_button_opacity",100));
  }
  public static boolean validHex(String text){return text!=null&&text.toUpperCase(Locale.ROOT).matches("#[0-9A-F]{6}");}
  public static CharSequence weight(Context c,CharSequence value){
    if(value==null)return "";
    String selected=AppPreferences.get(c,"widget_text_weight","regular");
    boolean bold="bold".equals(selected)||AppPreferences.visible(c,"widget_bold",false);
    if(value.length()==0||(!bold&&!"medium".equals(selected)))return value;
    SpannableStringBuilder text=new SpannableStringBuilder(value);
    if("medium".equals(selected))text.setSpan(new TypefaceSpan("sans-serif-medium"),0,text.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    if(bold&&text.getSpans(0,text.length(),StyleSpan.class).length==0)
      text.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),0,text.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    return text;
  }

  /** RemoteViews cannot accept arbitrary GradientDrawable instances. Draw a small scalable image. */
  public static Bitmap buttonBackground(Context c,boolean active){
    final int w=96,h=64;
    Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
    Canvas canvas=new Canvas(bitmap);
    Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    int fill=button(c),stroke=border(c);
    float inset=2f,radius=11f;
    if(active && Color.alpha(fill)>0){
      paint.setStyle(Paint.Style.FILL);paint.setColor(alpha(fill,22));
      canvas.drawRoundRect(inset,inset,w-inset,h-inset,radius,radius,paint);
    }
    int thickness=width(c,"widget_button_width",1);
    if(thickness>0&&Color.alpha(stroke)>0){
      float half=thickness/2f;paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(thickness);
      paint.setColor(stroke);
      canvas.drawRoundRect(inset+half,inset+half,w-inset-half,h-inset-half,radius,radius,paint);
    }
    return bitmap;
  }
  public static boolean customizedButton(Context c){
    return !"vi_orange".equals(AppPreferences.get(c,"widget_button_color","vi_orange"))
      ||percent(c,"widget_button_opacity",100)!=100||width(c,"widget_button_width",1)!=1
      ||!"white".equals(AppPreferences.get(c,"widget_border","white"))
      ||percent(c,"widget_border_opacity",100)!=100||width(c,"widget_border_width",2)!=2;
  }
  public static String label(Context c,String key){
    switch(key){
      case "white":case "light":return AppPreferences.tr(c,"白色","White");
      case "black":case "dark":return AppPreferences.tr(c,"黑色","Black");
      case "vi_orange":case "orange":return AppPreferences.tr(c,"VI 橙色","VI orange");
      case "nordic_teal":return AppPreferences.tr(c,"北欧青绿","Nordic teal");
      case "stone_gray":return AppPreferences.tr(c,"石墨灰","Stone grey");
      case "custom":return AppPreferences.tr(c,"自定义 HEX","Custom HEX");
      default:return AppPreferences.tr(c,"跟随系统","Follow system");
    }
  }
}