package io.github.polestarcn.companion;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.text.format.DateFormat;
import android.widget.RemoteViews;

public final class WidgetRenderer {
  public enum Size { LARGE, SQUARE, WIDE, MINI, DATA }
  private WidgetRenderer() {}

  public static void refreshAll(Context context) {
    AppWidgetManager m = AppWidgetManager.getInstance(context);
    refresh(context, m, VehicleWidgetProvider.class, Size.LARGE);
    refresh(context, m, CompactWidgetProvider.class, Size.SQUARE);
    refresh(context, m, WideStripWidgetProvider.class, Size.WIDE);
    refresh(context, m, MiniStripWidgetProvider.class, Size.MINI);
    refresh(context, m, DataWidgetProvider.class, Size.DATA);
  }
  private static void refresh(Context c, AppWidgetManager m, Class<?> receiver, Size size) {
    for (int id : m.getAppWidgetIds(new ComponentName(c, receiver))) update(c, m, id, size);
  }

  public static void update(Context c, AppWidgetManager m, int id, Size size) {
    m.updateAppWidget(id, createViews(c, id, size));
    ResidentService.scheduleUpdates(c);
  }

  public static boolean hasWidgets(Context c) {
    AppWidgetManager m = AppWidgetManager.getInstance(c);
    for (Class<?> provider : new Class<?>[]{VehicleWidgetProvider.class, CompactWidgetProvider.class,
        WideStripWidgetProvider.class, MiniStripWidgetProvider.class, DataWidgetProvider.class})
      if (m.getAppWidgetIds(new ComponentName(c, provider)).length > 0) return true;
    return false;
  }

