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
    String idle=ButtonFeedback.value(c,"air",off,"");
    assertTrue(idle.contains("PM2.5 --"));assertTrue(idle.contains("AQI --"));
    assertFalse(idle.contains("关闭"));
  }
  @Test public void minimalUtilityShowsOnlyActiveFeedbackAndCentersItsGlyph(){
    UiDisplayMode.setMinimal(c,true);UtilityButton button=new UtilityButton(c);
    button.setText("露营");button.setUtilityIcon(R.drawable.ic_camp);button.feedback("");
    assertEquals("",button.getText().toString());assertEquals(android.view.Gravity.CENTER,button.getGravity());
    button.feedback("10m");assertEquals("",button.getText().toString()); // User-approved Minimal mode hides ALL text, including active countdowns.
    UiDisplayMode.setMinimal(c,false);button.feedback("");assertTrue(button.getText().toString().contains("露营"));
  }

  @Test public void missingWindowStateDoesNotCrashMinimalOrStandardFeedback(){
    VehicleSnapshot unknown=new VehicleSnapshot("Polestar","masked",null,null,null,"未知",null,null,null,null,System.currentTimeMillis(),"LIVE",false,null,null);
    UiDisplayMode.setMinimal(c,true);assertEquals("",ButtonFeedback.value(c,"windows",unknown,""));
    UiDisplayMode.setMinimal(c,false);assertNotNull(ControlStatusText.value(c,"windows",unknown,""));
  }

}
