package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** The UI must never claim official approval based on projected local binding. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class OppoDualPathStatusV128Test {
  @Test public void localMirrorDoesNotBecomeOfficialPartnerAuthorization(){
    Bundle local=new Bundle();
    local.putBoolean("present",true);
    local.putBoolean("active",true);
    local.putBoolean("commandReady",false);
    local.putBoolean("cacheOnly",true);
    local.putString("lifecycle","STALE");
    Bundle status=OppoDualPathStatus.summarize(false,false,false,false,local);
    assertEquals("SDK_NOT_PACKAGED",status.getString("officialPath"));
    assertFalse(status.getBoolean("officialAuthorized"));
    assertTrue(status.getBoolean("localVisible"));
    assertTrue(status.getBoolean("cachedOnly"));
    assertFalse(status.getBoolean("localCommandReady"));
    assertEquals("STALE",status.getString("localLifecycle"));
  }
  @Test public void genuineSdkCallbackAndConfigAreRequiredSimultaneously(){
    Bundle local=new Bundle();
    assertFalse(OppoDualPathStatus.summarize(true,true,false,false,local)
        .getBoolean("officialAuthorized"));
    assertEquals("CONFIG_REQUIRED",OppoDualPathStatus.summarize(true,false,true,false,local)
        .getString("officialPath"));
    assertEquals("AUTHORIZED",OppoDualPathStatus.summarize(true,true,true,false,local)
        .getString("officialPath"));
    assertEquals("MOCK",OppoDualPathStatus.summarize(true,true,true,true,local)
        .getString("officialPath"));
    assertFalse(OppoDualPathStatus.summarize(true,true,true,true,local)
        .getBoolean("officialAuthorized"));
  }
}
