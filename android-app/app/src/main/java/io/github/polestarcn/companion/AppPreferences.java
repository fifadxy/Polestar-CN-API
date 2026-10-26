package io.github.polestarcn.companion;

import android.content.Context;
import android.content.res.Configuration;
import java.util.Locale;

public final class AppPreferences {
  private AppPreferences() {}
  public static String get(Context c, String key, String fallback) {
    return c.getSharedPreferences("preferences", 0).getString(key, fallback);
  }
  public static void set(Context c, String key, String value) {
    c.getSharedPreferences("preferences", 0).edit().putString(key, value).apply();
  }
  public static boolean visible(Context c, String key, boolean fallback) {
    return "on".equals(get(c, key, fallback ? "on" : "off"));
  }
  public static int slider(Context c, String key, int fallback) {
    try {
      return Math.max(0, Math.min(100, Integer.parseInt(get(c, key, "" + fallback))));
    } catch (NumberFormatException ignored) { return fallback; }
  }
  public static boolean enabled(Context c) {
    return "on".equals(get(c, "resident", "off"));
  }
  public static boolean english(Context c) {
    return languageEnglish(c, get(c, "language", "system"));
  }
  public static boolean widgetEnglish(Context c) {
    return languageEnglish(c, get(c, "widget_language", "system"));
  }
  private static boolean languageEnglish(Context c, String lang) {
    return "en".equals(lang)
        || ("system".equals(lang) && !"zh".equals(Locale.getDefault().getLanguage()));
  }
  public static boolean dark(Context c) {
    String theme = get(c, "theme", "system");
    return "dark".equals(theme)
        || ("system".equals(theme)
            && (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES);
  }
  public static String tr(Context c, String zh, String en) {
    return english(c) ? en : zh;
  }
  public static String widgetTr(Context c, String zh, String en) {
    return widgetEnglish(c) ? en : zh;
  }
  public static String status(Context c, String raw) {
    return translated(raw, english(c));
  }
  public static String widgetStatus(Context c, String raw) {
    return translated(raw, widgetEnglish(c));
  }
  private static String translated(String raw, boolean en) {
    if (raw == null || raw.trim().isEmpty()) return en ? "Unknown" : "未知";
    if (!en) return "处理中".equals(raw) ? "运行中（待确认）" : raw;
    if (raw.contains(" 个开启")) return raw.replace(" 个开启", " open").replace("部分未知", "partly unknown");
    switch (raw) {
      case "已上锁": case "已锁": return "Locked";
      case "未锁": return "Unlocked";
      case "已打开": return "Open";
      case "已关闭": case "全部关闭": return "Closed";
      case "未关严": return "Ajar";
      case "开启": return "On";
      case "关闭": return "Off";
      case "处理中": return "Running (unconfirmed)";
      case "充电中": return "Charging";
      case "空闲": return "Idle";
      case "计划中": return "Scheduled";
      case "放电中": return "Discharging";
      case "错误": return "Error";
      case "智能充电": return "Smart charging";
      case "已完成": return "Complete";
      case "智能充电暂停": return "Paused";
      case "未知": case "状态未知": return "Unknown";
      case "状态不完整": return "Incomplete";
      default: return raw;
    }
  }
}

