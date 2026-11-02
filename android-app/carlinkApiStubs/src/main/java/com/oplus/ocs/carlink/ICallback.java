package com.oplus.ocs.carlink;
/** Public contract only. */
public interface ICallback {
 default void onServiceConnected(){} default void onServiceDisconnected(){} default void onUnbindService(){} default void onError(int code,String message){}
}
