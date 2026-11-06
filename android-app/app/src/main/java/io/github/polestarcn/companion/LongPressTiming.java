package io.github.polestarcn.companion;
import android.content.Context;
import java.util.Locale;
/** One source of truth for user-adjustable hold gestures across the main app. */
public final class LongPressTiming {
  public static final String KEY="long_press_duration_ms";
  public static final int DEFAULT_MS=600;
  private LongPressTiming(){}
  public static int millis(Context c){
    try {
      int n=Integer.parseInt(AppPreferences.get(c,KEY,String.valueOf(DEFAULT_MS)));
      for(int value:new int[]{400,500,600,700,800,1000,1200,1500})
        if(n==value)return n;
    } catch(NumberFormatException ignored){}
    return DEFAULT_MS;
  }
  public static String label(Context c){
    return String.format(Locale.US,"%.1f s",millis(c)/1000f);
  }
}
