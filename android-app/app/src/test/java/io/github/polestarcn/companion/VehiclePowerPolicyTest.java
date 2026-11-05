package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class VehiclePowerPolicyTest {
 @Test public void saverSuspendsAllAutomaticSchedulesAndRestoresUserIntervals(){
   Context c=RuntimeEnvironment.getApplication();AppPreferences.set(c,"foreground_refresh_minutes","2");AppPreferences.set(c,"map_refresh_mode","fixed");AppPreferences.set(c,"map_refresh_minutes","5");AppPreferences.set(c,"app_background_minutes","15");
   AppPreferences.set(c,VehiclePowerPolicy.KEY,"on");
   assertEquals(0,RefreshPolicy.foreground(c));assertEquals(0,RefreshPolicy.map(c));assertEquals(0,RefreshPolicy.background(c,true,true));
   AppPreferences.set(c,VehiclePowerPolicy.KEY,"off");
   assertEquals(2,RefreshPolicy.foreground(c));assertEquals(5,RefreshPolicy.map(c));assertEquals(15,RefreshPolicy.background(c,true,false));
 }
 @Test public void defaultsPreserveExistingAutomaticPolicy(){Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("preferences",0).edit().clear().commit();VehicleSnapshotStore.clearChina(c);assertFalse(VehiclePowerPolicy.saver(c));assertEquals(10,RefreshPolicy.foreground(c));assertEquals(60,RefreshPolicy.background(c,true,false));assertEquals(120,RefreshPolicy.background(c,false,false));}
 @Test public void bridgeRefreshHasMonotonicCooldown(){BridgeRefreshThrottle t=new BridgeRefreshThrottle();assertTrue(t.admit(0));assertFalse(t.admit(1000));assertEquals(59000,t.retryAfter(1000));assertTrue(t.admit(60000));assertTrue(t.admit(1));}
}
