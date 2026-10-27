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

  static RemoteViews createViews(Context c, int id, Size size, int width, int height,
      Bitmap backing, VehicleSnapshot snapshot) {
    c=c.getApplicationContext();
    VehicleSnapshot s = AppState.region(c) == Region.CHINA ? snapshot : null;
    int layout = size == Size.DATA ? R.layout.widget_data : size == Size.LARGE ? R.layout.widget_standard
        : size == Size.SQUARE ? R.layout.widget_compact : size == Size.WIDE ? R.layout.widget_wide : R.layout.widget_mini;
    RemoteViews views = new RemoteViews(c.getPackageName(), layout);
    views.setImageViewBitmap(R.id.widget_bg, backing);
    boolean en = AppPreferences.widgetEnglish(c);
    boolean nio = UiSkin.nioLayout(c);
    boolean nioColors = UiSkin.nioColors(c);
    boolean includeTires = AppPreferences.visible(c, "show_tire_status", true);
    boolean showCharging = ChargingVisibility.shouldShow(s);
    java.util.List<DisplayMetric> selected = MetricSelection.read(c, metricKey(size), defaults(size));
    java.util.List<DisplayMetric> main = new java.util.ArrayList<>(), charging = new java.util.ArrayList<>();
    for (DisplayMetric metric : selected) {
      if (metric == DisplayMetric.TIRE_STATUS && !includeTires) continue;
      if (ChargingVisibility.isChargingMetric(metric)) { if (showCharging) charging.add(metric); }
      else main.add(metric);
    }
    // Preserve saved selection order; prioritize the shared battery/range cluster for display only.
    if (main.remove(DisplayMetric.RANGE)) main.add(0, DisplayMetric.RANGE);
    if (main.remove(DisplayMetric.BATTERY)) main.add(0, DisplayMetric.BATTERY);
    boolean battery = main.contains(DisplayMetric.BATTERY);
    boolean serviceSelected=selected.contains(DisplayMetric.VEHICLE_STATUS);
    Integer timeLeft=ServiceReading.timePercent(c,s),distanceLeft=ServiceReading.distancePercent(c,s);
    boolean serviceTime=serviceSelected&&height>=135&&timeLeft!=null&&AppPreferences.visible(c,"condition_service_time",false);
    boolean serviceDistance=serviceSelected&&height>=135&&distanceLeft!=null&&AppPreferences.visible(c,"condition_service_distance",false);
    boolean serviceBand=serviceTime||serviceDistance;
    float requestedFont=widgetFont(c,size,height);
    int serviceReserve=serviceBand?(int)Math.ceil(16*c.getResources().getConfiguration().fontScale*requestedFont)+3:0;
    WidgetLayoutPlan plan = new WidgetLayoutPlan(size, main.size(), charging.size(), battery,
        width, height-serviceReserve, c.getResources().getConfiguration().fontScale*requestedFont);
    float font=widgetFont(c,size,height);
    float density = c.getResources().getDisplayMetrics().density;
    int padding = Math.round(plan.padding * density);
    views.setViewPadding(R.id.widget_content, padding, padding, padding, padding);
    String preference = AppPreferences.get(c, "widget_ink", "system");
    boolean white = "light".equals(preference) || ("system".equals(preference) && AppPreferences.dark(c));
    int ink = PsaDesign.ink(white), secondary = PsaDesign.muted(white);
    int accentText = nioColors ? ink : PsaDesign.accentText(white);
    views.setViewVisibility(R.id.widget_title, plan.title ? android.view.View.VISIBLE : android.view.View.GONE);
    views.setTextViewText(R.id.widget_title, s == null ? c.getString(R.string.app_name) : s.name);
    views.setTextColor(R.id.widget_title, secondary);
    views.setTextViewTextSize(R.id.widget_title,android.util.TypedValue.COMPLEX_UNIT_SP,11*font);
    views.setTextViewTextSize(R.id.widget_lock_label,android.util.TypedValue.COMPLEX_UNIT_SP,10*font);
    views.setTextViewTextSize(R.id.widget_climate_label,android.util.TypedValue.COMPLEX_UNIT_SP,10*font);
    int[] ids = {R.id.widget_metric0, R.id.widget_metric1, R.id.widget_metric2, R.id.widget_metric3,
        R.id.widget_metric4, R.id.widget_metric5, R.id.widget_metric6, R.id.widget_metric7,
        R.id.widget_metric8, R.id.widget_metric9, R.id.widget_metric10, R.id.widget_metric11};
    int[] rows = {R.id.widget_metric_row0, R.id.widget_metric_row1, R.id.widget_metric_row2,
        R.id.widget_metric_row3, R.id.widget_metric_row4, R.id.widget_metric_row5};
    java.util.List<DisplayMetric> shown = new java.util.ArrayList<>();
    for (int i = 0; i < ids.length; i++) {
      int visibility = i < plan.count ? android.view.View.VISIBLE : plan.columns == 1
          ? android.view.View.GONE : android.view.View.INVISIBLE;
      views.setViewVisibility(ids[i], visibility);
      if (i < plan.count) {
        DisplayMetric metric = main.get(i); shown.add(metric);
        views.setTextViewTextSize(ids[i], android.util.TypedValue.COMPLEX_UNIT_SP, 10*font);
        views.setInt(ids[i], "setMaxLines", plan.stacked ? 2 : 1);
        views.setTextColor(ids[i], ink);
        views.setTextViewText(ids[i], metricText(c, metric, s, en, plan, ink, secondary, accentText,font));
        views.setContentDescription(ids[i], metric.label(en) + " · " + metricValue(c, metric, s, true, en));
      }
    }
    for (int i = 0; i < rows.length; i++)
      views.setViewVisibility(rows[i], i < plan.rows && plan.count > 0 ? android.view.View.VISIBLE : android.view.View.GONE);
    views.setViewVisibility(R.id.widget_metrics, plan.count > 0 ? android.view.View.VISIBLE : android.view.View.GONE);
    boolean progress = plan.progress && plan.count > 0 && main.get(0) == DisplayMetric.BATTERY;
    views.setViewVisibility(R.id.widget_soc_progress, progress ? android.view.View.VISIBLE : android.view.View.GONE);
    if (progress) {
      Integer soc = s == null ? null : s.batteryPercent;
      views.setImageViewBitmap(R.id.widget_soc_progress, batteryBar(soc,
          nioColors ? Color.rgb(32,196,121) : PsaDesign.ORANGE, white));
      views.setContentDescription(R.id.widget_soc_progress, DisplayMetric.BATTERY.label(en) + " · "
          + (soc == null ? "--" : soc + "%"));
    }
    ChargeContent chargeContent = chargeContent(c, s, charging, en, plan, width, density, ink, secondary,font);
    boolean chargeVisible = !chargeContent.shown.isEmpty();
    views.setViewVisibility(R.id.widget_charging_section, chargeVisible ? android.view.View.VISIBLE : android.view.View.GONE);
    views.setViewVisibility(R.id.widget_charging_title, chargeVisible && plan.chargeTitle ? android.view.View.VISIBLE : android.view.View.GONE);
    views.setViewVisibility(R.id.widget_charging_icon, plan.chargeTitle ? android.view.View.GONE : android.view.View.VISIBLE);
    views.setTextViewText(R.id.widget_charging_title, AppPreferences.widgetTr(c, "充电", "Charging"));
    views.setTextColor(R.id.widget_charging_title, secondary);
    views.setInt(R.id.widget_charging_icon, "setColorFilter", ink);
    views.setTextViewTextSize(R.id.widget_charging_data, android.util.TypedValue.COMPLEX_UNIT_SP, 12*font);
    views.setInt(R.id.widget_charging_data, "setMaxLines", Math.max(1, plan.chargeRows));
    views.setTextViewText(R.id.widget_charging_data, chargeContent.text);
    views.setTextColor(R.id.widget_charging_data, ink);
    StringBuilder chargeDescription = new StringBuilder(AppPreferences.widgetTr(c, "充电", "Charging"));
    for (DisplayMetric metric : chargeContent.shown)
      chargeDescription.append(" · ").append(metric.label(en)).append(" ").append(metricValue(c, metric, s, true, en));
    views.setContentDescription(R.id.widget_charging_section, chargeDescription.toString());
    shown.addAll(chargeContent.shown);
    views.setViewVisibility(R.id.widget_service_band,serviceBand?android.view.View.VISIBLE:android.view.View.GONE);
    views.setViewVisibility(R.id.widget_service_time_cell,serviceTime?android.view.View.VISIBLE:android.view.View.GONE);
    views.setViewVisibility(R.id.widget_service_distance_cell,serviceDistance?android.view.View.VISIBLE:android.view.View.GONE);
    for(int metric=0;metric<2;metric++) {
      int text=metric==0?R.id.widget_service_time_text:R.id.widget_service_distance_text;
      int bar=metric==0?R.id.widget_service_time_bar:R.id.widget_service_distance_bar;
      Integer percent=metric==0?timeLeft:distanceLeft;
      views.setTextViewText(text,(metric==0?"d ":"km ")+ServiceReading.number(percent,"%"));
      views.setTextColor(text,secondary);views.setTextViewTextSize(text,android.util.TypedValue.COMPLEX_UNIT_SP,10*font);
      if(percent!=null)views.setImageViewBitmap(bar,batteryBar(percent,PsaDesign.ORANGE,white));
    }
    views.setContentDescription(R.id.widget_service_band,AppPreferences.widgetTr(c,"保养剩余比例（按设置周期估算） · ","Service remaining (configured cycle estimate) · ")
        +(serviceTime?"d "+timeLeft+"% ":"")+(serviceDistance?"km "+distanceLeft+"%":""));
    java.util.List<DisplayMetric> timestampFields=new java.util.ArrayList<>(shown);
    if(serviceTime)timestampFields.add(DisplayMetric.SERVICE_TIME_PERCENT);if(serviceDistance)timestampFields.add(DisplayMetric.SERVICE_KM_PERCENT);
    String updated = updatedText(c,s,timestampFields,size);
    int hidden = Math.max(0, Math.min(capacity(size), main.size() + charging.size()) - shown.size());
    if (hidden > 0) updated += " · +" + hidden;
    views.setViewVisibility(R.id.widget_region, android.view.View.VISIBLE);
    views.setTextViewText(R.id.widget_region, updated);
    views.setTextColor(R.id.widget_region, secondary);
    views.setTextViewTextSize(R.id.widget_region, android.util.TypedValue.COMPLEX_UNIT_SP, 10*font);
    views.setContentDescription(R.id.widget_region, updated + AppPreferences.widgetTr(c, "，点击刷新", ". Tap to refresh"));
    views.setOnClickPendingIntent(R.id.widget_region, pending(c, id, size, "refresh"));
    views.setOnClickPendingIntent(R.id.widget_refresh_row,pending(c,id,size,"refresh"));
    views.setInt(R.id.widget_refresh_icon,"setColorFilter",secondary);
    boolean lockKnown = s != null && ("未锁".equals(s.lock) || "已上锁".equals(s.lock) || "已锁".equals(s.lock));
    action(views, c, true, s != null && "未锁".equals(s.lock), ink,
        !lockKnown ? AppPreferences.widgetTr(c, "车锁", "Lock")
          : en ? ("未锁".equals(s.lock) ? "Lock" : "Unlock") : ("未锁".equals(s.lock) ? "上锁" : "解锁"));
    action(views, c, false, s != null && "开启".equals(s.climate), ink,
        en ? (s != null && "开启".equals(s.climate) ? "Stop" : "Climate") : (s != null && "开启".equals(s.climate) ? "关闭" : "空调"));
    views.setOnClickPendingIntent(R.id.widget_summary, pending(c, id, size, "open"));
    views.setOnClickPendingIntent(R.id.widget_action_lock, pending(c, id, size, "lock-choice"));
    views.setOnClickPendingIntent(R.id.widget_action_climate, pending(c, id, size, "climate-choice"));
    return views;
  }

  private static float widgetFont(Context c,Size size,int height) {
    float requested=UiTypography.widgetScale(c);
    if(size==Size.MINI) return Math.min(requested,Math.max(.8f,(height-50f)/(26f*c.getResources().getConfiguration().fontScale)));
    return requested;
  }

  private static String metricValue(Context c, DisplayMetric metric, VehicleSnapshot s,
      boolean unit, boolean en) {
    if (s != null && metric == DisplayMetric.VEHICLE_STATUS)
      return VehicleCondition.summary(s,en,AppPreferences.visible(c,"show_tire_status",true));
    return metric.value(c, s, unit, en);
  }

  private static Bitmap batteryBar(Integer percent, int accent, boolean dark) {
    Bitmap bitmap = Bitmap.createBitmap(256, 2, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap); Paint paint = new Paint();
    paint.setColor(dark ? Color.rgb(77,80,83) : Color.rgb(205,208,208));
    canvas.drawRect(0, 0, 256, 2, paint);
    if (percent != null) {
      paint.setColor(accent);
      canvas.drawRect(0, 0, 256f * Math.max(0, Math.min(100, percent)) / 100f, 2, paint);
    }
    return bitmap;
  }

  private static final class ChargeContent {
    final CharSequence text; final java.util.List<DisplayMetric> shown;
    ChargeContent(CharSequence text, java.util.List<DisplayMetric> shown) { this.text = text; this.shown = shown; }
  }

  private static ChargeContent chargeContent(Context c, VehicleSnapshot s,
      java.util.List<DisplayMetric> selected, boolean en, WidgetLayoutPlan plan, int width,
      float density, int ink, int secondary,float font) {
    java.util.List<DisplayMetric> shown = new java.util.ArrayList<>();
    android.text.SpannableStringBuilder text = new android.text.SpannableStringBuilder();
    if (plan.chargeRows == 0 || selected.isEmpty()) return new ChargeContent(text, shown);
    float maxWidth = (width - 2f * plan.padding - (plan.wide ? 104 : 0)
        - (plan.chargeTitle ? 0 : 20)) * density;
    android.text.TextPaint valuePaint = new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
    valuePaint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
    valuePaint.setTextSize(12 * c.getResources().getDisplayMetrics().scaledDensity*font);
    android.text.TextPaint labelPaint = new android.text.TextPaint(valuePaint);
    labelPaint.setTextSize(10 * c.getResources().getDisplayMetrics().scaledDensity*font);
    int line = 0; float used = 0;
    for (DisplayMetric metric : selected) {
      String value = metricValue(c, metric, s, true, en);
      value = android.text.TextUtils.ellipsize(value, valuePaint, maxWidth,
          android.text.TextUtils.TruncateAt.END).toString();
      String label = chargeLabel(metric, en);
      float valueWidth = valuePaint.measureText(value);
      label = android.text.TextUtils.ellipsize(label, labelPaint,
          Math.max(0, maxWidth - valueWidth - 4 * density), android.text.TextUtils.TruncateAt.END).toString();
      float tokenWidth = valueWidth + (label.isEmpty() ? 0 : labelPaint.measureText(label) + 4 * density);
      if (used > 0 && used + 10 * density + tokenWidth > maxWidth) {
        if (line + 1 >= plan.chargeRows) break;
        text.append('\n'); line++; used = 0;
      }
      if (used > 0) { text.append(" · "); used += 10 * density; }
      int start = text.length();
      if (!label.isEmpty()) {
        text.append(label).append(' ');
        text.setSpan(new android.text.style.RelativeSizeSpan(10f / 12f), start, text.length(), 0);
        text.setSpan(new android.text.style.ForegroundColorSpan(secondary), start, text.length(), 0);
      }
      start = text.length(); text.append(value);
      text.setSpan(new android.text.style.ForegroundColorSpan(ink), start, text.length(), 0);
      used += tokenWidth; shown.add(metric);
    }
    return new ChargeContent(text, shown);
  }

  private static String chargeLabel(DisplayMetric metric, boolean en) {
    switch (metric) {
      case CHARGE_CONNECTION: return en ? "Plug" : "连接";
      case CHARGE_TYPE: return en ? "Type" : "类型";
      case CHARGE_POWER: return en ? "Power" : "功率";
      case CHARGE_CURRENT: return en ? "Current" : "电流";
      case CHARGE_ETA: return en ? "Full" : "充满";
      case POWER_STATUS: return en ? "Supply" : "电源";
      default: return metric.shortLabel(en);
    }
  }

  private static void action(RemoteViews views, Context c, boolean lock, boolean active, int ink, String label) {
    int root = lock ? R.id.widget_action_lock : R.id.widget_action_climate;
    int icon = lock ? R.id.widget_lock_icon : R.id.widget_climate_icon;
    int text = lock ? R.id.widget_lock_label : R.id.widget_climate_label;
    boolean nio = UiSkin.nioLayout(c);
    boolean nioColors = UiSkin.nioColors(c);
    boolean transparent = !"opaque".equals(AppPreferences.get(c, "widget_style", "transparent"));
    views.setInt(root, "setBackgroundResource", nio ? 0 : active ? R.drawable.widget_action_on
        : transparent ? R.drawable.widget_action_clear : R.drawable.widget_action_off);
    if (nio) views.setInt(icon, "setBackgroundResource", active
        ? lock || !nioColors ? R.drawable.widget_action_nio_lock_active : R.drawable.widget_action_nio_climate_active
        : AppPreferences.dark(c) ? R.drawable.widget_action_nio_dark : R.drawable.widget_action_nio_light);
    int color = active ? Color.rgb(21,22,23) : ink;
    views.setInt(icon, "setColorFilter", color); views.setTextColor(text, nio ? ink : color);
    views.setTextViewText(text, label);
    views.setContentDescription(root, AppPreferences.widgetTr(c, lock ? "门锁 · " : "空调 · ", lock ? "Door lock · " : "Climate · ")
        + label + AppPreferences.widgetTr(c, "，点击后生物验证", ". Verify biometrics to control"));
  }

  private static String updatedText(Context c, VehicleSnapshot s, java.util.List<DisplayMetric> metrics, Size size) {
    if (s == null) return AppPreferences.widgetTr(c, "尚无数据", "No data");
    long at = Long.MAX_VALUE;
    boolean unknown = false;
    for (DisplayMetric metric : metrics) {
      if ("--".equals(metricValue(c, metric, s, false, AppPreferences.widgetEnglish(c)))) continue;
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
      boolean en, WidgetLayoutPlan plan, int ink, int secondary, int accentText,float font) {
    float scale = c.getResources().getDisplayMetrics().scaledDensity*font;
    float width = plan.cellWidthDp * c.getResources().getDisplayMetrics().density;
    String value = metricValue(c, metric, s, false, en);
    if (!plan.stacked && (metric == DisplayMetric.BATTERY || metric == DisplayMetric.RANGE
        || metric == DisplayMetric.TEMPERATURE || metric == DisplayMetric.TARGET_TEMPERATURE))
      value = metricValue(c, metric, s, true, en);
    float valueSp = plan.valueSp;
    android.text.TextPaint paint = new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
    paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
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
    boolean accent = metric == DisplayMetric.BATTERY || metric == DisplayMetric.VEHICLE_STATUS && s != null
        && (s.health.allGood(AppPreferences.visible(c, "show_tire_status", true))
            || s.health.hasProblem(AppPreferences.visible(c, "show_tire_status", true)))
        || metric == DisplayMetric.TIRE_STATUS && s != null
        && (s.tires.allGood() || s.tires.hasProblem());
    styled.setSpan(new android.text.style.ForegroundColorSpan(accent ? accentText : ink), valueStart, text.length(), 0);
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
      case TEMPERATURE: return en ? "Cabin" : "车内";
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
    boolean nio = UiSkin.nioLayout(c);
    boolean nioColors = UiSkin.nioColors(c);
    boolean opaque = "opaque".equals(AppPreferences.get(c, "widget_style", "transparent"));
    int opacity = opaque ? 100 : AppPreferences.slider(c, "widget_opacity", 58);
    int frost = AppPreferences.slider(c, "widget_frost", 28);
    String cacheKey = size + ":" + dark + ":" + nioColors + ":" + opaque + ":" + opacity + ":" + frost;
    Bitmap cached = BACKGROUNDS.get(cacheKey);
    if (cached != null) return cached;
    int w = size == Size.MINI ? 190 : size == Size.SQUARE ? 190 : 480;
    int h = size == Size.DATA ? 350 : size == Size.LARGE ? 205 : size == Size.SQUARE ? 205 : 108;
    Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    int alpha = (int) Math.round(opacity * 2.55);
    int base = nioColors ? Color.parseColor(dark ? "#181D25" : "#F5F0F0") : PsaDesign.surface(dark);
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

