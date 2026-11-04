package io.github.polestarcn.companion;
import android.content.Context;import android.content.res.Configuration;
/** Map theme is independent of the app theme. System means the actual OS night setting. */
final class MapAppearance {
 static String mode(Context c){String v=AppPreferences.get(c,"map_theme","system");return "light".equals(v)||"dark".equals(v)?v:"system";}
 static boolean dark(Context c){String v=mode(c);return "dark".equals(v)||("system".equals(v)&&(c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);}
}
