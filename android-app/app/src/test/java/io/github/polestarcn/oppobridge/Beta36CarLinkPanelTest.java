package io.github.polestarcn.oppobridge;
import static org.junit.Assert.*;
import android.content.Intent;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class Beta36CarLinkPanelTest {
 @Test public void onlyExplicitlyMarkedRealRouterCanBeReplaced(){
  Intent i=new Intent();assertFalse(CarLinkLocalPanelHook.accepts(CarLinkLocalPanelHook.ROUTER,i));
  i.putExtra(CarLinkLocalPanelHook.MARKER,true);assertTrue(CarLinkLocalPanelHook.accepts(CarLinkLocalPanelHook.ROUTER,i));assertFalse(CarLinkLocalPanelHook.accepts("com.other.Activity",i));
 }
 @Test public void windowCommandsPassTheSameFreshSnapshotGate(){
  Bundle s=new Bundle();s.putBoolean("available",true);s.putBoolean("cardEnabled",true);s.putLong("capturedAt",System.currentTimeMillis());
  assertEquals("windows-close",VehicleShortcutActivity.resolveToggle("windows-close",s));s.putBoolean("demo",true);assertNull(VehicleShortcutActivity.resolveToggle("windows-close",s));
 }
}
