package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import android.content.res.Configuration;
import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class V076RegressionTest {
  private AppRegressionTest base;
  private MainActivity activity;
  private VehicleCoordinator model;
  @Before public void start() throws Exception {
    base=new AppRegressionTest(); base.start();
    Field f=AppRegressionTest.class.getDeclaredField("activity");f.setAccessible(true);activity=(MainActivity)f.get(base);
    model=VehicleCoordinator.get(activity);
    Field auth=VehicleCoordinator.class.getDeclaredField("signedIn");auth.setAccessible(true);auth.setBoolean(model,true);
    model.selected=new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();
    model.selected.vin="FIXTURE-V076";
    model.snapshot=fixture(System.currentTimeMillis());
    AppPreferences.set(activity,"vehicle",VehicleCoordinator.identity(model.selected.vin));
    AppPreferences.set(activity,"home_metrics","vehicle_status,odometer,ta_distance,ta_speed,ta_consumption,tm_distance,tm_speed,tm_consumption,aqi,temperature,charge_power,charge_current");
  }
  @After public void stop(){base.stop();}
  private VehicleSnapshot fixture(long now) {
    return new VehicleSnapshot("Polestar 2","masked",63,230,65845,"充电中","全部关闭","全部关闭","已关闭","已上锁",now,"LIVE",false,"开启","开启")
      .withMileage(new MileageSnapshot(65845000L,100.5,25.2,45.0,35.0,18.5,21.5,now-120000,now))
      .withEnvironment(new EnvironmentSnapshot("开启","开启",23.0,22.0,3.0,4,18,null,now-300000,now-60000))
      .withHealth(new VehicleHealthSnapshot(1,1,1,now-180000));
  }
  private void invoke(String name) throws Exception {Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(activity);}
  private View render(int w,int h) throws Exception {
    invoke("render");View root=activity.getWindow().getDecorView();
    root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));root.layout(0,0,w,h);return root;
  }
  private void font(float scale) {
    Configuration c=new Configuration(activity.getResources().getConfiguration());c.fontScale=scale;
    android.util.DisplayMetrics dm=activity.getResources().getDisplayMetrics();dm.scaledDensity=dm.density*scale;
    activity.getResources().updateConfiguration(c,dm);
    activity.getApplicationContext().getResources().updateConfiguration(c,dm);
  }
  private View tag(View v,String tag) {return v.findViewWithTag(tag);}
  private void assertTextFits(View v) {
    if(v instanceof TextView){TextView t=(TextView)v;
      if(t.getVisibility()==View.VISIBLE && t.getText().length()>0 && t.getLayout()!=null)
        assertTrue(t.getText()+" content="+t.getLayout().getHeight()+" actual="+t.getHeight(),t.getLayout().getHeight()<=t.getHeight()-t.getCompoundPaddingTop()-t.getCompoundPaddingBottom());
    }
    if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)assertTextFits(g.getChildAt(i));}
  }
  private void assertControls(View v,int[] found) {
    if("control_tile".equals(v.getTag())) {
      found[0]++;assertTextFits(v);Rect visible=new Rect();assertTrue(v.getGlobalVisibleRect(visible));assertEquals(v.getHeight(),visible.height());assertTrue(v.getHeight()>=48);
    }
    if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)assertControls(g.getChildAt(i),found);}
  }
  private void preview(View v,String name,int w,int h) throws Exception {
    File dir=new File("build/reports/previews/v078");dir.mkdirs();Bitmap b=Bitmap.createBitmap(w*2,h*2,Bitmap.Config.ARGB_8888);Canvas cv=new Canvas(b);cv.scale(2,2);v.draw(cv);
    try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();
  }
  private void check(int w,int h,float scale) throws Exception {
    font(scale);
    for(String language:new String[]{"zh","en"})for(String theme:new String[]{"light","dark"})for(String skin:new String[]{UiSkin.SIMPLE,UiSkin.DETAILED,UiSkin.VOLIKE_POLESTAR,UiSkin.NIO_LIKE,UiSkin.NIO_LIKE_2}){
      AppPreferences.set(activity,"language",language);AppPreferences.set(activity,"theme",theme);UiSkin.set(activity,skin);
      View root=render(w,h);int[] found={0};assertControls(root,found);assertEquals(4,found[0]);
      View content=tag(root,"control_pull_content");assertNotNull(content);
      assertNull("decorative glow is removed",tag(root,"vi_ambient_glow"));
      View car=tag(root,"polestar_vehicle"),summary=tag(root,"vehicle_status_button");
      assertNotNull(car);assertTrue(summary instanceof LinearLayout);
      int[] carLocation=new int[2],summaryLocation=new int[2];
      car.getLocationOnScreen(carLocation);tag(root,"health_charge_slot").getLocationOnScreen(summaryLocation);
      assertTrue("summary stays below the car image",summaryLocation[1]>=carLocation[1]+car.getHeight());
      assertTrue(tag(root,"vi_progress") instanceof ProgressBar);
      assertEquals(63,((ProgressBar)tag(root,"vi_progress")).getProgress());
      View charging=tag(root,"charging_section");assertNotNull(charging);
      assertEquals("fresh charging fixture has a dedicated section",View.VISIBLE,charging.getVisibility());
      assertTextFits(content);
      ViewGroup group=(ViewGroup)content;for(int i=0;i<group.getChildCount();i++)assertTrue("content child outside "+skin,group.getChildAt(i).getBottom()<=content.getHeight());
      preview(root,skin+"-"+w+"x"+h+"-"+language+"-"+theme,w,h);
    }
  }
  @Test @Config(qualifiers="w320dp-h568dp-mdpi") public void twelveMetricsAndControlsFitSmallLargeText() throws Exception {check(320,568,1.3f);}
  @Test @Config(qualifiers="w640dp-h320dp-land-mdpi") public void twelveMetricsAndControlsFitLandscapeLargeText() throws Exception {check(640,320,1.3f);}
  @Test @Config(qualifiers="w360dp-h780dp-mdpi") public void twoSkinsAndThemesFit() throws Exception {check(360,780,1f);}
  @Test @Config(qualifiers="w360dp-h780dp-mdpi") public void stateUpdatesKeepExistingViewsAndChangeFeedback() throws Exception {
    AppPreferences.set(activity,"language","zh");View root=render(360,780);View card=tag(root,"control_tile");
    View charging=tag(root,"charging_section");assertNotNull(charging);assertEquals(View.VISIBLE,charging.getVisibility());
    model.snapshot=new VehicleSnapshot("Polestar 2","masked",64,240,65845,"空闲","全部关闭","全部关闭","已关闭","未锁",System.currentTimeMillis(),"LIVE",false,"关闭","关闭");
    invoke("onModelChanged");assertSame(card,tag(activity.getWindow().getDecorView(),"control_tile"));
    assertSame("charging visibility reuses its existing section",charging,tag(activity.getWindow().getDecorView(),"charging_section"));
    assertEquals("returning to idle hides the charging section",View.GONE,charging.getVisibility());
    View circle=card.findViewWithTag("volike_control_circle");assertNotNull(circle);
    assertEquals(Color.rgb(255,107,0),((android.graphics.drawable.GradientDrawable)circle.getBackground()).getColor().getDefaultColor());
  }
  @Test public void countdownUsesReportTimeAndNeverClaimsVehicleStoppedAtExpiry() {
    long now=System.currentTimeMillis();VehicleSnapshot s=fixture(now);
    assertEquals(Integer.valueOf(13),ClimateRuntime.remainingMinutes(s,now));
    assertEquals(Integer.valueOf(0),ClimateRuntime.remainingMinutes(s,now+20*60000));
    assertEquals("开启",s.climate);
    VehicleSnapshot zero=s.withEnvironment(new EnvironmentSnapshot("开启","关闭",null,null,null,null,0,null,now,now));
    ClimateRuntime.started(activity,"vehicle",now);
    assertEquals(Integer.valueOf(0),ClimateRuntime.vehicleRemainingMinutes(zero));
  }
  @Test public void zeroAndUnrecognizedHealthEnumsAreUnknown() {
    for(long value:new long[]{0,99}){
      Map<Integer,Object> data=new LinkedHashMap<>();data.put(13,value);data.put(38,value);
      VehicleHealthSnapshot h=VehicleHealthSnapshot.fromData(data,new TireSnapshot(null,new int[]{1,1,1,1},1));
      assertEquals(VehicleHealthSnapshot.UNKNOWN,h.washerFluid);assertEquals(VehicleHealthSnapshot.UNKNOWN,h.lowVoltageBattery);assertFalse(h.allGood());
    }
  }
  @Test public void metricTimesComeFromTheirOwnSources() {
    VehicleSnapshot s=fixture(System.currentTimeMillis());
    assertEquals(s.environment.airReportedAt,DisplayMetric.AQI.reportedAt(s));
    assertEquals(s.mileage.reportedAt,DisplayMetric.TA_DISTANCE.reportedAt(s));
    assertEquals(s.health.reportedAt,DisplayMetric.VEHICLE_STATUS.reportedAt(s));
  }
  @Test public void minimumWidgetsKeepTimestampDataAndControlTargets() throws Exception {
    VehicleSnapshotStore.saveChina(activity,model.snapshot);
    for(float scale:new float[]{1f,1.3f}){font(scale);
      for(String lang:new String[]{"en","zh"})for(WidgetRenderer.Size size:new WidgetRenderer.Size[]{WidgetRenderer.Size.MINI,WidgetRenderer.Size.WIDE}){
        AppPreferences.set(activity,"widget_language",lang);
        int w=size==WidgetRenderer.Size.MINI?118:250,h=size==WidgetRenderer.Size.MINI?88:70;
        View root=WidgetRenderer.createViews(activity,70,size,w,h,WidgetRenderer.background(activity,size),model.snapshot).apply(activity,new FrameLayout(activity));
        root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));root.layout(0,0,w,h);
        TextView stamp=root.findViewById(R.id.widget_region);assertTrue(stamp.getHeight()>0);Rect visible=new Rect();assertTrue(stamp.getGlobalVisibleRect(visible));assertEquals(stamp.getHeight(),visible.height());
        assertTrue(root.findViewById(R.id.widget_action_lock).getHeight()>=48);assertTrue(root.findViewById(R.id.widget_action_climate).getHeight()>=48);
        assertTextFits(root);preview(root,"widget-"+size+"-"+lang+"-"+scale,w,h);
      }
    }
  }
}
