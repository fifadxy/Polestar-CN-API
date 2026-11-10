package io.github.polestarcn.companion;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/**
 * Explicit car-card click => one full read, including cold-start discovery.
 * No automatic follow-up vehicle command is ever dispatched from this helper.
 */
final class BridgeDemandRefresh {
  private static final long MIN_INTERVAL_MS=20_000L;
  private static long lastRequestAt=-1L;
  private static String lastVehicle="";
  private BridgeDemandRefresh() {}

  static synchronized boolean admit(String vehicle,long now) {
    String key=vehicle==null?"":vehicle;
    if(key.equals(lastVehicle)&&lastRequestAt>=0L&&now>=lastRequestAt
        &&now-lastRequestAt<MIN_INTERVAL_MS)return false;
    lastVehicle=key;lastRequestAt=now;
    return true;
  }

  static boolean request(Context context,VehicleCoordinator model) {
    if(context==null||model==null||!FirstUseConsent.accepted(context)
        ||!OppoDeviceIntegration.enabled(context)||!model.hasSession()
        ||model.busy||model.commandPending)return false;
    String key=model.selected==null?"cold-start"
        :VehicleCoordinator.identity(model.selected.vin);
    if(!admit(key,SystemClock.elapsedRealtime()))return false;
    new Handler(Looper.getMainLooper()).post(()->{
      if(OppoDeviceIntegration.enabled(context)&&model.hasSession()
          &&!model.busy&&!model.commandPending&&FirstUseConsent.accepted(context))
        model.refresh(null);
    });
    return true;
  }
}
