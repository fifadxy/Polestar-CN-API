package com.oplus.ocs.carlink.control;
import com.oplus.ocs.carlink.ICallback;
import com.oplus.ocs.carlink.model.*;
public interface ICarControlCallback extends ICallback {
 default void onComplete(){} default void inquireCarInfo(){} default void inquireCarStatus(){}
 default void onAuthResult(int code){} default void onSubscribeResult(int code){} default void onUnbindResult(int code){}
 default void onVerify(String result){} default void onSwitchCar(String carId){} default void onShowCard(){} default void onHideCard(){}
 default ControlTask onControl(ControlInstruction instruction){return null;}
}
