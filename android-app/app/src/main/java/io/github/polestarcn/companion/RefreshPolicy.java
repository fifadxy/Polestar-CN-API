package io.github.polestarcn.companion;

import android.content.Context;

/** Read-only refresh policy. Android decides actual background job timing. */
public final class RefreshPolicy {
  private RefreshPolicy() {}
  public static int minutes(Context c,String key,int fallback,int... allowed) {
    int value;
    try { value=Integer.parseInt(AppPreferences.get(c,key,String.valueOf(fallback))); }
    catch(NumberFormatException e){return fallback;}
    for(int option:allowed)if(value==option)return value;
    return fallback;
  }
  public static int foreground(Context c){return minutes(c,"foreground_refresh_minutes",5,0,1,2,5,10,15);}
  public static int map(Context c){return minutes(c,"map_refresh_minutes",15,0,5,15,30,60);}
  public static int background(Context c,boolean resident,boolean widgets){
    int a=(resident||AppPreferences.visible(c,"app_background_enabled",false))?minutes(c,"app_background_minutes",30,0,15,30,60,120):0;
    int b=widgets?minutes(c,"widget_refresh_minutes",30,0,15,30,60,120):0;
    return a==0?b:b==0?a:Math.min(a,b);
  }
}
