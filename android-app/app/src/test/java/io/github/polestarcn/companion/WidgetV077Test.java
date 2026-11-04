package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28, qualifiers="w360dp-h780dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class WidgetV077Test {
  private Context c;
  private Configuration original;
  @Before public void setup() {
    c=RuntimeEnvironment.getApplication();
    original=new Configuration(c.getResources().getConfiguration());
    c.getSharedPreferences("preferences",0).edit().clear().commit();
    AppState.setRegion(c,Region.CHINA);
    AppPreferences.set(c,"widget_language","en");
  }
  @After public void restore() { setFont(original.fontScale); }
  private void setFont(float scale) {
    Configuration config=new Configuration(c.getResources().getConfiguration());config.fontScale=scale;
    android.util.DisplayMetrics metrics=c.getResources().getDisplayMetrics();metrics.scaledDensity=metrics.density*scale;
    c.getResources().updateConfiguration(config,metrics);
  }
  private VehicleSnapshot snapshot(String charging) {
    return new VehicleSnapshot("Polestar 2","masked",63,230,65845,charging,
      "全部关闭","全部关闭","已关闭","已上锁",System.currentTimeMillis(),"LIVE",false,"关闭","关闭")
      .withCharging(new ChargingSnapshot(1,2,4,180,6000.0,29.0))
      .withHealth(new VehicleHealthSnapshot(1,1,VehicleHealthSnapshot.UNKNOWN,System.currentTimeMillis()));
  }
  private View render(WidgetRenderer.Size size,int w,int h,VehicleSnapshot s) {
    View root=WidgetRenderer.createViews(c,17,size,w,h,WidgetRenderer.background(c,size),s)
      .apply(c,new FrameLayout(c));
    root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));
    root.layout(0,0,w,h);return root;
  }
  private void textFits(View v) {
    if(v.getVisibility()!=View.VISIBLE)return;
    if(v instanceof TextView){TextView t=(TextView)v;
      if(t.getText().length()>0){assertNotNull(t.getLayout());assertTrue(t.getText()+" / "+t.getHeight(),
        t.getLayout().getHeight()<=t.getHeight()-t.getCompoundPaddingTop()-t.getCompoundPaddingBottom());}
    }
    if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++)textFits(group.getChildAt(i));}
  }
  @Test public void batteryBarStaysBelowSocWithRangeBesideItAtSmallestScaledSize() {
    setFont(1.3f);
    for(String skin:new String[]{UiSkin.SIMPLE,UiSkin.DETAILED,UiSkin.NIO_LIKE}) {
      UiSkin.set(c,skin);
      View root=render(WidgetRenderer.Size.MINI,118,88,snapshot("空闲"));
      TextView soc=root.findViewById(R.id.widget_metric0), range=root.findViewById(R.id.widget_metric1);
      View bar=root.findViewById(R.id.widget_soc_progress);
      assertEquals(View.VISIBLE,bar.getVisibility());assertEquals(2,bar.getHeight());
      assertSame(soc.getParent(),bar.getParent());assertTrue(bar.getTop()>=soc.getBottom());
      assertTrue(range.getLeft()>=((View)soc.getParent()).getRight());
      TextView stamp=root.findViewById(R.id.widget_region);Rect visible=new Rect();
      assertTrue(stamp.getGlobalVisibleRect(visible));assertEquals(stamp.getHeight(),visible.height());
      assertTrue(root.findViewById(R.id.widget_action_lock).getHeight()>=48);
      assertTrue(root.findViewById(R.id.widget_action_climate).getHeight()>=48);
      textFits(root);
      int color=((BitmapDrawable)((ImageView)bar).getDrawable()).getBitmap().getPixel(1,0);
      assertEquals(UiSkin.NIO_LIKE.equals(skin)?Color.rgb(32,196,121):PsaDesign.ORANGE,color);
    }
  }
  @Test public void chargingUsesSeparateBandAndIdleDoesNotEraseSavedSelection() {
    String key=WidgetRenderer.metricKey(WidgetRenderer.Size.LARGE);
    AppPreferences.set(c,key,"battery,range,charge_power,charge_current");
    List<DisplayMetric> before=MetricSelection.read(c,key,"");
    View idle=render(WidgetRenderer.Size.LARGE,300,170,snapshot("空闲"));
    assertEquals(View.GONE,idle.findViewById(R.id.widget_charging_section).getVisibility());
    assertEquals(before,MetricSelection.read(c,key,""));
    View active=render(WidgetRenderer.Size.LARGE,300,170,snapshot("充电中"));
    View section=active.findViewById(R.id.widget_charging_section);
    assertEquals(View.VISIBLE,section.getVisibility());
    assertNotSame(active.findViewById(R.id.widget_metrics),section.getParent());
    assertTrue(section.getContentDescription().toString().contains("6.0 kW"));
    assertTrue(section.getContentDescription().toString().contains("29.0 A"));
    assertEquals(before,MetricSelection.read(c,key,""));textFits(active);
    View finished=render(WidgetRenderer.Size.LARGE,300,170,snapshot("已完成"));
    assertEquals(View.GONE,finished.findViewById(R.id.widget_charging_section).getVisibility());
  }
  @Test public void hidingTyresExcludesThemFromAggregateWithoutChangingSavedFields() {
    String key=WidgetRenderer.metricKey(WidgetRenderer.Size.SQUARE);
    AppPreferences.set(c,key,"tire_status,vehicle_status,battery");
    AppPreferences.set(c,"show_tire_status","off");
    View root=render(WidgetRenderer.Size.SQUARE,150,170,snapshot("空闲"));
    assertEquals("tire_status,vehicle_status,battery",AppPreferences.get(c,key,""));
    TextView aggregate=root.findViewById(R.id.widget_metric1);
    assertTrue(aggregate.getContentDescription().toString().contains("All Good！"));
    assertTrue(((TextView)root.findViewById(R.id.widget_metric0)).getText().toString().contains("63"));
    textFits(root);
  }
}
