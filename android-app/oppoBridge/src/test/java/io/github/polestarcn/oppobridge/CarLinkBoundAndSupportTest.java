package io.github.polestarcn.oppobridge;

import static org.junit.Assert.*;
import android.os.Bundle;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class CarLinkBoundAndSupportTest {
  @Test public void carLinkPackageVariantsAreRecognized(){
    assertTrue(BridgeIdentity.isCarLinkHost("com.heytap.opluscarlink"));
    assertTrue(BridgeIdentity.isCarLinkHost("com.oplus.carlink"));
    assertTrue(BridgeIdentity.isCarLinkHost("com.oppo.carlink"));
    assertFalse(BridgeIdentity.isCarLinkHost("com.example.other"));
  }

  @Test public void bindingKeysProjectAsBoundOrConnected(){
    assertTrue(CarLinkBoundStateShim.bindingKey("hasCarBinding"));
    assertTrue(CarLinkBoundStateShim.bindingKey("has_car_binding"));
    assertTrue(CarLinkBoundStateShim.bindingKey("bindState"));
    assertTrue(CarLinkBoundStateShim.bindingKey("connect_state"));
    assertFalse(CarLinkBoundStateShim.bindingKey("accountId"));

    assertEquals(Boolean.TRUE,CarLinkBoundStateShim.valueForTest(boolean.class,"hasCarBinding"));
    assertEquals(1,CarLinkBoundStateShim.valueForTest(int.class,"bindState"));
    assertEquals(2,CarLinkBoundStateShim.valueForTest(int.class,"connectState"));
    assertEquals("BOUND",CarLinkBoundStateShim.valueForTest(String.class,"binding_state"));
    assertEquals("CONNECTED",CarLinkBoundStateShim.valueForTest(String.class,"sessionState"));
  }

  @Test public void onlyReadStyleBindMethodsAreForced(){
    assertTrue(PsAssistantVehicleHostHook.bindingReadMethod("hasCarBinding"));
    assertTrue(PsAssistantVehicleHostHook.bindingReadMethod("getBindingState"));
    assertTrue(PsAssistantVehicleHostHook.bindingReadMethod("isBound"));
    assertFalse(PsAssistantVehicleHostHook.bindingReadMethod("unbindCar"));
    assertFalse(PsAssistantVehicleHostHook.bindingReadMethod("bindVehicle"));
  }

  @Test public void supportedAppChecksTargetOnlyPsAssistant(){
    assertTrue(PsAssistantCarAppCatalogHook.targetsPsAssistant(
        Arrays.<Object>asList("io.github.polestarcn.companion")));
    assertTrue(PsAssistantCarAppCatalogHook.targetsPsAssistant(
        Arrays.<Object>asList("POLESTAR")));
    Bundle b=new Bundle();b.putString("packageName","io.github.polestarcn.companion");
    assertTrue(PsAssistantCarAppCatalogHook.targetsPsAssistant(Arrays.<Object>asList(b)));
    assertFalse(PsAssistantCarAppCatalogHook.targetsPsAssistant(
        Arrays.<Object>asList("com.example.other")));

    assertTrue(PsAssistantCarAppCatalogHook.carPermission("com.oppo.permission.safe.IOT"));
    assertTrue(PsAssistantCarAppCatalogHook.carPermission("com.oplus.carlink.permission.CONTROL"));
    assertFalse(PsAssistantCarAppCatalogHook.carPermission("android.permission.INTERNET"));
  }

  @Test public void signatureCompatibilityIsHostScoped(){
    assertTrue(PsAssistantCarAppCatalogHook.hostIdentity("com.heytap.opluscarlink","com.heytap.opluscarlink"));
    assertTrue(PsAssistantCarAppCatalogHook.hostIdentity("com.oplus.ocar","com.heytap.opluscarlink"));
    assertFalse(PsAssistantCarAppCatalogHook.hostIdentity("com.example.other","com.heytap.opluscarlink"));
  }
}
