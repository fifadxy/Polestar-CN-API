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
  @Test public void fourExactSlotsAndNoFinalSuccessBeforeTwentySeconds(){
    Clock clock=new Clock();int[] reads={0},done={0};
    new CommandReadback(()->{reads[0]++;return state("开启","LIVE");},Runnable::run,clock,(v,ok)->{assertTrue(ok);done[0]++;}).start("climate-start");
    clock.advance(4999);assertEquals(0,reads[0]);
    for(int n=1;n<=4;n++){clock.advance(n*5000);assertEquals(n,reads[0]);assertEquals(n==4?1:0,done[0]);}
    clock.advance(90000);assertEquals(4,reads[0]);assertEquals(1,done[0]);
  }
  @Test public void mismatchAfterFourReadsStaysUnconfirmed(){
    Clock clock=new Clock();int[] reads={0},done={0};new CommandReadback(()->{reads[0]++;return state("关闭","LIVE");},Runnable::run,clock,(v,ok)->{assertFalse(ok);done[0]++;}).start("climate-start");clock.advance(20000);assertEquals(4,reads[0]);assertEquals(1,done[0]);
  }
  @Test public void readFailuresRecoverWithoutRepeatingAWrite(){
    Clock clock=new Clock();int[] reads={0},done={0},progress={0};new CommandReadback(()->{if(++reads[0]<3)throw new Exception("offline");return state("开启","LIVE");},Runnable::run,clock,new CommandReadback.Listener(){public void progress(VehicleSnapshot v,int n){progress[0]=n;}public void finished(VehicleSnapshot v,boolean ok){assertTrue(ok);done[0]++;}}).start("climate-start");clock.advance(20000);assertEquals(4,reads[0]);assertEquals(4,progress[0]);assertEquals(1,done[0]);
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
