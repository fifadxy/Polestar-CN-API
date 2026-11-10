package io.github.polestarcn.oppobridge;

import static org.junit.Assert.*;
import org.junit.Test;

/** Feature-only RPK is a distinct path, not a fabricated exact SHA match. */
public final class QuickAppCapabilityV128Test {
  @Test public void exactAndFeatureDetectedRoutesReportDifferentModes(){
    QuickAppCarCapability.Result feature=new QuickAppCarCapability.Result(
        "17.5",true,true,false,false,false,true,"hash","structural");
    assertTrue(feature.routeSafe());
    assertEquals("feature-verified-rpk",feature.mode());
    QuickAppCarCapability.Result unknown=new QuickAppCarCapability.Result(
        "17.5",true,true,false,false,false,false,"hash","unrecognized");
    assertFalse(unknown.routeSafe());
    QuickAppCarCapability.Result exact=new QuickAppCarCapability.Result(
        "17.4.15",true,true,false,false,true,false,"hash","exact");
    assertEquals("exact-rpk-memory-patch",exact.mode());
  }
}
