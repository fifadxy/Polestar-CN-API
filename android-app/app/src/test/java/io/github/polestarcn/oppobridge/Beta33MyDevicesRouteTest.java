package io.github.polestarcn.oppobridge;

import static org.junit.Assert.*;
import org.junit.Test;

public final class Beta33MyDevicesRouteTest {
  @Test public void newerHostMayUseNativeCarEventsWithoutLegacyPatch() {
    QuickAppCarCapability.Result nativeRoute=new QuickAppCarCapability.Result(
        "17.25.10",true,true,true,true,false,"new-sha","native-car-events");
    assertTrue(nativeRoute.routeSafe());
    assertEquals("event-summary-oem-shortcuts",nativeRoute.mode());
    assertFalse(nativeRoute.exactPatchSupported);
  }

  @Test public void unknownHostWithoutNativeEventsFailsOpen() {
    QuickAppCarCapability.Result unknown=new QuickAppCarCapability.Result(
        "17.25.10",true,true,true,false,false,"new-sha","no-event-summary");
    assertFalse(unknown.routeSafe());
    assertEquals("fallback-native-home",unknown.mode());
  }

  @Test public void legacyExactPatchRemainsScopedToVerifiedBuild() {
    QuickAppCarCapability.Result legacy=new QuickAppCarCapability.Result(
        QuickAppCarCapability.HOST_VERSION,true,true,true,false,true,
        QuickAppCarCapability.INDEX_SHA256,"verified");
    assertTrue(legacy.routeSafe());
    assertEquals("exact-rpk-memory-patch",legacy.mode());
  }
}
