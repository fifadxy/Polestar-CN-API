package io.github.polestarcn.companion;
/** Stable compact map-dialog window bounds: at most 48.84% of the available screen area. */
final class DialogWindowGeometry {
  private DialogWindowGeometry(){}
  /** Large, single-page parked-vehicle preview. Settings dialogs still use the compact geometry. */
  static int[] parkingPreview(int widthPx,int heightPx,float density){
    int width=Math.max(1,Math.min(Math.round(widthPx*.92f),Math.round(480*Math.max(0.1f,density))));
    int height=Math.max(1,Math.round(heightPx*.88f));
    return new int[]{width,height};
  }
  static int[] map(int screenWidthPx,int screenHeightPx,float density){
    int width=Math.max(1,Math.min((int)(screenWidthPx*.74f),(int)(440*Math.max(0.1f,density))));
    int height=Math.max(1,(int)(screenHeightPx*.66f));
    return new int[]{width,height};
  }
}
