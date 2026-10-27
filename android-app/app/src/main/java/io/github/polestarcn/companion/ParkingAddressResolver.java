package io.github.polestarcn.companion;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Keyless, optional Android reverse-geocoder fallback for a user-requested parking point.
 * No coordinate persistence, remote log, app-side scraping or fabricated street names. */
final class ParkingAddressResolver {
  private ParkingAddressResolver() {}
  static void lookup(Context context,LocationSnapshot point,Consumer<String> result) {
    if(point==null||!point.valid()){result.accept(null);return;}
    Context app=context.getApplicationContext();
    if(!Geocoder.isPresent()){result.accept(null);return;}
    new Thread(()->{
      String name=null;
      try{
        List<Address> matches=new Geocoder(app,Locale.getDefault()).getFromLocation(point.latitude,point.longitude,1);
        if(matches!=null&&!matches.isEmpty()){
          Address a=matches.get(0);
          String street=a.getThoroughfare(), district=a.getSubLocality(),city=a.getLocality();
          name=(district!=null&&!district.trim().isEmpty()?district+" ":"")
              +(street!=null&&!street.trim().isEmpty()?street:city==null?"":city);
          if(name.trim().isEmpty())name=a.getFeatureName();
        }
      }catch(Exception unavailable){/* No provider, service or network: retain coordinate fallback. */}
      final String resolved=name;
      new Handler(Looper.getMainLooper()).post(()->result.accept(resolved));
    },"parking-geocode").start();
  }
}
