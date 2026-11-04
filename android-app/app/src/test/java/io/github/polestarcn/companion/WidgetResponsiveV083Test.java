package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.appwidget.*;
import android.content.Context;
import android.os.Bundle;
import android.util.SizeF;
import android.view.View;
import android.widget.TextView;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class WidgetResponsiveV083Test {
 @Test public void modernLauncherExactSizesRenderAndResizeWithoutReplacingTextWithBitmap(){
  Context context=RuntimeEnvironment.getApplication();context.getSharedPreferences("preferences",0).edit().clear().commit();AppPreferences.set(context,"widget_design",VoLikeWidgetRenderer.PSEU);AppState.setRegion(context,Region.CHINA);
  AppWidgetManager manager=AppWidgetManager.getInstance(context);org.robolectric.shadows.ShadowAppWidgetManager host=Shadows.shadowOf(manager);int id=host.createWidget(VehicleWidgetProvider.class,R.layout.widget_standard);
  Bundle sizes=new Bundle();sizes.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,new ArrayList<>(Arrays.asList(new SizeF(300,170),new SizeF(420,90))));
  new VehicleWidgetProvider().onAppWidgetOptionsChanged(context,manager,id,sizes);View root=host.getViewFor(id);assertNotNull(root);assertTrue(root.findViewById(R.id.widget_metric0) instanceof TextView);assertNotNull(root.findViewById(R.id.widget_action_lock));
 }
 @Test public void legacyLauncherReportedOptionsRemainAValidFallbackOnModernAndroid(){
  Context context=RuntimeEnvironment.getApplication();context.getSharedPreferences("preferences",0).edit().clear().commit();AppWidgetManager manager=AppWidgetManager.getInstance(context);org.robolectric.shadows.ShadowAppWidgetManager host=Shadows.shadowOf(manager);int id=host.createWidget(MiniStripWidgetProvider.class,R.layout.widget_mini);
  Bundle sizes=new Bundle();sizes.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,118);sizes.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,269);sizes.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,51);sizes.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,88);
  new MiniStripWidgetProvider().onAppWidgetOptionsChanged(context,manager,id,sizes);assertNotNull(host.getViewFor(id).findViewById(R.id.widget_region));
 }
}
