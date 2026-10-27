package io.github.polestarcn.companion;
import java.util.concurrent.Executor;
/** Four scheduled read slots, serialized by the shared worker. Never repeats a write. */
public final class CommandReadback {
 public static final long INITIAL_DELAY_MS=5_000L,RETRY_DELAY_MS=5_000L;
 public interface Reader {VehicleSnapshot read() throws Exception;}
 public interface Scheduler {void after(long delay,Runnable task);}
 public interface Listener {void finished(VehicleSnapshot state,boolean confirmed);default void progress(VehicleSnapshot state,int completed) {}}
 private final Reader reader;private final Executor worker;private final Scheduler scheduler;private final Listener listener;
 private VehicleSnapshot latest;private int completed;private boolean latestConfirmed;private boolean finished;
 public CommandReadback(Reader reader,Executor worker,Scheduler scheduler,Listener listener){this.reader=reader;this.worker=worker;this.scheduler=scheduler;this.listener=listener;}
 public void start(String action){long[] times={5_000L,10_000L,15_000L,21_000L};for(long delay:times)scheduler.after(delay,()->read(action));}
 private void read(String action){if(finished)return;worker.execute(()->{VehicleSnapshot state=null;try{state=reader.read();}catch(Exception unavailable){}final VehicleSnapshot observed=state;scheduler.after(0,()->{
  if(finished)return;
  if(observed!=null){latest=observed;latestConfirmed=ControlAction.reached(action,observed);}
  completed++;listener.progress(observed,completed);
  if(latestConfirmed||completed==4){finished=true;listener.finished(latest,latestConfirmed);}
 });});}
}
