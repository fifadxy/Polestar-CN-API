package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.Intent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import static org.robolectric.Shadows.shadowOf;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class OppoWidgetSetupTest {
 @Test public void observedPickerIntentHasExactComponentAndStateWithoutInventedAction(){Intent i=OppoWidgetSetupActivity.standardPickerIntent();assertEquals("com.android.launcher",i.getComponent().getPackageName());assertEquals("com.android.launcher.Launcher",i.getComponent().getClassName());assertEquals("ToggleBar_widget",i.getStringExtra("state"));assertNull(i.getAction());assertNull(i.getCategories());}
 @Test public void setupCanOpenAndLaunchPickerWithoutVehicleSession(){OppoWidgetSetupActivity a=Robolectric.buildActivity(OppoWidgetSetupActivity.class).setup().get();a.findViewById(android.R.id.button1).performClick();assertEquals(OppoWidgetSetupActivity.standardPickerIntent().getComponent(),shadowOf(a).getNextStartedActivity().getComponent());}
 @org.junit.Test public void hostExportUsesUserSelectedDocument() {
   android.content.Intent intent=OppoWidgetSetupActivity.hostExportIntent();
   org.junit.Assert.assertEquals(android.content.Intent.ACTION_CREATE_DOCUMENT,intent.getAction());
   org.junit.Assert.assertEquals("application/zip",intent.getType());
   org.junit.Assert.assertTrue(intent.hasCategory(android.content.Intent.CATEGORY_OPENABLE));
   org.junit.Assert.assertEquals("PsAssistant-OPPO-light.zip",intent.getStringExtra(android.content.Intent.EXTRA_TITLE));
 }
}
