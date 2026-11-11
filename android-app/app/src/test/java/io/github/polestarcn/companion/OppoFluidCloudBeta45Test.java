package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.app.Notification;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
public class OppoFluidCloudBeta45Test {
  private Context c(){Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("preferences",0).edit().clear().commit();return c;}
  @Test public void activeScenesAreOnByDefaultAndExplicitOffIsRespected(){
    Context c=c();assertTrue(OppoFluidPolicy.enabled(c));assertTrue(OppoFluidPolicy.notify(c,"lock"));
    assertTrue(OppoFluidPolicy.notify(c,"charging"));assertTrue(OppoFluidPolicy.cloud(c,"charging"));
    assertTrue(OppoFluidPolicy.cloud(c,"climate"));assertTrue(OppoFluidPolicy.cloud(c,"air"));
    assertTrue(OppoFluidPolicy.cloud(c,"camp"));
    AppPreferences.set(c,OppoFluidPolicy.MASTER,"off");
    assertFalse(OppoFluidPolicy.enabled(c));assertFalse(OppoFluidPolicy.notify(c,"charging"));
    AppPreferences.set(c,OppoFluidPolicy.MASTER,"on");
    assertTrue(OppoFluidPolicy.notify(c,"lock"));
    assertFalse(OppoFluidPolicy.actions(c));
    AppPreferences.set(c,OppoFluidPolicy.LEVEL,"2");AppPreferences.set(c,OppoFluidPolicy.ACTIONS,"on");
    AppPreferences.set(c,"oppo_compat_action_lock","on");assertTrue(OppoFluidPolicy.action(c,"lock"));
    AppPreferences.set(c,"oppo_compat_notify_lock","off");assertFalse(OppoFluidPolicy.notify(c,"lock"));
    OppoFluidPolicy.reset(c);assertTrue(OppoFluidPolicy.enabled(c));
  }
  @Test public void allSupportedScenariosAndEachTemplateBuildWithoutCommands(){
    Context c=c();assertEquals(11,OppoFluidPolicy.SCENES.length);
    assertEquals(88,OppoFluidCloud.validateMatrix(c));
    for(String scene:OppoFluidPolicy.SCENES){
      OppoFluidScene s=OppoFluidCloud.demo(c,scene);
      assertEquals(scene,s.id);assertTrue(s.synthetic);
      Notification primary=OppoFluidCloud.build(c,s,"simple",1,"v1",false);
      Notification expanded=OppoFluidCloud.build(c,s,"full",2,"v2",true);
      assertEquals(Notification.CATEGORY_SERVICE,primary.category);
      assertNotNull(primary.contentIntent);assertNotNull(expanded.contentIntent);
      assertEquals(s.ongoing,(expanded.flags&Notification.FLAG_ONGOING_EVENT)!=0);
      assertEquals(90_000L,expanded.getTimeoutAfter());
      assertEquals(0xffff6b00,expanded.color);
    }
  }
  @Test public void noUnconfirmedLockSuccessNoticeAndCommandActionsWhitelisted(){
    Context c=c();AppPreferences.set(c,OppoFluidPolicy.MASTER,"on");
    OppoFluidCloud.onVerifiedCommand(c,"lock",OperationState.SERVER_ACCEPTED);
    OppoFluidCloud.onVerifiedCommand(c,"unlock",OperationState.UNCONFIRMED);
    assertFalse(OppoFluidCloud.allowedAction("fake-unlock"));
    assertFalse(OppoFluidCloud.allowedAction("arbitrary"));
    assertTrue(OppoFluidCloud.allowedAction("climate-stop"));
    assertTrue(OppoFluidCloud.actionBelongs("climate","climate-stop"));
    assertTrue(OppoFluidCloud.actionBelongs("status","unlock"));
    assertFalse(OppoFluidCloud.actionBelongs("climate","unlock"));
    assertFalse(OppoFluidCloud.actionBelongs("fault","lock"));
  }
  @Test public void explicitWarningsUseKnownFlagsNotInventedPressureValues(){
    Context c=c();
    VehicleSnapshot state=new VehicleSnapshot("P2","VIN-MASK",67,340,12000,"空闲","全部关闭","全部关闭","已关闭","已锁",1000L,"LIVE",false);
    assertTrue(OppoVehicleIssues.collect(c,state).isEmpty());
    state=state.withTires(new TireSnapshot(new Double[]{250d,null,null,null},new int[]{TireSnapshot.LOW,1,1,1},1000));
    state=state.withHealth(new VehicleHealthSnapshot(VehicleHealthSnapshot.WARNING,VehicleHealthSnapshot.OK,VehicleHealthSnapshot.WARNING,1000));
    String detail=OppoVehicleIssues.detail(OppoVehicleIssues.collect(c,state),5);
    assertTrue(detail.contains("胎")||detail.contains("tyre"));
    assertTrue(detail.contains("12V"));
    assertFalse(detail.contains("250"));
  }
}
