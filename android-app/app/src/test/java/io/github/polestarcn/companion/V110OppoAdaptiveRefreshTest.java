package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import android.os.Bundle;
import io.github.polestarcn.oppobridge.OppoCardSummary;
import java.lang.reflect.Method;
import java.util.EnumSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class V110OppoAdaptiveRefreshTest {
  private Context context(){
    Context c=RuntimeEnvironment.getApplication();
    c.getSharedPreferences("preferences",0).edit().clear().commit();
    c.getSharedPreferences("oppo-device-integration",0).edit().putBoolean("enabled",true).commit();
    AppPreferences.set(c,"language","zh");
    return c;
  }
  private VehicleSnapshot vehicle(String climate,String air,long now){
    VehicleSnapshot s=new VehicleSnapshot("Polestar","masked",88,370,20000,"空闲",
        "全部关闭","全部关闭","已关闭","已上锁",now,"LIVE",false,climate,air);
    return s.withEnvironment(new EnvironmentSnapshot(climate,air,null,null,12.5,42,
        "开启".equals(climate)?30:null,"开启".equals(air)?4:null,now,now));
  }
  @Test public void bridgeThrottleIsAdjustableAndNeverBelowThirtySeconds(){
    BridgeRefreshThrottle gate=new BridgeRefreshThrottle();
    assertTrue(gate.admit(1000,30_000));
    assertEquals(30_000L,gate.retryAfter(1000,30_000));
    assertFalse(gate.admit(20_000,30_000));
    assertTrue(gate.admit(31_000,30_000));
    assertFalse(gate.admit(50_000,60_000));
  }
  @Test public void lockBoostIsPerVehicleAndTimeBounded(){
    Context c=context();long now=1_000_000L;
    assertTrue(OppoAdaptiveRefresh.enabled(c));
    assertEquals(600_000L,OppoAdaptiveRefresh.ttl(c,RefreshDomain.CORE,true,
        vehicle("关闭","关闭",now),"CAR-A",600_000L,now));
    OppoAdaptiveRefresh.confirmedAction(c,"CAR-A","lock",now);
    assertTrue(OppoAdaptiveRefresh.lockBoostActive(c,"CAR-A",now+30_000L));
    assertFalse(OppoAdaptiveRefresh.lockBoostActive(c,"CAR-B",now));
    assertEquals(30_000L,OppoAdaptiveRefresh.ttl(c,RefreshDomain.CORE,true,
        vehicle("关闭","关闭",now),"CAR-A",600_000L,now+30_000L));
    assertEquals(60_000L,OppoAdaptiveRefresh.ttl(c,RefreshDomain.CORE,false,
        vehicle("关闭","关闭",now),"CAR-A",3_600_000L,now+30_000L));
    assertFalse(OppoAdaptiveRefresh.lockBoostActive(c,"CAR-A",now+3*60_000L+1000));
    AppPreferences.set(c,"oppo_dynamic_refresh","off");
    assertEquals(600_000L,OppoAdaptiveRefresh.ttl(c,RefreshDomain.CORE,true,
        vehicle("关闭","关闭",now),"CAR-A",600_000L,now+10_000L));
  }
  @Test public void activeClimateAndAirUseTwoMinuteForegroundFifteenMinuteBackground(){
    Context c=context();long now=System.currentTimeMillis();
    VehicleSnapshot s=vehicle("开启","开启",now);
    assertEquals(120_000L,OppoAdaptiveRefresh.ttl(c,RefreshDomain.CORE,true,s,"CAR",600_000L,now));
    assertEquals(120_000L,OppoAdaptiveRefresh.ttl(c,RefreshDomain.AIR,true,s,"CAR",0L,now));
    assertEquals(900_000L,OppoAdaptiveRefresh.ttl(c,RefreshDomain.CLIMATE,false,s,"CAR",3_600_000L,now));
    assertEquals(15,OppoAdaptiveRefresh.backgroundMinutes(c,s,60));
  }
  @Test public void synchronizedTtlCoversOnlyActualRpcFamilies() throws Exception{
    Context c=context();String vin="TEST-NO-RAW-VIN";
    VehicleRefreshManager manager=VehicleRefreshManager.get(c);
    Method mark=VehicleRefreshManager.class.getDeclaredMethod("onScopesRead",String.class,EnumSet.class);
    mark.setAccessible(true);
    Method read=VehicleRefreshManager.class.getDeclaredMethod("readAt",String.class,RefreshDomain.class);
    read.setAccessible(true);
    mark.invoke(manager,vin,EnumSet.of(RefreshDomain.CHARGING,RefreshDomain.CLIMATE,RefreshDomain.AIR));
    assertTrue((long)read.invoke(manager,vin,RefreshDomain.CHARGING)>0);
    assertTrue((long)read.invoke(manager,vin,RefreshDomain.CLIMATE)>0);
    assertTrue((long)read.invoke(manager,vin,RefreshDomain.AIR)>0);
    assertEquals(0L,(long)read.invoke(manager,vin,RefreshDomain.HEALTH));
    assertEquals(0L,(long)read.invoke(manager,vin,RefreshDomain.CORE));
    assertFalse(c.getSharedPreferences("psa-refresh-beta43",0).getAll().toString().contains(vin));
  }
  @Test public void activeTimerReplacesRangeOnlyWhileDefaultTemplateEnabled(){
    Context c=context();long now=System.currentTimeMillis();
    Bundle b=new Bundle();b.putBoolean("batteryPercentKnown",true);b.putInt("batteryPercent",88);
    b.putBoolean("rangeKmKnown",true);b.putInt("rangeKm",370);
    OppoRunningStatus.write(b,c,vehicle("开启","关闭",now));
    assertEquals("空调30m",b.getString("oppoDynamicLabel"));
    assertEquals("88% · 空调30m",OppoCardSummary.format(b));
    b.putBoolean("oppoDynamicStatusEnabled",false);
    assertEquals("88% · 370KM",OppoCardSummary.format(b));
    b.putBoolean("oppoDynamicStatusEnabled",true);
    assertEquals("88% · 370KM",OppoCardSummary.format(b,"{soc}{sep}{range} "," · ").trim());
    assertEquals("空调30m / 370KM",OppoCardSummary.format(b,"{dynamic}{sep}{range}"," / "));
    b.putString("lock","已上锁");assertEquals("关锁",OppoCardSummary.format(b,"{lock}"," · "));
    b.putString("lock","未锁");assertEquals("开锁",OppoCardSummary.format(b,"{lock}"," · "));
  }
}
