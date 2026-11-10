package io.github.polestarcn.companion;

/**
 * A compact, scroll-capable condition dialog. Primary summary, timestamp,
 * vehicle diagram and the collapsed "more" disclosure fit on its first screen;
 * opening a detail section enables normal vertical scrolling.
 */
final class CarStatusPopupLayout {
  private CarStatusPopupLayout(){}
  static int viewportHeightDp(int screenHeightDp) {
    return Math.max(205,Math.min(355,Math.round(screenHeightDp*.52f)));
  }
  /** Reserve 110dp for status labels, padding and the collapsed details header. */
  static int diagramHeightDp(int screenHeightDp) {
    return Math.max(95,Math.min(236,viewportHeightDp(screenHeightDp)-110));
  }
}
