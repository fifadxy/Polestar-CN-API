package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.Calendar;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class EnergySummaryV124IntegratedTest {
  @Test public void tabsUseApprovedNamesAndThreeChartStyles(){
    Context c=RuntimeEnvironment.getApplication();
    AppPreferences.set(c,"language","zh");AppPreferences.set(c,"energy_chart_compare_days","7");
    EnergySummaryPanel panel=new EnergySummaryPanel(c,null,null);
    LinearLayout metrics=(LinearLayout)panel.getChildAt(10);
    String[] expected={"行驶能耗","驻车空调","行驶里程","能耗费"};
    for(int i=0;i<4;i++)assertEquals(expected[i],((TextView)metrics.getChildAt(i)).getText().toString());
    View mileStats=panel.findViewWithTag("energy_mileage_statistics");
    assertNotNull(mileStats);assertEquals(View.GONE,mileStats.getVisibility());
    assertTrue(metrics.getChildAt(2).performClick());
    assertTrue(metrics.getChildAt(2).isSelected());assertEquals(View.VISIBLE,mileStats.getVisibility());
    // All retained history must not force monthly aggregation: explicit day,
    // week and month selections remain active and visually selected.
    LinearLayout ranges=panel.findViewWithTag("energy_range_periods");
    assertTrue(ranges.getChildAt(4).isSelected());
    LinearLayout granularity=(LinearLayout)panel.findViewWithTag("energy_bucket_controls");
    assertNotNull(granularity);assertEquals(3,granularity.getChildCount());
    for(int index:new int[]{0,1,2,0,1}){
      assertTrue(granularity.getChildAt(index).performClick());
      assertTrue("Selected grouping must survive the All range",granularity.getChildAt(index).isSelected());
    }
    assertTrue(ranges.getChildAt(1).performClick());
    assertTrue(ranges.getChildAt(4).performClick());
    assertTrue("Week selection persists after returning to All",granularity.getChildAt(1).isSelected());
    assertEquals("week",AppPreferences.get(c,"energy_chart_step",""));
    LinearLayout styles=(LinearLayout)panel.findViewWithTag("energy_style_controls");
    assertNotNull(styles);assertEquals(4,styles.getChildCount());
    assertTrue(styles.getChildAt(2).performClick());assertTrue(styles.getChildAt(2).isSelected());
    TextView values=(TextView)panel.findViewWithTag("energy_value_labels_toggle");
    assertNotNull(values);assertTrue(values.getText().toString().contains("最新"));
    assertTrue(values.performClick());assertTrue(values.getText().toString().contains("全部"));
    assertTrue(values.performClick());assertTrue(values.getText().toString().contains("隐藏"));
    assertTrue(values.performClick());assertTrue(values.getText().toString().contains("最新"));
    assertTrue(SettingsBackup.allowed(EnergyBackdropView.METRIC));
  }
  @Test public void comparisonCyclesThrough7_31_3AndOff(){
    Context c=RuntimeEnvironment.getApplication();AppPreferences.set(c,"language","zh");AppPreferences.set(c,"energy_chart_compare_days","7");
    EnergySummaryPanel panel=new EnergySummaryPanel(c,null,null);
    TextView control=(TextView)panel.findViewWithTag("energy_compare_toggle");
    assertTrue(control.getText().toString().contains("前7天"));
    assertTrue(control.performClick());assertTrue(control.getText().toString().contains("前31天"));
    assertTrue(control.performClick());assertTrue(control.getText().toString().contains("前3天"));
    assertTrue(control.performClick());assertTrue(control.getText().toString().contains("不显示"));
  }
  @Test public void customRangeIncludesEndOfDayAndAllowsMonthAdjustments(){
    long begin=EnergySummaryPanel.startOfDate(2026,11,10);
    long until=EnergySummaryPanel.endOfDate(2026,11,10);
    assertTrue(until>=begin);assertTrue(until-begin>23*60*60*1000L);
    Calendar next=Calendar.getInstance();next.setTimeInMillis(until+1);
    assertEquals(11,next.get(Calendar.MONTH)+1);assertEquals(11,next.get(Calendar.DAY_OF_MONTH));
  }
}
