package com.oplus.ocs.carlink.control;
import com.oplus.ocs.carlink.mock.MockCarLinkServer;
import com.oplus.ocs.carlink.model.*;
import java.util.List;
/** TestLab CarControl implementation backed by the in-process mock server. */
public final class CarControl {
 private CarControl(){}
 public static void registerCallback(ICarControlCallback c){MockCarLinkServer.registerCallback(c);}
 public static void unregisterCallback(ICarControlCallback c){MockCarLinkServer.unregisterCallback(c);}
 public static void auth(){MockCarLinkServer.auth();}
 public static void subscribe(){MockCarLinkServer.subscribe();}
 public static void accountExpired(){MockCarLinkServer.accountExpired();}
 public static void accountUnbind(){MockCarLinkServer.accountUnbind();}
 public static void sendCarInfo(CarInfo info){MockCarLinkServer.receiveCarInfo(info);}
 public static void sendCarInfoList(List<CarInfo> info){MockCarLinkServer.receiveCarInfoList(info);}
 public static void sendCarStatus(CarStatus status){MockCarLinkServer.receiveCarStatus(status);}
 public static void sendCarStatusList(List<CarStatus> status){MockCarLinkServer.receiveCarStatusList(status);}
 public static void sendControlMessage(ControlMessage message){MockCarLinkServer.receiveControlMessage(message);}
 public static void sendCarInstruction(ControlInstruction instruction){}
 public static void sendSwitchCarResult(String carId,int code){MockCarLinkServer.sendSwitchCarResult(carId,code);}
 public static boolean supportCarControl(){return MockCarLinkServer.supportCarControl();}
 public static boolean hasCarBinding(){return MockCarLinkServer.hasCarBinding();}
 public static boolean checkCarBind(String carId){return MockCarLinkServer.checkCarBind(carId);}
}
