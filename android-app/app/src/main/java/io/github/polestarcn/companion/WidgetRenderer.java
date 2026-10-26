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
    VehicleSnapshot snapshot = VehicleSnapshotStore.loadChina(context);
    refresh(context, m, VehicleWidgetProvider.class, Size.LARGE, snapshot);
    refresh(context, m, CompactWidgetProvider.class, Size.SQUARE, snapshot);
    refresh(context, m, WideStripWidgetProvider.class, Size.WIDE, snapshot);
    refresh(context, m, MiniStripWidgetProvider.class, Size.MINI, snapshot);
    refresh(context, m, DataWidgetProvider.class, Size.DATA, snapshot);
  }
  private static void refresh(Context c, AppWidgetManager m, Class<?> receiver, Size size, VehicleSnapshot snapshot) {
    for (int id : m.getAppWidgetIds(new ComponentName(c, receiver))) update(c, m, id, size, snapshot);
  }

  public static void update(Context c, AppWidgetManager m, int id, Size size) {
    update(c, m, id, size, VehicleSnapshotStore.loadChina(c));
  }
  private static void update(Context c, AppWidgetManager m, int id, Size size, VehicleSnapshot snapshot) {
    android.os.Bundle options = m.getAppWidgetOptions(id);
    int pw = Math.max(118, options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, defaultWidth(size)));
    int ph = Math.max(70, options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, defaultHeight(size))));
    int lw = Math.max(118, options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, pw));
    int lh = Math.max(70, options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, ph));
    Bitmap backing = background(c, size);
    RemoteViews portrait = createViews(c, id, size, pw, ph, backing, snapshot);
    m.updateAppWidget(id, pw == lw && ph == lh ? portrait :
        new RemoteViews(createViews(c, id, size, lw, lh, backing, snapshot), portrait));
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
    return createViews(c, id, size, defaultWidth(size), defaultHeight(size), background(c, size), VehicleSnapshotStore.loadChina(c));
  }

  static int defaultWidth(Size size) { return size == Size.MINI || size == Size.SQUARE ? 150 : 300; }
  static int defaultHeight(Size size) { return size == Size.MINI ? 88 : size == Size.WIDE ? 70 : size == Size.DATA ? 260 : 170; }

  static RemoteViews createViews(Context c, int id, Size size, int width, int height, Bitmap backing, VehicleSnapshot snapshot) {
    VehicleSnapshot s = AppState.region(c) == Region.CHINA ? snapshot : null;
    int layout = size == Size.DATA ? R.layout.widget_data : size == Size.LARGE ? R.layout.widget_standard :
        size == Size.SQUARE ? R.layout.widget_compact : size == Size.WIDE ? R.layout.widget_wide : R.layout.widget_mini;
    RemoteViews views = new RemoteViews(c.getPackageName(), layout);
    views.setImageViewBitmap(R.id.widget_bg, backing);
    boolean en = AppPreferences.widgetEnglish(c);
    java.util.List<DisplayMetric> selected = MetricSelection.read(c, metricKey(size), defaults(size));
    WidgetLayoutPlan plan = new WidgetLayoutPlan(size, selected.size(), width, height,
        c.getResources().getConfiguration().fontScale);
    float density = c.getResources().getDisplayMetrics().density;
    int padding = Math.round(plan.padding * density);
    views.setViewPadding(R.id.widget_content, padding, padding, padding, padding);
    String preference = AppPreferences.get(c, "widget_ink", "system");
    boolean white = "light".equals(preference) || ("system".equals(preference) && AppPreferences.dark(c));
    int ink = white ? Color.rgb(244,244,242) : Color.rgb(21,22,23);
    int secondary = white ? Color.rgb(190,192,192) : Color.rgb(98,101,103);
    views.setViewVisibility(R.id.widget_title, plan.title ? android.view.View.VISIBLE : android.view.View.GONE);
    views.setTextViewText(R.id.widget_title, s == null ? "PsAssistant" : s.name);
    views.setTextColor(R.id.widget_title, secondary);
    int[] ids = {R.id.widget_metric0, R.id.widget_metric1, R.id.widget_metric2, R.id.widget_metric3,
        R.id.widget_metric4, R.id.widget_metric5, R.id.widget_metric6, R.id.widget_metric7,
        R.id.widget_metric8, R.id.widget_metric9, R.id.widget_metric10, R.id.widget_metric11};
    int[] rows = {R.id.widget_metric_row0, R.id.widget_metric_row1, R.id.widget_metric_row2,
        R.id.widget_metric_row3, R.id.widget_metric_row4, R.id.widget_metric_row5};
    for (int i = 0; i < ids.length; i++) {
      int visibility = i < plan.count ? android.view.View.VISIBLE : plan.columns == 1
          ? android.view.View.GONE : android.view.View.INVISIBLE;
      views.setViewVisibility(ids[i], visibility);
      if (i < plan.count) {
        DisplayMetric metric = selected.get(i);
        views.setTextViewTextSize(ids[i], android.util.TypedValue.COMPLEX_UNIT_SP, 10);
        views.setInt(ids[i], "setMaxLines", plan.stacked ? 2 : 1);
        views.setTextColor(ids[i], ink);
        views.setTextViewText(ids[i], metricText(c, metric, s, en, plan, ink, secondary));
        views.setContentDescription(ids[i], metric.label(en) + " · " + metric.value(c, s, true, en));
      }
    }
    for (int i = 0; i < rows.length; i++)
      views.setViewVisibility(rows[i], i < plan.rows && plan.count > 0 ? android.view.View.VISIBLE : android.view.View.GONE);
    boolean progress = plan.progress && s != null && s.batteryPercent != null && selected.contains(DisplayMetric.BATTERY);
    views.setViewVisibility(R.id.widget_soc_progress, progress ? android.view.View.VISIBLE : android.view.View.GONE);
    views.setProgressBar(R.id.widget_soc_progress, 100, s == null || s.batteryPercent == null ? 0 : s.batteryPercent, false);
    String updated = updatedText(c, s, selected.subList(0, plan.count), size);
    int hidden = Math.min(capacity(size), selected.size()) - plan.count;
    if (hidden > 0) updated += " · +" + hidden;
    views.setViewVisibility(R.id.widget_region, android.view.View.VISIBLE);
    views.setTextViewText(R.id.widget_region, updated);
    views.setTextColor(R.id.widget_region, secondary);
    views.setTextViewTextSize(R.id.widget_region, android.util.TypedValue.COMPLEX_UNIT_SP, 10);
    views.setContentDescription(R.id.widget_region, updated + AppPreferences.widgetTr(c, "，点击刷新", ". Tap to refresh"));
    views.setOnClickPendingIntent(R.id.widget_region, pending(c, id, size, "refresh"));
    action(views, c, true, s != null && "未锁".equals(s.lock), ink,
        en ? (s != null && "未锁".equals(s.lock) ? "Lock" : "Unlock") : (s != null && "未锁".equals(s.lock) ? "上锁" : "解锁"));
    action(views, c, false, s != null && "开启".equals(s.climate), ink,
        en ? (s != null && "开启".equals(s.climate) ? "Stop" : "Climate") : (s != null && "开启".equals(s.climate) ? "关闭" : "空调"));
    views.setOnClickPendingIntent(R.id.widget_summary, pending(c, id, size, "open"));
    views.setOnClickPendingIntent(R.id.widget_action_lock, pending(c, id, size, "lock-choice"));
    views.setOnClickPendingIntent(R.id.widget_action_climate, pending(c, id, size, "climate-choice"));
    return views;
  }

  private static void action(RemoteViews views, Context c, boolean lock, boolean active, int ink, String label) {
    int root = lock ? R.id.widget_action_lock : R.id.widget_action_climate;
    int icon = lock ? R.id.widget_lock_icon : R.id.widget_climate_icon;
    int text = lock ? R.id.widget_lock_label : R.id.widget_climate_label;
    boolean transparent = !"opaque".equals(AppPreferences.get(c, "widget_style", "transparent"));
    views.setInt(root, "setBackgroundResource", active ? R.drawable.widget_action_on
        : transparent ? R.drawable.widget_action_clear : R.drawable.widget_action_off);
    int color = active ? Color.rgb(21,22,23) : ink;
    views.setInt(icon, "setColorFilter", color); views.setTextColor(text, color);
    views.setTextViewText(text, label);
    views.setContentDescription(root, AppPreferences.widgetTr(c, lock ? "门锁 · " : "空调 · ", lock ? "Door lock · " : "Climate · ")
        + label + AppPreferences.widgetTr(c, "，点击后生物验证", ". Verify biometrics to control"));
  }

  private static String updatedText(Context c, VehicleSnapshot s, java.util.List<DisplayMetric> metrics, Size size) {
    if (s == null) return AppPreferences.widgetTr(c, "尚无数据", "No data");
    long at = Long.MAX_VALUE;
    boolean unknown = false;
    for (DisplayMetric metric : metrics) {
      if ("--".equals(metric.value(c, s, false, AppPreferences.widgetEnglish(c)))) continue;
      long stamp = metric.reportedAt(s);
      if (stamp <= 0 || stamp > System.currentTimeMillis() + 300_000L) unknown = true;
      else at = Math.min(at, stamp);
    }
    if (metrics.isEmpty()) at = s.timestampMillis;
    if (unknown || at == Long.MAX_VALUE || at <= 0) return AppPreferences.widgetTr(c, "更新时间未知", "Update time unknown");
    java.util.Calendar now = java.util.Calendar.getInstance(), measured = java.util.Calendar.getInstance();
    measured.setTimeInMillis(at);
    String format = measured.get(java.util.Calendar.YEAR) != now.get(java.util.Calendar.YEAR) ? "yyyy-MM-dd"
        : size == Size.MINI ? (measured.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR) ? "HH:mm" : "MM-dd") : "MM-dd HH:mm";
    return AppPreferences.widgetTr(c, "更新 ", "Upd ") + DateFormat.format(format, at);
  }

  private static CharSequence metricText(Context c, DisplayMetric metric, VehicleSnapshot s,
      boolean en, WidgetLayoutPlan plan, int ink, int secondary) {
    float scale = c.getResources().getDisplayMetrics().scaledDensity;
    float width = plan.cellWidthDp * c.getResources().getDisplayMetrics().density;
    String value = metric.value(c, s, false, en);
    if (!plan.stacked && (metric == DisplayMetric.BATTERY || metric == DisplayMetric.RANGE))
      value = metric.value(c, s, true, en);
    float valueSp = plan.valueSp;
    android.text.TextPaint paint = new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
    paint.setTypeface(android.graphics.Typeface.create(c.getResources().getFont(R.font.lato_regular), android.graphics.Typeface.NORMAL));
    paint.setTextSize(valueSp * scale);
    while (valueSp > 12 && paint.measureText(value) > width) { valueSp -= .5f; paint.setTextSize(valueSp * scale); }
    float valueWidth = paint.measureText(value);
    String label = plan.stacked ? metric.shortLabel(en) : compactLabel(metric, en);
    if (!plan.stacked && (metric == DisplayMetric.BATTERY || metric == DisplayMetric.RANGE)) label = "";
    paint.setTextSize(plan.labelSp * scale);
    float labelWidth = plan.stacked ? width : Math.max(0, width - valueWidth - 4 * scale);
    label = android.text.TextUtils.ellipsize(label, paint, labelWidth, android.text.TextUtils.TruncateAt.END).toString();
    String separator = label.isEmpty() ? "" : plan.stacked ? "\n" : " ";
    String text = label + separator + value;
    android.text.SpannableString styled = new android.text.SpannableString(text);
    int valueStart = label.length() + separator.length();
    if (!label.isEmpty()) styled.setSpan(new android.text.style.ForegroundColorSpan(secondary), 0, label.length(), 0);
    styled.setSpan(new android.text.style.RelativeSizeSpan(valueSp / 10f), valueStart, text.length(), 0);
    boolean accent = metric == DisplayMetric.BATTERY || metric == DisplayMetric.TIRE_STATUS && s != null
        && (s.tires.allGood() || s.tires.hasProblem());
    styled.setSpan(new android.text.style.ForegroundColorSpan(accent ? Color.rgb(255,107,0) : ink), valueStart, text.length(), 0);
    return styled;
  }

  private static String compactLabel(DisplayMetric metric, boolean en) {
    switch (metric) {
      case BATTERY: case RANGE: return "";
      case ODOMETER: return "ODO";
      case TA_DISTANCE: return "TA";
      case TM_DISTANCE: return "TM";
      case TA_SPEED: return "TA v";
      case TM_SPEED: return "TM v";
      case TA_CONSUMPTION: return "TA e";
      case TM_CONSUMPTION: return "TM e";
      case TEMPERATURE: return "°C";
      case TARGET_TEMPERATURE: return en ? "Tgt" : "目标";
      case AQI: return "AQI";
      case PM25: return "PM2.5";
      case CHARGE_POWER: return "kW";
      case CHARGE_CURRENT: return "A";
      case SERVICE_DAYS_LEFT: return en ? "S d" : "保养天";
      case SERVICE_KM_LEFT: return "S km";
      case SERVICE_TIME_PERCENT: case SERVICE_KM_PERCENT: return "S %";
      case VEHICLE_STATUS: return en ? "Vehicle" : "车况";
      case TIRE_STATUS: return en ? "Tires" : "胎压";
      default: return metric.shortLabel(en);
    }
  }

  public static int capacity(Size size) {
    return size == Size.DATA ? 8 : size == Size.LARGE ? 8 : size == Size.SQUARE ? 4
        : size == Size.WIDE ? 4 : 2;
  }
  public static String metricKey(Size size) { return "widget_metrics_" + size.name(); }
  public static String defaults(Size size) {
    if (size == Size.MINI) return "battery,range";
    if (size == Size.WIDE) return "battery,range,vehicle_status,service_km_left";
    if (size == Size.SQUARE) return "battery,range,vehicle_status,service_days_left";
    return size == Size.DATA ? "battery,range,vehicle_status,service_days_left,service_km_left,service_time_percent,service_km_percent,odometer"
        : "battery,range,vehicle_status,service_days_left,service_km_left,ta_distance,tm_distance,odometer";
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
  private static final android.util.LruCache<String, Bitmap> BACKGROUNDS = new android.util.LruCache<String, Bitmap>(2 * 1024 * 1024) {
    @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
  };
  static synchronized Bitmap background(Context c, Size size) {
    boolean dark = AppPreferences.dark(c);
    boolean opaque = "opaque".equals(AppPreferences.get(c, "widget_style", "transparent"));
    int opacity = opaque ? 100 : AppPreferences.slider(c, "widget_opacity", 58);
    int frost = AppPreferences.slider(c, "widget_frost", 28);
    String cacheKey = size + ":" + dark + ":" + opaque + ":" + opacity + ":" + frost;
    Bitmap cached = BACKGROUNDS.get(cacheKey);
    if (cached != null) return cached;
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
    BACKGROUNDS.put(cacheKey, bitmap);
    return bitmap;
  }
}

