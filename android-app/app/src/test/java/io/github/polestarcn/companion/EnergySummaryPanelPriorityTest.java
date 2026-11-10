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

/** Default popup must show TM distance, TM energy, cost and chart before secondary data. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class EnergySummaryPanelPriorityTest {
  private VehicleSnapshot trip(Context c) {
    long now=System.currentTimeMillis();
    return new VehicleSnapshot("Polestar 2","masked",75,250,12000,
        "空闲","全部关闭","全部关闭","已关闭","已上锁",now,"LIVE",false,"关闭","关闭")
        .withMileage(new MileageSnapshot(120000L,123.0,18.0,40.0,30.0,20.0,19.0,now,now));
  }
  @Test public void popupPrioritizesThreeTmKpisThenChart() {
    Context c=RuntimeEnvironment.getApplication();
    AppPreferences.set(c,"energy_price","1.2");
    EnergySummaryPanel p=new EnergySummaryPanel(c,trip(c),null);
    assertEquals("energy_kpi_tm_distance",p.getChildAt(1).getTag());
    assertEquals("energy_kpi_tm_total_energy",p.getChildAt(2).getTag());
    assertEquals("energy_kpi_tm_cost",p.getChildAt(3).getTag());
    assertEquals("energy_chart_heading",p.getChildAt(4).getTag());
    assertEquals("energy_history_chart",p.getChildAt(5).getTag());
    assertEquals("tm_average_consumption_secondary",p.getChildAt(8).getTag());
    TextView distance=(TextView)((LinearLayout)p.getChildAt(1)).getChildAt(1);
    TextView energy=(TextView)((LinearLayout)p.getChildAt(2)).getChildAt(1);
    TextView cost=(TextView)((LinearLayout)p.getChildAt(3)).getChildAt(1);
    assertTrue(distance.getText().toString().contains("123"));
    assertTrue(energy.getText().toString().contains("24"));
    assertTrue(cost.getText().toString().contains("29"));
  }
  @Test public void chartIsVisibleInitiallyAndCanBeCollapsed() {
    Context c=RuntimeEnvironment.getApplication();
    EnergySummaryPanel p=new EnergySummaryPanel(c,null,null);
    View chart=p.getChildAt(5);
    View toggle=p.getChildAt(7);
    assertEquals(View.VISIBLE,chart.getVisibility());
    assertTrue(toggle.performClick());
    assertEquals(View.GONE,chart.getVisibility());
    assertTrue(toggle.performClick());
    assertEquals(View.VISIBLE,chart.getVisibility());
  }
}
