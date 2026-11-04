package io.github.polestarcn.companion;

import android.content.Context;

/** Display-only compact/minimal mode. It never changes command authorization or vehicle state. */
public final class UiDisplayMode {
  public static final String KEY_MINIMAL="minimal_mode";
  private UiDisplayMode() {}
  public static boolean minimal(Context c){return AppPreferences.visible(c,KEY_MINIMAL,false);}
  public static void setMinimal(Context c,boolean value){AppPreferences.set(c,KEY_MINIMAL,value?"on":"off");}
  public static boolean toggleMinimal(Context c){boolean value=!minimal(c);setMinimal(c,value);return value;}
  public static String label(Context c){return AppPreferences.tr(c,minimal(c)?"极简模式":"标准模式",minimal(c)?"Minimal":"Standard");}
}