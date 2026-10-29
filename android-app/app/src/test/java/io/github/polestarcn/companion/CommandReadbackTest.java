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
  @Test public void confirmationStopsAtFirstValidatedLiveMatchAndSkipsRemainingReads(){
    Clock clock=new Clock();int[] reads={0},done={0};
    new CommandReadback(()->{reads[0]++;return state("开启","LIVE");},Runnable::run,clock,(v,ok)->{assertTrue(ok);done[0]++;}).start("climate-start");
    clock.advance(4999);assertEquals(0,reads[0]);
    long[] slots=CommandReadback.CHECKPOINT_MS;assertArrayEquals(new long[]{5000,10000,15000,18000,26000},slots);
    for(int n=1;n<=5;n++){clock.advance(slots[n-1]);assertEquals(1,reads[0]);assertEquals(1,done[0]);}
    clock.advance(90000);assertEquals(1,reads[0]);assertEquals(1,done[0]);
  }
  @Test public void targetMismatchDoesNotStopEarlyOnGenericClimateState(){Clock clock=new Clock();int[] count={0};boolean[] ok={true};new CommandReadback(()->{count[0]++;return state("开启","LIVE");},Runnable::run,clock,(v,verified)->ok[0]=verified).require(v->false).start("climate-start");clock.advance(90000);assertEquals(5,count[0]);assertFalse(ok[0]);}
  @Test public void mismatchAfterFiveReadsStaysUnconfirmed(){
    Clock clock=new Clock();int[] reads={0},done={0};new CommandReadback(()->{reads[0]++;return state("关闭","LIVE");},Runnable::run,clock,(v,ok)->{assertFalse(ok);done[0]++;}).start("climate-start");clock.advance(26000);assertEquals(5,reads[0]);assertEquals(1,done[0]);
  }
  @Test public void readFailuresRecoverWithoutRepeatingAWrite(){
    Clock clock=new Clock();int[] reads={0},done={0},progress={0};new CommandReadback(()->{if(++reads[0]<3)throw new Exception("offline");return state("开启","LIVE");},Runnable::run,clock,new CommandReadback.Listener(){public void progress(VehicleSnapshot v,int n){progress[0]=n;}public void finished(VehicleSnapshot v,boolean ok){assertTrue(ok);done[0]++;}}).start("climate-start");clock.advance(26000);assertEquals(3,reads[0]);assertEquals(3,progress[0]);assertEquals(1,done[0]);
  }
  @Test public void laterMatchingStateEndsVerificationImmediately(){
    Clock clock=new Clock();int[] reads={0},done={0};
    new CommandReadback(()->state(++reads[0]<4?"关闭":"开启","LIVE"),Runnable::run,clock,(v,ok)->{assertTrue(ok);done[0]++;}).start("climate-start");
    clock.advance(15000);assertEquals(0,done[0]);clock.advance(18000);assertEquals(1,done[0]);clock.advance(90000);assertEquals(4,reads[0]);
  }
  @Test public void cacheProcessingAndExtraValidationMustPassBeforeStopping(){
    Clock clock=new Clock();int[] reads={0},done={0};
    new CommandReadback(()->{int n=++reads[0];return state(n==2?"处理中":"开启",n==1?"CACHED":"LIVE");},Runnable::run,clock,(v,ok)->{assertTrue(ok);done[0]++;}).require(v->reads[0]>=4).start("climate-start");
    clock.advance(15000);assertEquals(0,done[0]);clock.advance(90000);assertEquals(4,reads[0]);assertEquals(1,done[0]);
  }
  @Test public void cancelledScheduledAndAlreadyQueuedReadsDoNoIo(){
    Clock clock=new Clock();java.util.List<Runnable> jobs=new java.util.ArrayList<>();int[] reads={0};
    CommandReadback readback=new CommandReadback(()->{reads[0]++;return state("开启","LIVE");},jobs::add,clock,(v,ok)->fail("Cancelled readback must not finish"));readback.start("climate-start");clock.advance(5000);assertEquals(1,jobs.size());readback.cancel();jobs.get(0).run();clock.advance(90000);assertEquals(0,reads[0]);assertEquals(1,jobs.size());
  }
  @Test public void campingStopsAfterFirstMatchingReadAndHonorsCancellation()throws Exception{
    long[] waited={0};int[] reads={0};
    assertTrue(CampReadback.verify(()->++reads[0]>=2,()->false,ms->waited[0]+=ms));assertEquals(2,reads[0]);assertEquals(10000,waited[0]);
    assertFalse(CampReadback.verify(()->{fail("Cancelled camping must not read");return false;},()->true,ms->fail("No wait after cancellation")));
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
