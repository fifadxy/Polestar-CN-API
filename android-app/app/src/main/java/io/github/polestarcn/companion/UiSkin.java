package io.github.polestarcn.companion;

import android.content.Context;

public final class UiSkin {
  public static final String LATEST = "latest";
  public static final String CURRENT = "current";
  public static final String EV_CONCEPT = "ev_concept";

  private UiSkin() {}

  public static String get(Context context) {
    String value = AppPreferences.get(context, "ui_skin", CURRENT);
    if (LATEST.equals(value) || EV_CONCEPT.equals(value)) return value;
    return CURRENT;
  }

  public static void set(Context context, String value) {
    AppPreferences.set(context, "ui_skin",
        LATEST.equals(value) || EV_CONCEPT.equals(value) ? value : CURRENT);
  }

  public static int index(Context context) {
    String value = get(context);
    if (LATEST.equals(value)) return 0;
    if (CURRENT.equals(value)) return 1;
    return 2;
  }

  public static String label(Context context, String value) {
    boolean en = AppPreferences.english(context);
    if (LATEST.equals(value)) return en ? "Latest" : "最新模板";
    if (EV_CONCEPT.equals(value)) return en ? "EV concept" : "此刻图片风格";
    return en ? "Current" : "当前版本";
  }
}
