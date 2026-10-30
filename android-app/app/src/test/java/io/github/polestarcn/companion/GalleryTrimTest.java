package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class GalleryTrimTest {
  @Test public void oldColorsKeepViewFamilyAndFollowTheme() {
    for(String key:new String[]{"top_blue","top_silver"}) {
      assertEquals("top_white",VehicleArt.normalizeKey(key,false));
      assertEquals("top_dark",VehicleArt.normalizeKey(key,true));
      assertEquals(R.drawable.psa_vehicle_top_white,VehicleArt.drawable(key,false,true,false));
    }
    for(String key:new String[]{"angled_blue","angled_silver"}) {
      assertEquals("angled_white",VehicleArt.normalizeKey(key,false));
      assertEquals("angled_black",VehicleArt.normalizeKey(key,true));
      assertEquals(R.drawable.psa_vehicle_angle_black,VehicleArt.drawable(key,true,false,true));
    }
  }
  @Test public void stalePreferenceIsMigratedAndRemovedChoicesAreHidden() {
    Context c=RuntimeEnvironment.getApplication();boolean dark=AppPreferences.dark(c);
    AppPreferences.set(c,VehicleArt.selectionKey(dark),"angled_blue");
    String expected=dark?"angled_black":"angled_white";
    assertEquals(expected,VehicleArt.key(c));
    assertEquals(expected,AppPreferences.get(c,VehicleArt.selectionKey(dark),"auto"));
    for(String key:VehicleArt.KEYS)assertFalse(key.endsWith("blue")||key.equals("top_silver")||key.equals("angled_silver"));
  }
  @Test public void defaultsAndOtherViewsRemainAvailable() {
    assertEquals("auto",VehicleArt.normalizeKey("auto",true));
    assertEquals(R.drawable.vehicle_top,VehicleArt.drawable("auto",false,true,false));
    assertEquals(R.drawable.psa_vehicle_top_dark,VehicleArt.drawable("auto",true,true,false));
    assertEquals(R.drawable.psa_vehicle_front_silver,VehicleArt.drawable("front_silver",false,true,false));
    assertEquals(R.drawable.official_p2_side_snow,VehicleArt.drawable("side_official_snow",false,true,false));
  }
}
