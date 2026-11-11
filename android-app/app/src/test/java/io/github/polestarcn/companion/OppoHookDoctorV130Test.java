package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class OppoHookDoctorV130Test {
  @Test public void restartTargetsOnlyFixedAllowlistAndNeverSystemUiOrOurApp(){
    String script=OppoHookProcessManager.command();
    assertTrue(script.contains("pidof com.heytap.mydevices"));
    assertTrue(script.contains("pidof com.coloros.assistantscreen"));
    assertTrue(script.contains("kill -TERM"));
    assertFalse(script.contains("com.android.systemui"));
    assertFalse(script.contains("io.github.polestarcn.companion"));
    assertFalse(script.contains("reboot"));
    assertTrue(OppoHookDoctor.allowed("com.heytap.opluscarlink"));
    assertFalse(OppoHookDoctor.allowed("com.android.systemui"));
    assertFalse(OppoHookDoctor.allowed("$(anything)"));
  }
  @Test public void exportRedactsTokensVinAndPhone(){
    String report=OppoHookDoctor.sanitized("access_token=secret123 VIN=YV1LZ123456789012 phone=13800138000");
    assertFalse(report.contains("secret123"));
    assertFalse(report.contains("YV1LZ123456789012"));
    assertFalse(report.contains("13800138000"));
  }
  @Test public void autoFluidDefaultsIncludeChargingAndRemainOptOut(){
    Context c=RuntimeEnvironment.getApplication();
    c.getSharedPreferences("preferences",0).edit().clear().commit();
    assertTrue(OppoFluidPolicy.enabled(c));
    for(String scene:new String[]{"climate","air","camp","charging"}){
      assertTrue("auto notify "+scene,OppoFluidPolicy.notify(c,scene));
      assertTrue("auto cloud "+scene,OppoFluidPolicy.cloud(c,scene));
    }
    AppPreferences.set(c,OppoFluidPolicy.MASTER,"off");
    assertFalse(OppoFluidPolicy.enabled(c));
    assertFalse(OppoFluidPolicy.cloud(c,"charging"));
  }
  @Test public void coolingHeatingAndChargingGetEfficientActiveRefresh(){
    Context c=RuntimeEnvironment.getApplication();
    c.getSharedPreferences("preferences",0).edit().clear().commit();
    assertTrue(ActiveVehicleRuntime.climate("制冷"));
    assertTrue(ActiveVehicleRuntime.climate("制热"));
    assertFalse(ActiveVehicleRuntime.climate("关闭"));
    assertEquals(2,RefreshPolicy.activeMinutes(c,RefreshDomain.CORE,true,
        new VehicleSnapshot("p","masked",65,300,1234,"空闲","未知","未知","关闭","已锁",
          System.currentTimeMillis(),"LIVE",false,"制热","关闭")));
    VehicleSnapshot charging=new VehicleSnapshot("p","masked",65,300,1234,"充电中",
        "未知","未知","关闭","已锁",System.currentTimeMillis(),"LIVE",false,"关闭","关闭");
    assertEquals(5,RefreshPolicy.activeMinutes(c,RefreshDomain.CORE,true,charging));
    assertEquals(30,RefreshPolicy.activeMinutes(c,RefreshDomain.CORE,false,charging));
    assertFalse(ActiveVehicleRuntime.working(c,new VehicleSnapshot("p","masked",65,300,1234,
        "空闲","未知","未知","关闭","已锁",System.currentTimeMillis(),"LIVE",false,"关闭","关闭")));
  }
}
