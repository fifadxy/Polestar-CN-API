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
public class PsAssistantVehicleHostHookTest {
  private Bundle state(){
    Bundle b=new Bundle();
    b.putBoolean("active",true);
    b.putString("virtualCarId","psa_car_123");
    b.putString("testVin","TST01234567890123");
    b.putString("lock","已锁");
    b.putString("climate","关闭");
    return b;
  }

  @Test public void onlyPolestarTargetIsEligible(){
    Bundle p=state();
    assertTrue(PsAssistantVehicleHostHook.matchesTarget(
        Arrays.asList("POLESTAR","psa_car_123","CAR-DOOR","OPEN"),p));
    assertTrue(PsAssistantVehicleHostHook.matchesTarget(
        Arrays.asList("io.github.polestarcn.companion","lock"),p));
    assertFalse(PsAssistantVehicleHostHook.matchesTarget(
        Arrays.asList("OTHER_BRAND","other_car","CAR-DOOR","OPEN"),p));
  }

  @Test public void officialInstructionSemanticsMapToExistingCommands(){
    Bundle s=state();
    assertEquals("unlock",PsAssistantVehicleHostHook.mapInstruction("CAR-DOOR","OPEN",s));
    assertEquals("lock",PsAssistantVehicleHostHook.mapInstruction("CAR-DOOR","CLOSE",s));
    assertEquals("lock",PsAssistantVehicleHostHook.mapInstruction("LOCK-CAR","OPEN",s));
    assertNull(PsAssistantVehicleHostHook.mapInstruction("LOCK-CAR","CLOSE",s));
    assertEquals("windows-close",PsAssistantVehicleHostHook.mapInstruction("ALL-WINDOW","CLOSE",s));
    assertEquals("trunk-open",PsAssistantVehicleHostHook.mapInstruction("CAR-BOOT","OPEN",s));
    assertEquals("climate-start",PsAssistantVehicleHostHook.mapInstruction("AC-TEMP","OPEN",s));
    assertEquals("air-stop",PsAssistantVehicleHostHook.mapInstruction("AIR-PURIFICATION","CLOSE",s));
    assertEquals("honk-flash",PsAssistantVehicleHostHook.mapInstruction("CAR-HORN&&DOUBLE-FLASH","OPEN",s));
  }

  @Test public void myDevicesShortcutUsesCurrentState(){
    Bundle s=state();
    assertEquals("unlock",PsAssistantVehicleHostHook.mapDirect("chesuo",s));
    assertEquals("climate-start",PsAssistantVehicleHostHook.mapDirect("kongtiao",s));
  }

  @Test public void implicitCurrentTargetRejectsExplicitForeignVehicle(){
    Bundle s=state();
    assertFalse(PsAssistantVehicleHostHook.matchesTarget(Arrays.asList("CAR-DOOR","OPEN"),s));
    assertFalse(PsAssistantVehicleHostHook.hasConflictingIdentity(Arrays.asList("CAR-DOOR","OPEN"),s));
    Bundle other=new Bundle();other.putString("deviceId","other_vehicle");other.putString("instruction","CAR-DOOR");
    assertTrue(PsAssistantVehicleHostHook.hasConflictingIdentity(Arrays.asList(other),s));
    Bundle ours=new Bundle();ours.putString("vin","TST01234567890123");ours.putString("instruction","AC-TEMP");
    assertTrue(PsAssistantVehicleHostHook.matchesTarget(Arrays.asList(ours),s));
    assertFalse(PsAssistantVehicleHostHook.hasConflictingIdentity(Arrays.asList(ours),s));
  }

  @Test public void bundleRequestExtractsTargetAndAction(){
    Bundle s=state(),request=new Bundle();
    request.putString("providerCode","POLESTAR");
    request.putString("deviceId","psa_car_123");
    request.putString("instruction","CAR-DOOR");
    request.putString("value","OPEN");
    assertTrue(PsAssistantVehicleHostHook.matchesTarget(Arrays.asList(request),s));
    assertEquals("unlock",PsAssistantVehicleHostHook.extractAction(Arrays.asList(request),s));
  }
}
