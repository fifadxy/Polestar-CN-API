package io.github.polestarcn.companion;

import android.content.Context;
import android.os.Bundle;

/**
 * User-controlled local state for the experimental ColorOS "My Vehicle" bridge.
 *
 * <p>This never writes OPPO/SceneService databases and never changes Polestar authentication.
 * It only tells the exact-build AssistantScreen hook whether POLESTAR should be exposed as a
 * locally bound provider after PsAssistant itself has a valid authorized vehicle session.</p>
 */
final class OppoOfficialVehicleBindState {
  private static final String PREF="oppo-official-vehicle-bind";
  private static final String KEY="enabled";

  private OppoOfficialVehicleBindState() {}

  static Bundle status(Context context, VehicleCoordinator coordinator) {
    Bundle out=new Bundle();
    android.content.SharedPreferences prefs=context.getSharedPreferences(PREF,0);
    boolean requested=prefs.contains(KEY)?prefs.getBoolean(KEY,false):true;
    boolean signedIn=coordinator!=null&&coordinator.hasSession();
    VehicleSnapshot snapshot=coordinator==null?null:coordinator.snapshot;
    if(snapshot==null)snapshot=VehicleSnapshotStore.loadChina(context);
    boolean vehicle=snapshot!=null&&!snapshot.isDemo;
    out.putBoolean("requested",requested);
    out.putBoolean("signedIn",signedIn);
    out.putBoolean("vehicleAvailable",vehicle);
    out.putBoolean("enabled",requested&&signedIn&&vehicle);
    out.putBoolean("automaticDefault",!prefs.contains(KEY));
    out.putBoolean("extraProviderVerificationRequired",false);
    out.putString("providerCode","POLESTAR");
    return out;
  }

  static Bundle set(Context context, VehicleCoordinator coordinator, boolean enable) {
    Bundle current=status(context,coordinator);
    if(enable&&(!current.getBoolean("signedIn")||!current.getBoolean("vehicleAvailable"))) {
      current.putBoolean("accepted",false);
      current.putString("reason","sign-in-and-refresh-required");
      return current;
    }
    context.getSharedPreferences(PREF,0).edit().putBoolean(KEY,enable).apply();
    Bundle out=status(context,coordinator);
    out.putBoolean("accepted",true);
    out.putString("reason",enable?"enabled":"disabled");
    return out;
  }
}