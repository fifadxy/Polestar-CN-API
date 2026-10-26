package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import io.github.polestarcn.companion.api.ProtoWire;
import org.junit.Test;

public class TelemetryMappingTest {
  @Test public void chargingReferenceUnitsAndEnumsMatchScreenshot() {
    byte[] data = ProtoWire.concat(ProtoWire.varintField(6, 1), ProtoWire.varintField(17, 2),
        ProtoWire.varintField(10, 6000), ProtoWire.fixed32Field(11, 29f),
        ProtoWire.varintField(5, 180), ProtoWire.varintField(26, 4));
    ChargingSnapshot s = ChargingSnapshot.fromData(ProtoWire.decode(data));
    assertEquals("已连接", s.connection(false)); assertEquals("Connected", s.connection(true));
    assertEquals("交流", s.type(false)); assertEquals("AC", s.type(true));
    assertEquals(6000.0, s.powerWatts, 0.001); assertEquals(29.0, s.currentAmps, 0.001);
    assertEquals("正在供电", s.powerStatus(false)); assertEquals("3 h", s.timeToFull(true));
    assertNull(ChargingSnapshot.unknown().powerWatts);
    assertEquals("Unknown", new ChargingSnapshot(99, 99, 99, null, null, null).type(true));
  }
  @Test public void climateAirHaveIndependentFieldsAndAllowNegativeTemperature() {
    byte[] climate = ProtoWire.concat(ProtoWire.varintField(2, 1), ProtoWire.fixed32Field(7, -4.5f),
        ProtoWire.fixed32Field(8, 22f), ProtoWire.varintField(3, 25));
    byte[] air = ProtoWire.concat(ProtoWire.varintField(6, 2), ProtoWire.varintField(9, 4),
        ProtoWire.fixed32Field(10, 3f), ProtoWire.varintField(11, 8));
    EnvironmentSnapshot s = EnvironmentSnapshot.fromData(ProtoWire.decode(climate), ProtoWire.decode(air));
    assertEquals("开启", s.climate); assertEquals("关闭", s.air);
    assertEquals(-4.5, s.temperatureC, 0.001); assertEquals(22.0, s.targetTemperatureC, 0.001);
    assertEquals(Integer.valueOf(4), s.aqi); assertEquals(3.0, s.pm25UgM3, 0.001);
    assertNull(EnvironmentSnapshot.unknown().temperatureC); assertNull(EnvironmentSnapshot.unknown().aqi);
  }
  @Test public void fourDoorAndWindowPositionsRemainDistinct() {
    byte[] data = ProtoWire.concat(ProtoWire.varintField(3, 1), ProtoWire.varintField(4, 2),
        ProtoWire.varintField(5, 3), ProtoWire.varintField(6, 0), ProtoWire.varintField(7, 2),
        ProtoWire.varintField(8, 1), ProtoWire.varintField(9, 2), ProtoWire.varintField(10, 2));
    ExteriorSnapshot s = ExteriorSnapshot.fromData(ProtoWire.decode(data));
    assertArrayEquals(new int[]{1,2,3,0}, s.states(false)); assertArrayEquals(new int[]{2,1,2,2}, s.states(true));
    assertTrue(s.summary(false).contains("2 个开启")); assertTrue(s.summary(false).contains("未知"));
    int[] copy = s.states(false); copy[0] = 2; assertEquals(1, s.state(false, 0));
    assertNotEquals("全部关闭", ExteriorSnapshot.unknown().summary(false));
  }
  @Test public void tripSpeedsConsumptionAndTimestampDoNotUseZeroForMissing() {
    byte[] odo = ProtoWire.concat(ProtoWire.varintField(2, 65_845_000),
        ProtoWire.fixed32Field(3, 100.5f), ProtoWire.fixed32Field(4, 25.25f),
        ProtoWire.varintField(5, 45), ProtoWire.varintField(6, 35));
    byte[] battery = ProtoWire.concat(ProtoWire.fixed32Field(3, 18.5f), ProtoWire.fixed32Field(12, 21.5f),
        ProtoWire.bytesField(1, ProtoWire.concat(ProtoWire.varintField(1, 1_800_000_000L), ProtoWire.varintField(2, 500_000_000))));
    MileageSnapshot s = MileageSnapshot.fromReply(ProtoWire.decode(ProtoWire.bytesField(3, odo)))
        .withConsumption(ProtoWire.decode(battery));
    assertEquals(45.0, s.manualAverageSpeed, 0.001); assertEquals(35.0, s.automaticAverageSpeed, 0.001);
    assertEquals(18.5, s.manualConsumption, 0.001); assertEquals(21.5, s.automaticConsumption, 0.001);
    assertEquals(1_800_000_000_500L, s.consumptionReportedAt);
    assertNull(MileageSnapshot.unknown().automaticConsumption);
  }
}
