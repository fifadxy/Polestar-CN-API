package io.github.polestarcn.oppobridge;

import static org.junit.Assert.*;
import android.content.Context;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class BridgeMode128Test {
  @Test public void modesStayIndependentWithStructuralRpkDefaultForNew(){
    Context c=RuntimeEnvironment.getApplication();
    c.getSharedPreferences("bridge-settings-v2",0).edit().clear().commit();
    assertEquals("stable",BridgeSettings.localSnapshot(c).getString(BridgeSettings.MODE));
    assertEquals("exact",BridgeSettings.localSnapshot(c).getString(BridgeSettings.RPK_MODE));
    BridgeSettings.updateLocalMode(c,BridgeSettings.MODE,BridgeSettings.MODE_NEW);
    Bundle newMode=BridgeSettings.localSnapshot(c);
    assertEquals("new",newMode.getString(BridgeSettings.MODE));
    assertEquals("feature",newMode.getString(BridgeSettings.RPK_MODE));
    BridgeSettings.updateLocalMode(c,BridgeSettings.RPK_MODE,BridgeSettings.RPK_EXACT);
    assertEquals("exact",BridgeSettings.localSnapshot(c).getString(BridgeSettings.RPK_MODE));
    BridgeSettings.updateLocalMode(c,BridgeSettings.MODE,BridgeSettings.MODE_MOCK);
    assertEquals("mock",BridgeSettings.localSnapshot(c).getString(BridgeSettings.MODE));
    c.getSharedPreferences("bridge-settings-v2",0).edit().clear().commit();
  }
}
