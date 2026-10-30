package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class OppoHostDiagnosticsTest {
 @Test public void mainAndCardsReportsRemainSeparateAndDoNotRequireCardOptIn(){
   Context c=RuntimeEnvironment.getApplication();Bundle in=new Bundle();in.putString("process","com.heytap.mydevices");in.putString("phase","hooks-installed");in.putBoolean("ready",true);in.putString("moduleVersion","0.5.0");
   OppoHostDiagnostics.record(c,in);in.putString("process","com.heytap.mydevices:cards");in.putString("phase","install-failed");in.putBoolean("ready",false);OppoHostDiagnostics.record(c,in);
   Bundle status=OppoHostDiagnostics.status(c);assertTrue(status.getBoolean("main_ready"));assertFalse(status.getBoolean("cards_ready"));assertEquals("hooks-installed",status.getString("main_phase"));assertTrue(status.getLong("main_seenAt")>0);assertFalse(OppoDeviceIntegration.enabled(c));
 }
 @Test(expected=IllegalArgumentException.class) public void unknownProcessIsRejected(){Bundle in=new Bundle();in.putString("process","unknown");OppoHostDiagnostics.record(RuntimeEnvironment.getApplication(),in);}
 @Test(expected=SecurityException.class) public void appCannotForgeMyDevicesIpcReport(){
   PsAssistantBridgeProvider p=new PsAssistantBridgeProvider();ProviderInfo info=new ProviderInfo();info.authority=PsAssistantBridgeProvider.AUTHORITY;p.attachInfo(RuntimeEnvironment.getApplication(),info);p.call("host-report",null,new Bundle());
 }
}
