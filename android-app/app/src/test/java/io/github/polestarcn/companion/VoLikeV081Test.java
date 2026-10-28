package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.app.AlertDialog;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.view.*;
import android.widget.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,qualifiers="w360dp-h780dp-mdpi")
public class VoLikeV081Test {
  private AppRegressionTest base;
  private MainActivity activity;
  private VehicleCoordinator model;
  @Before public void setUp()throws Exception{
    base=new AppRegressionTest();base.start();
    Field f=AppRegressionTest.class.getDeclaredField("activity");f.setAccessible(true);activity=(MainActivity)f.get(base);
    model=VehicleCoordinator.get(activity);
    Field signed=VehicleCoordinator.class.getDeclaredField("signedIn");signed.setAccessible(true);signed.setBoolean(model,true);
    model.selected=new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();model.selected.vin="FIXTURE-V081";
    model.snapshot=snapshot("空闲");
  }
  @After public void tearDown(){base.stop();}
  private VehicleSnapshot snapshot(String charge) {
    return new VehicleSnapshot("Polestar 2","masked",78,312,15000,charge,
        "全部关闭","全部关闭","已关闭","已上锁",System.currentTimeMillis(),"LIVE",false,
        "关闭","关闭").withCharging(new ChargingSnapshot(1,2,"充电中".equals(charge)?4:1,80,6000.0,25.0));
  }
  private View render()throws Exception{
    Method m=MainActivity.class.getDeclaredMethod("render");m.setAccessible(true);m.invoke(activity);
    View root=activity.getWindow().getDecorView();
    root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(780,View.MeasureSpec.EXACTLY));
    root.layout(0,0,360,780);return root;
  }
  private void invoke(String name)throws Exception{
    Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(activity);
  }
  private void collect(View v,String tag,java.util.List<View> out){
    if(tag.equals(v.getTag()))out.add(v);
    if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)collect(((ViewGroup)v).getChildAt(i),tag,out);
  }
  @Test public void polestarPaletteHasAnIndependentFifthSkin(){
    assertEquals(6,UiSkin.CHOICES.length);
    UiSkin.set(activity,UiSkin.VOLIKE_POLESTAR);
    assertEquals(UiSkin.VOLIKE_POLESTAR,UiSkin.get(activity));
    assertEquals(2,UiSkin.index(activity));
    assertTrue(UiSkin.polestarVo(activity));
    assertFalse(UiSkin.nioLayout(activity));
  }
  @Test public void skinPickerContainsFiveActualVisualPreviews()throws Exception{
    invoke("skinDialog");
    AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
    assertNotNull(dialog);
    java.util.List<View> previews=new java.util.ArrayList<>();
    collect(dialog.getWindow().getDecorView(),"skin_preview",previews);
    assertEquals(5,previews.size());
    for(View preview:previews)assertTrue(preview instanceof SkinPreviewView);
    dialog.dismiss();
  }
  @Test public void simpleButtonsMayHideLabelsButRetainAccessibleActions()throws Exception{
    UiSkin.set(activity,UiSkin.SIMPLE);
    AppPreferences.set(activity,"volike_simple_labels","off");
    View root=render();
    java.util.List<View> labels=new java.util.ArrayList<>();
    collect(root,"volike_control_label",labels);
    assertEquals(4,labels.size());
    for(View v:labels)assertEquals(View.GONE,v.getVisibility());
    java.util.List<View> tiles=new java.util.ArrayList<>();collect(root,"control_tile",tiles);
    assertEquals(4,tiles.size());
    for(View tile:tiles){assertNotNull(tile.getContentDescription());assertTrue(tile.getContentDescription().length()>1);}
    AppPreferences.set(activity,"volike_simple_labels","on");
    root=render();labels.clear();collect(root,"volike_control_label",labels);
    for(View v:labels)assertEquals(View.VISIBLE,v.getVisibility());
  }
  @Test public void volikePortraitMatchesReferenceStructure()throws Exception{
    UiSkin.set(activity,UiSkin.SIMPLE);
    View simple=render();
    assertNotNull(simple.findViewWithTag("volike_energy_strip"));
    assertNotNull(simple.findViewWithTag("volike_side_controls"));
    assertNotNull(simple.findViewWithTag("volike_hero"));
    assertNull(simple.findViewWithTag("volike_detail_rail"));
    java.util.List<View> circles=new java.util.ArrayList<>();collect(simple,"volike_control_circle",circles);
    assertEquals(4,circles.size());

    UiSkin.set(activity,UiSkin.DETAILED);
    View detailed=render();
    assertNotNull(detailed.findViewWithTag("volike_detail_rail"));
    java.util.List<View> serviceBars=new java.util.ArrayList<>();collect(detailed,"volike_service_bar",serviceBars);
    assertNotNull(detailed.findViewWithTag("home_value_odometer"));

    UiSkin.set(activity,UiSkin.VOLIKE_POLESTAR);
    View polestar=render();
    assertNotNull(polestar.findViewWithTag("volike_detail_rail"));
    assertNotNull(polestar.findViewWithTag("battery_soc"));
    assertNotNull(polestar.findViewWithTag("battery_range"));
  }

  @Test public void widgetStylesAreSelectableAndStillExposeVerifiedControls(){
    for(String style:new String[]{VoLikeWidgetRenderer.SIMPLE,VoLikeWidgetRenderer.DETAILED,VoLikeWidgetRenderer.POLESTAR}){
      AppPreferences.set(activity,"widget_design",style);
      for(WidgetRenderer.Size size:WidgetRenderer.Size.values()){
        int w=WidgetRenderer.defaultWidth(size),h=WidgetRenderer.defaultHeight(size);
        android.widget.RemoteViews rv=WidgetRenderer.createViews(activity,74,size,w,h,
            WidgetRenderer.background(activity,size),model.snapshot);
        View root=rv.apply(activity,new FrameLayout(activity));
        assertNotNull(root.findViewById(R.id.volike_widget_lock));
        assertNotNull(root.findViewById(R.id.volike_widget_climate));
        assertNotNull(root.findViewById(R.id.volike_widget_location));
        assertNotNull(root.findViewById(R.id.widget_bg));
      }
      Bitmap preview=VoLikeWidgetRenderer.preview(activity,model.snapshot,style,300,170);
      assertNotNull(preview);assertTrue(preview.getWidth()>=300);preview.recycle();
    }
    AppPreferences.set(activity,"widget_design",VoLikeWidgetRenderer.CLASSIC);
    assertEquals(VoLikeWidgetRenderer.CLASSIC,VoLikeWidgetRenderer.style(activity));
  }
  @Test public void chargingMotionFollowsReadbackAndNotUnrelatedAnimations()throws Exception{
    UiSkin.set(activity,UiSkin.VOLIKE_POLESTAR);
    View root=render();View pulse=root.findViewWithTag("charging_pulse");
    assertNotNull(pulse);assertEquals(View.GONE,pulse.getVisibility());
    model.snapshot=snapshot("充电中");
    root=render();pulse=root.findViewWithTag("charging_pulse");
    assertEquals(View.VISIBLE,pulse.getVisibility());
    model.snapshot=snapshot("空闲");root=render();pulse=root.findViewWithTag("charging_pulse");
    assertEquals(View.GONE,pulse.getVisibility());
    assertNull(root.findViewWithTag("vi_ambient_glow"));
  }
}
