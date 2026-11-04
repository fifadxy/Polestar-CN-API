package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class OppoShelfCardCapabilityTest {
  @Test public void officialPluginCardProbeIsReadOnlyAndDocumented() {
    Context context=org.robolectric.RuntimeEnvironment.getApplication();
    Bundle out=OppoShelfCardCapability.status(context);
    assertEquals("11733",out.getString("documentId"));
    assertTrue(out.getBoolean("officialSdkRequired"));
    assertTrue(out.getBoolean("oppoWhitelistingMayBeRequired"));
  }
}