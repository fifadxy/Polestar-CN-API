package io.github.polestarcn.companion;

import android.os.Bundle;

/**
 * PsAssistant vehicle snapshot -> OPPO Shelf/Seedling business-data preview.
 *
 * <p>No OPPO proprietary SDK dependency is required, so the mapping can be tested publicly. A
 * private/authorized adapter can later copy these values into the official Seedling card payload.</p>
 */
public final class OppoShelfCardSnapshotAdapter {
  private static final String[] ACTIONS = {
      "lock", "unlock", "climate-start", "climate-stop", "air-start", "air-stop",
      "trunk-open", "honk", "flash"
  };

  private OppoShelfCardSnapshotAdapter() {}

  public static Bundle preview(VehicleSnapshot state) {
    Bundle out = new Bundle();
    out.putInt("schemaVersion", 1);
    out.putString("cardType", "vehicle-status");
    out.putString("provider", "POLESTAR");
    out.putBoolean("available", state != null);
    out.putBoolean("isMilestone", false);
    out.putString("pageId", "pages/vehicle");
    out.putStringArray("actions", ACTIONS.clone());
    if (state == null) return out;

    out.putString("vehicleName", state.name == null || state.name.trim().isEmpty()
        ? "Polestar 2" : state.name.trim());
    out.putLong("updatedAt", state.timestampMillis);
    if (state.batteryPercent != null) {
      out.putBoolean("socKnown", true);
      out.putInt("socPercent", Math.max(0, Math.min(100, state.batteryPercent)));
    } else out.putBoolean("socKnown", false);
    if (state.rangeKm != null) {
      out.putBoolean("rangeKnown", true);
      out.putInt("rangeKm", Math.max(0, state.rangeKm));
    } else out.putBoolean("rangeKnown", false);
    out.putBoolean("charging", ChargingVisibility.shouldShow(state));
    out.putString("chargingLabel", safe(state.charging));
    out.putString("lockLabel", safe(state.lock));
    out.putString("climateLabel", safe(state.climate));
    out.putString("tailgateLabel", safe(state.tailgate));
    out.putString("sourceLabel", safe(state.source));
    return out;
  }

  private static String safe(String value) {
    if (value == null) return "";
    String clean = value.replace('\n', ' ').replace('\r', ' ').trim();
    return clean.substring(0, Math.min(clean.length(), 96));
  }
}