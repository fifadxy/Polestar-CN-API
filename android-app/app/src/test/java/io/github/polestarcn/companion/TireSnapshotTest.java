package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public final class TireSnapshotTest {
  @Test public void healthFieldsMapToFourWheelsInVehicleOrder() {
    Map<Integer,Object> data = new LinkedHashMap<>();
    data.put(9, 1L); data.put(10, 3L); data.put(11, 1L); data.put(12, 4L);
    data.put(39, 250.0); data.put(40, 205.0); data.put(41, 248.0); data.put(42, 285.0);
    TireSnapshot t = TireSnapshot.fromData(data);
    assertEquals(Integer.valueOf(TireSnapshot.OK), Integer.valueOf(t.warning(0)));
    assertEquals(Integer.valueOf(TireSnapshot.LOW), Integer.valueOf(t.warning(1)));
    assertEquals(205.0, t.pressureKpa(1), 0.01);
    assertTrue(t.hasProblem());
    assertFalse(t.allGood());
    assertTrue(t.label(1, false).contains("偏低"));
  }

  @Test public void explicitNoWarningProducesAllGood() {
    Map<Integer,Object> data = new LinkedHashMap<>();
    for (int f = 9; f <= 12; f++) data.put(f, 1L);
    TireSnapshot t = TireSnapshot.fromData(data);
    assertFalse(t.hasProblem());
    assertTrue(t.allGood());
    assertEquals("ALL GOOD", t.summary(false));
  }

  @Test public void missingHealthDataNeverClaimsGood() {
    TireSnapshot t = TireSnapshot.unknown();
    assertFalse(t.hasAnyReport());
    assertFalse(t.allGood());
    assertEquals("未知", t.summary(false));
  }
}
