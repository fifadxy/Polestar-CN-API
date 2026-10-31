package io.github.polestarcn.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

/** Privacy-bounded observations of the OEM SceneService account-binding layer. */
final class OppoAccountBindDiagnostics {
  private static final String PREF = "oppo-account-bind-diagnostics";

  private OppoAccountBindDiagnostics() {}

  static Bundle record(Context context, Bundle in) {
    if (in == null) throw new IllegalArgumentException("Missing account-bind report");
    SharedPreferences.Editor e = context.getSharedPreferences(PREF, 0).edit();
    e.putLong("seenAt", System.currentTimeMillis());
    if(in.containsKey("ready"))e.putBoolean("ready", in.getBoolean("ready", false));
    if(in.containsKey("bindCount"))e.putInt("bindCount", Math.max(0, in.getInt("bindCount", 0)));
    if(in.containsKey("reason"))e.putString("reason", bounded(in.getString("reason", ""), 160));
    if(in.containsKey("hostVersion"))e.putString("hostVersion", bounded(in.getString("hostVersion", ""), 80));
    if(in.containsKey("moduleVersion"))e.putString("moduleVersion", bounded(in.getString("moduleVersion", ""), 40));
    if(in.containsKey("boundCodes"))e.putString("boundCodes", bounded(in.getString("boundCodes", ""), 1600));
    if(in.containsKey("providerOwner"))e.putString("providerOwner", bounded(in.getString("providerOwner", ""), 120));
    if(in.containsKey("providerPermission"))e.putString("providerPermission", bounded(in.getString("providerPermission", ""), 180));
    if(in.containsKey("providerExported"))e.putBoolean("providerExported", in.getBoolean("providerExported", false));
    if(in.containsKey("bindActivity"))e.putString("bindActivity", bounded(in.getString("bindActivity", ""), 280));
    if(in.containsKey("dialogActivity"))e.putString("dialogActivity", bounded(in.getString("dialogActivity", ""), 280));
    if(in.containsKey("providerCatalog")) {
      String catalog = bounded(in.getString("providerCatalog", ""), 3200);
      e.putString("providerCatalog", catalog);
    }
    e.apply();
    return status(context);
  }

  static Bundle status(Context context) {
    SharedPreferences p = context.getSharedPreferences(PREF, 0);
    Bundle out = new Bundle();
    out.putLong("seenAt", p.getLong("seenAt", 0L));
    out.putBoolean("ready", p.getBoolean("ready", false));
    out.putInt("bindCount", p.getInt("bindCount", 0));
    for (String key : new String[]{"reason","hostVersion","moduleVersion","boundCodes","providerCatalog","providerOwner","providerPermission","bindActivity","dialogActivity"})
      out.putString(key, p.getString(key, ""));
    out.putBoolean("providerExported",p.getBoolean("providerExported",false));
    return out;
  }

  private static String bounded(String value, int limit) {
    if (value == null) return "";
    return value.substring(0, Math.min(value.length(), limit));
  }
}
