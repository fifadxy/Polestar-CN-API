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
      o.put("mileageSchema", 1);
      o.put("odometerMeters", snapshot.mileage.totalMeters);
      o.put("tripManualKm", snapshot.mileage.manualKm);
      o.put("tripAutomaticKm", snapshot.mileage.automaticKm);
      o.put("manualAverageSpeed", snapshot.mileage.manualAverageSpeed);
      o.put("automaticAverageSpeed", snapshot.mileage.automaticAverageSpeed);
      o.put("manualConsumption", snapshot.mileage.manualConsumption);
      o.put("automaticConsumption", snapshot.mileage.automaticConsumption);
      o.put("mileageReportedAt", snapshot.mileage.reportedAt);
      o.put("consumptionReportedAt", snapshot.mileage.consumptionReportedAt);
      o.put("doorStates", array(snapshot.exterior.states(false)));
      o.put("windowStates", array(snapshot.exterior.states(true)));
      o.put("temperatureC", snapshot.environment.temperatureC);
      o.put("targetTemperatureC", snapshot.environment.targetTemperatureC);
      o.put("aqi", snapshot.environment.aqi);
      o.put("pm25", snapshot.environment.pm25UgM3);
      o.put("climateRuntimeMinutes", snapshot.environment.climateRuntimeMinutes);
      o.put("airRuntimeMinutes", snapshot.environment.airRuntimeMinutes);
      o.put("climateReportedAt", snapshot.environment.climateReportedAt);
      o.put("airReportedAt", snapshot.environment.airReportedAt);
      o.put("chargerConnection", snapshot.chargingDetails.connection);
      o.put("chargingType", snapshot.chargingDetails.type);
      o.put("chargerPowerStatus", snapshot.chargingDetails.powerStatus);
      o.put("timeToFullMinutes", snapshot.chargingDetails.minutesToFull);
      o.put("powerWatts", snapshot.chargingDetails.powerWatts);
      o.put("currentAmps", snapshot.chargingDetails.currentAmps);
      o.put("charging", snapshot.charging);
      o.put("climate", snapshot.climate);
      o.put("air", snapshot.air);
      o.put("doors", snapshot.doors);
      o.put("windows", snapshot.windows);
      o.put("tailgate", snapshot.tailgate);
      o.put("lock", snapshot.lock);
      o.put("capturedAt", snapshot.timestampMillis);
      context
          .getSharedPreferences(PREF, Context.MODE_PRIVATE)
          .edit()
          .putString(KEY, o.toString())
          .apply();
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
      VehicleSnapshot snapshot = new VehicleSnapshot(
          o.optString("name", "Polestar"),
          o.optString("id", "VIN…"),
          integer(o, "batteryPercent"),
          integer(o, "rangeKm"),
          null,
          o.optString("charging", "未知"),
          o.optString("doors", "状态未知"),
          o.optString("windows", "状态未知"),
          o.optString("tailgate", "状态未知"),
          o.optString("lock", "未知"),
          o.optLong("capturedAt", 0L),
          "CACHED",
          false,
          o.optString("climate", "未知"),
          o.optString("air", "未知"));
      if (o.has("doorStates") || o.has("windowStates"))
        snapshot = snapshot.withExterior(new ExteriorSnapshot(
            states(o.optJSONArray("doorStates")), states(o.optJSONArray("windowStates"))));
      snapshot = snapshot.withEnvironment(new EnvironmentSnapshot(snapshot.climate, snapshot.air,
          decimal(o, "temperatureC"), decimal(o, "targetTemperatureC"), decimal(o, "pm25"),
          integer(o, "aqi"), integer(o, "climateRuntimeMinutes"), integer(o, "airRuntimeMinutes"),
          o.optLong("climateReportedAt", 0), o.optLong("airReportedAt", 0)));
      snapshot = snapshot.withCharging(new ChargingSnapshot(integer(o, "chargerConnection"),
          integer(o, "chargingType"), integer(o, "chargerPowerStatus"), integer(o, "timeToFullMinutes"),
          decimal(o, "powerWatts"), decimal(o, "currentAmps")));
      // v0.5.0 cached the wrong protobuf branch. Never promote that value into corrected mileage.
      if (o.optInt("mileageSchema", 0) != 1) return snapshot;
      Long total = !o.has("odometerMeters") || o.isNull("odometerMeters")
          ? null : o.getLong("odometerMeters");
      return snapshot.withMileage(new MileageSnapshot(total,
          decimal(o, "tripManualKm"), decimal(o, "tripAutomaticKm"),
          decimal(o, "manualAverageSpeed"), decimal(o, "automaticAverageSpeed"),
          decimal(o, "manualConsumption"), decimal(o, "automaticConsumption"),
          o.optLong("mileageReportedAt", 0), o.optLong("consumptionReportedAt", 0)));
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
    if (value == null) o.put(key, JSONObject.NULL);
    else o.put(key, value);
  }

  private static Integer integer(JSONObject o, String key) {
    return o.isNull(key) || !o.has(key) ? null : o.optInt(key);
  }

  private static Double decimal(JSONObject o, String key) {
    return o.isNull(key) || !o.has(key) ? null : o.optDouble(key, Double.NaN);
  }

  private static org.json.JSONArray array(int[] values) {
    org.json.JSONArray out = new org.json.JSONArray();
    for (int value : values) out.put(value);
    return out;
  }
  private static int[] states(org.json.JSONArray values) {
    int[] out = {-1, -1, -1, -1};
    if (values != null)
      for (int i = 0; i < Math.min(4, values.length()); i++) out[i] = values.optInt(i, -1);
    return out;
  }
}
