package io.github.polestarcn.companion;

import android.graphics.Color;
import android.graphics.Typeface;

/** Prototype tokens from Psa VI v5; system fonts avoid shipping an unlicensed brand font. */
public final class PsaDesign {
  public static final int ORANGE = Color.rgb(255,107,0);
  private static final Typeface REGULAR = Typeface.create("sans-serif", Typeface.NORMAL);
  private static final Typeface DISPLAY = Typeface.create("sans-serif-light", Typeface.NORMAL);
  private PsaDesign() {}
  public static int ink(boolean dark) { return Color.parseColor(dark ? "#F3F3F1" : "#151617"); }
  public static int muted(boolean dark) { return Color.parseColor(dark ? "#B5B7B8" : "#626560"); }
  public static int canvas(boolean dark) { return Color.parseColor(dark ? "#151617" : "#F3F3F1"); }
  public static int surface(boolean dark) { return Color.parseColor(dark ? "#292B2D" : "#FFFFFF"); }
  public static int line(boolean dark) { return Color.parseColor(dark ? "#47494B" : "#D8DAD7"); }
  public static int accentText(boolean dark) { return Color.parseColor(dark ? "#FF6B00" : "#B34700"); }
  public static Typeface regular() { return REGULAR; }
  public static Typeface display() { return DISPLAY; }
}
