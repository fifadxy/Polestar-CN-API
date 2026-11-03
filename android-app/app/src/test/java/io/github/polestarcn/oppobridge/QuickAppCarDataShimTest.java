package io.github.polestarcn.oppobridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public final class QuickAppCarDataShimTest {
  @Test public void normalizesOnlyThePsAssistantRowShapeToCar() throws Exception {
    JSONObject device=new JSONObject();
    device.put("type","UNKNOWN");
    device.put("mDeviceType","3");
    device.put("mConnectState","CONNECTED");
    device.put("connection",new JSONObject().put("connectState","CONNECTED"));

    QuickAppCarDataShim.normalizeCar(device);

    assertEquals("CAR",device.getString("type"));
    assertEquals("CAR",device.getString("mDeviceType"));
    assertEquals("CAR",device.getString("deviceType"));
    assertEquals("COORDINATED",device.getString("mConnectState"));
    JSONObject connection=device.getJSONObject("connection");
    assertEquals("COORDINATED",connection.getString("connectState"));
    assertEquals("COORDINATED",connection.getString("coordinationState"));
  }

  @Test public void beta31SessionAddsSyntheticVehicleIdentityAndCapabilities() throws Exception {
    JSONObject device=new JSONObject();
    android.os.Bundle session=new android.os.Bundle();
    session.putString("virtualCarId","psassistant_1234");session.putString("providerCode","POLESTAR");
    session.putString("sessionId","psa-session-1234");session.putString("testVin","TST0123456789ABCD");
    session.putString("testMac","02:12:34:56:78:9A");session.putBoolean("online",true);
    session.putString("lock","LOCKED");session.putString("climate","INACTIVE");
    session.putBoolean("batteryPercentKnown",true);session.putInt("batteryPercent",55);
    session.putStringArray("capabilities",new String[]{"LOCK","UNLOCK","SOC"});
    QuickAppCarDataShim.applySession(device,session);
    assertEquals("psassistant_1234",device.getString("deviceId"));
    assertEquals("POLESTAR",device.getString("providerCode"));
    assertEquals("02:12:34:56:78:9A",device.getString("mac"));
    assertEquals(55,device.getInt("soc"));
    assertTrue(device.getJSONArray("capabilities").toString().contains("LOCK"));
  }

  @Test public void dormantOemShortcutSetMatchesExact17415Ids() throws Exception {
    JSONArray shortcuts=QuickAppCarDataShim.oemShortcuts();
    assertEquals(4,shortcuts.length());
    String all=shortcuts.toString();
    assertTrue(all.contains("chesuo"));
    assertTrue(all.contains("kongtiao"));
    assertTrue(all.contains("shanguang"));
    assertTrue(all.contains("mingdi"));
    for(int i=0;i<shortcuts.length();i++)
      assertEquals(8,shortcuts.getJSONObject(i).getInt("style"));
  }
}
