package io.github.polestarcn.companion;

import android.os.Bundle;

/** Credential-free preview of how PsAssistant state maps onto OPPO CarLink CarStatus. */
public final class OppoCarLinkSnapshotAdapter {
  private static final String[] SUPPORTED_INSTRUCTIONS={
      "CAR-DOOR","LOCK-CAR","ALL-WINDOW","CAR-BOOT",
      "CAR-HORN","DOUBLE-FLASH","CAR-HORN&&DOUBLE-FLASH","AC-TEMP"
  };
  private OppoCarLinkSnapshotAdapter() {}
  public static Bundle preview(VehicleSnapshot state) {
    Bundle out=new Bundle();out.putBoolean("available",state!=null);
    out.putStringArray("availableInstructions",SUPPORTED_INSTRUCTIONS.clone());
    if(state==null)return out;
    out.putString("name",state.name==null?"Polestar":state.name);out.putLong("timestamp",state.timestampMillis);
    if(state.batteryPercent!=null){out.putBoolean("batteryKnown",true);out.putFloat("batteryLeftRatio",Math.max(0f,Math.min(1f,state.batteryPercent/100f)));}
    else out.putBoolean("batteryKnown",false);
    if(state.rangeKm!=null){out.putBoolean("batteryRangeKnown",true);out.putFloat("batteryEnduranceMileage",state.rangeKm.floatValue());out.putFloat("totalEnduranceMileage",state.rangeKm.floatValue());}
    else out.putBoolean("batteryRangeKnown",false);
    if(state.odometerKm!=null){out.putBoolean("odometerKnown",true);out.putFloat("totalMileage",state.odometerKm.floatValue());}
    else out.putBoolean("odometerKnown",false);
    out.putBoolean("charging",ChargingVisibility.shouldShow(state));
    out.putString("chargingLabel",state.charging==null?"":state.charging);
    out.putString("lockLabel",state.lock==null?"":state.lock);
    out.putString("tailgateLabel",state.tailgate==null?"":state.tailgate);
    out.putString("climateLabel",state.climate==null?"":state.climate);
    out.putIntArray("doorStates",state.exterior.states(false));out.putIntArray("windowStates",state.exterior.states(true));
    return out;
  }
}