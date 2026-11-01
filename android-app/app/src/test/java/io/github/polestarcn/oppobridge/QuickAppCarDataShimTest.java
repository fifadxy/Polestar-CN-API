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
