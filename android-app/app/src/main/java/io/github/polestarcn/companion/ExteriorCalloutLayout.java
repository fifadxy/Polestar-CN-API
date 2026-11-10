package io.github.polestarcn.companion;

/** Four exterior callouts must fit in two non-overlapping rows on compact dialogs. */
final class ExteriorCalloutLayout {
  private ExteriorCalloutLayout() {}

  /** Fit the upright vehicle between callout columns and clear front/rear labels. */
  static float carHalfLength(float width, float height, float density, float aspectRatio,
      float cardWidth, float textScale) {
    float d = Math.max(.1f, density);
    float ratio = Math.max(.1f, aspectRatio);
    float lane = Math.max(2 * d, width - 2 * cardWidth - 18 * d);
    float sideLimit = lane / (2 * ratio);
    float verticalLimit = Math.max(2 * d, height * .5f - 24 * Math.max(d, textScale));
    // Increase the art by ~20% while enforcing side-card and text-label clearance.
    float original = Math.min(height * .33f, width * .38f);
    return Math.max(1f, Math.min(original * 1.20f,
        Math.min(sideLimit, verticalLimit)));
  }

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
