package io.github.polestarcn.companion;

/** Separate from the compact location-settings window: prioritize a readable real map viewport. */
final class ParkingPreviewLayout {
  private ParkingPreviewLayout(){}
  static int mapHeightDp(int screenHeightDp,boolean portrait) {
    if(portrait) return Math.max(180,Math.min(320,Math.round(screenHeightDp*.35f)));
    return Math.max(55,Math.min(94,Math.round(screenHeightDp*.20f)));
  }
}
