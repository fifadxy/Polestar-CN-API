package io.github.polestarcn.companion;

import java.util.concurrent.Executor;

/** Read-only confirmation: first read after 15 s, at most two later reads, never repeats a write. */
public final class CommandReadback {
  public static final long INITIAL_DELAY_MS = 15_000L, RETRY_DELAY_MS = 5_000L;
  public interface Reader { VehicleSnapshot read() throws Exception; }
  public interface Scheduler { void after(long delay, Runnable task); }
  public interface Listener { void finished(VehicleSnapshot state, boolean confirmed); }
  private final Reader reader;
  private final Executor worker;
  private final Scheduler scheduler;
  private final Listener listener;
  private VehicleSnapshot latest;
  public CommandReadback(Reader reader, Executor worker, Scheduler scheduler, Listener listener) {
    this.reader = reader; this.worker = worker; this.scheduler = scheduler; this.listener = listener;
  }
  public void start(String action) { scheduler.after(INITIAL_DELAY_MS, () -> read(action, 0)); }
  private void read(String action, int attempt) {
    worker.execute(() -> {
      VehicleSnapshot state = null;
      try { state = reader.read(); } catch (Exception ignored) { /* Only reads may be retried. */ }
      final VehicleSnapshot observed = state;
      scheduler.after(0, () -> {
        if (observed != null) latest = observed;
        boolean confirmed = ControlAction.reached(action, observed);
        if (confirmed || attempt >= 2) listener.finished(latest, confirmed);
        else scheduler.after(RETRY_DELAY_MS, () -> read(action, attempt + 1));
      });
    });
  }
}
