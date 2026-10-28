package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.time.LocalDate;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class V083RegressionTest {
 private AppRegressionTest base;private MainActivity activity;private VehicleCoordinator model;
 @Before public void start()throws Exception{base=new AppRegressionTest();base.start();Field f=AppRegressionTest.class.getDeclaredField("activity");f.setAccessible(true);activity=(MainActivity)f.get(base);model=VehicleCoordinator.get(activity);f=VehicleCoordinator.class.getDeclaredField("signedIn");f.setAccessible(true);f.setBoolean(model,true);model.selected=new io.github.polestarcn.companion.api.ChinaPolestarApi.VehicleInfo();model.selected.vin="FIXTURE-V083";long now=System.currentTimeMillis();model.snapshot=new VehicleSnapshot("Polestar 2","masked",63,230,65845,"空闲","全部关闭","全部关闭","已关闭","已上锁",now,"LIVE",false,"开启","开启").withHealth(new VehicleHealthSnapshot(1,1,1,now,1,1,1,-1,1,931,75,14959));}
 @After public void stop(){AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();if(d!=null&&d.isShowing())d.dismiss();base.stop();}
 private void invoke(String name)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(activity);}
 private View render()throws Exception{Configuration config=new Configuration(activity.getResources().getConfiguration());config.screenWidthDp=360;config.screenHeightDp=780;config.fontScale=1f;android.util.DisplayMetrics metrics=activity.getResources().getDisplayMetrics();metrics.scaledDensity=metrics.density;activity.getResources().updateConfiguration(config,metrics);invoke("render");View v=activity.getWindow().getDecorView();v.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(780,View.MeasureSpec.EXACTLY));v.layout(0,0,360,780);return v;}
 private int count(View v,Class<?> type){int n=type.isInstance(v)?1:0;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)n+=count(((ViewGroup)v).getChildAt(i),type);return n;}
 private int absoluteTop(View v){int y=v.getTop();while(v.getParent() instanceof View){v=(View)v.getParent();y+=v.getTop();}return y;}
 @Test public void unknownClimateDurationHasAnExplicitReadableExplanation(){AppPreferences.set(activity,"language","en");assertTrue(ClimateRuntime.detail(activity,model.snapshot,"fixture").contains("Remaining time unavailable"));}
 @Test public void vehicleServiceNeverRequiresOrUsesLocalHistory(){
  AppPreferences.set(activity,"vehicle","fixture");MaintenanceSchedule.save(activity,"fixture",LocalDate.of(2000,1,1),1);AppPreferences.set(activity,"maintenance_interval_days","750");AppPreferences.set(activity,"maintenance_interval_km","99999");
  assertEquals(Integer.valueOf(14959),ServiceReading.km(activity,model.snapshot));assertEquals(Integer.valueOf(75),ServiceReading.days(activity,model.snapshot));assertEquals(Integer.valueOf(50),ServiceReading.distancePercent(activity,model.snapshot));assertEquals(Integer.valueOf(21),ServiceReading.timePercent(activity,model.snapshot));
  VehicleSnapshot missing=model.snapshot.withHealth(VehicleHealthSnapshot.unknown());assertNull(ServiceReading.km(activity,missing));assertNull(ServiceReading.days(activity,missing));assertEquals(0,DisplayMetric.SERVICE_KM_LEFT.reportedAt(missing));assertEquals(0,DisplayMetric.SERVICE_DAYS_LEFT.reportedAt(missing));
 }
 @Test public void serviceDialogIsReadOnlyAndUsesVehicleValues()throws Exception{invoke("maintenanceDialog");View root=activity.getWindow().getDecorView();assertNotNull(root.findViewWithTag("status_scroll"));assertEquals(0,count(root,EditText.class));assertEquals("14959 km",DisplayMetric.SERVICE_KM_LEFT.value(activity,model.snapshot,true,true));}
 @Test public void restoredPsDefaultIsAnIndependentSkinAndExportsDayNight()throws Exception{
  assertEquals(6,UiSkin.CHOICES.length);UiSkin.set(activity,UiSkin.PS_DEFAULT);assertEquals("PS Default",UiSkin.label(activity,UiSkin.PS_DEFAULT));AppPreferences.set(activity,"language","zh");assertEquals("PS默认皮肤",UiSkin.label(activity,UiSkin.PS_DEFAULT));
  for(String theme:new String[]{"light","dark"}){AppPreferences.set(activity,"theme",theme);View root=render();assertNull(root.findViewWithTag("volike_header"));assertNotNull(root.findViewWithTag("battery_summary"));assertNotNull(root.findViewWithTag("vehicle_location"));
   File file=new File("build/reports/previews/v083/ps-default-"+theme+".png");file.getParentFile().mkdirs();Bitmap image=Bitmap.createBitmap(720,1560,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.scale(2,2);root.draw(canvas);try(FileOutputStream out=new FileOutputStream(file)){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();
  }
 }
 @Test public void allSixSkinsShareGoodTextIconsAndEqualClimateGlyphSizes()throws Exception{
  for(String skin:UiSkin.CHOICES)for(String lang:new String[]{"zh","en"}){UiSkin.set(activity,skin);AppPreferences.set(activity,"language",lang);View root=render();LinearLayout status=root.findViewWithTag("vehicle_status_button");assertNotNull(status);assertEquals("All Good！",((TextView)status.getChildAt(1)).getText().toString());assertEquals(View.VISIBLE,status.findViewWithTag("health_symbol").getVisibility());
   ImageView climate=root.findViewWithTag("control_icon_climate"),air=root.findViewWithTag("control_icon_air");assertNotNull(skin,climate);assertNotNull(skin,air);assertEquals(climate.getWidth(),air.getWidth());assertEquals(climate.getHeight(),air.getHeight());
   if(UiSkin.nioLayout(activity)){assertEquals(absoluteTop(climate),absoluteTop(air));assertEquals(absoluteTop(root.findViewWithTag("control_title_climate")),absoluteTop(root.findViewWithTag("control_title_air")));}
   TextView camp=root.findViewWithTag("camp_button");assertNotNull(camp);assertNull(camp.getCompoundDrawablesRelative()[0]);VehicleConditionButton condition=root.findViewWithTag("exterior_button");assertNotNull(condition);assertTrue(condition.getContentDescription().toString().contains("21%"));
  }
 }
 @Test public void mapLocationNeedsBothOptInAndSystemPermission(){
  assertFalse(MapLocationPermission.enabled(activity));AppPreferences.set(activity,MapLocationPermission.KEY,"on");assertFalse(MapLocationPermission.enabled(activity));
  Shadows.shadowOf(activity.getApplication()).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION);assertTrue(MapLocationPermission.enabled(activity));assertTrue(MapLocationPermission.allowOrigin(activity,"https://m.amap.com"));assertFalse(MapLocationPermission.allowOrigin(activity,"https://amap.com.evil.invalid"));assertFalse(MapLocationPermission.allowOrigin(activity,null));
  AppPreferences.set(activity,MapLocationPermission.KEY,"off");assertFalse(MapLocationPermission.allowOrigin(activity,"https://m.amap.com"));
 }
 @Test public void optInAllowsOnlyTrustedWebGeolocationAndNeverRetainsGrant(){
  Shadows.shadowOf(activity.getApplication()).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);AppPreferences.set(activity,MapLocationPermission.KEY,"on");
  java.util.Map<Integer,Object> reply=new java.util.HashMap<>();reply.put(2,io.github.polestarcn.companion.api.ProtoWire.concat(io.github.polestarcn.companion.api.ProtoWire.fixed32Field(1,120.5f),io.github.polestarcn.companion.api.ProtoWire.fixed32Field(2,30.25f)));LocationSnapshot point=LocationSnapshot.fromReply(reply);
  ParkingWebPreview map=new ParkingWebPreview(activity,point,false,()->{});android.webkit.WebView web=(android.webkit.WebView)map.view.findViewWithTag("parking_webview");android.webkit.WebChromeClient chrome=Shadows.shadowOf(web).getWebChromeClient();final boolean[] grant={false},remember={true};map.requestPhoneLocation();
  chrome.onGeolocationPermissionsShowPrompt("https://m.amap.com",(o,g,r)->{grant[0]=g;remember[0]=r;});assertTrue(grant[0]);assertFalse(remember[0]);
  AppPreferences.set(activity,MapLocationPermission.KEY,"off");map.refreshPhoneLocation();chrome.onGeolocationPermissionsShowPrompt("https://m.amap.com",(o,g,r)->grant[0]=g);assertFalse(grant[0]);map.close();
 }
 @Test public void permissionDenialKeepsMapAvailableAndSwitchOff()throws Exception{invoke("mapLocationDialog");Switch toggle=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().findViewById(android.R.id.content).findViewWithTag("map_phone_location");assertNotNull(toggle);toggle.setChecked(true);assertNotNull(Shadows.shadowOf(activity).getLastRequestedPermission());activity.onRequestPermissionsResult(34,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},new int[]{android.content.pm.PackageManager.PERMISSION_DENIED});assertFalse(toggle.isChecked());assertFalse(MapLocationPermission.enabled(activity));}
}
