package com.oplus.ocs.carlink.model;
public final class ControlTask {
 private final String id;private final long timeout;
 public ControlTask(String taskId,long timeoutSeconds){id=taskId;timeout=timeoutSeconds;}
 public String getTaskId(){return id;}public long getTimeoutSeconds(){return timeout;}
}
