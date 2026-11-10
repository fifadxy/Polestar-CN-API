package io.github.polestarcn.companion;

/** Stable one-page vehicle condition dialog sizing; never needs a ScrollView. */
final class CarStatusPopupLayout {
  private CarStatusPopupLayout(){}
  static int viewportHeightDp(int screenHeightDp) {
    return Math.max(188,Math.min(355,Math.round(screenHeightDp*.52f)));
  }
  /** Leave room for summary, timestamp, full-details action and panel padding. */
  static int diagramHeightDp(int screenHeightDp) {
    return Math.max(96,Math.min(236,viewportHeightDp(screenHeightDp)-110));
  }
}
