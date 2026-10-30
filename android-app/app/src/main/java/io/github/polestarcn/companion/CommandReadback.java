package io.github.polestarcn.companion;
import java.util.concurrent.Executor;

/**
 * Read-only checkpoints after a write; lock actions use 3/5/7/15 seconds.
 * Explicitly configured locks may resend once after the failed third check.
 * The first matching LIVE state that passes extra validation ends verification;
 * cancelled or confirmed commands perform no later checkpoint reads.
 */
public final class CommandReadback {
  public static final long INITIAL_DELAY_MS=5_000L, RETRY_DELAY_MS=5_000L;
  public static final long[] LOCK_CHECKPOINT_MS={3_000L,5_000L,7_000L,15_000L};
  public interface Retry { void send() throws Exception; }
  private Retry retry;private Executor retryExecutor;private java.util.function.BooleanSupplier retryAllowed=()->true;private boolean retryDone;private long[] checkpoints=CHECKPOINT_MS;
  public CommandReadback retryLockOnce(Retry r,Executor e,java.util.function.BooleanSupplier guard){retry=r;retryExecutor=e;retryAllowed=guard;return this;}
  public static final long[] CHECKPOINT_MS={5_000L,10_000L,15_000L,18_000L,26_000L};
  public interface Reader { VehicleSnapshot read() throws Exception; }
  public interface Scheduler { void after(long delay,Runnable task); }
  public interface Listener {
    void finished(VehicleSnapshot state,boolean confirmed);
    default void progress(VehicleSnapshot state,int completed) {}
  }
  private final Reader reader;
  private final Executor worker;
  private final Scheduler scheduler;
  private final Listener listener;
  private VehicleSnapshot latest;
  private int completed;
  private volatile boolean ended;private java.util.function.Predicate<VehicleSnapshot> extraCheck=state->true;
  public CommandReadback require(java.util.function.Predicate<VehicleSnapshot> check){extraCheck=check;return this;}

  public CommandReadback(Reader reader,Executor worker,Scheduler scheduler,Listener listener){
    this.reader=reader;this.worker=worker;this.scheduler=scheduler;this.listener=listener;
  }

  public void cancel(){ended=true;}
  public void start(String action) {
    checkpoints=("lock".equals(action)||"unlock".equals(action))?LOCK_CHECKPOINT_MS:CHECKPOINT_MS;
    for(long at:checkpoints)scheduler.after(at,()->read(action));
  }

  private void read(String action) {
    if(ended)return;
    worker.execute(()->{
      if(ended)return;
      VehicleSnapshot observed=null;
      try { observed=reader.read(); } catch(Exception unavailable) { /* not a failed write */ }
      final VehicleSnapshot state=observed;
      scheduler.after(0,()->{
        if(ended)return;
        if(state!=null)latest=state;
        boolean confirmed=ControlAction.reached(action,state)&&extraCheck.test(state);
        completed++;
        boolean done=confirmed||completed>=checkpoints.length;if(done)ended=true;
        listener.progress(state,completed);
        if(done)listener.finished(latest,confirmed);
        else if(checkpoints==LOCK_CHECKPOINT_MS&&completed==3&&retry!=null&&!retryDone){retryDone=true;retryExecutor.execute(()->{if(ended||!retryAllowed.getAsBoolean())return;try{retry.send();}catch(Exception unavailable){/* Retain unconfirmed result; never retry a second time. */}});}

      });
    });
  }
}
