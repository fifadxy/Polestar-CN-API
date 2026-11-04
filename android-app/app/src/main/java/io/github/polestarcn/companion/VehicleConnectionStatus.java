package io.github.polestarcn.companion;
/** Cloud-data connection, not an unsupported claim of a live vehicle socket or driving state. */
final class VehicleConnectionStatus {
 static boolean connected(boolean signedIn,boolean selected,boolean readOk,VehicleSnapshot state,long now){return signedIn&&selected&&readOk&&EffectStateController.fresh(state,now);}
}
