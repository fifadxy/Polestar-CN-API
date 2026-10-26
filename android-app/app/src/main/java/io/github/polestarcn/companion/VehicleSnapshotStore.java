package io.github.polestarcn.companion;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

/**
 * Stores only the display snapshot used by the dashboard and widgets. The full VIN and all
 * authentication material stay out of this cache. A restored snapshot is always marked CACHED.
 */
public final class VehicleSnapshotStore {
    private static final String PREF = "vehicle_snapshot_display_v1";
    private static final String KEY = "china";

    private VehicleSnapshotStore() {}

    public static void saveChina(Context context, VehicleSnapshot snapshot) {
        if (snapshot == null || snapshot.isDemo) return;
        try {
            JSONObject o = new JSONObject();
            o.put("name", snapshot.name);
            o.put("id", snapshot.id);
            put(o, "batteryPercent", snapshot.batteryPercent);
            put(o, "rangeKm", snapshot.rangeKm);
            put(o, "odometerKm", snapshot.odometerKm);
            o.put("charging", snapshot.charging);
            o.put("doors", snapshot.doors);
            o.put("windows", snapshot.windows);
            o.put("tailgate", snapshot.tailgate);
            o.put("lock", snapshot.lock);
            o.put("capturedAt", snapshot.timestampMillis);
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                    .putString(KEY, o.toString()).apply();
        } catch (Exception e) {
            DiagnosticLog.add("Display snapshot cache write failed");
        }
    }

    public static VehicleSnapshot loadChina(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String raw = preferences.getString(KEY, null);
        if (raw == null || raw.isEmpty()) return null;
        try {
            JSONObject o = new JSONObject(raw);
            return new VehicleSnapshot(
                    o.optString("name", "Polestar"),
                    o.optString("id", "VIN…"),
                    integer(o, "batteryPercent"),
                    integer(o, "rangeKm"),
                    integer(o, "odometerKm"),
                    o.optString("charging", "未知"),
                    o.optString("doors", "状态未知"),
                    o.optString("windows", "状态未知"),
                    o.optString("tailgate", "状态未知"),
                    o.optString("lock", "未知"),
                    o.optLong("capturedAt", 0L),
                    "CACHED", false);
        } catch (Exception e) {
            clearChina(context);
            DiagnosticLog.add("Display snapshot cache discarded");
            return null;
        }
    }

    public static void clearChina(Context context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(KEY).apply();
    }

    private static void put(JSONObject o, String key, Integer value) throws Exception {
        if (value == null) o.put(key, JSONObject.NULL); else o.put(key, value);
    }

    private static Integer integer(JSONObject o, String key) {
        return o.isNull(key) || !o.has(key) ? null : o.optInt(key);
    }
}
