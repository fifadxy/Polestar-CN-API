package io.github.polestarcn.companion;
/** Stable compact map-dialog window bounds: at most 48.84% of the available screen area. */
final class DialogWindowGeometry {
  private DialogWindowGeometry(){}
  static int[] map(int screenWidthPx,int screenHeightPx,float density){
    int width=Math.max(1,Math.min((int)(screenWidthPx*.74f),(int)(440*Math.max(0.1f,density))));
    int height=Math.max(1,(int)(screenHeightPx*.66f));
    return new int[]{width,height};
  }
}
