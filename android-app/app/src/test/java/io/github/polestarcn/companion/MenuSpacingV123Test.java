package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class MenuSpacingV123Test {
 @Test public void spacingSettingsAreBoundedAndConfigurable(){
   Context c=RuntimeEnvironment.getApplication();
   AppPreferences.set(c,"menu_spacing_dp","2");assertEquals(2,MenuSpacing.dp(c));
   assertTrue(SettingsBackup.allowed("menu_spacing_dp"));
   AppPreferences.set(c,"menu_spacing_dp","8");assertEquals(8,MenuSpacing.dp(c));
   AppPreferences.set(c,"menu_spacing_dp","500");assertEquals(4,MenuSpacing.dp(c));
   AppPreferences.set(c,"menu_spacing_dp","garbage");assertEquals(4,MenuSpacing.dp(c));
 }
 @Test public void helpHasViOrangeOutlinedQuestionAfterLabelAndRemainsCollapsed(){
   Context c=RuntimeEnvironment.getApplication();
   LinearLayout parent=new LinearLayout(c);
   HelpDisclosure.add(parent,c,"说明","Reference detail");
   LinearLayout group=(LinearLayout)parent.getChildAt(0);
   TextView label=(TextView)group.getChildAt(0);
   assertTrue(label.getText().toString().startsWith("说明"));
   assertTrue(label.getContentDescription().toString().contains("说明？"));
   Spanned rich=(Spanned)label.getText();
   ImageSpan[] icons=rich.getSpans(0,rich.length(),ImageSpan.class);
   assertEquals(1,icons.length);
   Drawable icon=icons[0].getDrawable();assertTrue(icon.getIntrinsicWidth()>0);
   assertEquals(View.GONE,group.getChildAt(1).getVisibility());
   assertTrue(label.performClick());
   assertEquals(View.VISIBLE,group.getChildAt(1).getVisibility());
 }
}
