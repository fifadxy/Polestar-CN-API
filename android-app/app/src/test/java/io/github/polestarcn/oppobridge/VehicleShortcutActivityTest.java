package io.github.polestarcn.oppobridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public final class VehicleShortcutActivityTest {
  private static Bundle state(String key,String value) {
    Bundle out=new Bundle();
    out.putBoolean("available",true);
    out.putLong("capturedAt",System.currentTimeMillis());
    out.putString(key,value);
    return out;
  }

  @Test public void lockToggleUsesRealCachedState() {
    assertEquals("lock",VehicleShortcutActivity.resolveToggle("lock-toggle",state("lock","未锁")));
    assertEquals("unlock",VehicleShortcutActivity.resolveToggle(
        "lock-toggle",state("lock","已上锁")));
    assertEquals("lock",VehicleShortcutActivity.resolveToggle(
        "lock-toggle",state("lock","已解锁")));
  }

  @Test public void climateAirAndTailgateResolveWithoutTrustingOemCheckFlag() {
    assertEquals("climate-stop",VehicleShortcutActivity.resolveToggle(
        "climate-toggle",state("climate","运行中")));
    assertEquals("air-start",VehicleShortcutActivity.resolveToggle(
        "air-toggle",state("air","关闭")));
    assertEquals("trunk-close",VehicleShortcutActivity.resolveToggle(
        "trunk-toggle",state("tailgate","已打开")));
  }

  @Test public void englishStateValuesNeverMatchTheirOpposites() {
    assertEquals("lock",VehicleShortcutActivity.resolveToggle("lock-toggle",state("lock","UNLOCKED")));
    assertEquals("unlock",VehicleShortcutActivity.resolveToggle("lock-toggle",state("lock","LOCKED")));
    assertEquals("climate-start",VehicleShortcutActivity.resolveToggle("climate-toggle",state("climate","INACTIVE")));
    assertEquals("climate-stop",VehicleShortcutActivity.resolveToggle("climate-toggle",state("climate","ACTIVE")));
    assertEquals("air-start",VehicleShortcutActivity.resolveToggle("air-toggle",state("air","INACTIVE")));
  }

  @Test public void staleOrFutureDataNeverAuthorizesAToggle() {
    Bundle old=state("lock","已上锁");
    old.putLong("capturedAt",System.currentTimeMillis()-6*60_000L);
    assertNull(VehicleShortcutActivity.resolveToggle("lock-toggle",old));
    Bundle future=state("climate","关闭");
    future.putLong("capturedAt",System.currentTimeMillis()+60_000L);
    assertNull(VehicleShortcutActivity.resolveToggle("climate-toggle",future));
    Bundle missing=state("air","开启");missing.remove("capturedAt");
    assertNull(VehicleShortcutActivity.resolveToggle("air-toggle",missing));
  }

  @Test public void unknownStateNeverGuessesACommand() {
    assertNull(VehicleShortcutActivity.resolveToggle(
        "lock-toggle",state("lock","未知")));
    Bundle unavailable=new Bundle();
    assertNull(VehicleShortcutActivity.resolveToggle("climate-toggle",unavailable));
    assertNull(VehicleShortcutActivity.resolveToggle("unknown",state("lock","已上锁")));
  }
}
