package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Intent;
import org.junit.Test;\nimport org.junit.runner.RunWith;\nimport org.robolectric.RobolectricTestRunner;\nimport org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)\n@Config(sdk=28)\npublic class OppoCarLinkOfficialTest {
  @Test public void bindRouteMatchesOfficialDemoContract() {
    Intent intent=OppoOfficialCarLink.bindIntent();
    assertEquals(Intent.ACTION_VIEW,intent.getAction());
    assertEquals("card",intent.getData().getScheme());
    assertEquals("carlink.heytap.com",intent.getData().getHost());
    assertEquals("bind",intent.getData().getQueryParameter("operation"));
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
