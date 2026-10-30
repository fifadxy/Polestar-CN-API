package io.github.polestarcn.companion;
import android.content.Context;
/** Optional long-parking policy; explicit commands and camping are separate. */
public final class VehiclePowerPolicy {
  public static final String KEY="long_parking_saver";
  private VehiclePowerPolicy() {}
  public static boolean saver(Context c) {return AppPreferences.visible(c,KEY,false);}
  public static boolean automaticReads(Context c) {return !saver(c);}
}
