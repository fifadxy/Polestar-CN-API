package com.oplus.ocs.carlink.control;
import com.oplus.ocs.carlink.model.*;
/** No service binding and no fabricated successful callbacks. */
public final class CarControl {
 public static void registerCallback(ICarControlCallback c){} public static void unregisterCallback(ICarControlCallback c){}
 public static void auth(){} public static void subscribe(){} public static void accountExpired(){}
 public static void sendCarInfo(CarInfo info){} public static void sendCarStatus(CarStatus status){} public static void sendControlMessage(ControlMessage message){}
}
