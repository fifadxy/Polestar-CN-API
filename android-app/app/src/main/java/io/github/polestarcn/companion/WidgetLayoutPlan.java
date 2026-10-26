package io.github.polestarcn.companion;

/** Widget density uses launcher bounds and font scale, rather than grid-cell assumptions. */
public final class WidgetLayoutPlan {
  public final int count, rows, columns, padding;
  public final boolean wide, title, stacked, progress;
  public final float labelSp, valueSp, cellWidthDp;
  public WidgetLayoutPlan(WidgetRenderer.Size size, int requested, int width, int height, float scale) {
    wide = size == WidgetRenderer.Size.WIDE;
    padding = size == WidgetRenderer.Size.MINI || height < 110 ? 2 : 8;
    scale = Math.max(.8f, scale);
    title = !wide && size != WidgetRenderer.Size.MINI && height >= 130;
    progress = !wide && height >= 150 && requested <= 4 && size != WidgetRenderer.Size.MINI;
    float available = height - 2 * padding - (wide ? 0 : 48)
        - 14 * scale - (title ? 15 * scale : 0) - (progress ? 6 : 0);
    int wanted = Math.max(0, Math.min(WidgetRenderer.capacity(size), requested));
    int fit = Math.max(1, (int) Math.floor(available / (16f * scale))) * 2;
    count = Math.min(wanted, fit);
    columns = count == 1 ? 1 : 2;
    rows = Math.max(1, (count + columns - 1) / columns);
    cellWidthDp = Math.max(28, (width - 2f * padding - (wide ? 104 : 0)) / columns - 6);
    float row = Math.max(1, available / rows);
    stacked = row >= 32 * scale;
    labelSp = 10;
    float ideal = count <= 1 ? 30 : count <= 2 ? 24 : count <= 4 ? 20 : 16;
    valueSp = Math.max(12, Math.min(ideal, stacked ? row / scale - 15 : Math.min(14, row / (1.25f * scale))));
  }
}
