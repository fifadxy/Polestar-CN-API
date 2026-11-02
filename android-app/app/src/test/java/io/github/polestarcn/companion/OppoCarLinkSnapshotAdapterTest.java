package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class OppoCarLinkSnapshotAdapterTest {
  @Test public void mapsSocRangeMileageAndChargingUsingOfficialUnits() {
    VehicleSnapshot snapshot=new VehicleSnapshot(
        "Polestar 2","VIN…",59,240,32100,"充电中",
        "全部关闭","全部关闭","已关闭","已锁",System.currentTimeMillis(),"LIVE",false,
        "开启","关闭")
        .withExterior(new ExteriorSnapshot(new int[]{2,2,2,2},new int[]{2,2,2,2}));
    Bundle out=OppoCarLinkSnapshotAdapter.preview(snapshot);
    assertTrue(out.getBoolean("available"));
    assertEquals(.59f,out.getFloat("batteryLeftRatio"),.0001f);
    assertEquals(240f,out.getFloat("batteryEnduranceMileage"),.001f);
    assertEquals(32100f,out.getFloat("totalMileage"),.001f);
    assertTrue(out.getBoolean("charging"));
    assertArrayEquals(new int[]{2,2,2,2},out.getIntArray("doorStates"));
    assertEquals(8,out.getStringArray("availableInstructions").length);
  }

  @Test public void clampsBatteryRatioAndDoesNotInventMissingMetrics() {
    VehicleSnapshot snapshot=new VehicleSnapshot(
        "Polestar 2","VIN…",120,null,null,"空闲",
        "状态未知","状态未知","未知","未知",System.currentTimeMillis(),"LIVE",false);
    Bundle out=OppoCarLinkSnapshotAdapter.preview(snapshot);
    assertEquals(1f,out.getFloat("batteryLeftRatio"),0f);
    assertFalse(out.getBoolean("batteryRangeKnown"));
    assertFalse(out.getBoolean("odometerKnown"));
    assertFalse(out.getBoolean("charging"));
  }
}
