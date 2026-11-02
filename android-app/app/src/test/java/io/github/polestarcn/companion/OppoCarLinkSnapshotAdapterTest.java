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
  @Test public void mapsSocAsRatioAndKeepsRealVehicleMetrics() {
    VehicleSnapshot state=new VehicleSnapshot("vehicle","masked",59,240,32100,"充电中",
        "全部关闭","全部关闭","已关闭","已锁",System.currentTimeMillis(),"LIVE",false,"开启","关闭")
        .withExterior(new ExteriorSnapshot(new int[]{2,2,2,2},new int[]{2,2,2,2}));
    Bundle out=OppoCarLinkSnapshotAdapter.preview(state);
    assertEquals(.59f,out.getFloat("batteryLeftRatio"),.0001f);
    assertEquals(240f,out.getFloat("batteryEnduranceMileage"),.001f);
    assertEquals(32100f,out.getFloat("totalMileage"),.001f);
    assertTrue(out.getBoolean("charging"));
  }
}