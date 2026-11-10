package io.github.polestarcn.companion;

import static org.junit.Assert.*;
import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import io.github.polestarcn.companion.api.ProtoWire;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ParkingMapV079Test {
 private MainActivity activity;
 private org.robolectric.android.controller.ActivityController<MainActivity> controller;
 private static LocationSnapshot point(boolean stale){
  Map<Integer,Object> reply=new HashMap<>();reply.put(2,ProtoWire.concat(ProtoWire.fixed32Field(1,120.5f),ProtoWire.fixed32Field(2,30.25f),ProtoWire.bytesField(3,ProtoWire.varintField(1,1700000000))));reply.put(3,stale?1L:0L);return LocationSnapshot.fromReply(reply);
 }
 @Before public void start()throws Exception{
  Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("preferences",0).edit().clear().commit();c.getSharedPreferences("cn_vehicle_session_secure",0).edit().clear().commit();
  Field f=VehicleCoordinator.class.getDeclaredField("instance");f.setAccessible(true);f.set(null,null);
  org.robolectric.RuntimeEnvironment.getApplication().getSharedPreferences("legal",0).edit().putBoolean("accepted_disclaimer_v1",true).commit();
  controller=Robolectric.buildActivity(MainActivity.class).setup();activity=controller.get();
 }
 @After public void stop(){AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();if(d!=null&&d.isShowing())d.dismiss();controller.pause().stop().destroy();}
 @Test public void officialLinksPreserveAxesAndEncodeNameWithoutAKey(){
  LocationSnapshot p=point(false);String label="停车 & P2 ? #";
  android.net.Uri app=ParkingMapLinks.nativeUri(p,label),web=ParkingMapLinks.webUri(p,label);
  assertEquals("30.250000",app.getQueryParameter("lat"));assertEquals("120.500000",app.getQueryParameter("lon"));assertEquals("0",app.getQueryParameter("dev"));assertEquals(label,app.getQueryParameter("poiname"));
  assertEquals("120.500000,30.250000",web.getQueryParameter("position"));assertEquals(label,web.getQueryParameter("name"));assertEquals("gaode",web.getQueryParameter("coordinate"));assertEquals("0",web.getQueryParameter("callnative"));
  assertEquals("https",web.getScheme());assertEquals("uri.amap.com",web.getHost());assertNull(web.getQueryParameter("key"));assertFalse(web.toString().toLowerCase(Locale.US).contains("vin"));
  assertTrue(ParkingMapLinks.geoUri(p,label).toString().startsWith("geo:30.250000,120.500000?"));
 }
 public static class HandlerActivity extends Activity {
  final List<Intent> attempts=new ArrayList<>();int failures;
  @Override public void startActivity(Intent intent){attempts.add(intent);if(attempts.size()<=failures)throw new ActivityNotFoundException("fixture");}
 }
 @Test public void installedAmapOpensWithoutWebOrKey(){HandlerActivity host=Robolectric.buildActivity(HandlerActivity.class).setup().get();assertTrue(ParkingMapLinks.open(host,point(false),"Last parked location"));assertEquals(1,host.attempts.size());assertEquals("com.autonavi.minimap",host.attempts.get(0).getPackage());}
 @Test public void missingAmapFallsBackToOfficialWebThenSystemMap(){
  HandlerActivity host=Robolectric.buildActivity(HandlerActivity.class).setup().get();host.failures=1;assertTrue(ParkingMapLinks.open(host,point(false),"Last parked location"));assertEquals("https",host.attempts.get(1).getData().getScheme());
  host.attempts.clear();host.failures=2;assertTrue(ParkingMapLinks.open(host,point(false),"Last parked location"));assertEquals("geo",host.attempts.get(2).getData().getScheme());
 }
 @Test public void noMapOrBrowserIsHandledWithoutCrashing(){HandlerActivity host=Robolectric.buildActivity(HandlerActivity.class).setup().get();host.failures=3;assertFalse(ParkingMapLinks.open(host,point(false),"Last parked location"));assertEquals(3,host.attempts.size());assertFalse(ParkingMapLinks.open(host,null,"Last parked location"));assertEquals(3,host.attempts.size());}
 @Test public void upgradeDeletesLegacyKeyButPreservesVehiclePreferences(){
  activity.getSharedPreferences("map_provider_private",0).edit().putString("key","encrypted-fixture").commit();AppPreferences.set(activity,"amap_preview","on");AppPreferences.set(activity,"vehicle","fixture-hash");
  ParkingMapLinks.clearLegacySettings(activity);assertFalse(activity.getSharedPreferences("map_provider_private",0).contains("key"));assertEquals("absent",AppPreferences.get(activity,"amap_preview","absent"));assertEquals("fixture-hash",AppPreferences.get(activity,"vehicle",""));
 }
 private static int count(View view,Class<?> kind){int n=kind.isInstance(view)?1:0;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)n+=count(((ViewGroup)view).getChildAt(i),kind);return n;}
 private static void fitText(View view){if(view instanceof TextView){TextView text=(TextView)view;if(text.getVisibility()==View.VISIBLE&&text.length()>0&&text.getLayout()!=null)assertTrue(text.getText()+" layout="+text.getLayout().getHeight()+" view="+text.getHeight()+" padding="+(text.getCompoundPaddingTop()+text.getCompoundPaddingBottom()),text.getLayout().getHeight()<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());}if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)fitText(((ViewGroup)view).getChildAt(i));}
 private static void removeTestScrollIndicators(View view){view.setScrollIndicators(0);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)removeTestScrollIndicators(((ViewGroup)view).getChildAt(i));}
 @Test public void parkingDialogSupportsFourSkinsBothLanguagesThemesAndCompactScreens()throws Exception{
  Method render=MainActivity.class.getDeclaredMethod("render"),show=MainActivity.class.getDeclaredMethod("showParkingLocation",LocationSnapshot.class);render.setAccessible(true);show.setAccessible(true);
  for(String skin:new String[]{UiSkin.SIMPLE,UiSkin.DETAILED,UiSkin.NIO_LIKE,UiSkin.NIO_LIKE_2,UiSkin.VOLIKE_POLESTAR,UiSkin.PS_DEFAULT})for(String lang:new String[]{"zh","en"})for(String theme:new String[]{"light","dark"})for(int[] size:new int[][]{{320,568},{640,320},{360,640}}){
   UiSkin.set(activity,skin);AppPreferences.set(activity,"language",lang);AppPreferences.set(activity,"theme",theme);Configuration config=new Configuration(activity.getResources().getConfiguration());config.fontScale=1.3f;config.screenWidthDp=size[0];config.screenHeightDp=size[1];activity.getResources().updateConfiguration(config,activity.getResources().getDisplayMetrics());render.invoke(activity);show.invoke(activity,point(true));
   AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(dialog.isShowing());View decor=dialog.getWindow().getDecorView();
    android.util.DisplayMetrics display=activity.getResources().getDisplayMetrics();
    boolean portrait=size[0]<=size[1]&&size[1]>=480;
    int[] expected=portrait
        ?DialogWindowGeometry.parkingPreview(display.widthPixels,display.heightPixels,display.density)
        :DialogWindowGeometry.map(display.widthPixels,display.heightPixels,display.density);
    // First layout is established by the window pre-draw correction, before
    // the map becomes visible in a real OPPO host.
    decor.getViewTreeObserver().dispatchOnPreDraw();
    android.view.WindowManager.LayoutParams attrs=dialog.getWindow().getAttributes();
    assertEquals(android.view.Gravity.CENTER,attrs.gravity);
    assertEquals(expected[0],attrs.width);assertEquals(expected[1],attrs.height);
    if(portrait){
      assertTrue(attrs.width>=display.widthPixels*.88);
      assertTrue(attrs.height>=display.heightPixels*.78);
    }else assertTrue((double)attrs.width*attrs.height<=display.widthPixels*display.heightPixels*.5d);
    int w=attrs.width,h=attrs.height;
    decor.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));decor.layout(0,0,w,h);
   View entry=decor.findViewWithTag("parking_web_preview");assertNotNull(entry);
   int required=ParkingPreviewLayout.mapHeightDp(size[1],portrait);
   assertEquals(required,entry.getLayoutParams().height);
   assertTrue("Map preview must exceed the old 90dp thumbnail",entry.getLayoutParams().height>=145||!portrait);assertEquals(1,count(decor,android.webkit.WebView.class));
   View coordinatesOverlay=decor.findViewWithTag("parking_coordinates");
   assertNotNull("Parking coordinates remain visible as a map overlay",coordinatesOverlay);
   assertSame("Coordinates must not consume a separate dialog row",entry,
       ((ViewGroup)coordinatesOverlay.getParent()).getParent());
   assertNotNull(decor.findViewWithTag("parking_stale"));assertEquals(0,count(decor,EditText.class));
   try {fitText(decor);} catch(AssertionError clipped){
     throw new AssertionError("Parking UI clipped: skin="+skin+" lang="+lang+" theme="+theme
       +" viewport="+Arrays.toString(size)+" display="+display.widthPixels+"x"+display.heightPixels
       +" density="+display.density+" dialog="+w+"x"+h
       +" mapActual="+entry.getHeight()+" coordsActual="+coordinatesOverlay.getHeight()
       +" panelActual="+decor.findViewWithTag("parking_location_panel").getHeight()
       +" cause="+clipped.getMessage(),clipped);
   }
    View externalMap=decor.findViewWithTag("parking_open_map");
    assertNotNull("Keep compact optional external Open map action",externalMap);
    assertTrue("Open map is a smaller accessory button",externalMap.getLayoutParams().height<=Math.round(34*display.density));
    assertNotNull(decor.findViewWithTag("parking_zoom_in"));
    assertNotNull(decor.findViewWithTag("parking_zoom_out"));
    assertNotNull(decor.findViewWithTag("parking_center_vehicle"));
    assertNotNull(decor.findViewWithTag("parking_refresh_position"));
    assertNotNull(decor.findViewWithTag("parking_phone_location_toggle"));
    View panel=decor.findViewWithTag("parking_location_panel");
    assertNotNull(panel);
    if(portrait){
      boolean requiresOverflow=expected[1]/Math.max(.1f,display.density)<required+232f;
      assertEquals("Only space-constrained portrait dialogs may use overflow fallback",
          requiresOverflow,panel.getParent() instanceof ScrollView);
    }
    assertTrue(dialog.getButton(AlertDialog.BUTTON_NEGATIVE).getHeight()>0);
   if(skin.equals(UiSkin.NIO_LIKE_2)&&lang.equals("zh")&&size[0]==360){File file=new File("build/reports/previews/v079/parking-"+theme+".png");file.getParentFile().mkdirs();Bitmap bitmap=Bitmap.createBitmap(w*2,h*2,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.scale(2,2);removeTestScrollIndicators(decor);decor.draw(canvas);try(FileOutputStream out=new FileOutputStream(file)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}
   dialog.dismiss();
  }
 }
 @Test public void embeddedPreviewUsesOfficialKeylessPageWithRestrictedCapabilities(){
  ParkingWebPreview map=new ParkingWebPreview(activity,point(false),false,()->{});android.webkit.WebView web=(android.webkit.WebView)map.view.findViewWithTag("parking_webview");assertNotNull(web);
  assertTrue(web.getSettings().getJavaScriptEnabled());assertFalse(web.getSettings().getAllowFileAccess());assertFalse(web.getSettings().getAllowContentAccess());assertEquals(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW,web.getSettings().getMixedContentMode());assertEquals(android.webkit.WebSettings.LOAD_NO_CACHE,web.getSettings().getCacheMode());assertFalse(web.isSaveEnabled());
  String url=Shadows.shadowOf(web).getLastLoadedUrl();assertTrue(url.startsWith("https://www.amap.com/ssr/embed/place?"));assertFalse(url.contains("key="));assertFalse(url.contains("vin"));
  android.webkit.WebChromeClient chrome=Shadows.shadowOf(web).getWebChromeClient();final boolean[] allowed={true};chrome.onGeolocationPermissionsShowPrompt("https://m.amap.com",(origin,grant,retain)->allowed[0]=grant);assertFalse(allowed[0]);map.close();
 }
 @Test public void embeddedInteractiveMapUsesRealCoordinatesAndZoomWithoutApiKey(){
  LocationSnapshot parked=point(false);
  android.net.Uri url=ParkingMapLinks.embeddedPlaceUri(parked,"停车点",14);
  assertEquals("https",url.getScheme());assertEquals("www.amap.com",url.getHost());
  assertEquals("/ssr/embed/place",url.getPath());
  assertEquals("120.500000",url.getQueryParameter("lng"));
  assertEquals("30.250000",url.getQueryParameter("lat"));
  assertEquals("14",url.getQueryParameter("zoom"));
  assertEquals("pc",url.getQueryParameter("platform"));
  assertNull(url.getQueryParameter("key"));assertFalse(url.toString().toLowerCase(Locale.ROOT).contains("vin"));
  ParkingWebPreview map=new ParkingWebPreview(activity,parked,false,()->fail("Must not launch another map window"));
  android.webkit.WebView web=map.view.findViewWithTag("parking_webview");
  assertNotNull(web);
  assertTrue(web.getSettings().supportZoom());
  assertEquals(14,map.zoomLevel());
  assertEquals(15,map.adjustZoom(1));
  assertTrue(Shadows.shadowOf(web).getLastLoadedUrl().contains("zoom=15"));
  map.recenter();assertTrue(Shadows.shadowOf(web).getLastLoadedUrl().contains("zoom=15"));
  map.adjustZoom(25);
  assertEquals(19,map.zoomLevel());
  map.close();
 }
 @Test public void mapBrowsingIsInTheSameSecondaryDialogWithoutTertiaryActions() throws Exception {
  Method show=MainActivity.class.getDeclaredMethod("showParkingLocation",LocationSnapshot.class);show.setAccessible(true);
  show.invoke(activity,point(false));
  AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
  View decor=dialog.getWindow().getDecorView();
  assertNotNull(decor.findViewWithTag("parking_webview"));
  View externalMap=decor.findViewWithTag("parking_open_map");
  assertNotNull("Optional external map action stays available",externalMap);
  assertTrue("External map button remains compact",externalMap.getLayoutParams().height<=Math.round(34*activity.getResources().getDisplayMetrics().density));
  assertNull(decor.findViewWithTag("parking_phone_settings"));
  assertNotNull(decor.findViewWithTag("parking_zoom_in"));
  assertNotNull(decor.findViewWithTag("parking_zoom_out"));
  assertNotNull(decor.findViewWithTag("parking_center_vehicle"));
  assertNotNull(decor.findViewWithTag("parking_refresh_position"));
  assertNotNull(decor.findViewWithTag("parking_phone_location_toggle"));
  assertTrue(dialog.isShowing());
  assertTrue(decor.findViewWithTag("parking_zoom_in").performClick());
  assertSame("Zoom stays in the same dialog",dialog,org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog());
 }
 @Test public void navigationAllowlistRejectsSpoofedHostsAndUnsafeSchemes(){
  for(String url:new String[]{"https://uri.amap.com/marker","https://m.amap.com/","https://www.autonavi.com/"})assertTrue(ParkingWebPreview.allowedPage(android.net.Uri.parse(url)));
  for(String url:new String[]{"http://m.amap.com/","https://amap.com.evil.invalid/","https://evil.invalid/?amap.com","file:///etc/passwd","content://fixture","intent://fixture"})assertFalse(ParkingWebPreview.allowedPage(android.net.Uri.parse(url)));
 }
 @Test public void closingPreviewReleasesWebviewAndCanBeRepeated(){ParkingWebPreview map=new ParkingWebPreview(activity,point(false),true,()->{});assertNotNull(map.view.findViewWithTag("parking_webview"));map.close();assertNull(map.view.findViewWithTag("parking_webview"));map.close();}
 @Test public void backgroundingActivityClosesMapDialog()throws Exception{
  Method show=MainActivity.class.getDeclaredMethod("showParkingLocation",LocationSnapshot.class);show.setAccessible(true);show.invoke(activity,point(false));AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(dialog.isShowing());controller.pause().stop();assertFalse(dialog.isShowing());controller.start().resume();
 }
 @Test public void freshParkingDoesNotShowStaleWarningOrPersistCoordinates()throws Exception{
  Method show=MainActivity.class.getDeclaredMethod("showParkingLocation",LocationSnapshot.class);show.setAccessible(true);show.invoke(activity,point(false));AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNull(dialog.getWindow().getDecorView().findViewWithTag("parking_stale"));
  assertFalse(activity.getSharedPreferences("preferences",0).getAll().toString().contains("120.5"));assertFalse(DiagnosticLog.export().contains("120.5"));assertFalse(DiagnosticLog.export().contains("30.25"));
 }
}
