package io.github.polestarcn.companion;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import java.util.Locale;

/** Official, key-free Amap URI API; no static-map credentials or location SDK. */
public final class ParkingMapLinks {
  private ParkingMapLinks() {}
  public static void clearLegacySettings(android.content.Context context) {
    android.content.SharedPreferences old=context.getSharedPreferences("map_provider_private",0);
    if(old.contains("key"))old.edit().clear().apply();
    android.content.SharedPreferences preferences=context.getSharedPreferences("preferences",0);
    if(preferences.contains("amap_preview"))preferences.edit().remove("amap_preview").apply();
  }
  private static String number(double value) { return String.format(Locale.US, "%.6f", value); }
  public static Uri nativeUri(LocationSnapshot point, String title) {
    return new Uri.Builder().scheme("androidamap").authority("viewMap")
        .appendQueryParameter("sourceApplication", "PsAssistant")
        .appendQueryParameter("poiname", title)
        .appendQueryParameter("lat", number(point.latitude))
        .appendQueryParameter("lon", number(point.longitude))
        .appendQueryParameter("dev", "0").build();
  }
  public static Uri webUri(LocationSnapshot point, String title) {
    return Uri.parse("https://uri.amap.com/marker").buildUpon()
        .appendQueryParameter("position", number(point.longitude) + "," + number(point.latitude))
        .appendQueryParameter("name", title).appendQueryParameter("src", "PsAssistant")
        .appendQueryParameter("coordinate", "gaode").appendQueryParameter("callnative", "0").build();
  }
  public static Uri geoUri(LocationSnapshot point, String title) {
    String pair = number(point.latitude) + "," + number(point.longitude);
    return Uri.parse("geo:" + pair + "?q=" + pair + "(" + Uri.encode(title) + ")");
  }
  public static boolean open(Activity activity, LocationSnapshot point, String title) {
    if (point == null || !point.valid()) return false;
    Intent[] choices = {
      new Intent(Intent.ACTION_VIEW, nativeUri(point, title)).setPackage("com.autonavi.minimap"),
      new Intent(Intent.ACTION_VIEW, webUri(point, title)).addCategory(Intent.CATEGORY_BROWSABLE),
      new Intent(Intent.ACTION_VIEW, geoUri(point, title))
    };
    for (Intent intent : choices) {
      try { activity.startActivity(intent); return true; }
      catch (ActivityNotFoundException | SecurityException unavailable) { /* Try next official handler. */ }
    }
    return false;
  }
}
