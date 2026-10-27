package io.github.polestarcn.companion;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;
/** Native five-column widget, independently selectable and resizable. */
public final class FiveWideWidgetProvider extends AppWidgetProvider {
  @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
    for (int id : ids) WidgetRenderer.update(c, m, id, WidgetRenderer.Size.FIVE_WIDE);
  }
  @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle options) {
    WidgetRenderer.update(c, m, id, WidgetRenderer.Size.FIVE_WIDE,options);
  }
  @Override public void onDisabled(Context context) { ResidentService.scheduleUpdates(context); }
}
