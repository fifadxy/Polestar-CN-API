package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class OppoShelfCardSnapshotAdapterTest {
  @Test public void mapsVehicleSnapshotWithoutInventingMilestone() {
    VehicleSnapshot state = new VehicleSnapshot(
        "Polestar 2", "VIN…", 59, 240, 32100, "充电中",
        "全部关闭", "全部关闭", "已关闭", "已锁", 123456L, "LIVE", false,
        "开启", "关闭");
    Bundle out = OppoShelfCardSnapshotAdapter.preview(state);
    assertTrue(out.getBoolean("available"));
    assertEquals(59, out.getInt("socPercent"));
    assertEquals(240, out.getInt("rangeKm"));
    assertTrue(out.getBoolean("charging"));
    assertFalse(out.getBoolean("isMilestone"));
    assertEquals("POLESTAR", out.getString("provider"));
    assertTrue(out.getStringArray("actions").length >= 5);
  }

  @Test public void missingSnapshotDoesNotFakeVehicleData() {
    Bundle out = OppoShelfCardSnapshotAdapter.preview(null);
    assertFalse(out.getBoolean("available"));
    assertFalse(out.containsKey("socPercent"));
    assertFalse(out.getBoolean("isMilestone"));
  }
}