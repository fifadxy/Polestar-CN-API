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

  public static boolean enabled(Context c) {
    return "on".equals(get(c, "resident", "off"));
  }

  public static boolean english(Context c) {
    String lang = get(c, "language", "system");
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

  public static String status(Context c, String raw) {
    if (!english(c)) return raw;
    switch (raw == null ? "" : raw) {
      case "已上锁":
      case "已锁":
        return "Locked";
      case "未锁":
        return "Unlocked";
      case "已打开":
        return "Open";
      case "已关闭":
      case "全部关闭":
        return "Closed";
      case "未关严":
        return "Ajar";
      case "开启":
        return "On";
      case "关闭":
        return "Off";
      case "处理中":
        return "Pending";
      case "充电中":
        return "Charging";
      case "空闲":
        return "Idle";
      case "计划中":
        return "Scheduled";
      case "放电中":
        return "Discharging";
      case "错误":
        return "Error";
      case "智能充电":
        return "Smart charging";
      case "已完成":
        return "Complete";
      case "智能充电暂停":
        return "Paused";
      default:
        return "Unknown";
    }
  }
}
