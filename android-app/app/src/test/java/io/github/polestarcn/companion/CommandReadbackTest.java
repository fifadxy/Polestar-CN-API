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
  @Test public void confirmationStopsAfterThreeMatchesAndSkipsRemainingReads(){
    Clock clock=new Clock();int[] reads={0},done={0};
    new CommandReadback(()->{reads[0]++;return state("开启","LIVE");},Runnable::run,clock,(v,ok)->{assertTrue(ok);done[0]++;}).start("climate-start");
    clock.advance(4999);assertEquals(0,reads[0]);
    long[] slots=CommandReadback.CHECKPOINT_MS;assertArrayEquals(new long[]{5000,10000,15000,18000,26000},slots);
    for(int n=1;n<=5;n++){clock.advance(slots[n-1]);assertEquals(Math.min(n,3),reads[0]);assertEquals(n>=3?1:0,done[0]);}
    clock.advance(90000);assertEquals(3,reads[0]);assertEquals(1,done[0]);
  }
  @Test public void targetMismatchDoesNotStopEarlyOnGenericClimateState(){Clock clock=new Clock();int[] count={0};boolean[] ok={true};new CommandReadback(()->{count[0]++;return state("开启","LIVE");},Runnable::run,clock,(v,verified)->ok[0]=verified).require(v->false).start("climate-start");clock.advance(90000);assertEquals(5,count[0]);assertFalse(ok[0]);}
  @Test public void mismatchAfterFiveReadsStaysUnconfirmed(){
    Clock clock=new Clock();int[] reads={0},done={0};new CommandReadback(()->{reads[0]++;return state("关闭","LIVE");},Runnable::run,clock,(v,ok)->{assertFalse(ok);done[0]++;}).start("climate-start");clock.advance(26000);assertEquals(5,reads[0]);assertEquals(1,done[0]);
  }
  @Test public void readFailuresRecoverWithoutRepeatingAWrite(){
    Clock clock=new Clock();int[] reads={0},done={0},progress={0};new CommandReadback(()->{if(++reads[0]<3)throw new Exception("offline");return state("开启","LIVE");},Runnable::run,clock,new CommandReadback.Listener(){public void progress(VehicleSnapshot v,int n){progress[0]=n;}public void finished(VehicleSnapshot v,boolean ok){assertTrue(ok);done[0]++;}}).start("climate-start");clock.advance(26000);assertEquals(5,reads[0]);assertEquals(5,progress[0]);assertEquals(1,done[0]);
  }
  @Test public void twoPositiveReadsCannotBeMistakenForThreeConfirmations(){
    Clock clock=new Clock();int[] call={0};boolean[] verified={true};int[] done={0};
    new CommandReadback(()->{call[0]++;return state(call[0]<=3?"关闭":"开启","LIVE");},
      Runnable::run,clock,(v,ok)->{verified[0]=ok;done[0]++;}).start("climate-start");
    clock.advance(25000);assertEquals(0,done[0]);
    clock.advance(26000);assertEquals(5,call[0]);assertEquals(1,done[0]);assertFalse(verified[0]);
  }
  @Test public void inconsistentReadbackMustNotDeclareCommandSuccess(){
    Clock clock=new Clock();int[] call={0};boolean[] verified={true};
    new CommandReadback(()->{
      int n=++call[0];return state(n==2||n==4?"关闭":"开启","LIVE");},
      Runnable::run,clock,(v,ok)->verified[0]=ok).start("climate-start");
    clock.advance(26000);assertFalse(verified[0]);assertEquals(5,call[0]);
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
