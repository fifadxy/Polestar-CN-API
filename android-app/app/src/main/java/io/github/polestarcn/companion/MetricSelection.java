package io.github.polestarcn.companion;

import android.content.Context;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class MetricSelection {
  private MetricSelection() {}
  public static List<DisplayMetric> read(Context c, String key, String defaults) {
    LinkedHashSet<DisplayMetric> out = new LinkedHashSet<>();
    for (String id : AppPreferences.get(c, key, defaults).split(","))
      for (DisplayMetric metric : DisplayMetric.values()) if (metric.id.equals(id)) out.add(metric);
    return new ArrayList<>(out);
  }
  public static void save(Context c, String key, List<DisplayMetric> values) {
    StringBuilder out = new StringBuilder();
    for (DisplayMetric metric : values) { if (out.length() > 0) out.append(','); out.append(metric.id); }
    AppPreferences.set(c, key, out.toString());
  }
}
