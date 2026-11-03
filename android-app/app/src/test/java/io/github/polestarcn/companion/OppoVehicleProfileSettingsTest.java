package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public final class OppoVehicleProfileSettingsTest {
  @Test public void defaultsArePreFilledForPolestarTestLab(){
    Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences(OppoVehicleProfileSettings.PREF,0).edit().clear().commit();
    Bundle p=OppoVehicleProfileSettings.snapshot(c);
    assertEquals("POLESTAR",p.getString("providerCode"));assertEquals("CAR",p.getString("deviceType"));
    assertEquals("POLESTAR_2",p.getString(OppoVehicleProfileSettings.MODEL_ID));assertEquals("Polestar 2",p.getString(OppoVehicleProfileSettings.MODEL_NAME));
    assertEquals("com.oplus.ocs.carlink.control.CarControl",p.getString(OppoVehicleProfileSettings.CAR_CONTROL_CLASS));
    assertTrue(java.util.Arrays.asList(p.getStringArray("capabilities")).contains("LOCK"));
  }
  @Test public void restoreDefaultsReplacesEditedValues(){
    Context c=RuntimeEnvironment.getApplication();Bundle edit=new Bundle();edit.putString(OppoVehicleProfileSettings.MODEL_NAME,"Test Car");OppoVehicleProfileSettings.save(c,edit);
    assertEquals("Test Car",OppoVehicleProfileSettings.snapshot(c).getString(OppoVehicleProfileSettings.MODEL_NAME));
    OppoVehicleProfileSettings.restoreDefaults(c);assertEquals("Polestar 2",OppoVehicleProfileSettings.snapshot(c).getString(OppoVehicleProfileSettings.MODEL_NAME));
  }
}
