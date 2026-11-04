package io.github.polestarcn.companion;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import io.github.polestarcn.bridge.CarLinkBridgeContract;
/** Routes the uploaded protocol without changing any existing uploaded response keys or values. */
final class UploadedCarLinkEndpoint {
 private static io.github.polestarcn.companion.uploadedcarlink.PsAssistantBridgeProvider provider;
 private static synchronized io.github.polestarcn.companion.uploadedcarlink.PsAssistantBridgeProvider provider(Context c){if(provider==null){provider=new io.github.polestarcn.companion.uploadedcarlink.PsAssistantBridgeProvider();ProviderInfo info=new ProviderInfo();info.authority=CarLinkBridgeContract.AUTHORITY;provider.attachInfo(c.getApplicationContext(),info);}return provider;}
 static boolean handles(String method){return java.util.Arrays.asList("lock","unlock","climate_on","climate_off","window_close","window_vent","find_car","auth").contains(method)||"virtual-session-profile".equals(method)||CarLinkBridgeContract.readMethod(method)||CarLinkBridgeContract.DISPATCH.equals(method);}
 static Bundle call(Context c,String method,String arg,Bundle extras){if("virtual-session-profile".equals(method))method=CarLinkBridgeContract.SESSION;Bundle out=provider(c).call(method,arg,extras);if(out==null)return null;
  // Additional diagnostics/legacy aliases; preserve every uploaded field, including exec_success.
  if(!out.containsKey("accepted")&&out.containsKey("exec_success"))out.putBoolean("accepted",out.getBoolean("exec_success"));
  out.putInt("bridgeProtocolVersion",1);out.putBoolean("uploadedLogic",true);out.putString("resultSemantics","uploaded-compatibility");out.putBoolean("realVehicleConfirmed",false);
  if(CarLinkBridgeContract.SESSION.equals(method)){Bundle aliases=VirtualVehicleSessionManager.status(c,VehicleCoordinator.get(c));aliases.putAll(out);out=aliases;boolean bound=out.getBoolean("hasCarBinding",true);out.putBoolean("active",bound);out.putBoolean("present",bound);String vin=out.getString("vehicle_vin","");out.putString("virtualCarId",OppoCarLinkModels.carId(vin));out.putBoolean("rawVinExposed",!vin.isEmpty());out.putString("mode","UPLOADED_LOGIC");out.putString("lock",out.getBoolean("is_locked",true)?"已锁":"未锁");out.putString("climate",out.getBoolean("is_climate_active",false)?"开启":"关闭");out.putInt("batteryPercent",out.getInt("soc_percent"));out.putInt("rangeKm",out.getInt("range_km"));VehicleSnapshot snapshot=VehicleCoordinator.get(c).snapshot;if(snapshot==null)snapshot=VehicleSnapshotStore.loadChina(c);boolean socMatches=snapshot!=null&&!snapshot.isDemo&&snapshot.batteryPercent!=null&&snapshot.batteryPercent==out.getInt("soc_percent");boolean rangeMatches=snapshot!=null&&!snapshot.isDemo&&snapshot.rangeKm!=null&&snapshot.rangeKm==out.getInt("range_km");out.putBoolean("batteryPercentKnown",socMatches);out.putBoolean("rangeKmKnown",rangeMatches);out.putString("batteryPercentSource",socMatches?"snapshot-match":"simulated-or-unverified");out.putString("rangeKmSource",rangeMatches?"snapshot-match":"simulated-or-unverified");out.putLong("snapshotTimestampMillis",snapshot==null?0:snapshot.timestampMillis);}
  return out;
 }
 static Cursor query(Context c,Uri uri,String[] projection,String selection,String[] args,String order){return provider(c).query(uri,projection,selection,args,order);}
}
