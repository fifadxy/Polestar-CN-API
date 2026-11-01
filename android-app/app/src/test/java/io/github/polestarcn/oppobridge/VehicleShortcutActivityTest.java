package io.github.polestarcn.oppobridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import android.os.Bundle;
import org.junit.Test;

public final class VehicleShortcutActivityTest {
  private static Bundle state(String key,String value) {
    Bundle out=new Bundle();
    out.putBoolean("available",true);
    out.putString(key,value);
    return out;
  }

  @Test public void lockToggleUsesRealCachedState() {
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

  @Test public void unknownStateNeverGuessesACommand() {
    assertNull(VehicleShortcutActivity.resolveToggle(
        "lock-toggle",state("lock","未知")));
    Bundle unavailable=new Bundle();
    assertNull(VehicleShortcutActivity.resolveToggle("climate-toggle",unavailable));
    assertNull(VehicleShortcutActivity.resolveToggle("unknown",state("lock","已上锁")));
  }
}
