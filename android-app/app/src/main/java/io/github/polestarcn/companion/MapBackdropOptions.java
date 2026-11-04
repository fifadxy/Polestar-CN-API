package io.github.polestarcn.companion;
import android.content.Context;
import android.net.Uri;
import java.util.Locale;
/** Official public map pages; no SDK keys. Coordinates are sent only after map opt-in. */
public final class MapBackdropOptions {
  private MapBackdropOptions(){}
  public static String provider(Context c){
    String p=AppPreferences.get(c,"map_backdrop_provider","amap");
    return "osm".equals(p)||"topo".equals(p)?p:"amap";
  }
  public static int zoom(Context c){return EffectSettings.number(c,"map_backdrop_zoom",16,10,19);}
  public static String attribution(String provider){
    if("osm".equals(provider))return "© OpenStreetMap contributors";
    if("topo".equals(provider))return "© OpenTopoMap · © OpenStreetMap contributors";
    return "© 高德地图";
  }
  public static Uri page(LocationSnapshot point,String title,String provider,int zoom){
    if("amap".equals(provider))return ParkingMapLinks.webUri(point,title).buildUpon()
       .appendQueryParameter("zoom",""+zoom).build();
    String coordinate=String.format(Locale.US,"%.6f/%.6f",point.latitude,point.longitude);
    String root="topo".equals(provider)?"https://opentopomap.org/#map=":"https://www.openstreetmap.org/#map=";
    return Uri.parse(root+zoom+"/"+coordinate);
  }
}
