package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class Beta48UiStabilityTest {
  private VehicleSnapshot snapshot(){
    VehicleSnapshot base=new VehicleSnapshot("Polestar 2","TEST",79,322,0,
        "空闲","全部关闭","全部关闭","已关闭","已锁",System.currentTimeMillis(),
        "LIVE",false,"关闭","关闭");
    return base.withEnvironment(new EnvironmentSnapshot("关闭","关闭",null,null,12.5,43,
        null,null,0L,0L));
  }

  @Test public void longPressDefaultsToSixTenthsAndSupportsSafeSettings(){
    Context c=RuntimeEnvironment.getApplication();
    c.getSharedPreferences("preferences",0).edit().clear().commit();
    assertEquals(600,LongPressTiming.millis(c));
    assertEquals("0.6 s",LongPressTiming.label(c));
    AppPreferences.set(c,LongPressTiming.KEY,"400");assertEquals(400,LongPressTiming.millis(c));
    AppPreferences.set(c,LongPressTiming.KEY,"1500");assertEquals(1500,LongPressTiming.millis(c));
    AppPreferences.set(c,LongPressTiming.KEY,"1100");assertEquals(600,LongPressTiming.millis(c));
    AppPreferences.set(c,LongPressTiming.KEY,"invalid");assertEquals(600,LongPressTiming.millis(c));
  }

  @Test public void longPressDurationIsIncludedInSettingsBackups() throws Exception {
    Context c=RuntimeEnvironment.getApplication();
    c.getSharedPreferences("preferences",0).edit().clear().commit();
    AppPreferences.set(c,LongPressTiming.KEY,"800");
    assertTrue(SettingsBackup.allowed(LongPressTiming.KEY));
    org.json.JSONObject settings=SettingsBackup.export(c).getJSONObject("values");
    c.getSharedPreferences("preferences",0).edit().clear().commit();
    SettingsBackup.restore(c,settings);
    assertEquals(800,LongPressTiming.millis(c));
  }

  @Test public void airUsesEqualSizeLabelsAndWeightedValuesWithFineDivider(){
    Context c=RuntimeEnvironment.getApplication();
    c.getSharedPreferences("preferences",0).edit().clear().commit();
    AppPreferences.set(c,AirButtonMetrics.KEY,"both");
    CharSequence shown=AirButtonMetrics.styled(c,snapshot());
    assertEquals("PM2.5 12.5  │  AQI 43",shown.toString());
    Spanned spans=(Spanned)shown;
    StyleSpan[] bold=spans.getSpans(0,shown.length(),StyleSpan.class);
    assertEquals(2,bold.length);
    for(StyleSpan run:bold)assertEquals(Typeface.BOLD,run.getStyle());
    AppPreferences.set(c,AirButtonMetrics.LAYOUT_KEY,"double");
    assertEquals("PM2.5 12.5\nAQI 43",AirButtonMetrics.styled(c,snapshot()).toString());
    AppPreferences.set(c,AirButtonMetrics.KEY,"aqi");
    assertEquals("AQI 43",AirButtonMetrics.styled(c,snapshot()).toString());
    AppPreferences.set(c,AirButtonMetrics.KEY,"off");
    assertEquals("",AirButtonMetrics.styled(c,snapshot()).toString());
  }

  @Test public void compactMapWindowFitsHalfOfEveryScreenAndIsCentered(){
    for(int[] size:new int[][]{{320,568},{360,780},{640,320},{1080,2400},{1200,900}}){
      int[] map=DialogWindowGeometry.map(size[0],size[1],3f);
      assertTrue(map[0]>0&&map[1]>0);
      assertTrue(map[0]<=size[0]&&map[1]<=size[1]);
      assertTrue((double)map[0]*map[1] <= size[0]*size[1]*.5d);
    }
    Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
    HapticDialogBuilder builder=new HapticDialogBuilder(activity).compactMapWindow();
    AlertDialog dialog=builder.setTitle("Map").setView(new TextView(activity)).create();
    assertNotNull(dialog.getWindow());
    assertEquals(Gravity.CENTER,dialog.getWindow().getAttributes().gravity);
    int[] expected=DialogWindowGeometry.map(activity.getResources().getDisplayMetrics().widthPixels,
        activity.getResources().getDisplayMetrics().heightPixels,
        activity.getResources().getDisplayMetrics().density);
    assertEquals(expected[0],dialog.getWindow().getAttributes().width);
    assertEquals(expected[1],dialog.getWindow().getAttributes().height);
    assertEquals(R.style.PsaDialogFadeAnimation,dialog.getWindow().getAttributes().windowAnimations);
    dialog.show();
    // Android can restore WRAP_CONTENT after OnShow. The registered first
    // pre-draw performs the correction before either visible frame.
    dialog.getWindow().getDecorView().getViewTreeObserver().dispatchOnPreDraw();
    assertEquals(expected[0],dialog.getWindow().getAttributes().width);
    assertEquals(expected[1],dialog.getWindow().getAttributes().height);
    assertEquals(Gravity.CENTER,dialog.getWindow().getAttributes().gravity);
    dialog.dismiss();
    activity.finish();
  }
}
