package io.github.polestarcn.companion;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.*;
import android.widget.*;
import java.util.Locale;

/** On-demand official Amap web page, no API key, SDK or JavaScript-to-app bridge. */
final class ParkingWebPreview {
  final FrameLayout view;
  private final TextView status;
  private final Handler handler=new Handler(Looper.getMainLooper());
  private WebView web;
  private boolean closed,failed,phoneRequestAllowed,autoPhoneRequested;
  private boolean backdrop,fallbackUsed;
  private String provider;private int zoom;
  private Runnable ready;
  void whenReady(Runnable callback){ready=callback;}
  android.graphics.Bitmap capture(){if(web==null||closed||web.getWidth()<=0)return null;int width=Math.min(640,web.getWidth()),height=Math.max(1,Math.round(web.getHeight()*width/(float)web.getWidth()));android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(width,height,android.graphics.Bitmap.Config.ARGB_8888);android.graphics.Canvas canvas=new android.graphics.Canvas(image);canvas.scale(width/(float)web.getWidth(),height/(float)web.getHeight());try{web.draw(canvas);return image;}catch(RuntimeException unavailable){image.recycle();return null;}}
  private LocationSnapshot point;
  private final Runnable timeout=()->fail();

  @SuppressLint("SetJavaScriptEnabled")
  ParkingWebPreview(Context context,LocationSnapshot point,boolean dark,Runnable openExternal){
    this(context,point,dark,openExternal,false);
  }
  @SuppressLint("SetJavaScriptEnabled")
  ParkingWebPreview(Context context,LocationSnapshot point,boolean dark,Runnable openExternal,boolean backdrop){
    this.point=point;this.backdrop=backdrop;
    this.provider=backdrop?MapBackdropOptions.provider(context):"amap";
    this.zoom=MapBackdropOptions.zoom(context);
    view=new FrameLayout(context);view.setTag("parking_web_preview");view.setBackgroundColor(PsaDesign.surface(dark));
    status=new TextView(context);status.setTextColor(PsaDesign.muted(dark));status.setTypeface(PsaDesign.regular());status.setTextSize(12);status.setPadding(12,8,12,8);
    try{
      web=new WebView(context);web.setTag("parking_webview");web.setSaveEnabled(false);web.setSaveFromParentEnabled(false);web.setBackgroundColor(PsaDesign.surface(dark));
      WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
      settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setGeolocationEnabled(false);
      settings.setCacheMode(WebSettings.LOAD_NO_CACHE);settings.setSaveFormData(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
      settings.setSupportMultipleWindows(false);settings.setJavaScriptCanOpenWindowsAutomatically(false);settings.setMediaPlaybackRequiresUserGesture(true);
      settings.setSafeBrowsingEnabled(true);if(Build.VERSION.SDK_INT>=33)settings.setAlgorithmicDarkeningAllowed(false);
      CookieManager.getInstance().setAcceptCookie(false);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
      web.setWebChromeClient(new WebChromeClient(){
        @Override public void onGeolocationPermissionsShowPrompt(String origin,GeolocationPermissions.Callback callback){callback.invoke(origin,phoneRequestAllowed&&!closed&&MapLocationPermission.allowOrigin(context,origin),false);}
        @Override public void onPermissionRequest(PermissionRequest request){request.deny();}
      });
      web.setWebViewClient(new WebViewClient(){
        @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest request){
          Uri uri=request.getUrl();if(allowedPage(uri))return false;
          if(request.isForMainFrame()&&request.hasGesture())openExternal.run();return true;
        }
        @Override public void onPageStarted(WebView v,String url,Bitmap icon){
          if(!closed){failed=false;status.setVisibility(View.VISIBLE);
            status.setText(AppPreferences.tr(context,"正在加载地图…","Loading map…"));
          }
        }
        @Override public void onPageFinished(WebView v,String url){
          if(closed||failed)return;
          Uri current=Uri.parse(url);
          if(!allowedPage(current))return;
          handler.removeCallbacks(timeout);status.setVisibility(View.GONE);
          boolean amap=current.getHost()!=null&&(current.getHost().equalsIgnoreCase("amap.com")
              ||current.getHost().toLowerCase(Locale.US).endsWith(".amap.com")
              ||current.getHost().toLowerCase(Locale.US).endsWith(".autonavi.com"));
          if(amap){
            v.evaluateJavascript(compactScript(context),null);
            if(dark&&!backdrop)v.evaluateJavascript("(function(){var s=document.createElement('style');s.textContent='html{background:#151a20!important}canvas,.amap-layer{filter:invert(.88) hue-rotate(180deg) brightness(.72) saturate(.55)!important}.common_bottom{background:#20262c!important;color:#dce2e7!important}';document.head.appendChild(s);})();",null);
          }
          if(backdrop){
            String hide=amap?
              ".common_bottom,.common_top,.amap-controls,.amap-toolbar,.amap-marker,.amap-markers,.call_native_button_wrapper,header,nav,button,#psa-parking-distance":
              "header,nav,button,.leaflet-control-zoom,.site-header,.sidebar";
            String css="(function(){var s=document.createElement('style');s.textContent='"+hide+"{display:none!important}html,body{overflow:hidden!important;pointer-events:none!important}';document.head.appendChild(s);})();";
            v.evaluateJavascript(css,null);
            // PageFinished precedes raster tile painting. Delay capture so a blank WebView is not cached.
            handler.postDelayed(()->{if(!closed&&ready!=null)ready.run();},1400);
            return;
          }
          if(amap){
            String distance=resourceScript(context,R.raw.parking_map_distance)
              +String.format(Locale.US,";PsaParkingDistance.show(%.8f,%.8f,%s,%s);",
                point.latitude,point.longitude,AppPreferences.english(context)?"true":"false","false");
            v.evaluateJavascript(distance,null);
            if(!autoPhoneRequested&&"on_open".equals(AppPreferences.get(context,"phone_location_refresh","manual"))){
              autoPhoneRequested=true;requestPhoneLocation();
            }
          }
        }
        @Override public void onReceivedError(WebView v,WebResourceRequest request,WebResourceError error){if(request.isForMainFrame())fail();}
        @Override public void onReceivedHttpError(WebView v,WebResourceRequest request,WebResourceResponse error){if(request.isForMainFrame())fail();}
      });
      web.setOnTouchListener((v,event)->{v.getParent().requestDisallowInterceptTouchEvent(event.getActionMasked()!=android.view.MotionEvent.ACTION_UP&&event.getActionMasked()!=android.view.MotionEvent.ACTION_CANCEL);return false;});
      view.addView(web,new FrameLayout.LayoutParams(-1,-1));
      web.loadUrl(MapBackdropOptions.page(point,AppPreferences.tr(context,"最后停车位置","Last parked location"),provider,zoom).toString());
      handler.postDelayed(timeout,20_000L);
    }catch(RuntimeException unavailable){fail();}
    FrameLayout.LayoutParams message=new FrameLayout.LayoutParams(-1,-2,android.view.Gravity.TOP);view.addView(status,message);
    if(!failed)status.setText(AppPreferences.tr(context,"正在加载地图…","Loading map…"));
  }
  private static String compactScript(Context context){return resourceScript(context,R.raw.parking_map_compact);}
  private static String resourceScript(Context context,int resource){
    try(java.io.InputStream input=context.getResources().openRawResource(resource);
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream()){
      byte[] buffer=new byte[2048];int n;while((n=input.read(buffer))!=-1)bytes.write(buffer,0,n);
      return new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
    }catch(java.io.IOException unavailable){return "void 0";}
  }
  void refreshPhoneLocation(){
    if(closed||web==null)return;phoneRequestAllowed=false;web.getSettings().setGeolocationEnabled(false);
    try{GeolocationPermissions.getInstance().clearAll();}catch(RuntimeException unavailable){}
    if(MapLocationPermission.enabled(view.getContext())&&"on_open".equals(AppPreferences.get(view.getContext(),"phone_location_refresh","manual")))requestPhoneLocation();
  }
  void requestPhoneLocation(){
    Context c=view.getContext();if(closed||web==null||!MapLocationPermission.enabled(c))return;
    phoneRequestAllowed=true;web.getSettings().setGeolocationEnabled(true);
    String script=resourceScript(c,R.raw.parking_map_distance)+String.format(Locale.US,";PsaParkingDistance.show(%.8f,%.8f,%s,true);",point.latitude,point.longitude,AppPreferences.english(c)?"true":"false");
    web.evaluateJavascript(script,null);handler.postDelayed(()->{phoneRequestAllowed=false;if(!closed&&web!=null){web.getSettings().setGeolocationEnabled(false);try{GeolocationPermissions.getInstance().clearAll();}catch(RuntimeException unavailable){}}},10_000L);
  }
  static boolean allowedPage(Uri uri){
    if(uri==null||!"https".equalsIgnoreCase(uri.getScheme()))return false;
    String host=uri.getHost();if(host==null)return false;host=host.toLowerCase(Locale.US);
    return host.equals("amap.com")||host.endsWith(".amap.com")
      ||host.equals("autonavi.com")||host.endsWith(".autonavi.com")
      ||host.equals("openstreetmap.org")||host.endsWith(".openstreetmap.org")
      ||host.equals("opentopomap.org")||host.endsWith(".opentopomap.org");
  }
  private void fail(){
    if(closed)return;
    // Amap web pages occasionally fail inside WebView. Try OSM once, never loop indefinitely.
    if(backdrop&&!fallbackUsed&&"amap".equals(provider)&&web!=null){
      fallbackUsed=true;provider="osm";failed=false;
      web.stopLoading();
      web.loadUrl(MapBackdropOptions.page(point,"Last parked location","osm",zoom).toString());
      handler.removeCallbacks(timeout);handler.postDelayed(timeout,15_000L);
      return;
    }
    failed=true;handler.removeCallbacks(timeout);
    status.setVisibility(View.VISIBLE);status.setText(AppPreferences.tr(view.getContext(),"地图暂不可用，请使用下方“打开地图”。","Map unavailable. Use Open map below."));
    if(web!=null)web.stopLoading();
  }
  void close(){
    if(closed)return;closed=true;phoneRequestAllowed=false;handler.removeCallbacksAndMessages(null);
    if(web!=null){web.stopLoading();web.setWebChromeClient(null);web.setWebViewClient(new WebViewClient());web.loadUrl("about:blank");web.clearHistory();web.clearCache(true);web.clearFormData();view.removeView(web);web.destroy();web=null;}
    try{WebStorage.getInstance().deleteAllData();}catch(RuntimeException unavailable){/* Missing WebView provider keeps external links available. */}
  }
}
