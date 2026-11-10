package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class ControlStatusV122Test {
  private Context context() {
    Context c=RuntimeEnvironment.getApplication();
    c.getSharedPreferences("preferences",0).edit().clear().commit();
    c.getSharedPreferences("camp_status",0).edit().clear().commit();
    AppPreferences.set(c,"language","zh");
    UiDisplayMode.setMinimal(c,false);
    return c;
  }
  private VehicleSnapshot vehicle(String trunk,String lock) {
    return new VehicleSnapshot("Polestar 2","fixture",70,280,10000,"空闲",
        "全部关闭","全部关闭",trunk,lock,System.currentTimeMillis(),
        "LIVE",false,"关闭","关闭");
  }
  @Test public void findCarIdleIsHornFlashEvenWithoutTelemetry() {
    Context c=context();
    assertEquals("鸣闪",ControlStatusText.value(c,"signal",null,"未知"));
    assertEquals("鸣闪",ButtonFeedback.value(c,"signal",null,"未知"));
    assertEquals("鸣闪",ButtonFeedback.value(c,"signal",vehicle("已关闭","已上锁"),"未知"));
    assertEquals("待确认",ButtonFeedback.value(c,"signal",null,"待确认"));
  }
  @Test public void inactiveCampingUsesOffButKeepsFullDetails() {
    Context c=context();
    assertEquals("关闭",CampModeManager.compactStatus(c));
    assertEquals("关闭",ControlStatusText.value(c,"camp",null,"未托管"));
    assertEquals("关闭",ButtonFeedback.value(c,"camp",null,"托管已停止"));
    assertTrue(CampModeManager.status(c).contains("托管"));
  }
  @Test public void tailgateReportsActualOpeningNotAnInferredLock() {
    Context c=context();
    assertEquals("已关",ControlStatusText.value(c,"trunk",vehicle("已关闭","已上锁"),"未知"));
    assertEquals("已关",ControlStatusText.value(c,"trunk",vehicle("已关闭","未锁"),"未知"));
    assertEquals("已开",ControlStatusText.value(c,"trunk",vehicle("已打开","已上锁"),"未知"));
    assertEquals("未关严",ControlStatusText.value(c,"trunk",vehicle("未关严","已上锁"),"未知"));
    assertEquals("未知",ControlStatusText.value(c,"trunk",vehicle("未知","已上锁"),"未知"));
  }
  @Test public void englishValuesRemainConcise() {
    Context c=context();AppPreferences.set(c,"language","en");
    assertEquals("Horn / flash",ControlStatusText.value(c,"signal",null,"Unknown"));
    assertEquals("Off",ControlStatusText.value(c,"camp",null,"Unknown"));
    assertEquals("Closed",ControlStatusText.value(c,"trunk",vehicle("已关闭","未锁"),"Unknown"));
    assertEquals("Open",ControlStatusText.value(c,"trunk",vehicle("已打开","已上锁"),"Unknown"));
  }
}
