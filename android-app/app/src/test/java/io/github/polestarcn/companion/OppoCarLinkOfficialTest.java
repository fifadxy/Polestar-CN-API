package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class OppoCarLinkOfficialTest {
  @Test public void bindRouteMatchesOfficialDemoContract() {
    Intent intent=OppoOfficialCarLink.bindIntent();
    assertEquals(Intent.ACTION_VIEW,intent.getAction());
    assertEquals("card",intent.getData().getScheme());
    assertEquals("carlink.heytap.com",intent.getData().getHost());
    assertEquals("bind",intent.getData().getQueryParameter("operation"));
  }

  @Test public void protectedManifestEntryIsNotTreatedAsLaunchable() {
    assertFalse(OppoOfficialCarLink.directLaunchAllowed(
        false,"",PackageManager.PERMISSION_GRANTED));
    assertFalse(OppoOfficialCarLink.directLaunchAllowed(
        true,"oppo.permission.PRIVATE",PackageManager.PERMISSION_DENIED));
    assertTrue(OppoOfficialCarLink.directLaunchAllowed(
        true,"",PackageManager.PERMISSION_DENIED));
    assertTrue(OppoOfficialCarLink.directLaunchAllowed(
        true,"oppo.permission.PRIVATE",PackageManager.PERMISSION_GRANTED));
  }

  @Test public void localFallbackTargetsTheCurrentApplicationOnly() {
    Intent intent=OppoOfficialCarLink.localBindIntentForPackage("io.github.polestarcn.companion");
    assertEquals(OppoOfficialCarLink.LOCAL_BIND_ACTION,intent.getAction());
    assertEquals("io.github.polestarcn.companion",intent.getPackage());
  }

  @Test public void mapsSupportedInstructions() {
    assertEquals("unlock",OppoCarLinkCommandMap.toPsAssistantAction("CAR-DOOR","OPEN"));
    assertEquals("lock",OppoCarLinkCommandMap.toPsAssistantAction("LOCK-CAR","LOCK"));
    assertEquals("windows-open",OppoCarLinkCommandMap.toPsAssistantAction("ALL-WINDOW","OPEN"));
    assertEquals("windows-close",OppoCarLinkCommandMap.toPsAssistantAction("ALL-WINDOW","CLOSE"));
    assertEquals("trunk-open",OppoCarLinkCommandMap.toPsAssistantAction("CAR-BOOT","OPEN"));
    assertEquals("honk",OppoCarLinkCommandMap.toPsAssistantAction("CAR-HORN",""));
    assertEquals("flash",OppoCarLinkCommandMap.toPsAssistantAction("DOUBLE-FLASH",""));
    assertEquals("honk-flash",OppoCarLinkCommandMap.toPsAssistantAction("CAR-HORN&&DOUBLE-FLASH",""));
    assertEquals("climate-start",OppoCarLinkCommandMap.toPsAssistantAction("AC-TEMP","22"));
    assertEquals("climate-stop",OppoCarLinkCommandMap.toPsAssistantAction("AC-TEMP","OFF"));
  }

  @Test public void unsupportedFunctionsStayUnsupported() {
    assertNull(OppoCarLinkCommandMap.toPsAssistantAction("SUNROOF","OPEN"));
    assertNull(OppoCarLinkCommandMap.toPsAssistantAction("LEFT-FRONT-WINDOW","OPEN"));
    assertNull(OppoCarLinkCommandMap.toPsAssistantAction("CAR-ENGINE","OPEN"));
    assertNull(OppoCarLinkCommandMap.toPsAssistantAction("ACTIVE-CAR","1"));
  }
}
