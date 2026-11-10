package io.github.polestarcn.companion;
/** Parking preview wraps content. Compact location-settings dialogs keep their own bounds. */
final class DialogWindowGeometry {
  private DialogWindowGeometry(){}
  /** Full-width parking preview: never force empty window space below the map and controls. */
  static int[] parkingPreview(int widthPx,int heightPx,float density){
    int width=Math.max(1,Math.min(Math.round(widthPx*.92f),Math.round(480*Math.max(0.1f,density))));
    return new int[]{width,android.view.WindowManager.LayoutParams.WRAP_CONTENT};
  }
  static int[] map(int screenWidthPx,int screenHeightPx,float density){
    int width=Math.max(1,Math.min((int)(screenWidthPx*.74f),(int)(440*Math.max(0.1f,density))));
    int height=Math.max(1,(int)(screenHeightPx*.66f));
    return new int[]{width,height};
  }
}
