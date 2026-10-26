package io.github.polestarcn.companion.auth;

import android.content.Context;
import android.util.Base64;
import io.github.polestarcn.companion.CampSession;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Full target VIN is encrypted, never copied into display preferences or diagnostics. */
public final class SecureCampStore {
  private SecureCampStore() {}
  public static void save(Context c, CampSession s) throws Exception {
    JSONObject o = new JSONObject();
    o.put("id", s.id).put("vin", s.vin).put("name", s.vehicleName).put("endsAt", s.endsAt)
        .put("interval", s.intervalMinutes).put("minimum", s.minimumBattery).put("air", s.keepAir);
    String encrypted = Base64.encodeToString(SecureSessionStore.encrypt(
        o.toString().getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
    if (!c.getSharedPreferences("camp_mode_secure", 0).edit().putString("config", encrypted).commit())
      throw new IllegalStateException("Camping configuration could not be saved");
  }
  public static CampSession load(Context c) {
    String blob = c.getSharedPreferences("camp_mode_secure", 0).getString("config", null);
    if (blob == null) return null;
    try {
      JSONObject o = new JSONObject(new String(SecureSessionStore.decrypt(
          Base64.decode(blob, Base64.NO_WRAP)), StandardCharsets.UTF_8));
      return new CampSession(o.getString("id"), o.getString("vin"), o.optString("name", "Vehicle"),
          o.getLong("endsAt"), o.getInt("interval"), o.getInt("minimum"), o.getBoolean("air"));
    } catch (Exception e) { clear(c); return null; }
  }
  public static void clear(Context c) {
    c.getSharedPreferences("camp_mode_secure", 0).edit().clear().commit();
  }
}
