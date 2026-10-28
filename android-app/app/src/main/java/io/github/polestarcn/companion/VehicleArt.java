package io.github.polestarcn.companion;

import android.content.Context;

/** User-provided vehicle photographs; stored without geometric distortion. */
public final class VehicleArt {
  public static final String[] KEYS = {
      "auto", "top_white", "top_dark", "angled_original", "front_silver",
      "side_dark_left", "side_dark_right", "side_blue_basic", "side_blue_perf",
      "side_white_basic", "side_white_perf", "side_white_low"
  };
  public static final String[] TYPES = {"auto","overhead","three_quarter","front","side"};
  private VehicleArt() {}
  public static String type(String key) {
    if("top_white".equals(key)||"top_dark".equals(key))return "overhead";
    if(key.startsWith("angled_"))return "three_quarter";
    if(key.startsWith("front_"))return "front";
    if(key.startsWith("side_"))return "side";
    return "auto";
  }
  public static String typeLabel(Context c,String group){
    switch(group){
      case "overhead":return AppPreferences.tr(c,"顶视图","Top view");
      case "three_quarter":return AppPreferences.tr(c,"45°视角","45° angle");
      case "front":return AppPreferences.tr(c,"正面视图","Front view");
      case "side":return AppPreferences.tr(c,"侧面视图","Side view");
      default:return AppPreferences.tr(c,"自动 · 跟随皮肤","Automatic · follow skin");
    }
  }
  public static String[] variants(String category){
    switch(category){
      case "overhead":return new String[]{"top_white","top_dark"};
      case "three_quarter":return new String[]{"angled_original"};
      case "front":return new String[]{"front_silver"};
      case "side":return new String[]{"side_dark_left","side_dark_right","side_blue_basic","side_blue_perf",
          "side_white_basic","side_white_perf","side_white_low"};
      default:return new String[]{"auto"};
    }
  }

  public static String key(Context c) {
    String field = AppPreferences.dark(c) ? "vehicle_art_night" : "vehicle_art_day";
    return AppPreferences.get(c, field, "auto");
  }

  public static String label(Context c, String key) {
    switch (key) {
      case "top_white": return AppPreferences.tr(c, "白色顶视图（旋转）", "White overhead (rotated)");
      case "top_dark": return AppPreferences.tr(c, "黑色顶视图", "Black overhead");
      case "angled_original": return AppPreferences.tr(c, "45° · 原版车辆照片", "45° · original vehicle image");
      case "front_silver": return AppPreferences.tr(c, "黑色正面 · 浅背景", "Dark front · light background");
      case "side_dark_left": return AppPreferences.tr(c, "黑色侧面 · 左向", "Dark profile · left");
      case "side_dark_right": return AppPreferences.tr(c, "黑色侧面 · 右向", "Dark profile · right");
      case "side_blue_basic": return AppPreferences.tr(c, "蓝灰侧面 · 标准", "Blue-grey profile · standard");
      case "side_blue_perf": return AppPreferences.tr(c, "蓝灰侧面 · 性能", "Blue-grey profile · performance");
      case "side_white_basic": return AppPreferences.tr(c, "白色侧面 · 标准", "White profile · standard");
      case "side_white_perf": return AppPreferences.tr(c, "白色侧面 · 性能", "White profile · performance");
      case "side_white_low": return AppPreferences.tr(c, "白色侧面 · 简洁", "White profile · minimal");
      default: return AppPreferences.tr(c, "自动 · 当前皮肤默认", "Automatic · skin default");
    }
  }

