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
  private boolean closed,failed,phoneRequestAllowed,autoPhoneRequested;private Runnable ready;
  void whenReady(Runnable callback){ready=callback;}
  android.graphics.Bitmap capture(){if(web==null||closed||web.getWidth()<=0)return null;int width=Math.min(640,web.getWidth()),height=Math.max(1,Math.round(web.getHeight()*width/(float)web.getWidth()));android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(width,height,android.graphics.Bitmap.Config.ARGB_8888);android.graphics.Canvas canvas=new android.graphics.Canvas(image);canvas.scale(width/(float)web.getWidth(),height/(float)web.getHeight());try{web.draw(canvas);return image;}catch(RuntimeException unavailable){image.recycle();return null;}}
  private boolean backgroundMode;private int mapChecks,reloads;private boolean paused;
  private LocationSnapshot point;
  private final Runnable timeout=()->fail();

  @SuppressLint("SetJavaScriptEnabled")
  ParkingWebPreview(Context context,LocationSnapshot point,boolean dark,Runnable openExternal){
    this(context,point,dark,openExternal,false);
  }
  @SuppressLint("SetJavaScriptEnabled")
  ParkingWebPreview(Context context,LocationSnapshot point,boolean dark,Runnable openExternal,boolean backdrop){
    this.point=point;backgroundMode=backdrop;
    view=new FrameLayout(context);view.setTag("parking_web_preview");view.setBackgroundColor(PsaDesign.surface(dark));
    status=new TextView(context);status.setTextColor(PsaDesign.muted(dark));status.setTypeface(PsaDesign.regular());status.setTextSize(12);status.setPadding(12,8,12,8);
    try{
      web=new WebView(context){@Override public boolean dispatchTouchEvent(android.view.MotionEvent event){if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(event.getActionMasked()!=android.view.MotionEvent.ACTION_UP&&event.getActionMasked()!=android.view.MotionEvent.ACTION_CANCEL);return super.dispatchTouchEvent(event);}};web.setTag("parking_webview");web.setSaveEnabled(false);web.setSaveFromParentEnabled(false);web.setBackgroundColor(PsaDesign.surface(dark));
      WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
      settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setGeolocationEnabled(false);
      if(backdrop){web.setLayerType(View.LAYER_TYPE_HARDWARE,null);settings.setUseWideViewPort(true);settings.setLoadWithOverviewMode(true);}
      settings.setCacheMode(backdrop?WebSettings.LOAD_DEFAULT:WebSettings.LOAD_NO_CACHE);settings.setSaveFormData(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
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
        @Override public void onPageStarted(WebView v,String url,Bitmap icon){if(!closed){failed=false;status.setVisibility(View.VISIBLE);status.setText(AppPreferences.tr(context,"正在加载高德地图…","Loading Amap…"));}}
        @Override public void onPageFinished(WebView v,String url){if(!closed&&!failed){if(!backdrop)handler.removeCallbacks(timeout);status.setVisibility(View.GONE);
          if(allowedPage(Uri.parse(url))){v.evaluateJavascript(compactScript(context),null);
            if(dark&&!backdrop)v.evaluateJavascript("(function(){var s=document.createElement('style');s.textContent='html{background:#151a20!important}canvas,.amap-layer{filter:invert(.88) hue-rotate(180deg) brightness(.72) saturate(.55)!important}.common_bottom{background:#20262c!important;color:#dce2e7!important}';document.head.appendChild(s);})();",null);
            if(backdrop){styleBackdrop(.5f,.42f);checkMapReady();return;}

            String distance=resourceScript(context,R.raw.parking_map_distance)+String.format(Locale.US,";PsaParkingDistance.show(%.8f,%.8f,%s,%s);",point.latitude,point.longitude,AppPreferences.english(context)?"true":"false","false");v.evaluateJavascript(distance,null);
            if(!autoPhoneRequested&&"on_open".equals(AppPreferences.get(context,"phone_location_refresh","manual"))){autoPhoneRequested=true;requestPhoneLocation();}
          }}}
        @Override public void onReceivedError(WebView v,WebResourceRequest request,WebResourceError error){if(request.isForMainFrame())fail();}
        @Override public void onReceivedHttpError(WebView v,WebResourceRequest request,WebResourceResponse error){if(request.isForMainFrame())fail();}
      });
      view.addView(web,new FrameLayout.LayoutParams(-1,-1));
      web.loadUrl(ParkingMapLinks.webUri(point,AppPreferences.tr(context,"最后停车位置","Last parked location")).buildUpon().appendQueryParameter("zoom",String.valueOf(EffectSettings.number(context,"map_backdrop_zoom",16,10,19))).build().toString());
      handler.postDelayed(timeout,20_000L);
    }catch(RuntimeException unavailable){fail();}
    FrameLayout.LayoutParams message=new FrameLayout.LayoutParams(-1,-2,android.view.Gravity.TOP);view.addView(status,message);
    if(!failed)status.setText(AppPreferences.tr(context,"正在加载高德地图…","Loading Amap…"));
  }
  void styleBackdrop(float x,float y){if(closed||web==null||!backgroundMode)return;Context c=view.getContext();String script=resourceScript(c,R.raw.parking_backdrop)+String.format(Locale.US,";PsaBackdrop.style(%s,%.4f,%.4f,%d,%d,%d);",AppPreferences.dark(c)?"true":"false",x,y,EffectSettings.number(c,"map_backdrop_blur",12,0,24),EffectSettings.number(c,"map_backdrop_spread",60,20,100),EffectSettings.number(c,"map_backdrop_zoom",16,10,19));web.evaluateJavascript(script,null);}
  private void checkMapReady(){if(closed||web==null||!backgroundMode||paused)return;web.evaluateJavascript("(function(){var m=document.querySelector('.amap-container');if(!m)return false;var c=m.querySelector('canvas');return !!((c&&c.width>0&&c.height>0)||m.querySelector('img.amap-tile'));})()",value->{if(closed||web==null)return;if("true".equals(value)){handler.removeCallbacks(timeout);status.setVisibility(View.GONE);AppPreferences.set(view.getContext(),"map_background_status","ready");if(ready!=null)ready.run();}else if(++mapChecks<40)handler.postDelayed(this::checkMapReady,500);else fail();});}
  void pause(){paused=true;if(web!=null)web.onPause();}
  void resume(){paused=false;if(web!=null){web.onResume();if(backgroundMode)checkMapReady();}}
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
    return host.equals("amap.com")||host.endsWith(".amap.com")||host.equals("autonavi.com")||host.endsWith(".autonavi.com");
  }
  private void fail(){
    if(closed)return;
    if(backgroundMode&&web!=null&&reloads++<1){mapChecks=0;web.reload();handler.removeCallbacks(timeout);handler.postDelayed(timeout,25000);return;}
    failed=true;handler.removeCallbacks(timeout);AppPreferences.set(view.getContext(),"map_background_status","unavailable");
    status.setVisibility(View.VISIBLE);status.setText(backgroundMode?AppPreferences.tr(view.getContext(),"地图背景暂不可用","Map background unavailable"):AppPreferences.tr(view.getContext(),"地图暂不可用，请使用下方“打开地图”。","Map unavailable. Use Open map below."));
    if(web!=null)web.stopLoading();
  }
  void close(){
    if(closed)return;closed=true;phoneRequestAllowed=false;handler.removeCallbacksAndMessages(null);
    if(web!=null){web.stopLoading();web.setWebChromeClient(null);web.setWebViewClient(new WebViewClient());web.loadUrl("about:blank");web.clearHistory();web.clearFormData();view.removeView(web);web.destroy();web=null;}
    try{WebStorage.getInstance().deleteAllData();}catch(RuntimeException unavailable){/* Missing WebView provider keeps external links available. */}
  }
}
