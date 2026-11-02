package io.github.polestarcn.companion;

import android.os.Bundle;

/**
 * Credential-free preview of how a PsAssistant vehicle snapshot maps onto OPPO CarLink CarStatus.
 *
 * <p>This class deliberately depends only on PsAssistant models. The proprietary OCS SDK adapter
 * can consume this contract later after OPPO issues credentials for this application/signature.</p>
 */
public final class OppoCarLinkSnapshotAdapter {
  private static final String[] SUPPORTED_INSTRUCTIONS = {
      "CAR-DOOR", "LOCK-CAR", "ALL-WINDOW", "CAR-BOOT",
      "CAR-HORN", "DOUBLE-FLASH", "CAR-HORN&&DOUBLE-FLASH", "AC-TEMP"
  };

  private OppoCarLinkSnapshotAdapter() {}

  public static Bundle preview(VehicleSnapshot state) {
    Bundle out=new Bundle();
    out.putBoolean("available",state!=null);
    out.putStringArray("availableInstructions",SUPPORTED_INSTRUCTIONS.clone());
    if(state==null) return out;

    out.putString("name",state.name==null?"Polestar":state.name);
    out.putLong("timestamp",state.timestampMillis);
    int mapped=0;

    if(state.batteryPercent!=null) {
      float ratio=Math.max(0f,Math.min(1f,state.batteryPercent/100f));
      out.putBoolean("batteryKnown",true);
      out.putFloat("batteryLeftRatio",ratio);
      mapped++;
    } else out.putBoolean("batteryKnown",false);

    if(state.rangeKm!=null) {
      out.putBoolean("batteryRangeKnown",true);
      out.putFloat("batteryEnduranceMileage",state.rangeKm.floatValue());
      out.putFloat("totalEnduranceMileage",state.rangeKm.floatValue());
      mapped++;
    } else out.putBoolean("batteryRangeKnown",false);

    if(state.odometerKm!=null) {
      out.putBoolean("odometerKnown",true);
      out.putFloat("totalMileage",state.odometerKm.floatValue());
      mapped++;
    } else out.putBoolean("odometerKnown",false);

    out.putBoolean("charging",ChargingVisibility.shouldShow(state));
    out.putString("chargingLabel",state.charging==null?"":state.charging);
    out.putString("lockLabel",state.lock==null?"":state.lock);
    out.putString("tailgateLabel",state.tailgate==null?"":state.tailgate);
    out.putString("climateLabel",state.climate==null?"":state.climate);
    out.putIntArray("doorStates",state.exterior.states(false));
    out.putIntArray("windowStates",state.exterior.states(true));
    out.putInt("mappedMetricCount",mapped);
    return out;
  }
}