  static boolean isTop(String key) {
    return "top_white".equals(key) || "top_dark".equals(key) || "auto".equals(key);
  }
  public static int drawable(String key, boolean dark, boolean psDefault, boolean nio) {
    switch (key) {
      case "top_white": return R.drawable.psa_vehicle_top_white;
      case "top_dark": return R.drawable.psa_vehicle_top_dark;
      case "angled_original": return R.drawable.vehicle_hero;
      case "front_silver": return R.drawable.psa_vehicle_front_silver;
      case "side_dark_left": return R.drawable.psa_vehicle_side_dark_left;
      case "side_dark_right": return R.drawable.psa_vehicle_side_dark_right;
      case "side_blue_basic": return R.drawable.psa_vehicle_side_blue_basic;
      case "side_blue_perf": return R.drawable.psa_vehicle_side_blue_perf;
      case "side_white_basic": return R.drawable.psa_vehicle_side_white_basic;
      case "side_white_perf": return R.drawable.psa_vehicle_side_white_perf;
      case "side_white_low": return R.drawable.psa_vehicle_side_white_low;
      default:
        return psDefault ? (dark ? R.drawable.psa_vehicle_top_dark : R.drawable.psa_vehicle_top_white)
            : nio ? R.drawable.vehicle_hero : R.drawable.vehicle_top;
    }
  }
  public static int dashboard(Context c) {
    return drawable(key(c),AppPreferences.dark(c),
        UiSkin.PS_DEFAULT.equals(UiSkin.get(c)),UiSkin.nioLayout(c));
  }
  /** Screen-visible roof images are landscape; preserve proportions for all other views. */
  static int portraitRotation(Context c) {
    String key = key(c);
    if ("top_white".equals(key)) return 90; // source explicitly rotated left once
    if ("top_dark".equals(key)) return -90;
    if ("auto".equals(key) && (UiSkin.PS_DEFAULT.equals(UiSkin.get(c))
        || !UiSkin.nioLayout(c))) return AppPreferences.dark(c) && UiSkin.PS_DEFAULT.equals(UiSkin.get(c)) ? -90
            : UiSkin.PS_DEFAULT.equals(UiSkin.get(c)) ? 90 : -90;
    return 0;
  }
  public static final String[] WIDGET_CHOICES = {
      "car", "app_outline", "app_red", "app_skid",
      "photo_dark", "line_black", "line_white"
  };
  public static String widgetChoice(Context c) {
    String chosen=AppPreferences.get(c,"widget_visual","car");
    for(String key:WIDGET_CHOICES)if(key.equals(chosen))return key;
    return "car";
  }
  public static String widgetLabel(Context c,String key) {
    switch(key){
      case "app_outline":return AppPreferences.tr(c,"程序图标 · 车辆轮廓","App icon · vehicle outline");
      case "app_red":return AppPreferences.tr(c,"程序图标 · 红色车辆","App icon · red car");
      case "app_skid":return AppPreferences.tr(c,"程序图标 · 防滑图形","App icon · skid");
      case "photo_dark":return AppPreferences.tr(c,"黑武士正面照片","Dark warrior photograph");
      case "line_black":return AppPreferences.tr(c,"你提供的黑色线稿 · 透明","Your black line icon · transparent");
      case "line_white":return AppPreferences.tr(c,"你提供的白色线稿 · 透明","Your white line icon · transparent");
      default:return AppPreferences.tr(c,"车辆照片 · 随白天/夜间","Car image · follow day/night");
    }
  }
  public static int widgetDrawable(Context c) { return widgetDrawableFor(c,widgetChoice(c)); }
  public static int widgetDrawableFor(Context c,String choice) {
    switch(choice){
      case "app_outline":return AppPreferences.dark(c)?R.drawable.ic_app_outline_night:R.drawable.ic_app_outline_day;
      case "app_red":return AppPreferences.dark(c)?R.drawable.ic_app_red_night:R.drawable.ic_app_red_day;
      case "app_skid":return AppPreferences.dark(c)?R.drawable.ic_app_skid_night:R.drawable.ic_app_skid_day;
      case "photo_dark":return R.drawable.psa_widget_photo_dark;
      case "line_black":return R.drawable.psa_icon_line_black;
      case "line_white":return R.drawable.psa_icon_line_white;
      default:return dashboard(c);
    }
  }
}
