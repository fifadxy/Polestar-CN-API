package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.view.View;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class Beta47SkinButtonLayoutTest {
 @Test public void eachSkinAndControlIsIndependentAndClamped(){
   Context c=RuntimeEnvironment.getApplication();
   c.getSharedPreferences("preferences",0).edit().clear().commit();
   String skin=UiSkin.PS_DEFAULT,other=UiSkin.NIO_LIKE;
   assertEquals(100,SkinButtonLayout.read(c,skin,"air","width"));
   assertEquals(100,SkinButtonLayout.read(c,other,"air","width"));
   int[] values=SkinButtonLayout.values(c,skin,"air");
   values[0]=500;values[1]=70;values[2]=4;values[6]=-13;
   SkinButtonLayout.store(c,skin,"air",values);
   assertEquals(100,SkinButtonLayout.read(c,skin,"air","width"));
   assertEquals(70,SkinButtonLayout.read(c,skin,"air","height"));
   assertEquals(4,SkinButtonLayout.read(c,skin,"air","stroke"));
   assertEquals(-13,SkinButtonLayout.read(c,skin,"air","position_x"));
   assertEquals(100,SkinButtonLayout.read(c,other,"air","height"));
   assertEquals(100,SkinButtonLayout.read(c,skin,"climate","height"));
   SkinButtonLayout.clear(c,skin,"air");
   assertEquals(100,SkinButtonLayout.read(c,skin,"air","height"));
   assertEquals(100,SkinButtonLayout.read(c,other,"air","height"));
 }
 @Test public void slotStaysWeightedAndDoesNotChangeTouchTargetForOtherButtons(){
   Context c=RuntimeEnvironment.getApplication();
   LinearLayout group=new LinearLayout(c);
   LinearLayout tile=new LinearLayout(c);
   SkinButtonLayout.mount(group,tile,"lock",new LinearLayout.LayoutParams(0,70,1));
   assertEquals(1,group.getChildCount());
   assertTrue(group.getChildAt(0) instanceof FrameLayout);
   FrameLayout slot=(FrameLayout)group.getChildAt(0);
   assertSame(tile,slot.getChildAt(0));
   assertEquals("lock",SkinButtonLayout.controlOf(tile));
   assertEquals("lock",SkinButtonLayout.controlOf(slot.getChildAt(0)));
   assertEquals(1f,((LinearLayout.LayoutParams)slot.getLayoutParams()).weight,0f);
 }
 @Test public void backupAllowlistIncludesSkinButtonsWithoutCredentials(){
   Context c=RuntimeEnvironment.getApplication();
   SkinButtonLayout.store(c,UiSkin.PS_DEFAULT,"air",SkinButtonLayout.DEFAULTS);
   assertTrue(SettingsBackup.allowed(SkinButtonLayout.key(UiSkin.PS_DEFAULT,"air","icon")));
   assertFalse(SettingsBackup.allowed("skin_button_ps_default_air_key"));
   assertFalse(SettingsBackup.allowed("auth_token"));
 }
}
