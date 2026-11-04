package com.oplus.ocs.carlink.model;
import java.util.List;
/** Minimal documented data contract, no SDK transport. */
public final class CarStatus {
 private final String carId;
 public CarStatus(String carId){this.carId=carId;}
 public String getCarId(){return carId;}
 private long timestamp;
 public long getTimestamp(){return timestamp;} public void setTimestamp(long value){timestamp=value;}
 private boolean active;
 public boolean isActive(){return active;} public void setActive(boolean value){active=value;}
 private boolean charging;
 public boolean isCharging(){return charging;} public void setCharging(boolean value){charging=value;}
 private boolean allDoorStatus;
 public boolean isAllDoorStatus(){return allDoorStatus;} public void setAllDoorStatus(boolean value){allDoorStatus=value;}
 private float batteryLeftPercent;
 public float getBatteryLeftPercent(){return batteryLeftPercent;} public void setBatteryLeftPercent(float value){batteryLeftPercent=value;}
 private float batteryEnduranceMileage;
 public float getBatteryEnduranceMileage(){return batteryEnduranceMileage;} public void setBatteryEnduranceMileage(float value){batteryEnduranceMileage=value;}
 private float totalEnduranceMileage;
 public float getTotalEnduranceMileage(){return totalEnduranceMileage;} public void setTotalEnduranceMileage(float value){totalEnduranceMileage=value;}
 private float totalMileage;
 public float getTotalMileage(){return totalMileage;} public void setTotalMileage(float value){totalMileage=value;}
 private int airConditionerStatus;
 public int getAirConditionerStatus(){return airConditionerStatus;} public void setAirConditionerStatus(int value){airConditionerStatus=value;}
 private List<ControlInstruction> availableControlInstructions;
 public List<ControlInstruction> getAvailableControlInstructions(){return availableControlInstructions;} public void setAvailableControlInstructions(List<ControlInstruction> value){availableControlInstructions=value;}
}
