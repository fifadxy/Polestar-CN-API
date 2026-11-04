package com.oplus.ocs.carlink.model;
public final class ControlMessage {
 public static final int CONTROL_EXECUTING=0,CONTROL_SUCCESS=1,CONTROL_FAILED=2;
 private final int code;private final String message;private final Object data;
 public ControlMessage(int code,String message,Object data){this.code=code;this.message=message;this.data=data;}
 public int getCode(){return code;}public String getMessage(){return message;}public Object getData(){return data;}
 public static final class ControlData {
  public final String taskId,message;public final int code;
  public ControlData(String taskId,int code,String message){this.taskId=taskId;this.code=code;this.message=message;}
 }
}
