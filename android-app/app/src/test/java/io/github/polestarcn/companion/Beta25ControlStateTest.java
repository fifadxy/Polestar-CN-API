package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class Beta25ControlStateTest {
  private Context c;
  @Before public void clear(){
    c=RuntimeEnvironment.getApplication();
    c.getSharedPreferences("preferences",0).edit().clear().commit();
    AppPreferences.set(c,"language","zh");AppPreferences.set(c,"vehicle","car-a");
    AirRuntime.stopped(c);ClimateRuntime.stopped(c);
  }
  private VehicleSnapshot state(String climate,String air){
    return new VehicleSnapshot("Polestar 2","masked",51,210,12000,"空闲","全部关闭","全部关闭","已关闭","已上锁",
        System.currentTimeMillis(),"LIVE",false,climate,air);
  }
  @Test public void purificationCountdownRequiresConfirmedStartAndIsScopedToVehicle(){
    VehicleSnapshot on=state("关闭","开启");long now=System.currentTimeMillis();
    assertNull(AirRuntime.remaining(c,on,"car-a",now));
    assertEquals("开启",AirRuntime.label(c,on,"car-a"));
    AirRuntime.started(c,"car-a",now);
    assertEquals(Long.valueOf(300_000),AirRuntime.remaining(c,on,"car-a",now));
    assertEquals(Long.valueOf(239_000),AirRuntime.remaining(c,on,"car-a",now+61_000));
    assertNull(AirRuntime.remaining(c,on,"car-b",now));
    assertNull(AirRuntime.remaining(c,state("关闭","关闭"),"car-a",now));
    assertEquals(Long.valueOf(0),AirRuntime.remaining(c,on,"car-a",now+300_001));
    AirRuntime.started(c,"car-a",now-300_001);
    assertEquals("待确认",AirRuntime.label(c,on,"car-a"));
    assertEquals("开启",on.air); // Timer expiry must not invent a stopped vehicle.
    AirRuntime.stopped(c);assertEquals("开启",AirRuntime.label(c,on,"car-a"));
  }
  @Test public void compactClimateAndAirLabelsKeepActualStatusAndTime(){
    long now=System.currentTimeMillis();
    VehicleSnapshot on=state("开启","开启").withEnvironment(new EnvironmentSnapshot("开启","开启",20.0,22.0,3.0,4,15,5,now,now));
    assertEquals("15m",ControlStatusText.value(c,"climate",on,""));
    assertEquals("5:00",ControlStatusText.value(c,"air",on,""));
    assertEquals("关闭",ControlStatusText.value(c,"climate",state("关闭","关闭"),""));
    assertEquals("关闭",ControlStatusText.value(c,"air",state("关闭","关闭"),""));
  }
  @Test public void minimalModeHidesInactiveFeedbackAndPreservesPendingAndActive(){
    VehicleSnapshot off=state("关闭","关闭"),on=state("开启","开启");
    UiDisplayMode.setMinimal(c,true);
    for(String key:new String[]{"climate","air","lock","trunk","windows"})
      assertEquals(key,"",ButtonFeedback.value(c,key,off,""));
    assertEquals("开启",ButtonFeedback.value(c,"climate",on,""));
    assertEquals("开启",ButtonFeedback.value(c,"air",on,""));
    assertEquals("发送中",ButtonFeedback.value(c,"air",off,"发送中"));
    UiDisplayMode.setMinimal(c,false);
    assertEquals("关闭",ButtonFeedback.value(c,"air",off,""));
  }
}
