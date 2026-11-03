package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.Context;
import android.content.ComponentName;
import android.content.pm.*;
import android.app.PendingIntent;
import android.os.Bundle;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class BridgeAuthorizationBeta27Test {
  private Context c;private VehicleCoordinator model;
  @Before public void setup()throws Exception{
    c=RuntimeEnvironment.getApplication();BridgeCommandRequests.cancelAll();c.getSharedPreferences("preferences",0).edit().clear().commit();
    c.getSharedPreferences("legal",0).edit().putBoolean("accepted_disclaimer_v1",true).commit();OppoDeviceIntegration.setEnabled(c,true);
    java.lang.reflect.Field field=VehicleCoordinator.class.getDeclaredField("instance");field.setAccessible(true);field.set(null,null);model=VehicleCoordinator.get(c);
    field=VehicleCoordinator.class.getDeclaredField("signedIn");field.setAccessible(true);field.setBoolean(model,true);
    model.selected=new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();model.selected.vin="PRIVATE-TEST-VEHICLE";
    model.snapshot=new VehicleSnapshot("fixture","masked",64,270,1234,"空闲","全部关闭","全部关闭","已关闭","已锁",System.currentTimeMillis(),"LIVE",false,"关闭","关闭");
  }
  @After public void clear(){BridgeCommandRequests.cancelAll();}
  private String token(Bundle response){PendingIntent p=response.getParcelable("authorizationIntent");assertNotNull(p);return org.robolectric.Shadows.shadowOf(p).getSavedIntent().getStringExtra("task");}
  @Test public void providerDefaultsToApprovalWithoutSendingVehicleCommand(){
    PsAssistantBridgeProvider provider=new PsAssistantBridgeProvider();ProviderInfo info=new ProviderInfo();info.authority=PsAssistantBridgeProvider.AUTHORITY;provider.attachInfo(c,info);
    Bundle args=new Bundle();args.putString("action","unlock");Bundle result=provider.call("command",null,args);
    assertFalse(result.getBoolean("commandSent"));assertFalse(model.commandPending);
    if(BuildConfig.OPPO_SDK_MOCK){
      // A real login/snapshot must not redirect a Mock system-card request to the real backend.
      assertFalse(result.getBoolean("accepted"));assertTrue(result.getBoolean("mock"));
      assertEquals("mock-not-ready-run-initialization",result.getString("reason"));
      assertNull(result.getParcelable("authorizationIntent"));
    }else{
      assertTrue(result.getBoolean("accepted"));
      assertEquals("awaiting-authorization",result.getString("reason"));assertEquals("unlock",BridgeCommandRequests.action(token(result)));
    }
  }
  @Test public void switchingVehicleConsumesOldApprovalWithoutSending(){
    String id=token(BridgeCommandRequests.request(c,"unlock",model));model.selected.vin="OTHER-VEHICLE";
    BridgeCommandRequests.approve(c,id,"biometric");assertFalse(model.commandPending);assertNull(BridgeCommandRequests.action(id));
  }
  @Test public void modeChangeAndCancellationInvalidateOneUseTask(){
    String id=token(BridgeCommandRequests.request(c,"unlock",model));AppPreferences.set(c,"control_action_auth_unlock","biometric");
    BridgeCommandRequests.approve(c,id,"confirm");assertFalse(model.commandPending);assertNull(BridgeCommandRequests.action(id));
    id=token(BridgeCommandRequests.request(c,"unlock",model));BridgeCommandRequests.cancel(id);assertNull(BridgeCommandRequests.action(id));
  }
  @Test public void disablingIntegrationCancelsPendingAuthorizations(){
    String id=token(BridgeCommandRequests.request(c,"unlock",model));OppoDeviceIntegration.setEnabled(c,false);assertNull(BridgeCommandRequests.action(id));assertFalse(model.commandPending);
  }
  @Test public void repairRestoresComponentButKeepsExplicitOptOut(){
    ComponentName component=new ComponentName(c.getPackageName(),OppoDeviceIntegration.PROVIDER_CLASS);
    c.getPackageManager().setComponentEnabledSetting(component,PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);
    OppoDeviceIntegration.reconcile(c);assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED,c.getPackageManager().getComponentEnabledSetting(component));
    OppoDeviceIntegration.setEnabled(c,false);OppoDeviceIntegration.reconcile(c);assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED,c.getPackageManager().getComponentEnabledSetting(component));
  }
  @Test public void staleSnapshotCannotQueueAnApproval(){
    assertFalse(PsAssistantBridgeProvider.commandStateReady(model.snapshot,model.snapshot.timestampMillis+6*60_000L));
    assertTrue(PsAssistantBridgeProvider.commandStateReady(model.snapshot,model.snapshot.timestampMillis));
  }
}
