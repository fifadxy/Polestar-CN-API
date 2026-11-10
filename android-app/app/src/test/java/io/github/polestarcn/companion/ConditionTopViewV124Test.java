package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class ConditionTopViewV124Test {
 @Test public void dayAndNightUseDedicatedOverheadPhotosNotOriginal(){
   Context c=RuntimeEnvironment.getApplication();
   AppPreferences.set(c,"theme","light");
   assertEquals(R.drawable.psa_vehicle_top_white,VehicleArt.conditionTopDrawable(c));
   assertNotEquals(R.drawable.vehicle_top,VehicleArt.conditionTopDrawable(c));
   assertNotNull(new ExteriorView(c));assertNotNull(new TirePressureView(c));
   AppPreferences.set(c,"theme","dark");
   assertEquals(R.drawable.psa_vehicle_top_dark,VehicleArt.conditionTopDrawable(c));
   assertNotEquals(R.drawable.vehicle_top,VehicleArt.conditionTopDrawable(c));
   assertNotNull(new ExteriorView(c));assertNotNull(new TirePressureView(c));
 }
}
