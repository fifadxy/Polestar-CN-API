package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class Beta36SystemAccessTest {
 @Test public void realBuildAndUnifiedSwitchDriveAllExistingGates(){
  assertFalse(BuildConfig.OPPO_SDK_MOCK);Context c=RuntimeEnvironment.getApplication();
  OppoSystemAccess.setEnabled(c,true);assertTrue(OppoDeviceIntegration.enabled(c));assertTrue(OppoOemCarExperimentState.status(c).getBoolean("enabled"));
  Bundle module=c.getContentResolver().call(android.net.Uri.parse("content://io.github.polestarcn.companion.bridge-settings"),"snapshot",null,null);
  assertNotNull(module);assertTrue(module.getBoolean("masterEnabled"));assertTrue(module.getBoolean("directControlEnabled"));
  OppoSystemAccess.setEnabled(c,false);assertFalse(OppoDeviceIntegration.enabled(c));assertFalse(OppoOemCarExperimentState.status(c).getBoolean("enabled"));
  assertFalse(VirtualVehicleSessionManager.status(c,VehicleCoordinator.get(c)).getBoolean("requested"));
  OppoSystemAccess.setEnabled(c,true);
  assertTrue(OppoOfficialVehicleBindState.status(c,VehicleCoordinator.get(c)).getBoolean("requested"));
  assertFalse(OppoOfficialVehicleBindState.status(c,VehicleCoordinator.get(c)).getBoolean("enabled"));
 }
 @Test public void rootLaunchUsesExplicitMarkerAndVerifiedComponent(){
  Bundle route=new Bundle();route.putString("bindActivityPackage",OppoOfficialCarLink.PACKAGE);route.putString("bindActivityName",OppoOfficialCarLink.PACKAGE+".carcontrol.view.ControlRouterActivity");
  String command=OppoCarLinkRoot.panelCommand(route);assertTrue(command.contains("--user current"));assertTrue(command.endsWith("--ez psassistant.local_panel true"));assertFalse(command.contains("pm grant"));
 }
 @Test public void liveWindowsAndTrunkCommandsResolveAcrossNativeGateway(){
  assertEquals("windows-open",VehicleProtocolGateway.resolveAction("windows-open",null));assertEquals("windows-close",VehicleProtocolGateway.resolveAction("windows-close",null));assertEquals("trunk-close",VehicleProtocolGateway.resolveAction("trunk-close",null));assertNull(VehicleProtocolGateway.resolveAction("unknown",null));
 }
 @Test public void mainSettingsAndModuleSettingsConverge(){
  OppoWidgetSetupActivity legacy=Robolectric.buildActivity(OppoWidgetSetupActivity.class).setup().get();
  Intent next=org.robolectric.Shadows.shadowOf(legacy).getNextStartedActivity();assertEquals(OppoSystemAccessActivity.class.getName(),next.getComponent().getClassName());
 }
 @Test public void unifiedSettingsOpensWithoutLoginAndHasOneMasterSwitch(){
  OppoSystemAccessActivity page=Robolectric.buildActivity(OppoSystemAccessActivity.class).setup().get();
  assertEquals(1,countSwitches(page.getWindow().getDecorView()));
 }
 private static int countSwitches(android.view.View view){int n=view instanceof android.widget.Switch?1:0;if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)n+=countSwitches(group.getChildAt(i));}return n;}
 @Test public void noCloudPermissionOrVehicleSuccessIsInferredFromPanelMount(){
  Context c=RuntimeEnvironment.getApplication();Bundle input=new Bundle();input.putBoolean("ready",true);input.putString("moduleVersion","2.0.0");
  Bundle b=OppoSystemAccess.recordPanel(c,input);assertTrue(b.getBoolean("panelReady"));assertTrue(b.getString("commandState","").isEmpty());
  OppoSystemAccess.commandEvent(c,"lock",1,"SERVER_ACCEPTED");assertEquals("SERVER_ACCEPTED",OppoSystemAccess.observations(c).getString("commandState"));
 }
}
