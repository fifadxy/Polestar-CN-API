package io.github.polestarcn.companion;
import static org.junit.Assert.*;import android.content.Context;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class TrunkIconTest {
 @Test public void newIconIsDefaultAndDoorAlternativeTracksActualState(){Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("preferences",0).edit().clear().commit();for(String theme:new String[]{"light","dark"}){AppPreferences.set(c,"theme",theme);assertEquals(R.drawable.ic_tailgate_detail,TrunkIcon.drawable(c));AppPreferences.set(c,"trunk_icon","car");assertEquals(R.drawable.ic_tailgate,TrunkIcon.drawable(c));AppPreferences.set(c,"trunk_icon","door");assertEquals(R.drawable.ic_lock,TrunkIcon.drawable(c,false));assertEquals(R.drawable.ic_unlock,TrunkIcon.drawable(c,true));AppPreferences.set(c,"trunk_icon","detail");assertEquals(R.drawable.ic_tailgate_detail,WidgetButtons.icon(c,"trunk",null));}}
}
