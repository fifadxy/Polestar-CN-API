package io.github.polestarcn.oppobridge;

import static org.junit.Assert.*;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** No LSPosed host/real car involved. Exercises only isolated allowlists and gates. */
@RunWith(RobolectricTestRunner.class)
public final class Beta46CarLinkDualModeTest {
  @Test public void modesFailClosed(){
    assertEquals("stable",BridgeSettings.validMode(null));
    assertEquals("stable",BridgeSettings.validMode("unknown"));
    assertEquals("stable",BridgeSettings.validMode("stable"));
    assertEquals("new",BridgeSettings.validMode("new"));
    assertEquals("mock",BridgeSettings.validMode("mock"));
    assertEquals("exact",BridgeSettings.validRpkMode("wildcard"));
    assertEquals("feature",BridgeSettings.validRpkMode("feature"));
  }
  @Test public void onlyOwnedIntentSchemeCanBeIntercepted(){
    Intent ok=new Intent(Intent.ACTION_VIEW,Uri.parse("psassistant-companion://command?action=lock-toggle"));
    assertEquals("lock-toggle",AdaptiveShelfShortcutInterceptor.requestedAction(ok));
    assertEquals("climate-start",AdaptiveShelfShortcutInterceptor.requestedAction(
      new Intent(Intent.ACTION_VIEW,Uri.parse("psassistant-oppo-bridge://command?action=climate-start"))));
    assertNull(AdaptiveShelfShortcutInterceptor.requestedAction(
      new Intent(Intent.ACTION_VIEW,Uri.parse("https://example.org/command?action=unlock"))));
    assertNull(AdaptiveShelfShortcutInterceptor.requestedAction(
      new Intent(Intent.ACTION_VIEW,Uri.parse("psassistant-companion://external?action=unlock"))));
    assertNull(AdaptiveShelfShortcutInterceptor.requestedAction(
      new Intent(Intent.ACTION_VIEW,Uri.parse("psassistant-companion://command?action=delete-files"))));
  }
  @Test public void remoteActionsNeverUseStaleOrDemoSnapshot(){
    Bundle s=new Bundle();s.putBoolean("available",true);s.putBoolean("cardEnabled",true);
    s.putLong("capturedAt",System.currentTimeMillis());s.putString("vehicleKey","sha256:demo-vehicle");
    assertTrue(AdaptiveShelfShortcutInterceptor.freshAndReal(s));
    s.putBoolean("demo",true);
    assertFalse(AdaptiveShelfShortcutInterceptor.freshAndReal(s));
    s.putBoolean("demo",false);s.putLong("capturedAt",System.currentTimeMillis()-360_000L);
    assertFalse(AdaptiveShelfShortcutInterceptor.freshAndReal(s));
    s.putLong("capturedAt",System.currentTimeMillis()+180_000L);
    assertFalse(AdaptiveShelfShortcutInterceptor.freshAndReal(s));
  }
  @Test public void rpkFeatureGateRejectsUnknownAndMalformed(){
    assertTrue(AdaptiveQuickAppRpkPatch.supportedEntry("cards/SingleDeviceCard4x2/index.js"));
    assertTrue(AdaptiveQuickAppRpkPatch.supportedEntry("cards/SingleDeviceCard2x2/index.js"));
    assertFalse(AdaptiveQuickAppRpkPatch.supportedEntry("wallet/index.js"));
    assertNull(QuickAppCarCapability.patchFeature(new byte[]{1,2,3}));
    assertNull(QuickAppCarCapability.patchFeature(new byte[32]));
  }
  @Test public void exactRpkIsNotRequiredForFeatureVerifiedNewPath(){
    String js="const eventSummary = ['sportData', 'healthData'];\n"
        +"/**/ var _default = exports.default = {\n"
        +"carEventIconName: _config.carEventIconName,\n"
        +"buttonAction: function(item) { clickEvent(this.deviceInfo.id, item.id, Number(!item.state)); },\n"
        +"children: [], enabled: true, name: 'SingleDeviceCard4x2'\n"
        +"/***/ }),\n";
    byte[] bytes=js.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] patched=QuickAppCarCapability.patchFeature(bytes);
    assertNotNull(patched);
    String result=new String(patched,java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(result.contains("'chesuo', 'kongtiao', 'shanguang', 'mingdi'"));
    assertTrue(result.contains("var clickEvent = __webpack_require__"));
    assertNull(QuickAppCarCapability.patchFeature(patched)); // fail on repeated patch
  }
}
