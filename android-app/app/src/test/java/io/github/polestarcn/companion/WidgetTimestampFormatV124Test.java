package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.Calendar;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class WidgetTimestampFormatV124Test {
 @Test public void defaultDateHasMonthDayAndTimeAlsoOnCompactWidgets(){
   Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("preferences",0).edit().clear().commit();
   AppPreferences.set(c,"widget_language","zh");
   Calendar when=Calendar.getInstance();when.clear();when.set(2026,Calendar.NOVEMBER,10,9,12,0);
   assertEquals("MM-dd HH:mm",WidgetTimestampFormat.pattern(c));
   assertEquals("11-10 09:12",WidgetTimestampFormat.format(c,when.getTimeInMillis()));
   assertEquals("11-10 09:12",WidgetTimestampFormat.compactText(WidgetTimestampFormat.text(c,when.getTimeInMillis(),false)));
   assertEquals("11-10 09:12",WidgetTimestampFormat.text(c,when.getTimeInMillis(),true));
 }
 @Test public void acceptedPresetsAndCustomFormatsSurviveBackup(){
   Context c=RuntimeEnvironment.getApplication();
   assertTrue(SettingsBackup.allowed(WidgetTimestampFormat.KEY));
   for(String p:WidgetTimestampFormat.PRESETS)assertTrue(p,WidgetTimestampFormat.valid(p));
   assertTrue(WidgetTimestampFormat.valid("yyyy年MM月dd日 HH:mm"));
   assertFalse(WidgetTimestampFormat.valid("HH:mm\n{token}"));
   assertFalse(WidgetTimestampFormat.valid("EEE yyyy"));
   assertFalse(WidgetTimestampFormat.valid(""));
   AppPreferences.set(c,WidgetTimestampFormat.KEY,"yyyy-MM-dd HH:mm");
   assertEquals("yyyy-MM-dd HH:mm",WidgetTimestampFormat.pattern(c));
   AppPreferences.set(c,WidgetTimestampFormat.KEY,"invalid-format");
   assertEquals(WidgetTimestampFormat.DEFAULT,WidgetTimestampFormat.pattern(c));
 }
}
