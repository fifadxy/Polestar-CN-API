package io.github.polestarcn.oppobridge;

import static org.junit.Assert.*;
import android.os.Bundle;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Boundary cases from MyDevices 17.4.15's ShortcutMenu/CarStatusInfo readers. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class VehicleCardNativeContractTest {
  @Test public void homeShortcutsSurviveOemStyleFilterAndRetainProviderTransport() throws Exception {
    Bundle source = new Bundle(); source.putString("lock", "已上锁");
    JSONObject data = VehicleDeviceCardProvider.deviceData(source, "sample", "02:00:00:00:00:01");
    JSONArray shortcuts = data.getJSONArray("shortcuts");
    assertEquals(4, shortcuts.length());
    for (int i=0; i<shortcuts.length(); i++) {
      JSONObject shortcut = shortcuts.getJSONObject(i);
      // OEM ShortcutMenuKt sets isShowOnHomeCards=false for CAR, so style bit 8
      // must survive conversion for isSupportShowOnHomeCards to accept it.
      assertEquals(8, shortcut.getInt("style") & 8);
      assertEquals(2, shortcut.getInt("componentType"));
      JSONObject target = new JSONObject(shortcut.getString("componentExtra"));
      assertEquals(VehicleDeviceCardProvider.AUTHORITY, target.getString("authority"));
      assertEquals(VehicleDeviceCardProvider.METHOD_COMMAND, target.getString("method"));
    }
    assertEquals("unlock", new JSONObject(shortcuts.getJSONObject(0).getString("componentExtra")).getString("extra"));
  }

  @Test public void nativeCarDataIsAJsonStringAndUsesKnownSourceRange() throws Exception {
    Bundle source = new Bundle(); source.putBoolean("rangeKmKnown", true);
    source.putInt("rangeKm", 270); source.putLong("capturedAt", 123456L);
    source.putBoolean("batteryPercentKnown", true); source.putInt("batteryPercent", 64);
    JSONObject row = VehicleDeviceCardProvider.deviceData(source, "sample", "02:00:00:00:00:01");
    assertTrue(row.get("data") instanceof String);
    JSONObject status = new JSONObject(row.getString("data"));
    assertEquals(270, status.getInt("batteryEnduranceMileage"));
    assertEquals(270, status.getInt("totalEnduranceMileage"));
    assertEquals(-1, status.getInt("oilEnduranceMileage"));
    assertFalse(status.getBoolean("isRefreshing"));
    assertTrue(status.getJSONObject("msg").getString("text").contains("缓存"));
    assertTrue(status.getJSONObject("msg").getString("text").contains("64%"));
    assertEquals(123456L, row.getLong("timestamp"));
    assertFalse(status.has("state")); // No fabricated authorization or command state.
  }

  @Test public void missingRangeIsNotRenderedAsZeroAndRealZeroIsPreserved() throws Exception {
    Bundle source = new Bundle(); source.putInt("rangeKm", 270);
    assertEquals("", VehicleDeviceCardProvider.carStatusData(source));
    source.putBoolean("rangeKmKnown", true); source.putInt("rangeKm", -1);
    assertEquals("", VehicleDeviceCardProvider.carStatusData(source));
    source.putInt("rangeKm", 0);
    assertEquals(0, new JSONObject(VehicleDeviceCardProvider.carStatusData(source)).getInt("totalEnduranceMileage"));
  }
}
