package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** No root commands, live car reads, account data or OEM auth spoofing. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class OppoBridgeAutoDiagnosticV1210Test {
 @Test public void distinguishesHookFromVehicleReadbackAndRequiresExplicitRoot(){
   assertTrue(OppoBridgeAutoDiagnostic.analysis(false,true,false,false,false,false)
       .contains("ROOT_NOT_GRANTED"));
   assertTrue(OppoBridgeAutoDiagnostic.analysis(true,true,false,false,false,false)
       .contains("NO_LIVE_HOOK"));
   assertTrue(OppoBridgeAutoDiagnostic.analysis(true,true,true,false,true,false)
       .contains("QUICKAPP_NO_CALLBACK"));
   assertTrue(OppoBridgeAutoDiagnostic.analysis(true,true,true,true,false,false)
       .contains("SESSION_NOT_READY"));
   assertTrue(OppoBridgeAutoDiagnostic.analysis(true,true,true,true,true,false)
       .contains("LOCAL_ONLY"));
   assertTrue(OppoBridgeAutoDiagnostic.analysis(true,true,true,true,true,true)
       .contains("READY_FOR_DEVICE_TEST"));
 }
 @Test public void historicalTimestampsAreNotCurrentHookProof(){
   long now=System.currentTimeMillis();
   assertFalse(OppoBridgeAutoDiagnostic.recent(0,now));
   assertFalse(OppoBridgeAutoDiagnostic.recent(now-60*60_000,now));
   assertTrue(OppoBridgeAutoDiagnostic.recent(now-60_000,now));
 }
 @Test public void zipDiagnosticDoesNotIncludePrivatePrefOrVin(){
   Context c=RuntimeEnvironment.getApplication();
   c.getSharedPreferences("private-account",0).edit()
       .putString("access_token","MY_PRIVATE_ACCESS_TOKEN")
       .putString("VIN","12345678901234567").commit();
   String report=OppoBridgeAutoDiagnostic.capture(c,"NOT_PROBED");
   assertTrue(report.contains("psa-lsposed-diagnostic-v1210"));
   assertFalse(report.contains("MY_PRIVATE_ACCESS_TOKEN"));
   assertFalse(report.contains("12345678901234567"));
   assertTrue(report.contains("myDevicesHookRecent"));
 }
}
