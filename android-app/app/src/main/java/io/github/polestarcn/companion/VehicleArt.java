package io.github.polestarcn.companion;

import android.content.Context;

/** High-resolution alpha gallery. FIT_CENTER only; no image reconstruction or rotation in UI. */
public final class VehicleArt {
  public static final String POLESTAR_2="ps2";
  public static final String[] MODELS={POLESTAR_2};
  public static final String[] TYPES={"auto","overhead","three_quarter","front","side"};
  public static final String[] KEYS={
      "auto","top_white","top_dark","top_silver","top_blue","top_original",
      "angled_original","angled_black","angled_blue","angled_silver","angled_white",
      "front_silver","side_official_snow","side_official_space"
  };

  private VehicleArt() {}

  public static String modelKey(boolean night){return night?"vehicle_art_model_night":"vehicle_art_model_day";}
  public static String selectionKey(boolean night){return night?"vehicle_art_night":"vehicle_art_day";}
  public static String model(Context c){return modelFor(c,AppPreferences.dark(c));}
  public static String modelFor(Context c,boolean night){
    return POLESTAR_2;
  }
  public static String modelLabel(Context c,String model){return AppPreferences.tr(c,"极星 2","Polestar 2");}
  public static String[] types(String model){return TYPES;}
  public static String type(String key){
    if(key.startsWith("top_"))return "overhead";
    if(key.startsWith("angled_"))return "three_quarter";
    if(key.startsWith("front_"))return "front";
    if(key.startsWith("side_"))return "side";
    return "auto";
  }
  public static String typeLabel(Context c,String group){
    switch(group){
      case "overhead":return AppPreferences.tr(c,"顶视图（全部横向）","Overhead (all horizontal)");
      case "three_quarter":return AppPreferences.tr(c,"45°视角","Three-quarter view");
      case "front":return AppPreferences.tr(c,"正面视图","Front view");
      case "side":return AppPreferences.tr(c,"侧面视图","Side view");
      default:return AppPreferences.tr(c,"恢复原版黑/白默认车图","Use original black/white defaults");
    }
  }
  public static String[] variants(String group){return variants(group,POLESTAR_2);}
  public static String[] variants(String group,String model){
    switch(group){
      case "overhead":return new String[]{"top_original","top_dark","top_white","top_silver","top_blue"};
      case "three_quarter":return new String[]{"angled_original","angled_black","angled_white","angled_silver","angled_blue"};
      case "front":return new String[]{"front_silver"};
      case "side":return new String[]{"side_official_snow","side_official_space"};
      default:return new String[]{"auto"};
    }
  }
  public static String key(Context c){
    String value=AppPreferences.get(c,selectionKey(AppPreferences.dark(c)),"auto");
    if(value.equals("side_dark_left")||value.equals("side_dark_right"))return "side_official_space";
    if(value.equals("side_white_basic")||value.equals("side_white_perf")||value.equals("side_white_low"))return "side_official_snow";
    if("auto".equals(value))return value;
    for(String key:KEYS)if(key.equals(value))return value;
    return "auto";
  }
  public static String label(Context c,String key){
    switch(key){
      case "top_original":return AppPreferences.tr(c,"原版高清白车顶视","Original HD white overhead");
      case "top_white":return AppPreferences.tr(c,"白色顶视","Snow overhead");
      case "top_dark":return AppPreferences.tr(c,"黑色顶视","Space overhead");
      case "top_silver":return AppPreferences.tr(c,"银色顶视","Silver overhead");
      case "top_blue":return AppPreferences.tr(c,"蓝灰顶视","Blue-grey overhead");
      case "angled_original":return AppPreferences.tr(c,"原版黑车 45°","Original dark three-quarter");
      case "angled_black":return AppPreferences.tr(c,"黑色 45°","Black three-quarter");
      case "angled_white":return AppPreferences.tr(c,"白色 45°","White three-quarter");
      case "angled_silver":return AppPreferences.tr(c,"银色 45°","Silver three-quarter");
      case "angled_blue":return AppPreferences.tr(c,"蓝灰 45°","Blue-grey three-quarter");
      case "front_silver":return AppPreferences.tr(c,"黑色正面","Black front");
      case "side_dark_left":case "side_dark_right":return AppPreferences.tr(c,"极星官网 · 深空黑","Official Polestar · space");
      case "side_white_basic":case "side_white_low":case "side_white_perf":case "side_official_snow":return AppPreferences.tr(c,"极星官网 · 雪白","Official Polestar · snow");
      case "side_official_space":return AppPreferences.tr(c,"极星官网 · 深空黑","Official Polestar · space");
      default:return AppPreferences.tr(c,"默认 · 黑白车视图","Default · original black/white");
    }
  }
  public static int drawable(String key,boolean dark,boolean psDefault,boolean nio){
    switch(key){
      case "top_original":return R.drawable.vehicle_top;
      case "top_white":return R.drawable.psa_vehicle_top_white;
      case "top_dark":return R.drawable.psa_vehicle_top_dark;
      case "top_silver":return R.drawable.psa_vehicle_top_silver;
      case "top_blue":return R.drawable.psa_vehicle_top_blue;
      case "angled_original":return R.drawable.vehicle_hero;
      case "angled_black":return R.drawable.psa_vehicle_angle_black;
      case "angled_white":return R.drawable.psa_vehicle_angle_white;
      case "angled_silver":return R.drawable.psa_vehicle_angle_silver;
      case "angled_blue":return R.drawable.psa_vehicle_angle_blue;
      case "front_silver":return R.drawable.psa_vehicle_front_silver;
      case "side_dark_left":case "side_dark_right":return R.drawable.official_p2_side_space;

      case "side_white_basic":case "side_white_low":case "side_white_perf":return R.drawable.official_p2_side_snow;
      case "side_official_snow":return R.drawable.official_p2_side_snow;
      case "side_official_space":return R.drawable.official_p2_side_space;
      default:return psDefault?(dark?R.drawable.psa_vehicle_top_dark:R.drawable.vehicle_top)
          :nio?R.drawable.vehicle_hero:R.drawable.vehicle_top;
    }
  }
  public static boolean isTop(String key){return "auto".equals(key)||key.startsWith("top_");}
  public static int dashboard(Context c){
    if(UiSkin.PS_DEFAULT.equals(UiSkin.get(c))&&"auto".equals(key(c)))return R.drawable.vehicle_top;
    if(UiSkin.SIMPLE_2.equals(UiSkin.get(c))&&"auto".equals(key(c)))return R.drawable.official_p2_side_snow;
    String selection=key(c);
    if("auto".equals(selection)){
      if(UiSkin.nioLayout(c))return drawable(AppPreferences.dark(c)?"angled_black":"angled_white",AppPreferences.dark(c),false,true);
      if(UiSkin.SIMPLE.equals(UiSkin.get(c))||UiSkin.DETAILED.equals(UiSkin.get(c))||UiSkin.polestarVo(c))return drawable(AppPreferences.dark(c)?"top_dark":"top_white",AppPreferences.dark(c),false,false);
    }
    return drawable(selection,AppPreferences.dark(c),UiSkin.PS_DEFAULT.equals(UiSkin.get(c)),UiSkin.nioLayout(c));
  }
  /** Keep the latest VoLike portrait presentation; source pixels and aspect ratio stay intact. */
  static int portraitRotation(Context c){String skin=UiSkin.get(c),key=key(c);boolean vo=UiSkin.SIMPLE.equals(skin)||UiSkin.DETAILED.equals(skin)||UiSkin.polestarVo(c);return vo&&(key.startsWith("top_")||"auto".equals(key))?(frontAtRight(dashboard(c))?-90:90):0;}
  static boolean frontAtRight(int res){return res==R.drawable.vehicle_top||res==R.drawable.psa_vehicle_angle_black||res==R.drawable.psa_vehicle_angle_blue||res==R.drawable.psa_vehicle_angle_silver||res==R.drawable.psa_vehicle_angle_white;}
  /** Existing original assets contain transparent margins; keep anchors in vehicle space. */
  static void effectBounds(int res,android.graphics.RectF rect){float w=rect.width(),h=rect.height(),l=rect.left,t=rect.top;
    if(res==R.drawable.vehicle_top)rect.set(l+w*.14f,t+h*.22f,l+w*.86f,t+h*.80f);
    else if(res==R.drawable.vehicle_hero)rect.set(l+w*.15f,t+h*.10f,l+w*.92f,t+h*.94f);
  }
  public static final String[] WIDGET_CHOICES={"car","app_outline","app_red","app_skid","photo_dark","line_black","line_white"};
  public static String widgetChoice(Context c){
    String chosen=AppPreferences.get(c,"widget_visual","car");
    for(String value:WIDGET_CHOICES)if(value.equals(chosen))return value;
    return "car";
  }
  public static String widgetLabel(Context c,String key){
    switch(key){
      case "app_outline":return AppPreferences.tr(c,"程序图标 · 轮廓","App icon · outline");
      case "app_red":return AppPreferences.tr(c,"程序图标 · 红色车辆","App icon · red");
      case "app_skid":return AppPreferences.tr(c,"程序图标 · 防滑图形","App icon · skid");
      case "photo_dark":return AppPreferences.tr(c,"黑色正面车辆 · 透明","Black front · transparent");
      case "line_black":return AppPreferences.tr(c,"黑色透明线稿","Black transparent outline");
      case "line_white":return AppPreferences.tr(c,"白色透明线稿","White transparent outline");
      default:return AppPreferences.tr(c,"车辆图片 · 当前车型/颜色","Vehicle image · selected model/color");
    }
  }
  public static int widgetDrawable(Context c){return widgetDrawableFor(c,widgetChoice(c));}
  public static int widgetDrawableFor(Context c,String choice){
    switch(choice){
      case "app_outline":return AppPreferences.dark(c)?R.drawable.ic_app_outline_night:R.drawable.ic_app_outline_day;
      case "app_red":return AppPreferences.dark(c)?R.drawable.ic_app_red_night:R.drawable.ic_app_red_day;
      case "app_skid":return AppPreferences.dark(c)?R.drawable.ic_app_skid_night:R.drawable.ic_app_skid_day;
      case "photo_dark":return R.drawable.psa_vehicle_front_silver;
      case "line_black":return R.drawable.psa_icon_line_black;
      case "line_white":return R.drawable.psa_icon_line_white;
      default:return dashboard(c);
    }
  }
}
