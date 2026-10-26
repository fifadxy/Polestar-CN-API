package io.github.polestarcn.companion;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
public final class WideStripWidgetProvider extends AppWidgetProvider {
  @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
    for (int id : ids) WidgetRenderer.update(c, m, id, WidgetRenderer.Size.WIDE);
  }
  @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle opts) {
    WidgetRenderer.update(c, m, id, WidgetRenderer.Size.WIDE);
  }
  @Override public void onDisabled(Context context) { ResidentService.scheduleUpdates(context); }
}

