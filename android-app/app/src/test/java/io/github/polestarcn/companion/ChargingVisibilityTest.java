package io.github.polestarcn.companion;

import static org.junit.Assert.*;

import java.util.EnumSet;
import org.junit.Test;

public final class ChargingVisibilityTest {
  private static final long NOW = 1_760_000_000_000L;

  private VehicleSnapshot snapshot(String state, long at, String source, boolean demo,
      Integer connection, Integer powerStatus) {
    return new VehicleSnapshot("fixture", "masked", 63, 230, 65845, state,
        "全部关闭", "全部关闭", "已关闭", "已上锁", at, source, demo)
        .withCharging(new ChargingSnapshot(connection, 2, powerStatus, 180, 9000.0, 40.0));
  }

  private VehicleSnapshot snapshot(String state, Integer connection, Integer powerStatus) {
    return snapshot(state, NOW, "LIVE", false, connection, powerStatus);
  }

  @Test public void missingReportNeverCreatesChargingSection() {
    assertFalse(ChargingVisibility.shouldShow(null, NOW));
    assertFalse(ChargingVisibility.shouldShow(snapshot("充电中", 0, "LIVE", false, 1, 4), NOW));
    assertFalse(ChargingVisibility.shouldShow(snapshot("充电中", NOW, null, false, 1, 4), NOW));
  }

  @Test public void freshPrimaryChargingStateIsVisibleWithoutOptionalReadings() {
    for (String state : new String[]{"充电中", "智能充电", "Charging", "Smart charging"}) {
      assertTrue(state, ChargingVisibility.shouldShow(snapshot(state, null, null), NOW));
      assertTrue(state, ChargingVisibility.shouldShow(
          snapshot(state, NOW, "CACHED", false, null, null), NOW));
    }
  }

  @Test public void explicitChargingStartStateIsVisible() {
    for (String state : new String[]{"开始充电", "正在开始充电", "充电启动中", "正在启动充电",
        "充电初始化中", "Starting charging", "Initializing charging"}) {
      assertTrue(state, ChargingVisibility.shouldShow(snapshot(state, null, null), NOW));
    }
  }

  @Test public void connectedInitializingSupplyRepresentsStartHandshake() {
    for (String state : new String[]{"空闲", "未指定", "未知", "Idle", "Unknown", null}) {
      assertTrue(String.valueOf(state),
          ChargingVisibility.shouldShow(snapshot(state, 1, 2), NOW));
    }
  }

  @Test public void cableAndRetainedPositivePowerDoNotEstablishCharging() {
    for (String state : new String[]{"空闲", "未指定", "未知", "Idle", "Unknown", null}) {
      // The fixture deliberately retains 9 kW, 40 A and an ETA, even when it is not charging.
      assertFalse(String.valueOf(state),
          ChargingVisibility.shouldShow(snapshot(state, 1, 4), NOW));
      assertFalse(String.valueOf(state),
          ChargingVisibility.shouldShow(snapshot(state, 1, 3), NOW));
    }
  }

  @Test public void explicitInactiveStatesOverrideAnInitializingSupply() {
    for (String state : new String[]{"计划中", "已完成", "智能充电暂停", "错误", "放电中", "未充电",
        "Scheduled", "Complete", "Paused", "Fault", "Discharging", "Not charging"}) {
      assertFalse(state, ChargingVisibility.shouldShow(snapshot(state, 1, 2), NOW));
      assertFalse(state, ChargingVisibility.shouldShow(snapshot(state, 1, 4), NOW));
    }
  }

  @Test public void disconnectedAndFaultySupplyCannotBeAStartHandshake() {
    for (Integer connection : new Integer[]{null, 0, 2, 3}) {
      assertFalse(ChargingVisibility.shouldShow(snapshot("空闲", connection, 2), NOW));
    }
    for (Integer supply : new Integer[]{null, 0, 1, 3, 4, 5}) {
      assertFalse(ChargingVisibility.shouldShow(snapshot("空闲", 1, supply), NOW));
    }
  }

  @Test public void staleAndFutureReportsNeverAppearAsCurrentCharging() {
    long boundary = NOW - ChargingVisibility.FRESHNESS_WINDOW_MILLIS;
    assertTrue(ChargingVisibility.shouldShow(
        snapshot("充电中", boundary, "CACHED", false, 1, 4), NOW));
    assertFalse(ChargingVisibility.shouldShow(
        snapshot("充电中", boundary - 1, "CACHED", false, 1, 4), NOW));
    assertFalse(ChargingVisibility.shouldShow(
        snapshot("开始充电", NOW + 1, "LIVE", false, 1, 2), NOW));
    assertFalse(ChargingVisibility.shouldShow(
        snapshot("空闲", boundary - 1, "CACHED", false, 1, 2), NOW));
  }

  @Test public void demoAndUnrecognizedStatesRemainHidden() {
    assertFalse(ChargingVisibility.shouldShow(
        snapshot("充电中", NOW, "LIVE", true, 1, 4), NOW));
    assertFalse(ChargingVisibility.shouldShow(
        snapshot("充电中", NOW, "UNVERIFIED", false, 1, 4), NOW));
    assertFalse(ChargingVisibility.shouldShow(snapshot("unrecognized", 1, 2), NOW));
  }

  @Test public void returningToIdleHidesTheSectionDespiteRetainedMeasurements() {
    assertTrue(ChargingVisibility.shouldShow(snapshot("充电中", 1, 4), NOW));
    assertFalse(ChargingVisibility.shouldShow(snapshot("空闲", 1, 4), NOW));
    assertTrue(ChargingVisibility.shouldShow(snapshot("充电中", 1, 4), NOW));
  }

  @Test public void classificationContainsExactlyChargingMetrics() {
    EnumSet<DisplayMetric> charging = EnumSet.of(DisplayMetric.CHARGE_CONNECTION,
        DisplayMetric.CHARGE_TYPE, DisplayMetric.CHARGE_POWER, DisplayMetric.CHARGE_CURRENT,
        DisplayMetric.CHARGE_ETA, DisplayMetric.POWER_STATUS);
    for (DisplayMetric metric : DisplayMetric.values()) {
      assertEquals(metric.name(), charging.contains(metric), ChargingVisibility.isChargingMetric(metric));
    }
    assertFalse(ChargingVisibility.isChargingMetric(null));
  }
}
