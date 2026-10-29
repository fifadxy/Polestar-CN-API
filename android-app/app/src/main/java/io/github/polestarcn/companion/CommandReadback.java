package io.github.polestarcn.companion;
import java.util.concurrent.Executor;

/**
 * Five vehicle READ-ONLY checkpoints after one remote write: 5/10/15/18/26 seconds.
 * The first matching LIVE state that passes extra validation ends verification;
 * cancelled or confirmed commands perform no later checkpoint reads.
 */
public final class CommandReadback {
  public static final long INITIAL_DELAY_MS=5_000L, RETRY_DELAY_MS=5_000L;
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
    for(long at:CHECKPOINT_MS)scheduler.after(at,()->read(action));
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
        listener.progress(state,completed);
        if(confirmed||completed>=CHECKPOINT_MS.length) {
          ended=true;
          listener.finished(latest,confirmed);
        }
      });
    });
  }
}
