package io.github.polestarcn.companion;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public final class WidgetRenderer {
    private WidgetRenderer() {}
    public static void update(Context context, AppWidgetManager manager, int widgetId, boolean compact) {
        Region region = AppState.region(context);
        if (region == null) region = Region.CHINA;
        VehicleSnapshot snapshot = new DemoRepository().snapshot(region);
        RemoteViews views = new RemoteViews(context.getPackageName(),
            compact ? R.layout.widget_compact : R.layout.widget_standard);
        views.setTextViewText(R.id.widget_title, compact ? "POLESTAR · DEMO" : "POLESTAR COMPANION · DEMO");
        views.setTextViewText(R.id.widget_soc, VehicleSnapshot.metric(snapshot.batteryPercent, "%"));
        views.setTextViewText(R.id.widget_range, VehicleSnapshot.metric(snapshot.rangeKm, " km"));
        views.setTextViewText(R.id.widget_region, region.english + " · OFFLINE");
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent open = PendingIntent.getActivity(context, widgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, open);
        manager.updateAppWidget(widgetId, views);
    }
}
