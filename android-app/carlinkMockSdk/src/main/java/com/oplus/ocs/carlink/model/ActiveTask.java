package com.oplus.ocs.carlink.model;
public final class ActiveTask {
  private final long timeoutSeconds;
  public ActiveTask(long timeoutSeconds){this.timeoutSeconds=timeoutSeconds;}
  public long getTimeoutSeconds(){return timeoutSeconds;}
}
