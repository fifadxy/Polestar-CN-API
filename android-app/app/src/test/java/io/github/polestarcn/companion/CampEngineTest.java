package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class CampEngineTest {
  private static final long NOW = 1_800_000_000_000L;
  private static VehicleSnapshot state(Integer battery, String climate, String air, long timestamp) {
    return new VehicleSnapshot("test", "masked", battery, null, null, "空闲", "全部关闭",
        "全部关闭", "已关闭", "已上锁", timestamp, "LIVE", false, climate, air);
  }
  private static final class Fixture implements CampEngine.Backend, CampEngine.Control {
    CampSession session = new CampSession("one", "TEST", "fixture", NOW + 3_600_000L, 31, 10, true);
    VehicleSnapshot state = state(60, "关闭", "关闭", NOW);
    List<String> writes = new ArrayList<>();
    String current = "one", result = "", reason = "";
    boolean active = true, cancelled, confirmed = true, throwWrite;
    long time = NOW;
    Runnable afterRead = () -> {};
    public VehicleSnapshot read(CampSession s) { afterRead.run(); return state; }
    public void write(CampSession s, String action) throws Exception {
      writes.add(action); if (throwWrite) throw new Exception("synthetic timeout");
    }
    public boolean confirm(CampSession s, String action) { return confirmed; }
    public boolean current(String id) { return current.equals(id); }
    public boolean active(String id) { return current(id) && active; }
    public boolean cancelled() { return cancelled; }
    public long now() { return time; }
    public void deactivate(String id, String why) { active = false; reason = why; }
    public void report(String text) { result = text; }
    void run() { new CampEngine(this, this).check(session, false, NOW); }
  }
  @Test public void maintainsClimateAndOptionalAirOnlyWhenKnownOff() {
    Fixture f = new Fixture(); f.run();
    assertEquals(java.util.Arrays.asList("climate-start", "air-start"), f.writes);
    assertEquals("RUNNING", f.result);
  }
  @Test public void runningFeaturesAreNotRestarted() {
    Fixture f = new Fixture(); f.state = state(60, "开启", "开启", NOW); f.run();
    assertTrue(f.writes.isEmpty()); assertEquals("RUNNING", f.result);
  }
  @Test public void batteryBelowLimitStopsBothBeforeAnyStart() {
    Fixture f = new Fixture(); f.state = state(9, "关闭", "关闭", NOW); f.run();
    assertEquals(java.util.Arrays.asList("climate-stop", "air-stop"), f.writes);
    assertFalse(f.active); assertEquals("LOW_BATTERY:STOPPED", f.result);
  }
  @Test public void batteryExactlyAtLimitIsAllowed() {
    Fixture f = new Fixture(); f.state = state(10, "关闭", "关闭", NOW); f.run();
    assertTrue(f.writes.contains("climate-start"));
  }
  @Test public void unknownBatteryDoesNotStartAnything() {
    Fixture f = new Fixture(); f.state = state(null, "关闭", "关闭", NOW); f.run();
    assertTrue(f.writes.isEmpty()); assertEquals("WAITING_FOR_BATTERY", f.result);
  }
  @Test public void staleBatteryDoesNotStartAnything() {
    Fixture f = new Fixture(); f.state = state(60, "关闭", "关闭", NOW - 11 * 60_000L); f.run();
    assertTrue(f.writes.isEmpty()); assertEquals("WAITING_FOR_BATTERY", f.result);
  }
  @Test public void pendingClimateDoesNotCauseDuplicateStart() {
    Fixture f = new Fixture(); f.state = state(60, "处理中", "关闭", NOW); f.run();
    assertTrue(f.writes.isEmpty()); assertEquals("STATE_UNCONFIRMED", f.result);
  }
  @Test public void timeoutDoesNotReplayWrite() {
    Fixture f = new Fixture(); f.throwWrite = true; f.run();
    assertEquals(java.util.Collections.singletonList("climate-start"), f.writes);
    assertEquals("WAITING_FOR_NETWORK", f.result);
  }
  @Test public void unconfirmedStartDoesNotProceedToAir() {
    Fixture f = new Fixture(); f.confirmed = false; f.run();
    assertEquals(java.util.Collections.singletonList("climate-start"), f.writes);
    assertEquals("COMMAND_UNCONFIRMED", f.result);
  }
  @Test public void cancelledOrReplacedTaskNeverWrites() {
    Fixture f = new Fixture(); f.afterRead = () -> f.cancelled = true; f.run(); assertTrue(f.writes.isEmpty());
    Fixture other = new Fixture(); other.afterRead = () -> other.current = "two"; other.run(); assertTrue(other.writes.isEmpty());
  }
  @Test public void expiryDuringReadStopsInsteadOfStarting() {
    Fixture f = new Fixture(); f.afterRead = () -> f.time = f.session.endsAt; f.run();
    assertEquals(java.util.Arrays.asList("climate-stop", "air-stop"), f.writes);
    assertEquals("EXPIRED:STOPPED", f.result);
  }
  @Test public void expiryAndManualStopOnlyStopManagedFeatures() {
    Fixture f = new Fixture(); f.session = new CampSession("one", "TEST", "fixture", NOW - 1, 31, 10, false); f.run();
    assertEquals(java.util.Collections.singletonList("climate-stop"), f.writes);
    Fixture stop = new Fixture(); new CampEngine(stop, stop).check(stop.session, true, NOW);
    assertEquals("MANUAL:STOPPED", stop.result);
  }
  @Test public void configurationEnforcesRequestedRanges() {
    assertFalse(CampSession.batteryAllows(4, 5)); assertTrue(CampSession.batteryAllows(90, 90));
    for (int interval : new int[]{19, 61}) {
      try { new CampSession("one", "TEST", "fixture", NOW, interval, 10, false); fail(); }
      catch (IllegalArgumentException expected) {}
    }
    for (int minimum : new int[]{4, 91}) {
      try { new CampSession("one", "TEST", "fixture", NOW, 31, minimum, false); fail(); }
      catch (IllegalArgumentException expected) {}
    }
  }
}
