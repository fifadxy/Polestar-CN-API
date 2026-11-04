package io.github.polestarcn.companion;

import android.content.Context;
import android.os.Bundle;

/** User opt-in for the exact-build MyDevices OEM 4x2 CAR route. */
final class OppoOemCarExperimentState {
  private static final String PREF="oppo-oem-car-experiment";
  private static final String KEY="enabled";

  private OppoOemCarExperimentState() {}

  static Bundle status(Context context) {
    Bundle out=new Bundle();
    out.putBoolean("enabled",context.getSharedPreferences(PREF,0).getBoolean(KEY,false));
    out.putString("targetHost","com.heytap.mydevices");
    out.putString("targetVersion","17.4.15");
    return out;
  }

  static Bundle set(Context context,boolean enabled) {
    context.getSharedPreferences(PREF,0).edit().putBoolean(KEY,enabled).apply();
    Bundle out=status(context);
    out.putBoolean("accepted",true);
    out.putString("reason",enabled?"enabled":"disabled");
    return out;
  }
}
