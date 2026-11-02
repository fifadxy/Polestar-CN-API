package com.oplus.ocs.carlink;
import android.content.Context;
/** Deliberately unavailable transport for public validation builds. */
public final class CarLink {
 public static final int FLAG_CAR_CONTROL=2;
 public static boolean isCarControlInitComplete(){return false;}
 public static void init(Context c,int flags,ICallback callback,boolean debug){if(callback!=null)callback.onError(-1,"SDK implementation absent in validation build");}
}
