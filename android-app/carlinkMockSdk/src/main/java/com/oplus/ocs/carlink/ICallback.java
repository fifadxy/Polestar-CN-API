package com.oplus.ocs.carlink;
/** TestLab-compatible public callback contract. */
public interface ICallback {
 default void onServiceConnected(){}
 default void onServiceDisconnected(){}
 default void onUnbindService(){}
 default void onCarControlSdkInitComplete(){}
 default void onError(int code,String message){}
}
