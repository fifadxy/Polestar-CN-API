package io.github.polestarcn.companion;
import java.util.concurrent.Executor;

/**
 * Five vehicle READ-ONLY checkpoints after one remote write: 5/10/15/18/26 seconds.
 * A confirmed result requires three consecutive actual LIVE states matching the command,
 * and no result is finalized before the final checkpoint at 26 s.
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
  private int completed,consecutiveSuccess;
  private boolean ended;

  public CommandReadback(Reader reader,Executor worker,Scheduler scheduler,Listener listener){
    this.reader=reader;this.worker=worker;this.scheduler=scheduler;this.listener=listener;
  }

  public void start(String action) {
    for(long at:CHECKPOINT_MS)scheduler.after(at,()->read(action));
  }

  private void read(String action) {
    worker.execute(()->{
      VehicleSnapshot observed=null;
      try { observed=reader.read(); } catch(Exception unavailable) { /* not a failed write */ }
      final VehicleSnapshot state=observed;
      scheduler.after(0,()->{
        if(ended)return;
        if(state!=null)latest=state;
        if(ControlAction.reached(action,state))consecutiveSuccess++;
        else consecutiveSuccess=0;
        completed++;
        listener.progress(state,completed);
        if(completed>=CHECKPOINT_MS.length) {
          ended=true;
          listener.finished(latest,consecutiveSuccess>=3);
        }
      });
    });
  }
}
