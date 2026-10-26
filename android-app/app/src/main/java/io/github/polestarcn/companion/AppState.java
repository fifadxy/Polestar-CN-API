package io.github.polestarcn.companion;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppState {
    private static final String PREF = "companion_dev_state";
    private AppState() {}
    public static Region region(Context context) {
        return Region.fromStored(context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("region", ""));
    }
    public static void setRegion(Context context, Region region) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString("region", region.name()).apply();
    }
    public static String sessionState(Context context, Region region) {
        // Deliberately no token exchange. Regions have separate namespaces from day one.
        SharedPreferences p = context.getSharedPreferences("session_" + region.name().toLowerCase(java.util.Locale.ROOT), Context.MODE_PRIVATE);
        return p.contains("authenticated") ? "PENDING_API_VERIFICATION" : "NOT_CONNECTED";
    }
}
