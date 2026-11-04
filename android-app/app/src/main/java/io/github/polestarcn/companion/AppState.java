package io.github.polestarcn.companion;

import android.content.Context;
import android.content.SharedPreferences;

import io.github.polestarcn.companion.auth.SecureSessionStore;

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
        if (region == Region.CHINA)
            return SecureSessionStore.hasSession(context) ? "CONNECTED / ENCRYPTED" : "NOT_CONNECTED";
        // Global remains in its own namespace and has no authorization flow in this build.
        SharedPreferences p = context.getSharedPreferences("session_" + region.name().toLowerCase(java.util.Locale.ROOT), Context.MODE_PRIVATE);
        return p.contains("authenticated") ? "PENDING_GLOBAL_VERIFICATION" : "NOT_CONNECTED";
    }
}
