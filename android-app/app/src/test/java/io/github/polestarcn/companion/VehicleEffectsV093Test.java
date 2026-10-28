package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.Context;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
/** The master effect switch must stop even an explicit settings-page preview. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class VehicleEffectsV093Test {
  @Test public void masterSwitchStopsPreviewAndCanBeRestored() throws Exception {
    Context context=RuntimeEnvironment.getApplication();
    context.getSharedPreferences("preferences",0).edit().clear().commit();
    VehicleHeroView view=new VehicleHeroView(context);
    Field field=VehicleHeroView.class.getDeclaredField("renderer");
    field.setAccessible(true);
    VehicleEffectRenderer engine=(VehicleEffectRenderer)field.get(view);
    view.preview(VehicleEffectSettings.Mode.CHARGING);
    assertTrue(engine.active());
    AppPreferences.set(context,"effect_enabled","off");
    view.reloadEffectSettings();
    assertFalse(engine.active());
    AppPreferences.set(context,"effect_enabled","on");
    view.reloadEffectSettings();
    assertTrue(engine.active());
    view.preview(VehicleEffectSettings.Mode.NONE);
    assertFalse(engine.active());
  }
  @Test public void fiveStylesAllowBothDirectionsAndClampParameters(){
    Context c=RuntimeEnvironment.getApplication();
    c.getSharedPreferences("preferences",0).edit().clear().commit();
    for(String style:new String[]{"C1a","C1d","C1+a","C1+d","C1+c"}){
      AppPreferences.set(c,"effect_charge_style",style);
      assertEquals(style,VehicleEffectSettings.style(c));
      for(String direction:new String[]{"rise","absorb"}){
        AppPreferences.set(c,"effect_charge_direction",direction);
        assertEquals("absorb".equals(direction),VehicleEffectSettings.absorption(c));
      }
    }
    AppPreferences.set(c,"effect_particles","200");
    assertEquals(150,VehicleEffectSettings.count(c));
    AppPreferences.set(c,"effect_speed","10");
    assertEquals(.5f,VehicleEffectSettings.speed(c),.001f);
  }
}
