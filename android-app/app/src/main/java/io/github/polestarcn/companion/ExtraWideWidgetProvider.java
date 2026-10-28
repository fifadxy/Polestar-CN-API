package io.github.polestarcn.companion;
import android.appwidget.*;import android.content.Context;import android.os.Bundle;
public final class ExtraWideWidgetProvider extends AppWidgetProvider {
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)WidgetRenderer.update(c,m,id,WidgetRenderer.Size.EXTRA_WIDE);}
 @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){WidgetRenderer.update(c,m,id,WidgetRenderer.Size.EXTRA_WIDE,options);}
 @Override public void onDisabled(Context c){ResidentService.scheduleUpdates(c);}
}
