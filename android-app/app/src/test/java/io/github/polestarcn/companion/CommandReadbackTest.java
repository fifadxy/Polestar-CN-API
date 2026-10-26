package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import java.util.PriorityQueue;
import org.junit.Test;

public final class CommandReadbackTest {
  private static final class Clock implements CommandReadback.Scheduler {
    long now; int sequence;
    final PriorityQueue<Task> tasks = new PriorityQueue<>((a, b) -> a.at == b.at ? a.order - b.order : Long.compare(a.at, b.at));
    static final class Task { long at; int order; Runnable run; }
    public void after(long delay, Runnable run) {
      Task task = new Task(); task.at = now + delay; task.run = run; task.order = sequence++; tasks.add(task);
    }
    void advance(long time) {
      while (!tasks.isEmpty() && tasks.peek().at <= time) {
        Task next = tasks.remove(); now = next.at; next.run.run();
      }
      now = time;
    }
  }
  private static VehicleSnapshot state(String climate, String source) {
    return new VehicleSnapshot("fixture", "masked", 63, 230, 65845, "空闲", "全部关闭", "全部关闭",
        "已关闭", "已上锁", 1, source, false, climate, "关闭");
  }
  @Test public void noReadOrSuccessBeforeFifteenSeconds() {
    Clock clock = new Clock(); int[] reads = {0}, callbacks = {0};
    new CommandReadback(() -> { reads[0]++; return state("开启", "LIVE"); }, Runnable::run, clock,
        (snapshot, confirmed) -> { assertTrue(confirmed); callbacks[0]++; }).start("climate-start");
    clock.advance(14_999); assertEquals(0, reads[0]); assertEquals(0, callbacks[0]);
    clock.advance(15_000); assertEquals(1, reads[0]); assertEquals(1, callbacks[0]);
    clock.advance(60_000); assertEquals(1, reads[0]); assertEquals(1, callbacks[0]);
  }
  @Test public void mismatchedVehicleStateIsUnconfirmedAfterThreeBoundedReads() {
    Clock clock = new Clock(); int[] reads = {0}, callbacks = {0};
    new CommandReadback(() -> { reads[0]++; return state("关闭", "LIVE"); }, Runnable::run, clock,
        (snapshot, confirmed) -> { assertFalse(confirmed); callbacks[0]++; }).start("climate-start");
    clock.advance(19_999); assertEquals(1, reads[0]); assertEquals(0, callbacks[0]);
    clock.advance(20_000); assertEquals(2, reads[0]);
    clock.advance(25_000); assertEquals(3, reads[0]); assertEquals(1, callbacks[0]);
    clock.advance(90_000); assertEquals(3, reads[0]);
  }
  @Test public void readFailureCanRecoverWithoutSendingAnotherCommand() {
    Clock clock = new Clock(); int[] reads = {0}, callbacks = {0};
    new CommandReadback(() -> { if (++reads[0] == 1) throw new Exception("offline"); return state("开启", "LIVE"); },
        Runnable::run, clock, (snapshot, confirmed) -> { assertTrue(confirmed); callbacks[0]++; }).start("climate-start");
    clock.advance(15_000); assertEquals(0, callbacks[0]);
    clock.advance(20_000); assertEquals(2, reads[0]); assertEquals(1, callbacks[0]);
  }
  @Test public void cacheAndProcessingNeverProveSuccessAndUnknownToggleIsBlocked() {
    assertFalse(ControlAction.reached("climate-start", state("开启", "CACHED")));
    assertFalse(ControlAction.reached("climate-start", state("处理中", "LIVE")));
    assertNull(ControlAction.next("climate", state("未知", "LIVE")));
    assertNull(ControlAction.next("climate", state("处理中", "LIVE")));
    assertEquals("climate-start", ControlAction.next("climate", state("关闭", "LIVE")));
    assertEquals("climate-stop", ControlAction.next("climate", state("开启", "LIVE")));
    assertEquals("unlock", ControlAction.next("lock", state("关闭", "LIVE")));
    assertEquals("trunk-open", ControlAction.next("trunk", state("关闭", "LIVE")));
    assertEquals("air-start", ControlAction.next("air", state("关闭", "LIVE")));
  }
}
