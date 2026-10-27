package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.app.AlertDialog;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.io.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,qualifiers="w360dp-h780dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class V077RegressionTest {
  private AppRegressionTest base;
  private MainActivity activity;
  private VehicleCoordinator model;
  @Before public void start() throws Exception {
    base=new AppRegressionTest();base.start();Field f=AppRegressionTest.class.getDeclaredField("activity");f.setAccessible(true);activity=(MainActivity)f.get(base);
    model=VehicleCoordinator.get(activity);Field signed=VehicleCoordinator.class.getDeclaredField("signedIn");signed.setAccessible(true);signed.setBoolean(model,true);
    model.selected=new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();model.selected.vin="FIXTURE-V077";
    model.snapshot=snapshot("充电中");AppPreferences.set(activity,"language","zh");
  }
  @After public void stop(){base.stop();}
  private VehicleSnapshot snapshot(String charging) {
    long now=System.currentTimeMillis();
    return new VehicleSnapshot("Polestar 2","masked",63,230,65845,charging,"全部关闭","全部关闭","已关闭","已上锁",now,"LIVE",false,"开启","关闭")
      .withCharging(new ChargingSnapshot(1,2,4,180,6000.0,29.0))
      .withEnvironment(new EnvironmentSnapshot("开启","关闭",23.0,22.0,3.0,4,18,null,now,now))
      .withHealth(new VehicleHealthSnapshot(1,1,VehicleHealthSnapshot.WARNING,now));
  }
  private void invoke(String method) throws Exception {Method m=MainActivity.class.getDeclaredMethod(method);m.setAccessible(true);m.invoke(activity);}
  private View render() throws Exception {
    invoke("render");View root=activity.getWindow().getDecorView();root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(780,View.MeasureSpec.EXACTLY));root.layout(0,0,360,780);return root;
  }
  private boolean text(View v,String part) {
    if(v instanceof TextView && ((TextView)v).getText().toString().contains(part))return true;
    if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(text(g.getChildAt(i),part))return true;}return false;
  }
  @Test public void tyreVisibilityChangesOnlyPresentationAndPreservesSavedFields() throws Exception {
    String fields="tire_status,vehicle_status,odometer";AppPreferences.set(activity,"home_metrics",fields);AppPreferences.set(activity,"experimental_tire","on");
    View root=render();assertFalse(text(root.findViewWithTag("vehicle_status_button"),"ALL GOOD"));
    AppPreferences.set(activity,"show_tire_status","off");root=render();assertTrue(text(root.findViewWithTag("vehicle_status_button"),"ALL GOOD"));
    assertEquals(fields,AppPreferences.get(activity,"home_metrics",""));assertEquals(VehicleHealthSnapshot.WARNING,model.snapshot.health.tirePressure);
    invoke("vehicleStatusDialog");AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
    assertFalse(text(dialog.getWindow().getDecorView(),"胎压告警"));dialog.dismiss();
    assertFalse(new VehicleHealthSnapshot(1,-1,1,1).allGood(false));
  }
  @Test public void settingsCanSelectThirdSkinAndReuseItsSavedIdentity() throws Exception {
    invoke("skinDialog");AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
    assertEquals(3,dialog.getListView().getAdapter().getCount());
    dialog.getListView().performItemClick(null,2,2);assertEquals(UiSkin.NIO_LIKE,UiSkin.get(activity));assertEquals(2,UiSkin.index(activity));
    assertEquals("NIOlike",UiSkin.label(activity,UiSkin.get(activity)));
    assertTrue(render().findViewWithTag("polestar_vehicle") instanceof VehicleHeroView);
  }
  @Test public void detailsChargingFieldsAppearOnlyDuringFreshActiveCharging() throws Exception {
    Field page=MainActivity.class.getDeclaredField("page");page.setAccessible(true);page.setInt(activity,1);
    assertTrue(text(render(),"充电功率"));model.snapshot=snapshot("已完成");assertFalse(text(render(),"充电功率"));
    model.snapshot=snapshot("智能充电暂停");assertFalse(text(render(),"充电功率"));
  }
  @Test public void nioWidgetsKeepStaticTransparentStyleAndExportPreview() throws Exception {
    UiSkin.set(activity,UiSkin.NIO_LIKE);AppPreferences.set(activity,"widget_language","en");AppPreferences.set(activity,"widget_style","transparent");
    AppPreferences.set(activity,WidgetRenderer.metricKey(WidgetRenderer.Size.LARGE),"battery,range,temperature,aqi,ta_distance,tm_distance,charge_power,charge_current");
    for(String theme:new String[]{"light","dark"}) {
      AppPreferences.set(activity,"theme",theme);
      View v=WidgetRenderer.createViews(activity,77,WidgetRenderer.Size.LARGE,300,170,WidgetRenderer.background(activity,WidgetRenderer.Size.LARGE),model.snapshot).apply(activity,new FrameLayout(activity));
      v.measure(View.MeasureSpec.makeMeasureSpec(300,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(170,View.MeasureSpec.EXACTLY));v.layout(0,0,300,170);
      assertEquals(View.VISIBLE,v.findViewById(R.id.widget_charging_section).getVisibility());
      File directory=new File("build/reports/previews/v077");directory.mkdirs();Bitmap b=Bitmap.createBitmap(900,510,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);canvas.scale(3,3);v.draw(canvas);
      try(FileOutputStream out=new FileOutputStream(new File(directory,"widget-niolike-"+theme+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();
    }
  }
}
