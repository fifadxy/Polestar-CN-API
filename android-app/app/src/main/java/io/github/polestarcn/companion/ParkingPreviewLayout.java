package io.github.polestarcn.companion;

/** Separate from the compact location-settings window: prioritize a readable real map viewport. */
final class ParkingPreviewLayout {
  private ParkingPreviewLayout(){}
  /** Reserve space for dialog title/close control, map toolbar and GPS row.
   * Keeping everything in one fixed layout prevents outer-window swiping. */
  static int mapHeightDp(int screenHeightDp,boolean portrait) {
    int available=Math.round(Math.max(0,screenHeightDp)*.86f)-260;
    return Math.max(52,Math.min(portrait?320:170,available));
  }
  static boolean compactControls(int screenHeightDp){return screenHeightDp<410;}
}
