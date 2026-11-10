package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Assume;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class CarLinkCachedSessionV128Test {
  @Test public void staleVehicleCanRemainVisibleButNeverCommandReady() throws Exception {
    Assume.assumeFalse(BuildConfig.OPPO_SDK_MOCK);
    Context c=RuntimeEnvironment.getApplication();
    c.getSharedPreferences("oppo-virtual-vehicle-session-v1",0).edit().clear().commit();
    c.getSharedPreferences("oppo-device-integration",0).edit().clear().commit();
    java.lang.reflect.Field singleton=VehicleCoordinator.class.getDeclaredField("instance");
    singleton.setAccessible(true);singleton.set(null,null);
    VehicleCoordinator coordinator=VehicleCoordinator.get(c);
    java.lang.reflect.Field signed=VehicleCoordinator.class.getDeclaredField("signedIn");
    signed.setAccessible(true);signed.setBoolean(coordinator,true);
    coordinator.selected=new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();
    coordinator.selected.vin="FIXTURE-NOT-A-REAL-VIN";
    coordinator.snapshot=new VehicleSnapshot("Polestar 2","masked",70,300,12345,
        "空闲","全部关闭","全部关闭","已关闭","已上锁",
        System.currentTimeMillis()-12*60_000L,"LIVE",false,"关闭","关闭");
    OppoDeviceIntegration.setEnabled(c,true);
    Bundle local=VirtualVehicleSessionManager.status(c,coordinator);
    assertTrue(local.getBoolean("present"));
    assertTrue(local.getBoolean("active"));
    assertTrue(local.getBoolean("cacheOnly"));
    assertFalse(local.getBoolean("online"));
    assertFalse(local.getBoolean("commandReady"));
    assertEquals("STALE",local.getString("lifecycle"));
    assertFalse(VehicleProtocolGateway.resolve(c,coordinator,"unlock").getBoolean("accepted"));
    OppoDeviceIntegration.setEnabled(c,false);
    singleton.set(null,null);
  }
}