  static RemoteViews createViews(Context c, int id, Size size) {
    VehicleSnapshot s = AppState.region(c) == Region.CHINA ? VehicleSnapshotStore.loadChina(c) : null;
    TelemetryStatus state = s == null ? TelemetryStatus.UNKNOWN : TelemetryStatus.classify(
        s.isDemo, s.source, s.timestampMillis, System.currentTimeMillis(), 15 * 60 * 1000L);
    int layout = size == Size.DATA ? R.layout.widget_data : size == Size.LARGE ? R.layout.widget_standard :
        size == Size.SQUARE ? R.layout.widget_compact :
        size == Size.WIDE ? R.layout.widget_wide : R.layout.widget_mini;
    RemoteViews views = new RemoteViews(c.getPackageName(), layout);
    views.setImageViewBitmap(R.id.widget_bg, background(c, size));
    boolean en = AppPreferences.widgetEnglish(c);
    String updated = s == null || s.timestampMillis <= 0
        ? AppPreferences.widgetTr(c, "更新时间 --", "Updated --")
        : AppPreferences.widgetTr(c, "更新 ", "Updated ") + DateFormat.format("MM-dd HH:mm", s.timestampMillis);
    views.setTextViewText(R.id.widget_title, s == null ? "PsAssistant" : s.name);
    views.setTextViewText(R.id.widget_region, updated);
    java.util.List<DisplayMetric> selected = MetricSelection.read(c, metricKey(size), defaults(size));
    int[] ids = {R.id.widget_metric0, R.id.widget_metric1, R.id.widget_metric2, R.id.widget_metric3,
        R.id.widget_metric4, R.id.widget_metric5, R.id.widget_metric6, R.id.widget_metric7};
    int[] rows = {R.id.widget_metric_row0, R.id.widget_metric_row1, R.id.widget_metric_row2, R.id.widget_metric_row3};
    int columns = 2;
    int count = Math.min(capacity(size), selected.size());
    int ink = AppPreferences.dark(c) ? Color.WHITE : Color.rgb(21,22,23);
    views.setTextColor(R.id.widget_title, ink);
    views.setTextColor(R.id.widget_region, AppPreferences.dark(c) ? Color.LTGRAY : Color.DKGRAY);
    for (int i = 0; i < capacity(size); i++) {
      views.setViewVisibility(ids[i], i < count ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
      if (i < count) {
        DisplayMetric metric = selected.get(i);
        String label = metric.shortLabel(en), value = metric.value(c, s, false, en);
        boolean dense = count >= 7;
        boolean singleLine = dense || size == Size.WIDE || size == Size.MINI;
        String separator = singleLine ? " · " : "\n";
        String content = label + separator + value;
        int labelSp = dense ? 8 : count >= 5 ? 9 : count >= 3 ? 10 : 11;
        int valueSp = dense ? 10 : count >= 5 ? 12 : count >= 3 ? 14 : 16;
        if (size == Size.WIDE) { labelSp = count >= 4 ? 8 : 9; valueSp = count >= 4 ? 11 : 13; }
        if (size == Size.MINI) { labelSp = 8; valueSp = 11; }
        android.text.SpannableString styled = new android.text.SpannableString(content);
        styled.setSpan(new android.text.style.AbsoluteSizeSpan(labelSp, true), 0, label.length(), 0);
        styled.setSpan(new android.text.style.AbsoluteSizeSpan(valueSp, true), label.length() + separator.length(), content.length(), 0);
        views.setTextViewTextSize(ids[i], android.util.TypedValue.COMPLEX_UNIT_SP, labelSp);
        views.setInt(ids[i], "setMaxLines", singleLine ? 1 : 2);
        views.setTextViewText(ids[i], styled); views.setTextColor(ids[i], ink);
        views.setContentDescription(ids[i], metric.label(en) + " · " + metric.value(c, s, true, en));
      }
    }
    for (int i = 0; i < (capacity(size) + columns - 1) / columns; i++)
      views.setViewVisibility(rows[i], i * columns < count ? android.view.View.VISIBLE : android.view.View.GONE);
    views.setTextViewText(R.id.widget_action_lock, en ? "LOCK" : "车锁");
    views.setTextViewText(R.id.widget_action_climate, en ? "CLIMATE" : "空调");
    views.setOnClickPendingIntent(R.id.widget_summary, pending(c, id, size, "open"));
    views.setOnClickPendingIntent(R.id.widget_action_lock, pending(c, id, size, "lock-choice"));
    views.setOnClickPendingIntent(R.id.widget_action_climate, pending(c, id, size, "climate-choice"));
    return views;
  }

  public static int capacity(Size size) {
    return size == Size.DATA ? 8 : size == Size.LARGE ? 8 : size == Size.SQUARE ? 4
        : size == Size.WIDE ? 4 : 2;
  }
  public static String metricKey(Size size) { return "widget_metrics_" + size.name(); }
  public static String defaults(Size size) {
    if (size == Size.MINI) return "battery,range";
    if (size == Size.WIDE) return "battery,range,tire_status,service_km_left";
    if (size == Size.SQUARE) return "battery,range,tire_status,service_days_left";
    return size == Size.DATA ? "battery,range,tire_status,service_days_left,service_km_left,service_time_percent,service_km_percent,odometer"
        : "battery,range,tire_status,service_days_left,service_km_left,ta_distance,tm_distance,odometer";
  }

  private static PendingIntent pending(Context c, int id, Size size, String action) {
    Intent i = new Intent(c, MainActivity.class);
    i.setAction("io.github.polestarcn.companion.WIDGET");
    i.setData(Uri.parse("psassistant://widget/" + id + "/" + size + "/" + action));
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    if (!"open".equals(action)) i.putExtra("widget_action", action);
    return PendingIntent.getActivity(c, 0, i,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  /** Rounded background and simulated frosting; actual launcher-wallpaper blur is not available. */
  static Bitmap background(Context c, Size size) {
    boolean dark = AppPreferences.dark(c);
    boolean opaque = "opaque".equals(AppPreferences.get(c, "widget_style", "transparent"));
    int opacity = opaque ? 100 : AppPreferences.slider(c, "widget_opacity", 58);
    int frost = AppPreferences.slider(c, "widget_frost", 28);
    int w = size == Size.MINI ? 190 : size == Size.SQUARE ? 190 : 480;
    int h = size == Size.DATA ? 350 : size == Size.LARGE ? 205 : size == Size.SQUARE ? 205 : 108;
    Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    int alpha = (int) Math.round(opacity * 2.55);
    int base = dark ? Color.rgb(28,29,31) : Color.rgb(247,247,244);
    p.setColor(Color.argb(alpha, Color.red(base), Color.green(base), Color.blue(base)));
    canvas.drawRoundRect(1, 1, w - 1, h - 1, 23, 23, p);
    if (!opaque && opacity > 0 && frost > 0) {
      int shimmer = (int) (frost * opacity / 100.0 * 0.85);
      int top = dark ? Color.argb(shimmer, 210,210,210) : Color.argb(shimmer,255,255,255);
      p.setShader(new LinearGradient(0, 0, w, h, top, Color.TRANSPARENT, Shader.TileMode.CLAMP));
      canvas.drawRoundRect(1, 1, w - 1, h - 1, 23, 23, p);
      p.setShader(null);
    }
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(2f);
    p.setColor(Color.argb(Math.min(105, alpha / 2),
        dark ? 220 : 95, dark ? 220 : 95, dark ? 220 : 95));
    canvas.drawRoundRect(2, 2, w - 2, h - 2, 22, 22, p);
    return bitmap;
  }
}

