package io.github.polestarcn.oppobridge;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class VehicleCommandConfirmationTest {
  private Bundle state() {
    Bundle s=new Bundle();
    s.putBoolean("available",true);
    return s;
  }

  @Test public void confirmsObservableVehicleStates() {
    Bundle s=state();
    s.putString("lock","已上锁");
    assertTrue(VehicleCommandConfirmation.matches(s,"lock"));
    s.putString("lock","已解锁");
    assertTrue(VehicleCommandConfirmation.matches(s,"unlock"));
    s.putString("climate","开启");
    assertTrue(VehicleCommandConfirmation.matches(s,"climate-start"));
    s.putString("air","关闭");
    assertTrue(VehicleCommandConfirmation.matches(s,"air-stop"));
    s.putString("tailgate","已打开");
    assertTrue(VehicleCommandConfirmation.matches(s,"trunk-open"));
    s.putString("windows","已关闭");
    assertTrue(VehicleCommandConfirmation.matches(s,"windows-close"));
  }

  @Test public void englishOppositesAndMainAppChineseValuesAreExact(){
    Bundle s=state();s.putString("lock","UNLOCKED");assertFalse(VehicleCommandConfirmation.matches(s,"lock"));assertTrue(VehicleCommandConfirmation.matches(s,"unlock"));
    s.putString("lock","未锁");assertTrue(VehicleCommandConfirmation.matches(s,"unlock"));
    s.putString("climate","INACTIVE");assertFalse(VehicleCommandConfirmation.matches(s,"climate-start"));assertTrue(VehicleCommandConfirmation.matches(s,"climate-stop"));
    s.putString("windows","全部关闭");assertTrue(VehicleCommandConfirmation.matches(s,"windows-close"));
    assertEquals("lock",CardShortcutCommand.fromId("chesuo",s));assertEquals("climate-start",CardShortcutCommand.fromId("kongtiao",s));
  }

  @Test public void transientCommandsDoNotClaimReadbackSupport() {
    assertFalse(VehicleCommandConfirmation.supports("honk"));
    assertFalse(VehicleCommandConfirmation.supports("flash"));
    assertFalse(VehicleCommandConfirmation.supports("honk-flash"));
  }
}
