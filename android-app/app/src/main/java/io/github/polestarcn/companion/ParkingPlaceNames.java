package io.github.polestarcn.companion;
import android.content.Context;import android.location.*;import java.util.*;
/** Keyless platform reverse-geocoder. Uses the parked vehicle point, never phone GPS. */
final class ParkingPlaceNames {
 static String caption(Context c,LocationSnapshot point,String name){if(point==null||!point.valid())return AppPreferences.tr(c,"停车数据未更新","Parking data not updated");return name!=null&&!name.trim().isEmpty()?name:String.format(Locale.US,"%.5f, %.5f",point.latitude,point.longitude);}
 static String resolve(Context context,LocationSnapshot point){if(point==null||!point.valid()||!Geocoder.isPresent())return "";try{List<Address> results=new Geocoder(context,Locale.getDefault()).getFromLocation(point.latitude,point.longitude,1);return results==null||results.isEmpty()?"":label(results.get(0));}catch(Exception e){return "";}}
 static String label(Address a){LinkedHashSet<String> parts=new LinkedHashSet<>();for(String item:new String[]{a.getSubLocality(),a.getThoroughfare(),a.getSubThoroughfare(),a.getFeatureName()})if(item!=null&&!item.trim().isEmpty()&&!item.matches("[-+0-9., ]+"))parts.add(item.trim());String name=android.text.TextUtils.join(" ",parts);if(name.isEmpty()){String line=a.getAddressLine(0);if(line!=null&&!line.matches("[-+0-9., ]+"))name=line;}return name==null?"":name.replace('\n',' ').trim();}
}
