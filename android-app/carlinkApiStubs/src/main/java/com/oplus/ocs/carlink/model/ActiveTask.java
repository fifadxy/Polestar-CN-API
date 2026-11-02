package com.oplus.ocs.carlink.model;
/** Public API contract only; this class does not implement OPPO service behavior. */
public final class ActiveTask {
  private final long timeoutSeconds;
  public ActiveTask(long timeoutSeconds){this.timeoutSeconds=timeoutSeconds;}
  public long getTimeoutSeconds(){return timeoutSeconds;}
}
