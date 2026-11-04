package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import java.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class Beta35MockSurfaceTest {
  private Context context;
  @Before public void prepare(){
    context=RuntimeEnvironment.getApplication();
    context.getSharedPreferences("legal",0).edit().putBoolean("accepted_disclaimer_v1",true).commit();
    if(BuildConfig.OPPO_SDK_MOCK)OppoCarLinkMockHarness.reset(context);
  }
  private void start(){
    OppoCarLinkMockHarness.runFull(context,null);
    Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4));
    assertFalse(OppoCarLinkMockHarness.report(),OppoCarLinkMockHarness.running());
    assertFalse(OppoCarLinkMockHarness.report(),OppoCarLinkMockHarness.report().contains("FAIL"));
  }
  @Test public void fullTestAndNativeProviderShareOneMockVehicleWithoutLogin(){
    if(!BuildConfig.OPPO_SDK_MOCK)return;
    start();
    PsAssistantBridgeProvider provider=Robolectric.buildContentProvider(PsAssistantBridgeProvider.class).create().get();
    Bundle state=provider.call("snapshot",null,null);
    assertTrue(state.getBoolean("mock"));assertEquals("Polestar 2 TestLab",state.getString("name"));
    Bundle session=provider.call("virtual-session-profile",null,null);
    assertTrue(session.getBoolean("active"));assertEquals("MOCK_TESTLAB",session.getString("mode"));
    Bundle request=new Bundle();request.putString("action","lock-toggle");
    Bundle route=provider.call("gateway-resolve",null,request);
    assertTrue(route.getBoolean("accepted"));assertEquals("unlock",route.getString("command"));
    // Cooldown is five seconds for a repeated same-direction SDK command.
    Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5));
    request.putString("action","unlock");
    Bundle result=provider.call("command",null,request);
    assertTrue(result.getBoolean("accepted"));assertTrue(result.getBoolean("mock"));assertFalse(result.getBoolean("commandSent"));
    Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300));
    assertEquals("未锁",provider.call("snapshot",null,null).getString("lock"));
    assertEquals(result.getString("taskId"),OppoCarLinkMockHarness.status().getString("lastTaskId"));
    assertEquals(1,OppoCarLinkMockHarness.status().getInt("lastControlCode"));
  }
  @Test public void resetCancelsQueuedMutationAndAllowsAnotherFullRun(){
    if(!BuildConfig.OPPO_SDK_MOCK)return;
    start();Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5));
    assertTrue(OppoCarLinkMockHarness.submit(context,"unlock").getBoolean("accepted"));
    Shadows.shadowOf(Looper.getMainLooper()).idle();
    OppoCarLinkMockHarness.reset(context);
    Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500));
    assertEquals("已锁",OppoCarLinkMockFixture.snapshot().lock);
    assertFalse(OppoCarLinkMockHarness.status().getBoolean("bound"));
    start();
  }
  @Test public void unsupportedMockActionNeverFallsThroughToRealBackend(){
    if(!BuildConfig.OPPO_SDK_MOCK)return;
    start();
    Bundle b=OppoCarLinkMockHarness.submit(context,"air-start");
    assertFalse(b.getBoolean("accepted"));assertFalse(b.getBoolean("commandSent"));
    assertEquals("mock-command-unsupported",b.getString("reason"));
  }
}
