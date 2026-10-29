package io.github.polestarcn.companion;
import io.github.polestarcn.companion.api.ChinaPolestarApi;
/**
 * No camping recovery write before a fresh, live read of the SAME vehicle following valid login.
 * Network availability alone is never proof of an active authenticated vehicle connection.
 */
public final class CampRecoveryGate {
  private CampRecoveryGate(){}
  public static boolean permits(CampSession session,ChinaPolestarApi.VehicleInfo selected,
      VehicleSnapshot state,boolean signedIn,boolean readSucceeded,long now){
    return signedIn&&readSucceeded&&session!=null&&selected!=null
        &&session.vin.equals(selected.vin)&&EffectStateController.fresh(state,now);
  }
}
