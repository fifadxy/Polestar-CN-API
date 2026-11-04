package com.oplus.ocs.carlink.mock;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import com.oplus.ocs.carlink.CarLink;
import com.oplus.ocs.carlink.ICallback;
import com.oplus.ocs.carlink.control.ICarControlCallback;
import com.oplus.ocs.carlink.model.*;
import java.util.*;

/**
 * In-process TestLab server that emulates the public OCS 1.4.0 control contract.
 * It is intentionally incapable of authenticating against OPPO production services.
 */
public final class MockCarLinkServer {
  public static final int COMPANY_ID=990001;
  public static final String AUTH_CODE="TESTLAB_MOCK_AUTH_20261103";
  public static final String TYPE_CODE="POLESTAR_2_TESTLAB";

  private static boolean initialized,authorized,subscribed,bound;
  private static ICallback initCallback;
  private static ICarControlCallback callback;
  private static String currentCar="";
  private static String lastEvent="reset";
  private static int lastControlCode=-1;
  private static String lastControlMessage="",lastTaskId="";
  private static final LinkedHashMap<String,CarInfo> cars=new LinkedHashMap<>();
  private static final LinkedHashMap<String,CarStatus> statuses=new LinkedHashMap<>();
  private static final ArrayList<ControlMessage> controlMessages=new ArrayList<>();

  private MockCarLinkServer(){}

  public static synchronized boolean credentialsValid(Context context) {
    try {
      ApplicationInfo app=context.getPackageManager().getApplicationInfo(context.getPackageName(),PackageManager.GET_META_DATA);
      Bundle meta=app.metaData==null?Bundle.EMPTY:app.metaData;
      return meta.getInt("oplus.app.carlink.sdk.companyid",0)==COMPANY_ID
          && AUTH_CODE.equals(meta.getString("com.coloros.ocs.car.AUTH_CODE",""));
    } catch(Exception unavailable) {return false;}
  }

  public static synchronized void init(Context context,int flags,ICallback cb) {
    resetTransportOnly();
    initCallback=cb;
    if((flags&CarLink.FLAG_CAR_CONTROL)==0) {
      lastEvent="init-missing-control-flag";
      if(cb!=null)cb.onError(400,"Mock OCS requires FLAG_CAR_CONTROL");
      return;
    }
    if(!credentialsValid(context)) {
      lastEvent="init-credential-rejected";
      if(cb!=null)cb.onError(1005,"TestLab mock credentials mismatch");
      return;
    }
    initialized=true;lastEvent="init-complete";
    if(cb!=null){cb.onServiceConnected();cb.onCarControlSdkInitComplete();}
    if(callback!=null)callback.onComplete();
  }

  public static synchronized void registerCallback(ICarControlCallback cb){callback=cb;lastEvent="callback-registered";}
  public static synchronized void unregisterCallback(ICarControlCallback cb){if(callback==cb)callback=null;lastEvent="callback-unregistered";}

  public static synchronized void auth() {
    if(!initialized){if(callback!=null)callback.onAuthResult(500);lastEvent="auth-before-init";return;}
    authorized=true;lastEvent="auth-success";
    if(callback!=null){callback.onAuthResult(1);callback.inquireCarInfo();}
  }

  public static synchronized void subscribe() {
    if(!authorized){if(callback!=null)callback.onSubscribeResult(500);lastEvent="subscribe-before-auth";return;}
    subscribed=true;lastEvent="subscribe-success";
    if(callback!=null){callback.onSubscribeResult(1);callback.inquireCarStatus();callback.onShowCard();}
  }

  public static synchronized void accountExpired() {
    authorized=false;subscribed=false;bound=false;lastEvent="account-expired";
    if(callback!=null)callback.onHideCard();
  }

  public static synchronized void accountUnbind() {
    authorized=false;subscribed=false;bound=false;currentCar="";lastEvent="account-unbound";
    if(callback!=null){callback.onUnbindResult(1);callback.onHideCard();}
  }

