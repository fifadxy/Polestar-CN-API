package io.github.polestarcn.oppobridge;

import static org.junit.Assert.*;
import android.os.Bundle;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class Beta28ExactCarRouteTest {
  @Test public void bridgeIdentityRecognizesBothPackagingModes() {
    assertTrue(BridgeIdentity.isBridgePackage(BridgeIdentity.STANDALONE_PACKAGE));
    assertTrue(BridgeIdentity.isBridgePackage(BridgeIdentity.INTEGRATED_PACKAGE));
    assertTrue(BridgeIdentity.isDeviceAuthority(BridgeIdentity.STANDALONE_AUTHORITY));
    assertTrue(BridgeIdentity.isDeviceAuthority(BridgeIdentity.INTEGRATED_AUTHORITY));
    assertFalse(BridgeIdentity.isBridgePackage("com.example.foreign"));
  }

  @Test public void verifiedTextPatchAddsOnlyTheFourExistingCarIds() {
    String source="const eventSummary = exports.eventSummary = [\n"
        + "'sportData', 'healthData'];";
    String patched=QuickAppCarCapability.patchVerifiedText(source);
    assertNotNull(patched);
    assertTrue(QuickAppCarCapability.hasCarEvents(patched));
    assertEquals(1,QuickAppCarCapability.occurrences(patched,"'chesuo'"));
    assertNull(QuickAppCarCapability.patchVerifiedText(patched));
    assertNull(QuickAppCarCapability.patchVerifiedText("const eventSummary=[];"));
  }

  @Test public void bytePatchFailsClosedWithoutExactSizeAndHash() {
    byte[] lookalike=(QuickAppCarCapability.PATCH_ANCHOR).getBytes(StandardCharsets.UTF_8);
    assertNull(QuickAppCarCapability.patchExact(lookalike));
  }

  @Test public void oemIdsRouteThroughOwnedActivityRequests() {
    assertEquals("lock-toggle",MyDevicesVehicleHook.shortcutRequest("chesuo"));
    assertEquals("climate-toggle",MyDevicesVehicleHook.shortcutRequest("kongtiao"));
    assertEquals("flash",MyDevicesVehicleHook.shortcutRequest("shanguang"));
    assertEquals("honk",MyDevicesVehicleHook.shortcutRequest("mingdi"));
    assertNull(MyDevicesVehicleHook.shortcutRequest("foreign"));
  }

  @Test public void providerPublishesNativeCarModelFields() throws Exception {
    Bundle state=new Bundle();state.putLong("capturedAt",System.currentTimeMillis());
    state.putString("vehicleKey","sha256-vehicle-fixture");
    org.json.JSONObject row=VehicleDeviceCardProvider.deviceData(state,"id","mac");
    assertEquals("CAR",row.getString("mDeviceType"));
    assertEquals("POLESTAR_2",row.getString("modelId"));
    assertEquals("Polestar 2",row.getString("modelName"));
    assertEquals("sha256-vehicle-fixture",row.getString("vehicleKey"));
  }
}
