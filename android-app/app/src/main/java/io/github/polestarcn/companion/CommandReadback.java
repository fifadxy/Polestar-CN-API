package io.github.polestarcn.companion;
import java.util.concurrent.Executor;
/** Four scheduled read slots, serialized by the shared worker. Never repeats a write. */
public final class CommandReadback {
 public static final long INITIAL_DELAY_MS=5_000L,RETRY_DELAY_MS=5_000L;
 public interface Reader {VehicleSnapshot read() throws Exception;}
 public interface Scheduler {void after(long delay,Runnable task);}
 public interface Listener {void finished(VehicleSnapshot state,boolean confirmed);default void progress(VehicleSnapshot state,int completed) {}}
 private final Reader reader;private final Executor worker;private final Scheduler scheduler;private final Listener listener;
 private VehicleSnapshot latest;private int completed;private boolean latestConfirmed;
 public CommandReadback(Reader reader,Executor worker,Scheduler scheduler,Listener listener){this.reader=reader;this.worker=worker;this.scheduler=scheduler;this.listener=listener;}
 public void start(String action){for(int i=1;i<=4;i++)scheduler.after(i*5_000L,()->read(action));}
 private void read(String action){worker.execute(()->{VehicleSnapshot state=null;try{state=reader.read();}catch(Exception unavailable){}final VehicleSnapshot observed=state;scheduler.after(0,()->{
  if(observed!=null){latest=observed;latestConfirmed=ControlAction.reached(action,observed);}
  completed++;listener.progress(observed,completed);
  if(completed==4)listener.finished(latest,latestConfirmed);
 });});}
}
