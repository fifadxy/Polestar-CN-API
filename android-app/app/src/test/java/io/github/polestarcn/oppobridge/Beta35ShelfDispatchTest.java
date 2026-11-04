package io.github.polestarcn.oppobridge;

import static org.junit.Assert.*;
import android.os.Bundle;
import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class Beta35ShelfDispatchTest {
  @Test public void actualRpkBundleKeysMatchOnlyCurrentVehicle(){
    Bundle event=new Bundle();event.putString("deviceId","psa_123");event.putString("shortcutId","chesuo");event.putInt("valueInt",1);
    assertTrue(QuickAppCarDispatchShim.matchesEvent(event,"psa_123"));
    assertFalse(QuickAppCarDispatchShim.matchesEvent(event,"psa_other"));
    event.putString("shortcutId","power_11520_1");assertFalse(QuickAppCarDispatchShim.matchesEvent(event,"psa_123"));
  }
  @Test public void duplicateClickIsRecognizedWithoutReexecuting(){
    assertTrue(QuickAppCarDispatchShim.admit("test-car|chesuo",10000));
    assertFalse(QuickAppCarDispatchShim.admit("test-car|chesuo",10100));
    assertTrue(QuickAppCarDispatchShim.admit("test-car|chesuo",11200));
  }
  @Test public void carModuleReceivesItsMissingApiBinding(){
    String source="var _default = exports.default = { data: { carEventIconName: _config.carEventIconName, }, click: function(){ clickEvent(this.deviceInfo.id, item.id, Number(!item.state)); }}; /***/ }),";
    String fixed=QuickAppCarCapability.bindCarClick(source);
    assertNotNull(fixed);assertTrue(fixed.indexOf("var clickEvent = __webpack_require__")<fixed.indexOf("var _default"));
    assertNull(QuickAppCarCapability.bindCarClick("unrelated module"));
  }
  @Test public void shortcutStatesReflectTheCachedVehicle() throws Exception {
    Bundle s=new Bundle();s.putString("lock","LOCKED");s.putString("climate","ACTIVE");
    JSONArray buttons=QuickAppCarDataShim.oemShortcuts(s);
    assertEquals(1,buttons.getJSONObject(0).getInt("state"));assertEquals(1,buttons.getJSONObject(1).getInt("state"));
    s.putString("lock","UNLOCKED");s.putString("climate","INACTIVE");
    buttons=QuickAppCarDataShim.oemShortcuts(s);
    assertEquals(0,buttons.getJSONObject(0).getInt("state"));assertEquals(0,buttons.getJSONObject(1).getInt("state"));
  }
}
