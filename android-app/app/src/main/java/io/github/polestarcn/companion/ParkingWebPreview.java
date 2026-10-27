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
  private boolean closed,failed;
  private final Runnable timeout=()->fail();

  @SuppressLint("SetJavaScriptEnabled")
  ParkingWebPreview(Context context,LocationSnapshot point,boolean dark,Runnable openExternal){
    view=new FrameLayout(context);view.setTag("parking_web_preview");view.setBackgroundColor(PsaDesign.surface(dark));
    status=new TextView(context);status.setTextColor(PsaDesign.muted(dark));status.setTypeface(PsaDesign.regular());status.setTextSize(12);status.setPadding(12,8,12,8);
    try{
      web=new WebView(context);web.setTag("parking_webview");web.setSaveEnabled(false);web.setSaveFromParentEnabled(false);web.setBackgroundColor(PsaDesign.surface(dark));
      WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
      settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setGeolocationEnabled(MapLocationPermission.enabled(context));
      settings.setCacheMode(WebSettings.LOAD_NO_CACHE);settings.setSaveFormData(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
      settings.setSupportMultipleWindows(false);settings.setJavaScriptCanOpenWindowsAutomatically(false);settings.setMediaPlaybackRequiresUserGesture(true);
      settings.setSafeBrowsingEnabled(true);if(Build.VERSION.SDK_INT>=33)settings.setAlgorithmicDarkeningAllowed(dark);
      CookieManager.getInstance().setAcceptCookie(false);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
      web.setWebChromeClient(new WebChromeClient(){
        @Override public void onGeolocationPermissionsShowPrompt(String origin,GeolocationPermissions.Callback callback){callback.invoke(origin,MapLocationPermission.allowOrigin(context,origin),false);}
        @Override public void onPermissionRequest(PermissionRequest request){request.deny();}
      });
      web.setWebViewClient(new WebViewClient(){
        @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest request){
          Uri uri=request.getUrl();if(allowedPage(uri))return false;
          if(request.isForMainFrame()&&request.hasGesture())openExternal.run();return true;
        }
        @Override public void onPageStarted(WebView v,String url,Bitmap icon){if(!closed){failed=false;status.setVisibility(View.VISIBLE);status.setText(AppPreferences.tr(context,"正在加载高德地图…","Loading Amap…"));}}
        @Override public void onPageFinished(WebView v,String url){if(!closed&&!failed){handler.removeCallbacks(timeout);status.setVisibility(View.GONE);
          if(allowedPage(Uri.parse(url))){v.evaluateJavascript(compactScript(context),null);
            String distance=resourceScript(context,R.raw.parking_map_distance)+String.format(Locale.US,";PsaParkingDistance.show(%.8f,%.8f,%s,%s);",point.latitude,point.longitude,AppPreferences.english(context)?"true":"false",MapLocationPermission.enabled(context)?"true":"false");v.evaluateJavascript(distance,null);}}}
        @Override public void onReceivedError(WebView v,WebResourceRequest request,WebResourceError error){if(request.isForMainFrame())fail();}
        @Override public void onReceivedHttpError(WebView v,WebResourceRequest request,WebResourceResponse error){if(request.isForMainFrame())fail();}
      });
      web.setOnTouchListener((v,event)->{v.getParent().requestDisallowInterceptTouchEvent(event.getActionMasked()!=android.view.MotionEvent.ACTION_UP&&event.getActionMasked()!=android.view.MotionEvent.ACTION_CANCEL);return false;});
      view.addView(web,new FrameLayout.LayoutParams(-1,-1));
      web.loadUrl(ParkingMapLinks.webUri(point,AppPreferences.tr(context,"最后停车位置","Last parked location")).toString());
      handler.postDelayed(timeout,20_000L);
    }catch(RuntimeException unavailable){fail();}
    FrameLayout.LayoutParams message=new FrameLayout.LayoutParams(-1,-2,android.view.Gravity.TOP);view.addView(status,message);
    if(!failed)status.setText(AppPreferences.tr(context,"正在加载高德地图…","Loading Amap…"));
  }
  private static String compactScript(Context context){return resourceScript(context,R.raw.parking_map_compact);}
  private static String resourceScript(Context context,int resource){
    try(java.io.InputStream input=context.getResources().openRawResource(resource);
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream()){
      byte[] buffer=new byte[2048];int n;while((n=input.read(buffer))!=-1)bytes.write(buffer,0,n);
      return new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
    }catch(java.io.IOException unavailable){return "void 0";}
  }
  void refreshPhoneLocation(){if(!closed&&web!=null){web.getSettings().setGeolocationEnabled(MapLocationPermission.enabled(view.getContext()));try{GeolocationPermissions.getInstance().clearAll();}catch(RuntimeException unavailable){}web.reload();}}
  static boolean allowedPage(Uri uri){
    if(uri==null||!"https".equalsIgnoreCase(uri.getScheme()))return false;
    String host=uri.getHost();if(host==null)return false;host=host.toLowerCase(Locale.US);
    return host.equals("amap.com")||host.endsWith(".amap.com")||host.equals("autonavi.com")||host.endsWith(".autonavi.com");
  }
  private void fail(){
    if(closed)return;failed=true;handler.removeCallbacks(timeout);
    status.setVisibility(View.VISIBLE);status.setText(AppPreferences.tr(view.getContext(),"地图暂不可用，请使用下方“打开地图”。","Map unavailable. Use Open map below."));
    if(web!=null)web.stopLoading();
  }
  void close(){
    if(closed)return;closed=true;handler.removeCallbacksAndMessages(null);
    if(web!=null){web.stopLoading();web.setWebChromeClient(null);web.setWebViewClient(new WebViewClient());web.loadUrl("about:blank");web.clearHistory();web.clearCache(true);web.clearFormData();view.removeView(web);web.destroy();web=null;}
    try{WebStorage.getInstance().deleteAllData();}catch(RuntimeException unavailable){/* Missing WebView provider keeps external links available. */}
  }
}
