package io.github.polestarcn.oppobridge;

import static org.junit.Assert.*;
import android.os.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class VehicleCardCacheDisplayTest {
  @Test public void cachedMetricsComeFirstAndDoNotInventMissingValues() {
    Bundle s=new Bundle();s.putString("name","Polestar 2");
    assertEquals("Polestar 2 · 暂无缓存",VehicleDeviceCardProvider.cardDisplayName(s));
    s.putBoolean("batteryPercentKnown",true);s.putInt("batteryPercent",64);
    s.putBoolean("rangeKmKnown",true);s.putInt("rangeKm",270);
    assertEquals("64% · 270km 缓存 · Polestar 2",VehicleDeviceCardProvider.cardDisplayName(s));
    s.putInt("batteryPercent",101);s.putInt("rangeKm",-1);
    assertEquals("Polestar 2 · 暂无缓存",VehicleDeviceCardProvider.cardDisplayName(s));
  }
  @Test public void zeroBatteryIsRealAndMissingRangeIsNotZero() {
    Bundle s=new Bundle();s.putString("name","Polestar 2");s.putBoolean("batteryPercentKnown",true);
    assertEquals("0% 缓存 · Polestar 2",VehicleDeviceCardProvider.cardDisplayName(s));
  }
  @Test public void summaryUsesCacheSourceTimeAndUnavailableIsExplicit() {
    Bundle s=new Bundle();assertTrue(VehicleCardDetailActivity.formatSnapshot(s).contains("车辆不可用"));
    s.putBoolean("available",true);s.putString("lock","已上锁");s.putString("climate","关闭");
    String text=VehicleCardDetailActivity.formatSnapshot(s);
    assertTrue(text.contains("时间未知"));assertTrue(text.contains("已上锁"));assertTrue(text.contains("关闭"));
    assertFalse(text.contains("0%"));assertFalse(text.contains("0 km"));
    s.putLong("capturedAt",123456789L);
    assertFalse(VehicleCardDetailActivity.formatSnapshot(s).contains("时间未知"));
  }
}
