package com.oplus.ocs.carlink.control;
import com.oplus.ocs.carlink.model.*;
import java.util.List;
/** Public OCS 1.4.0 contract stub. No service binding and no fabricated successful callbacks. */
public final class CarControl {
 public static void registerCallback(ICarControlCallback c){} public static void unregisterCallback(ICarControlCallback c){}
 public static void auth(){} public static void subscribe(){} public static void accountExpired(){} public static void accountUnbind(){}
 public static void sendCarInfo(CarInfo info){} public static void sendCarInfoList(List<CarInfo> info){}
 public static void sendCarStatus(CarStatus status){} public static void sendCarStatusList(List<CarStatus> status){}
 public static void sendControlMessage(ControlMessage message){}
 public static void sendCarInstruction(ControlInstruction instruction){}
 public static void sendSwitchCarResult(String carId,int code){}
 public static boolean supportCarControl(){return false;}
 public static boolean hasCarBinding(){return false;}
 public static boolean checkCarBind(String carId){return false;}
}
