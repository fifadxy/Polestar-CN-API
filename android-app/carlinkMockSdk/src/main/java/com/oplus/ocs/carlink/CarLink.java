package com.oplus.ocs.carlink;
import android.content.Context;
import com.oplus.ocs.carlink.mock.MockCarLinkServer;
/** TestLab OCS-compatible transport. Never contacts OPPO production services. */
public final class CarLink {
 public static final int FLAG_CAR_CONTROL=2;
 public static final int FLAG_CAR_BLUETOOTH_KEY=1;
 private CarLink(){}
 public static boolean isInitComplete(){return MockCarLinkServer.status().getBoolean("initialized",false);}
 public static boolean isCarControlInitComplete(){return isInitComplete();}
 public static boolean isCarKeyInitComplete(){return false;}
 public static void init(Context c,int flags,ICallback callback,boolean debug){MockCarLinkServer.init(c,flags,callback);}
}
