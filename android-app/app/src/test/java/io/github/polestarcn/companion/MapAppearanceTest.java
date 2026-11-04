package io.github.polestarcn.companion;
import android.content.*;import android.content.res.Configuration;import static org.junit.Assert.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class MapAppearanceTest {
 private Context c;
 @Before public void start(){c=RuntimeEnvironment.getApplication();c.getSharedPreferences("preferences",0).edit().clear().commit();}
 private Context system(boolean night){Configuration conf=new Configuration(c.getResources().getConfiguration());conf.uiMode=(conf.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|(night?Configuration.UI_MODE_NIGHT_YES:Configuration.UI_MODE_NIGHT_NO);return c.createConfigurationContext(conf);}
 @Test public void systemModeFollowsOsRatherThanAppTheme(){AppPreferences.set(c,"theme","dark");assertFalse(MapAppearance.dark(system(false)));AppPreferences.set(c,"theme","light");assertTrue(MapAppearance.dark(system(true)));}
 @Test public void manualModesOverrideOsAndInvalidModeFallsBack(){AppPreferences.set(c,"map_theme","light");assertFalse(MapAppearance.dark(system(true)));AppPreferences.set(c,"map_theme","dark");assertTrue(MapAppearance.dark(system(false)));AppPreferences.set(c,"map_theme","invalid");assertEquals("system",MapAppearance.mode(c));assertTrue(MapAppearance.dark(system(true)));}
 @Test public void themeIsBackedUpAndReset()throws Exception{AppPreferences.set(c,"map_theme","dark");org.json.JSONObject v=SettingsBackup.export(c).getJSONObject("values");assertEquals("dark",v.getString("map_theme"));SettingsBackup.reset(c);assertEquals("system",MapAppearance.mode(c));SettingsBackup.restore(c,v);assertEquals("dark",MapAppearance.mode(c));}
 @Test public void cropFocusDefaultsBoundsBackupAndReset()throws Exception{MapMarkerSettings m=new MapMarkerSettings(c);assertEquals(50,m.cropX);assertEquals(50,m.cropY);AppPreferences.set(c,"map_crop_focus_x","999");AppPreferences.set(c,"map_crop_focus_y","-1");m=new MapMarkerSettings(c);assertEquals(100,m.cropX);assertEquals(0,m.cropY);org.json.JSONObject values=SettingsBackup.export(c).getJSONObject("values");assertTrue(values.has("map_crop_focus_x"));SettingsBackup.reset(c);assertEquals(50,new MapMarkerSettings(c).cropX);SettingsBackup.restore(c,values);assertEquals(100,new MapMarkerSettings(c).cropX);}
}
