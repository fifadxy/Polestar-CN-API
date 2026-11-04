package com.oplus.ocs.carlink.model;
import java.util.List;
public final class CarStatus {
 private final String carId;private long timestamp;private boolean active,charging,allDoorStatus;private float batteryLeftPercent,batteryEnduranceMileage,totalEnduranceMileage,totalMileage;private int airConditionerStatus;private List<ControlInstruction> availableControlInstructions;
 public CarStatus(String carId){this.carId=carId;}
 public String getCarId(){return carId;}
 public long getTimestamp(){return timestamp;} public void setTimestamp(long value){timestamp=value;}
 public boolean isActive(){return active;} public void setActive(boolean value){active=value;}
 public boolean isCharging(){return charging;} public void setCharging(boolean value){charging=value;}
 public boolean isAllDoorStatus(){return allDoorStatus;} public void setAllDoorStatus(boolean value){allDoorStatus=value;}
 public float getBatteryLeftPercent(){return batteryLeftPercent;} public void setBatteryLeftPercent(float value){batteryLeftPercent=value;}
 public float getBatteryEnduranceMileage(){return batteryEnduranceMileage;} public void setBatteryEnduranceMileage(float value){batteryEnduranceMileage=value;}
 public float getTotalEnduranceMileage(){return totalEnduranceMileage;} public void setTotalEnduranceMileage(float value){totalEnduranceMileage=value;}
 public float getTotalMileage(){return totalMileage;} public void setTotalMileage(float value){totalMileage=value;}
 public int getAirConditionerStatus(){return airConditionerStatus;} public void setAirConditionerStatus(int value){airConditionerStatus=value;}
 public List<ControlInstruction> getAvailableControlInstructions(){return availableControlInstructions;} public void setAvailableControlInstructions(List<ControlInstruction> value){availableControlInstructions=value;}
}
