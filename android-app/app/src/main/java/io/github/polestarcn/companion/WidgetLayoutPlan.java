package io.github.polestarcn.companion;

/** Responsive widget density; reserves timestamps, actions and separate charging content first. */
public final class WidgetLayoutPlan {
  public final int count, rows, columns, padding, chargeRows;
  public final boolean wide, title, stacked, progress, chargeTitle;
  public final float labelSp, valueSp, cellWidthDp, rowHeightDp, typeScale;

  public WidgetLayoutPlan(WidgetRenderer.Size size, int requested, int width, int height, float scale) {
    this(size, requested, 0, false, width, height, scale);
  }

  public WidgetLayoutPlan(WidgetRenderer.Size size, int requested, int requestedCharge,
      boolean battery, int width, int height, float scale) {
    wide = size == WidgetRenderer.Size.WIDE;
    scale = Math.max(.8f, scale);typeScale=scale;
    padding = height<60?0:size == WidgetRenderer.Size.MINI && height <= 96 && scale > 1.1f ? 0
        : size == WidgetRenderer.Size.MINI || height < 110 ? 2 : 8;
    title = !wide && size != WidgetRenderer.Size.MINI && height >= 130;
    progress = battery;
    float available = height - 2 * padding - (wide ? 0 : 48)
        - 14 * scale - (title ? 15 * scale : 0);
    float rowMinimum = 18 * scale;
    float mainMinimum = requested > 0 ? rowMinimum + (battery ? 2 : 0) : 0;
    int possibleChargeRows = Math.max(0, (int) Math.floor((available - mainMinimum - 3) / rowMinimum));
    chargeRows = Math.min((requestedCharge + 1) / 2, possibleChargeRows);
    chargeTitle = chargeRows > 0 && height >= 130
        && available - mainMinimum - 3 - chargeRows * rowMinimum >= 14 * scale;
    available -= chargeRows == 0 ? 0 : 3 + chargeRows * rowMinimum + (chargeTitle ? 14 * scale : 0);
    available -= battery ? 2 : 0;
    int wanted = Math.max(0, Math.min(WidgetRenderer.capacity(size), requested));
    int fit = Math.max(1, (int) Math.floor(available / rowMinimum)) * 2;
    count = Math.min(wanted, fit);
    columns = count == 1 ? 1 : 2;
    rows = Math.max(1, (count + columns - 1) / columns);
    cellWidthDp = Math.max(28, (width - 2f * padding - (wide ? 104 : 0)) / columns - 6);
    float row = Math.max(1, available / rows);
    rowHeightDp=row;
    stacked = row >= 42 * scale;
    labelSp = 10;
    float ideal = count <= 1 ? 30 : count <= 2 ? 24 : count <= 4 ? 20 : 16;
    valueSp = Math.max(12, Math.min(ideal,
        stacked ? row / scale - 16 : Math.min(14, row / (1.25f * scale))));
  }
}
