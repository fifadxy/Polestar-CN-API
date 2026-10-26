package io.github.polestarcn.companion;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public final class WidgetRenderer {
  private WidgetRenderer() {}

  public static void refreshAll(Context context) {
    AppWidgetManager m = AppWidgetManager.getInstance(context);
    for (int id :
        m.getAppWidgetIds(new android.content.ComponentName(context, VehicleWidgetProvider.class)))
      update(context, m, id, false);
    for (int id :
        m.getAppWidgetIds(new android.content.ComponentName(context, CompactWidgetProvider.class)))
      update(context, m, id, true);
  }

  public static void update(
      Context context, AppWidgetManager manager, int widgetId, boolean compact) {
    Region region = AppState.region(context);
    if (region == null) region = Region.CHINA;
    VehicleSnapshot snapshot =
        region == Region.CHINA ? VehicleSnapshotStore.loadChina(context) : null;
    if (snapshot == null)
      snapshot =
          new VehicleSnapshot(
              "Polestar",
              "",
              null,
              null,
              null,
              "Unknown",
              "Unknown",
              "Unknown",
              "Unknown",
              "Unknown",
              0L,
              "OFFLINE",
              false);
    TelemetryStatus status =
        TelemetryStatus.classify(
            snapshot.isDemo,
            snapshot.source,
            snapshot.timestampMillis,
            System.currentTimeMillis(),
            15L * 60L * 1000L);
    RemoteViews views =
        new RemoteViews(
            context.getPackageName(), compact ? R.layout.widget_compact : R.layout.widget_standard);
    views.setTextViewText(
        R.id.widget_title,
        compact ? "POLESTAR · " + status.name() : "POLESTAR COMPANION · " + status.name());
    views.setTextViewText(R.id.widget_soc, VehicleSnapshot.metric(snapshot.batteryPercent, "%"));
    views.setTextViewText(R.id.widget_range, VehicleSnapshot.metric(snapshot.rangeKm, " km"));
    views.setTextViewText(
        R.id.widget_region,
        "China · "
            + (snapshot.timestampMillis == 0
                ? "Not connected"
                : android.text.format.DateFormat.format("MM-dd HH:mm", snapshot.timestampMillis)));
    Intent intent = new Intent(context, MainActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent open =
        PendingIntent.getActivity(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    views.setOnClickPendingIntent(R.id.widget_root, open);
    manager.updateAppWidget(widgetId, views);
  }
}