  public static synchronized void receiveCarInfo(CarInfo info) {
    if(info==null)return;
    cars.put(info.getCarId(),info);
    if(info.isCurrentCar()||currentCar.isEmpty())currentCar=info.getCarId();
    bound=authorized&&!cars.isEmpty();
    lastEvent="car-info";
  }

  public static synchronized void receiveCarInfoList(List<CarInfo> list) {
    if(list==null)return;
    cars.clear();
    for(CarInfo info:list)if(info!=null)receiveCarInfo(info);
    bound=authorized&&!cars.isEmpty();
    lastEvent="car-info-list";
  }

  public static synchronized void receiveCarStatus(CarStatus status) {
    if(status==null)return;
    statuses.put(status.getCarId(),status);lastEvent="car-status";
  }

  public static synchronized void receiveCarStatusList(List<CarStatus> list) {
    if(list==null)return;
    for(CarStatus status:list)if(status!=null)statuses.put(status.getCarId(),status);
    lastEvent="car-status-list";
  }

  public static synchronized void receiveControlMessage(ControlMessage message) {
    if(message==null)return;
    controlMessages.add(message);
    while(controlMessages.size()>32)controlMessages.remove(0);
    lastControlCode=message.getCode();lastControlMessage=message.getMessage();
    if(message.getData() instanceof ControlMessage.ControlData)lastTaskId=((ControlMessage.ControlData)message.getData()).taskId;
    lastEvent="control-message-"+message.getCode();
  }

  public static synchronized boolean supportCarControl(){return initialized;}
  public static synchronized boolean hasCarBinding(){return initialized&&authorized&&bound;}
  public static synchronized boolean checkCarBind(String carId){return hasCarBinding()&&carId!=null&&cars.containsKey(carId);}

  public static synchronized void requestCarInfo(){lastEvent="host-inquire-car-info";if(callback!=null)callback.inquireCarInfo();}
  public static synchronized void requestCarStatus(){lastEvent="host-inquire-car-status";if(callback!=null)callback.inquireCarStatus();}
  public static synchronized void showCard(){lastEvent="host-show-card";if(callback!=null)callback.onShowCard();}
  public static synchronized void hideCard(){lastEvent="host-hide-card";if(callback!=null)callback.onHideCard();}
  public static synchronized void switchCar(String carId){lastEvent="host-switch-car";if(callback!=null)callback.onSwitchCar(carId);}
  public static synchronized ActiveTask activate(List<String> carIds){lastEvent="host-activate-car";return callback==null?null:callback.activateCar(carIds);}
  public static synchronized ControlTask issueControl(String carId,String instruction,String value) {
    lastEvent="host-control-"+String.valueOf(instruction);
    if(callback==null||!checkCarBind(carId))return null;
    return callback.onControl(new ControlInstruction(carId,instruction,value));
  }

  public static synchronized void sendSwitchCarResult(String carId,int code) {
    if(code==1&&cars.containsKey(carId))currentCar=carId;
    lastEvent="switch-result-"+code;
  }

  public static synchronized Bundle status() {
    Bundle out=new Bundle();
    out.putBoolean("mock",true);out.putBoolean("initialized",initialized);out.putBoolean("authorized",authorized);
    out.putBoolean("subscribed",subscribed);out.putBoolean("bound",bound);
    out.putInt("companyId",COMPANY_ID);out.putString("authCodeLabel","TESTLAB_MOCK_ONLY");
    out.putString("typeCode",TYPE_CODE);out.putString("currentCar",currentCar);
    out.putInt("carCount",cars.size());out.putInt("statusCount",statuses.size());
    out.putString("lastTaskId",lastTaskId);out.putString("lastEvent",lastEvent);out.putInt("lastControlCode",lastControlCode);
    out.putString("lastControlMessage",lastControlMessage==null?"":lastControlMessage);
    return out;
  }

  public static synchronized void reset() {
    resetTransportOnly();cars.clear();statuses.clear();controlMessages.clear();currentCar="";
    callback=null;initCallback=null;lastControlCode=-1;lastControlMessage="";lastTaskId="";lastEvent="reset";
  }

  private static void resetTransportOnly() {
    initialized=false;authorized=false;subscribed=false;bound=false;
  }
}
