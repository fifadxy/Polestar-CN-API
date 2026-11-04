package io.github.polestarcn.companion;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;

public final class DataWidgetProvider extends AppWidgetProvider {
  @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
    for (int id : ids) WidgetRenderer.update(c, m, id, WidgetRenderer.Size.DATA);
  }
  @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle options) {
    WidgetRenderer.update(c, m, id, WidgetRenderer.Size.DATA,options);
  }
  @Override public void onDisabled(Context context) { ResidentService.scheduleUpdates(context); }
}
