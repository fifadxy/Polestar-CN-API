package com.oplus.ocs.carlink.model;
public final class ControlInstruction {
 private final String carId;private String instruction,value;private int status;
 public ControlInstruction(String carId,String instruction,int status){this.carId=carId;this.instruction=instruction;this.status=status;}
 public ControlInstruction(String carId,String instruction,String value){this.carId=carId;this.instruction=instruction;this.value=value;}
 public String getCarId(){return carId;}public String getInstruction(){return instruction;}public int getStatus(){return status;}public String getInstructionValue(){return value;}
}
