package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.app.AlertDialog;
import android.content.res.Configuration;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.io.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,qualifiers="w360dp-h780dp-mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class V078RegressionTest {
 private AppRegressionTest base;private MainActivity activity;private VehicleCoordinator model;
 @Before public void start()throws Exception{base=new AppRegressionTest();base.start();Field f=AppRegressionTest.class.getDeclaredField("activity");f.setAccessible(true);activity=(MainActivity)f.get(base);
  model=VehicleCoordinator.get(activity);Field signed=VehicleCoordinator.class.getDeclaredField("signedIn");signed.setAccessible(true);signed.setBoolean(model,true);
  model.selected=new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();model.selected.vin="FIXTURE-V078";long now=System.currentTimeMillis();
  model.snapshot=new VehicleSnapshot("Polestar 2","masked",63,230,65845,"充电中","全部关闭","全部关闭","已关闭","已上锁",now,"LIVE",false,"开启","开启")
    .withHealth(new VehicleHealthSnapshot(1,-1,-1,now,1,1,-1,-1,1,931,75,14959)).withMileage(new MileageSnapshot(65845000L,100.5,25.2,45.0,35.0,18.5,21.5,now,now))
    .withEnvironment(new EnvironmentSnapshot("开启","开启",23.0,22.0,3.0,4,18,null,now,now)).withCharging(new ChargingSnapshot(1,2,4,180,6000.0,29.0));
  AppPreferences.set(activity,"condition_service_time","on");AppPreferences.set(activity,"condition_service_distance","on");
  AppPreferences.set(activity,"home_metrics","odometer,ta_distance,ta_speed,ta_consumption,tm_distance,tm_speed,tm_consumption,temperature,aqi,ta_duration,tm_duration,since_charge_distance");
 }
 @After public void stop(){base.stop();}
 private void invoke(String method)throws Exception{Method m=MainActivity.class.getDeclaredMethod(method);m.setAccessible(true);m.invoke(activity);}
 private View render(int w,int h,float font)throws Exception{Configuration c=new Configuration(activity.getResources().getConfiguration());c.fontScale=font;c.screenWidthDp=w;c.screenHeightDp=h;
  android.util.DisplayMetrics dm=activity.getResources().getDisplayMetrics();dm.scaledDensity=dm.density*font;activity.getResources().updateConfiguration(c,dm);invoke("render");
  View v=activity.getWindow().getDecorView();v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h);return v;
 }
 private int tagged(View v,String tag){int n=tag.equals(v.getTag())?1:0;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)n+=tagged(g.getChildAt(i),tag);}return n;}
 private void fits(View v,boolean single){if(v.getVisibility()!=View.VISIBLE)return;
  if(v instanceof TextView){TextView t=(TextView)v;if(t.getText().length()>0&&t.getLayout()!=null){assertTrue(t.getText()+" height="+t.getHeight()+" text="+t.getLayout().getHeight(),t.getLayout().getHeight()<=t.getHeight()-t.getCompoundPaddingTop()-t.getCompoundPaddingBottom());if(single)assertTrue(t.getText().toString(),t.getLineCount()<=1);}}
  if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View child=g.getChildAt(i);if(child.getVisibility()==View.GONE)continue;assertTrue("child bottom "+child.getTag()+" "+child.getBottom()+" parent="+g.getHeight(),child.getBottom()<=g.getHeight());fits(child,single);}}
 }
 private void export(View v,String name,int w,int h)throws Exception{File dir=new File("build/reports/previews/v078");dir.mkdirs();Bitmap b=Bitmap.createBitmap(w*2,h*2,Bitmap.Config.ARGB_8888);Canvas cv=new Canvas(b);cv.scale(2,2);v.draw(cv);try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
 private void matrix(int w,int h)throws Exception{
  for(float font:new float[]{1f,1.3f,1.56f})for(String lang:new String[]{"zh","en"})for(String theme:new String[]{"light","dark"})for(String skin:new String[]{UiSkin.SIMPLE,UiSkin.DETAILED,UiSkin.NIO_LIKE,UiSkin.NIO_LIKE_2}){
   AppPreferences.set(activity,"language",lang);AppPreferences.set(activity,"theme",theme);UiSkin.set(activity,skin);View root=render(w,h,font),content=root.findViewWithTag("control_pull_content");try{fits(content,UiSkin.nioLayout(activity));}catch(AssertionError e){export(root,"failure-"+skin+"-"+w+"-"+font,w,h);throw new AssertionError(skin+" "+w+"x"+h+" font="+font+" "+lang+" "+theme,e);}assertEquals(4,tagged(content,"control_tile"));
   LinearLayout summary=root.findViewWithTag("vehicle_status_button");TextView label=(TextView)summary.getChildAt(1),soc=root.findViewWithTag("battery_soc");assertEquals("All Good!",label.getText().toString());assertEquals(soc.getTextSize(),label.getTextSize(),.01);
   if(UiSkin.nioLayout(activity)&&font==1&&w==360){assertTrue(tagged(content,"shortcut_circle")>=4);export(root,skin+"-"+theme+"-"+lang,w,h);}
  }
 }
 @Test public void appFontAndSystemFontTogetherFitPortraitAndLandscape()throws Exception{matrix(320,568);matrix(640,320);}
 @Test public void allSkinsExportAtNormalSizeAndRemainSingleLine()throws Exception{matrix(360,780);}
 @Test public void conditionSummaryIncludesBodyWarningsButPartialReportingCanBeGood(){
  assertEquals("All Good!",VehicleCondition.summary(model.snapshot,true,true));
  VehicleSnapshot opening=model.snapshot.withExterior(new ExteriorSnapshot(new int[]{1,2,2,2},new int[]{2,2,2,2}));assertTrue(VehicleCondition.problem(opening,true));assertNotEquals("All Good!",VehicleCondition.summary(opening,true,true));
  VehicleSnapshot none=new VehicleSnapshot("car","masked",60,200,null,"未知","未知","未知","未知","未知",1,"LIVE",false);assertFalse(VehicleCondition.reported(none,true));
 }
 @Test public void fontPrefsAreIndependentAndWrapNativeDialogsWithSystemScaling(){
  UiSkin.set(activity,UiSkin.NIO_LIKE);AppPreferences.set(activity,UiTypography.key(activity),"120");assertEquals(120,UiTypography.percent(activity,UiTypography.key(activity)));
  float baseScale=activity.getResources().getConfiguration().fontScale;assertEquals(baseScale*1.2f,UiTypography.wrap(activity).getResources().getConfiguration().fontScale,.001);
  UiSkin.set(activity,UiSkin.NIO_LIKE_2);assertEquals(100,UiTypography.percent(activity,UiTypography.key(activity)));
 }
 @Test public void detailsKeepFiveRowsPerGroupAndOfferSecondaryTripData()throws Exception{
  Field page=MainActivity.class.getDeclaredField("page");page.setAccessible(true);page.setInt(activity,1);View root=render(360,780,1);
  View more=root.findViewWithTag("detail_more");assertNotNull(more);ViewGroup list=(ViewGroup)more.getParent();int rows=0;
  for(int i=0;i<list.getChildCount();i++){View v=list.getChildAt(i);if("detail_section".equals(v.getTag())){assertTrue(rows<=5);rows=0;}if("detail_row".equals(v.getTag()))rows++;}assertTrue(rows<=5);
  invoke("tripDialog");AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(dialog);dialog.dismiss();
 }
 @Test public void oldTaEnergyCacheIsInvalidatedWhileTripMetersSurvive()throws Exception{
  VehicleSnapshotStore.saveChina(activity,model.snapshot);String pref="vehicle_snapshot_display_v1";JSONObject o=new JSONObject(activity.getSharedPreferences(pref,0).getString("china","{}"));o.remove("consumptionSchema");
  activity.getSharedPreferences(pref,0).edit().putString("china",o.toString()).commit();VehicleSnapshot restored=VehicleSnapshotStore.loadChina(activity);
  assertNull(restored.mileage.automaticConsumption);assertEquals(25.2,restored.mileage.automaticKm,.001);assertEquals(18.5,restored.mileage.manualConsumption,.001);
 }
 @Test public void widgetFontSizesKeepRefreshTimeAnd48DpControls()throws Exception{
  android.content.Context widgetContext=activity.getApplicationContext();
  Configuration systemConfig=new Configuration(widgetContext.getResources().getConfiguration());systemConfig.fontScale=1.3f;
  android.util.DisplayMetrics metrics=widgetContext.getResources().getDisplayMetrics();metrics.scaledDensity=metrics.density*1.3f;widgetContext.getResources().updateConfiguration(systemConfig,metrics);
  for(float font:new float[]{.8f,1f,1.2f}){AppPreferences.set(activity,"widget_font_percent",String.valueOf(Math.round(font*100)));
   for(WidgetRenderer.Size size:WidgetRenderer.Size.values()){int w=size==WidgetRenderer.Size.MINI?118:size==WidgetRenderer.Size.SQUARE?150:300,h=size==WidgetRenderer.Size.MINI?88:size==WidgetRenderer.Size.WIDE?70:size==WidgetRenderer.Size.DATA?260:170;
    View root=WidgetRenderer.createViews(activity,78,size,w,h,WidgetRenderer.background(activity,size),model.snapshot).apply(widgetContext,new FrameLayout(widgetContext));root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));root.layout(0,0,w,h);fits(root,false);
    if(size==WidgetRenderer.Size.LARGE||size==WidgetRenderer.Size.SQUARE||size==WidgetRenderer.Size.DATA)assertEquals(View.VISIBLE,root.findViewById(R.id.widget_service_band).getVisibility());
    assertTrue(root.findViewById(R.id.widget_action_lock).getHeight()>=48);assertTrue(root.findViewById(R.id.widget_action_climate).getHeight()>=48);assertNotNull(root.findViewById(R.id.widget_refresh_icon));
   }
  }
 }
}
