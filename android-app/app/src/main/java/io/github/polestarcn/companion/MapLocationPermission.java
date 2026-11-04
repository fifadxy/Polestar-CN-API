package io.github.polestarcn.companion;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
/** Optional foreground phone location for the Amap web origin only; never a background permission. */
final class MapLocationPermission {
 static final String KEY="map_phone_location";
 private MapLocationPermission(){}
 static boolean granted(Context c){return c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED||c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
 static boolean enabled(Context c){return AppPreferences.visible(c,KEY,false)&&granted(c);}
 static boolean allowOrigin(Context c,String origin){return origin!=null&&enabled(c)&&ParkingWebPreview.allowedPage(android.net.Uri.parse(origin));}
}
