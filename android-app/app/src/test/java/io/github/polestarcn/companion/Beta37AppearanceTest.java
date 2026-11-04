package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.RectF;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class Beta37AppearanceTest {
 private Context c;
 @Before public void start(){c=RuntimeEnvironment.getApplication();c.getSharedPreferences("preferences",0).edit().clear().commit();}
 @Test public void expandedRangesAndSmoothEntryPreserveSavedValues(){AppPreferences.set(c,"ui_icon_percent","250");AppPreferences.set(c,"ui_button_text_percent","220");AppPreferences.set(c,UiTypography.key(c),"200");AppPreferences.set(c,"fx_charge_field_range","800");assertEquals(250,ButtonAppearance.iconPercent(c));assertEquals(220,ButtonAppearance.textPercent(c));assertEquals(200,UiTypography.percent(c,UiTypography.key(c)));assertEquals(800,new EffectSettings(c).chargeField.range);assertEquals(0f,EffectSettings.entryAlpha(0,100),0f);assertEquals(1f,EffectSettings.entryAlpha(.5f,100),0f);assertTrue(EffectSettings.entryAlpha(.2f,100)<EffectSettings.entryAlpha(.3f,100));}
 @Test public void distributedOriginsDoNotShareOneRectangularBoundary(){ParticleField field=new EffectSettings(c).chargeField;RectF bounds=new RectF(0,0,100,100);float[] a=new float[2],b=new float[2];field.start(bounds,0,.1f,.5f,a);field.start(bounds,1,.1f,.5f,b);assertNotEquals(a[1],b[1],.001f);}
 @Test public void previewAndServiceWidgetUseReadOnlyComponents(){SettingsPreviewView preview=new SettingsPreviewView(c);preview.previewIcons(250);preview.previewLabels(220);preview.previewFont(200);preview.previewWidget(60);assertNotNull(preview.findViewWithTag("settings_live_preview"));assertEquals("service",WidgetButtons.intentAction("status"));AppPreferences.set(c,"widget_language","zh");assertEquals("保养",WidgetButtons.label(c,"status"));}
}
