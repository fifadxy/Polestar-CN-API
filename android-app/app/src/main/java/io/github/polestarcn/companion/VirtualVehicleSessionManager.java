package io.github.polestarcn.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import java.util.Locale;

/**
 * Test-lab virtual CAR session projected to OPPO vehicle surfaces.
 *
 * <p>The session mirrors the selected real Polestar vehicle but exposes only stable synthetic
 * identifiers to OEM hosts. No raw VIN or hardware MAC address leaves PsAssistant.</p>
 */
final class VirtualVehicleSessionManager {
  private static final String PREF="oppo-virtual-vehicle-session-v1";
  private static final String KEY_ENABLED="enabled";
  private static final long STALE_MS=10*60_000L;

  private VirtualVehicleSessionManager() {}

  static Bundle setEnabled(Context context, VehicleCoordinator coordinator, boolean enabled) {
    context.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED,enabled).apply();
    Bundle out=status(context,coordinator);
    out.putBoolean("accepted",!enabled||out.getBoolean("active",false));
    out.putString("reason",!enabled?"disabled":out.getBoolean("active",false)?"active":"sign-in-and-fresh-vehicle-required");
    return out;
  }

  static Bundle status(Context context, VehicleCoordinator coordinator) {
    SharedPreferences p=context.getSharedPreferences(PREF,Context.MODE_PRIVATE);
    boolean requested=p.contains(KEY_ENABLED)?p.getBoolean(KEY_ENABLED,false):true;
    VehicleSnapshot state=coordinator==null?null:coordinator.snapshot;
    if(state==null)state=VehicleSnapshotStore.loadChina(context);
    boolean signedIn=coordinator!=null&&coordinator.hasSession();
    boolean selected=coordinator!=null&&coordinator.selected!=null&&coordinator.selected.vin!=null&&!coordinator.selected.vin.isEmpty();
    boolean live=state!=null&&!state.isDemo&&state.timestampMillis>0;
    long now=System.currentTimeMillis();
    String vehicleKey=selected?VehicleCoordinator.identity(coordinator.selected.vin):"";
    String sessionId=vehicleKey.isEmpty()?"":"psa-session-"+vehicleKey.substring(0,Math.min(16,vehicleKey.length()));
    String carId=vehicleKey.isEmpty()?"":"psassistant_"+vehicleKey.substring(0,Math.min(20,vehicleKey.length()));
    long age=state==null||state.timestampMillis<=0?Long.MAX_VALUE:Math.max(0L,now-state.timestampMillis);
    boolean present=requested&&OppoDeviceIntegration.enabled(context)&&signedIn&&selected&&live;
    boolean active=present&&age<=STALE_MS;
    boolean commandReady=present&&PsAssistantBridgeProvider.commandStateReady(state,now);
    String lifecycle=!requested?"DISABLED":!signedIn?"DISCOVERED":!selected?"BOUND":!live?"READY":age>STALE_MS?"STALE":"ACTIVE";

    Bundle profile=OppoVehicleProfileSettings.snapshot(context);
    Bundle out=new Bundle(profile);
    out.putBoolean("requested",requested);out.putBoolean("present",present);out.putBoolean("active",active);
    out.putBoolean("commandReady",commandReady);out.putString("lifecycle",lifecycle);
    out.putString("mode","MIRROR_REAL");out.putString("sessionId",sessionId);
    out.putString("providerCode",OppoVehicleProfileSettings.PROVIDER_CODE);out.putString("deviceType",OppoVehicleProfileSettings.DEVICE_TYPE);
    out.putString("vehicleKey",vehicleKey);out.putString("virtualCarId",carId);
    out.putString("testVin",testVin(vehicleKey,profile.getString(OppoVehicleProfileSettings.VIN_PREFIX,"TST0")));out.putString("testMac",testMac(vehicleKey));
    out.putLong("snapshotAt",state==null?0L:state.timestampMillis);
    out.putLong("snapshotAgeMs",age==Long.MAX_VALUE?-1L:age);
    out.putBoolean("syntheticIdentifiers",true);out.putBoolean("rawVinExposed",false);
    out.putBoolean("hardwareMacExposed",false);out.putBoolean("online",active);
    if(state!=null){
      if(state.batteryPercent!=null){out.putBoolean("batteryPercentKnown",true);out.putInt("batteryPercent",state.batteryPercent);}
      if(state.rangeKm!=null){out.putBoolean("rangeKmKnown",true);out.putInt("rangeKm",state.rangeKm);}
      if(state.odometerKm!=null){out.putBoolean("odometerKmKnown",true);out.putInt("odometerKm",state.odometerKm);}
      out.putString("lock",safe(state.lock));out.putString("climate",safe(state.climate));
      out.putString("charging",safe(state.charging));out.putString("tailgate",safe(state.tailgate));
    }
    return out;
  }

  private static String testVin(String vehicleKey,String prefix) {
    if(vehicleKey==null||vehicleKey.isEmpty())return "";
    String safePrefix=(prefix==null?"TST0":prefix.toUpperCase(Locale.US).replaceAll("[^A-Z0-9]",""));
    if(safePrefix.isEmpty())safePrefix="TST0";
    safePrefix=safePrefix.substring(0,Math.min(6,safePrefix.length()));
    String upper=vehicleKey.toUpperCase(Locale.US).replaceAll("[^A-F0-9]","");
    return (safePrefix+upper+"00000000000000000").substring(0,17);
  }

  private static String testMac(String vehicleKey) {
    if(vehicleKey==null||vehicleKey.length()<10)return "";
    StringBuilder out=new StringBuilder("02");
    for(int i=0;i<10;i+=2)out.append(':').append(vehicleKey.substring(i,i+2).toUpperCase(Locale.US));
    return out.toString();
  }

  private static String safe(String value){return value==null?"":value;}
}
