package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class Beta46ModeDiagnosticsTest {
  @Test public void onlyKnownHostsAndValidModesReported(){
    Context c=RuntimeEnvironment.getApplication();
    Bundle valid=new Bundle();valid.putString("packageName","com.heytap.mydevices");
    valid.putString("process","com.heytap.mydevices:cards");valid.putString("mode","new");
    assertTrue(OppoBridgeModeDiagnostics.record(c,valid).getBoolean("accepted",false));
    assertEquals("new",OppoBridgeModeDiagnostics.status(c).getString("com.heytap.mydevices|com.heytap.mydevices:cards"));
    valid.putString("packageName","com.evil.app");
    assertFalse(OppoBridgeModeDiagnostics.record(c,valid).getBoolean("accepted",false));
    valid.putString("packageName","com.heytap.mydevices");valid.putString("mode","elevated");
    assertFalse(OppoBridgeModeDiagnostics.record(c,valid).getBoolean("accepted",false));
  }
}
