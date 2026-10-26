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
  public enum Size { LARGE, SQUARE, WIDE, MINI }
  private WidgetRenderer() {}

  public static void refreshAll(Context context) {
    AppWidgetManager m = AppWidgetManager.getInstance(context);
    refresh(context, m, VehicleWidgetProvider.class, Size.LARGE);
    refresh(context, m, CompactWidgetProvider.class, Size.SQUARE);
    refresh(context, m, WideStripWidgetProvider.class, Size.WIDE);
    refresh(context, m, MiniStripWidgetProvider.class, Size.MINI);
  }
  private static void refresh(Context c, AppWidgetManager m, Class<?> receiver, Size size) {
    for (int id : m.getAppWidgetIds(new ComponentName(c, receiver))) update(c, m, id, size);
  }

  public static void update(Context c, AppWidgetManager m, int id, Size size) {
    VehicleSnapshot s = AppState.region(c) == Region.CHINA ? VehicleSnapshotStore.loadChina(c) : null;
    TelemetryStatus state = s == null ? TelemetryStatus.UNKNOWN : TelemetryStatus.classify(
        s.isDemo, s.source, s.timestampMillis, System.currentTimeMillis(), 15 * 60 * 1000L);
    int layout = size == Size.LARGE ? R.layout.widget_standard :
        size == Size.SQUARE ? R.layout.widget_compact :
        size == Size.WIDE ? R.layout.widget_wide : R.layout.widget_mini;
    RemoteViews views = new RemoteViews(c.getPackageName(), layout);
    views.setImageViewBitmap(R.id.widget_bg, background(c, size));
    boolean en = AppPreferences.widgetEnglish(c);
    String updated = s == null || s.timestampMillis <= 0
        ? AppPreferences.widgetTr(c, "未连接", "Not connected")
        : (state == TelemetryStatus.STALE ? AppPreferences.widgetTr(c, "已过期", "Stale") + " · " : "")
        + DateFormat.format("MM-dd HH:mm", s.timestampMillis);
    views.setTextViewText(R.id.widget_title,
        (s == null ? "PsAssistant" : s.name) + (state == TelemetryStatus.STALE ? " · !" : ""));
    views.setTextViewText(R.id.widget_soc,
        s == null ? "--%" : VehicleSnapshot.metric(s.batteryPercent, "%"));
    views.setTextViewText(R.id.widget_range,
        s == null ? "-- km" : VehicleSnapshot.metric(s.rangeKm, " km"));
    views.setTextViewText(R.id.widget_region, updated);
    if (size == Size.LARGE) {
      views.setTextViewText(R.id.widget_detail,
          s == null ? AppPreferences.widgetTr(c, "无车辆数据", "No vehicle data")
          : AppPreferences.widgetTr(c, "充电 ", "Charge ") + AppPreferences.widgetStatus(c, s.charging)
            + "  ·  " + AppPreferences.widgetTr(c, "空调 ", "Climate ")
            + ( "开启".equals(s.climate) ? AppPreferences.widgetTr(c, "运行中", "Running")
                : AppPreferences.widgetStatus(c, s.climate)));
    }
    views.setTextViewText(R.id.widget_action_lock, en ? "LOCK" : "车锁");
    views.setTextViewText(R.id.widget_action_climate, en ? "CLIMATE" : "空调");
    views.setOnClickPendingIntent(R.id.widget_summary, pending(c, id, size, "open"));
    views.setOnClickPendingIntent(R.id.widget_action_lock, pending(c, id, size, "lock-choice"));
    views.setOnClickPendingIntent(R.id.widget_action_climate, pending(c, id, size, "climate-choice"));
    m.updateAppWidget(id, views);
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
    boolean opaque = "opaque".equals(AppPreferences.get(c, "widget_style", "opaque"));
    int opacity = opaque ? 100 : AppPreferences.slider(c, "widget_opacity", 65);
    int frost = AppPreferences.slider(c, "widget_frost", 45);
    int w = size == Size.MINI ? 190 : size == Size.SQUARE ? 190 : 480;
    int h = size == Size.LARGE ? 205 : size == Size.SQUARE ? 205 : 108;
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

