package io.github.polestarcn.companion;

import android.content.Context;

public final class UiSkin {
  public static final String SIMPLE = "simple";
  public static final String DETAILED = "detailed";
  public static final String NIO_LIKE = "niolike";
  private static final String LEGACY_LATEST = "latest";
  private static final String LEGACY_CURRENT = "current";
  private static final String LEGACY_EV_CONCEPT = "ev_concept";

  private UiSkin() {}

  public static String get(Context context) {
    String value = AppPreferences.get(context, "ui_skin", DETAILED);
    if (SIMPLE.equals(value) || DETAILED.equals(value) || NIO_LIKE.equals(value)) return value;
    String migrated =
        LEGACY_CURRENT.equals(value)
            ? DETAILED
            : (LEGACY_LATEST.equals(value) || LEGACY_EV_CONCEPT.equals(value))
                ? SIMPLE
                : DETAILED;
    AppPreferences.set(context, "ui_skin", migrated);
    return migrated;
  }

  public static void set(Context context, String value) {
    AppPreferences.set(context, "ui_skin", NIO_LIKE.equals(value) ? NIO_LIKE : SIMPLE.equals(value) ? SIMPLE : DETAILED);
  }

  public static int index(Context context) {
    return NIO_LIKE.equals(get(context)) ? 2 : SIMPLE.equals(get(context)) ? 0 : 1;
  }

  public static String label(Context context, String value) {
    boolean en = AppPreferences.english(context);
    return NIO_LIKE.equals(value) ? "NIOlike" : SIMPLE.equals(value) ? (en ? "Simple" : "简洁风格")
        : (en ? "Detailed" : "详细风格");
  }
}
