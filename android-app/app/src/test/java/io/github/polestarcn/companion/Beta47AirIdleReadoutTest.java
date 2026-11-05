package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class Beta47AirIdleReadoutTest {
  private VehicleSnapshot snapshot(String air){
    VehicleSnapshot base=new VehicleSnapshot("Polestar 2","VIN_TEST",80,350,1000,
        "空闲","全部关闭","全部关闭","已关闭","已锁",System.currentTimeMillis(),
        "LIVE",false,"关闭",air);
    return base.withEnvironment(new EnvironmentSnapshot("关闭",air,null,null,12.5,43,
        null,null,0L,0L));
  }
  @Test public void inactiveAirShowsActualMetricsAndNeverSaysOff(){
    Context c=RuntimeEnvironment.getApplication();
    c.getSharedPreferences("preferences",0).edit().clear().commit();
    AppPreferences.set(c,AirButtonMetrics.KEY,"both");
    String v=ButtonFeedback.value(c,"air",snapshot("关闭"),"关闭");
    assertTrue(v.contains("AQI 43"));
    assertTrue(v.contains("PM2.5 12.5"));
    assertFalse(v.contains("已关闭"));assertFalse(v.contains("关闭"));
    assertFalse(v.contains("Off"));assertFalse(v.contains("Closed"));
  }
  @Test public void missingMetricsNeverProduceInventedNumbers(){
    Context c=RuntimeEnvironment.getApplication();
    c.getSharedPreferences("preferences",0).edit().clear().commit();
    String v=ButtonFeedback.value(c,"air",null,"关闭");
    assertTrue(v.contains("AQI --"));assertTrue(v.contains("PM2.5 --"));
    assertFalse(v.contains("已关闭"));
  }
  @Test public void pendingAndHiddenSettingsStillTakePriority(){
    Context c=RuntimeEnvironment.getApplication();
    c.getSharedPreferences("preferences",0).edit().clear().commit();
    assertEquals("待确认",ButtonFeedback.value(c,"air",snapshot("关闭"),"待确认"));
    AppPreferences.set(c,"feedback_air","hidden");
    assertEquals("",ButtonFeedback.value(c,"air",snapshot("关闭"),"关闭"));
  }
}