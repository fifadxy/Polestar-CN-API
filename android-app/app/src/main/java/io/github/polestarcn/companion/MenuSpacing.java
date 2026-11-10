package io.github.polestarcn.companion;
import android.content.Context;
/** Shared spacing for rows and sections inside menus; never alters the car dashboard layout. */
public final class MenuSpacing {
  private MenuSpacing() {}
  public static int dp(Context c) {
    try {
      int value=Integer.parseInt(AppPreferences.get(c,"menu_spacing_dp","4"));
      return value==2||value==4||value==6||value==8||value==12?value:4;
    } catch(Exception e) { return 4; }
  }
  public static int pixels(Context c){return Math.round(dp(c)*c.getResources().getDisplayMetrics().density);}
}
