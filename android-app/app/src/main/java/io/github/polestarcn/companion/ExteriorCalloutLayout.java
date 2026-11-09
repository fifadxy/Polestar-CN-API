package io.github.polestarcn.companion;

/** Four exterior callouts must fit in two non-overlapping rows on compact dialogs. */
final class ExteriorCalloutLayout {
  private ExteriorCalloutLayout() {}

  /** Returns front-row top, rear-row top and card height in pixels. */
  static float[] rows(float height, float density, float fontScale) {
    float d = Math.max(0.1f, density);
    float scale = Math.min(1.25f, Math.max(1f, fontScale));
    float header = 20f * d * scale;
    float footer = 20f * d * scale;
    float available = Math.max(0f, height - header - footer);
    float gap = Math.min(8f * d, available * 0.09f);
    float card = Math.min(64f * d, Math.max(0f, (available - gap) / 2f));
    float first = header + Math.max(0f, available - (2f * card + gap)) * 0.5f;
    return new float[]{first, first + card + gap, card};
  }
}
