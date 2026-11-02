package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import org.junit.Test;

public class OppoCardWidgetRuntimeCapabilityTest {
  @Test public void supportFlagUsesOfficialBitZeroContract() {
    assertFalse(OppoCardWidgetRuntimeCapability.supportedByFlag(0));
    assertTrue(OppoCardWidgetRuntimeCapability.supportedByFlag(1));
    assertFalse(OppoCardWidgetRuntimeCapability.supportedByFlag(2));
    assertTrue(OppoCardWidgetRuntimeCapability.supportedByFlag(3));
  }

  @Test public void smartEngineVersionWinsOverAssistantMetadata() {
    assertEquals("220011", OppoCardWidgetRuntimeCapability.resolveUiEngineVersion(220011L, "legacy"));
    assertEquals("legacy", OppoCardWidgetRuntimeCapability.resolveUiEngineVersion(-1L, "legacy"));
    assertEquals("", OppoCardWidgetRuntimeCapability.resolveUiEngineVersion(-1L, null));
  }

  @Test public void preservesOfficialSdkArtifactIdentity() {
    assertEquals("10203", OppoCardWidgetRuntimeCapability.SDK_VERSION_CODE);
    assertEquals("external", OppoCardWidgetRuntimeCapability.SDK_BUILD_TYPE);
    assertEquals("com.oplus.cardservice.repository.provider.CardServiceServerProvider",
        OppoCardWidgetRuntimeCapability.CARD_SERVICE_SERVER_PROVIDER);
  }
}