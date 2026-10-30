package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class OppoDeviceIntegrationTest {
  private Context context;
  private PsAssistantBridgeProvider provider;
  @Before public void setup() {
    context=RuntimeEnvironment.getApplication();
    context.getSharedPreferences(OppoDeviceIntegration.PREFS,Context.MODE_PRIVATE).edit().clear().commit();
    provider=new PsAssistantBridgeProvider();
    ProviderInfo info=new ProviderInfo();info.authority=PsAssistantBridgeProvider.AUTHORITY;
    provider.attachInfo(context,info);
  }
  @Test public void defaultOffRejectsCommandsBeforeVehicleAccess() {
    assertFalse(OppoDeviceIntegration.enabled(context));
    Bundle input=new Bundle();input.putString("action","unlock");
    Bundle result=provider.call("command",null,input);
    assertFalse(result.getBoolean("accepted"));
    assertFalse(result.getBoolean("cardEnabled"));
    assertEquals("integration-disabled",result.getString("reason"));
    assertFalse(result.containsKey("batteryPercent"));
  }
  @Test public void optInPersistsAndDisablingStopsAlreadyBoundProvider() {
    OppoDeviceIntegration.setEnabled(context,true);
    assertTrue(OppoDeviceIntegration.enabled(context));
    ComponentName component=new ComponentName(context.getPackageName(),OppoDeviceIntegration.PROVIDER_CLASS);
    assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED,context.getPackageManager().getComponentEnabledSetting(component));
    Bundle ready=provider.call("capabilities",null,null);
    assertTrue(ready.getBoolean("state"));assertTrue(ready.getBoolean("cardEnabled"));
    OppoDeviceIntegration.setEnabled(context,false);
    assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED,context.getPackageManager().getComponentEnabledSetting(component));
    Bundle stopped=provider.call("snapshot",null,null);
    assertFalse(stopped.getBoolean("available"));assertEquals("integration-disabled",stopped.getString("reason"));
  }
}
