package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.Context;import android.view.View;
import org.junit.Test;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.annotation.GraphicsMode;
/** Preserve v093 master-switch and five-style regression intent on the unified renderer. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,qualifiers="w360dp-h780dp-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class VehicleEffectsV093Test {
 @Test public void masterSwitchStopsPreviewAndCanBeRestored(){
  Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("preferences",0).edit().clear().commit();
  org.robolectric.android.controller.ActivityController<VehicleEffectsActivity> controller=Robolectric.buildActivity(VehicleEffectsActivity.class).setup();View root=controller.get().getWindow().getDecorView();root.measure(View.MeasureSpec.makeMeasureSpec(360,1073741824),View.MeasureSpec.makeMeasureSpec(780,1073741824));root.layout(0,0,360,780);VehicleHeroView hero=(VehicleHeroView)root.findViewWithTag("effect_test_preview");hero.setEffectsHostActive(true);assertTrue(hero.effectsRunning());
  AppPreferences.set(c,"light_effects","off");org.robolectric.shadows.ShadowLooper.shadowMainLooper().idle();assertFalse(hero.effectsRunning());
  AppPreferences.set(c,"light_effects","on");org.robolectric.shadows.ShadowLooper.shadowMainLooper().idle();assertTrue(hero.effectsRunning());hero.setPreviewMode(EffectStateController.Mode.OFF);assertFalse(hero.effectsRunning());controller.pause().stop().destroy();
 }
 @Test public void fiveStylesAllowBothDirectionsAndClampParameters(){Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("preferences",0).edit().clear().commit();for(int i=0;i<5;i++){AppPreferences.set(c,"fx_style",EffectSettings.STYLES[i]);assertEquals(i,new EffectSettings(c).style);for(String direction:new String[]{"up","absorb"}){AppPreferences.set(c,"fx_direction",direction);assertEquals("absorb".equals(direction),new EffectSettings(c).absorb);}}AppPreferences.set(c,"fx_particles","200");assertEquals(150,new EffectSettings(c).particles);AppPreferences.set(c,"fx_charge_speed","10");assertEquals(.5f,new EffectSettings(c).chargeSpeed,.001f);}
}
