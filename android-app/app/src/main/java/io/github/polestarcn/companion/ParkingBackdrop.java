package io.github.polestarcn.companion;
import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;import android.widget.*;import java.util.concurrent.*;
/** Experimental one-shot official web-map capture; native progressive blur thereafter. No map polling/GPS. */
final class ParkingBackdrop extends FrameLayout {
 private final boolean dark;private final Handler main=new Handler(Looper.getMainLooper());private ParkingWebPreview preview;private Bitmap sharp,soft;private boolean released;private int captureAttempts;private View loadingCover;private final Paint paint=new Paint(3),mask=new Paint(3);private final Matrix transform=new Matrix();private RadialGradient radial;private float x=.5f,y=.42f;private final ExecutorService worker=Executors.newSingleThreadExecutor();
 ParkingBackdrop(Context c,LocationSnapshot point){
  super(c);dark=AppPreferences.dark(c);
  setWillNotDraw(false);setBackgroundColor(PsaDesign.canvas(dark));
  setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
  preview=new ParkingWebPreview(c,point,dark,()->{},true);
  // alpha=0 prevents some WebView engines from painting raster map tiles at all.
  preview.view.setAlpha(1f);addView(preview.view,new LayoutParams(-1,-1));
  // Keep the live web page behind a solid, non-interactive cover until a real snapshot exists.
  loadingCover=new View(c);loadingCover.setBackgroundColor(PsaDesign.canvas(dark));
  loadingCover.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  addView(loadingCover,new LayoutParams(-1,-1));
  preview.whenReady(()->main.postDelayed(this::captureIfReady,700));
  main.postDelayed(()->{
    if(released||preview==null)return;
    preview.close();preview=null;removeAllViews();
  },22_000);
 }
 private static boolean hasMapDetail(Bitmap bitmap){
  if(bitmap==null)return false;
  int w=bitmap.getWidth(),h=bitmap.getHeight();
  if(w<16||h<16)return false;
  int center=bitmap.getPixel(w/2,h/2),different=0;
  for(int row=1;row<5;row++)for(int col=1;col<5;col++){
    int color=bitmap.getPixel(col*w/5,row*h/5);
    if(Math.abs(Color.red(color)-Color.red(center))+Math.abs(Color.green(color)-Color.green(center))
        +Math.abs(Color.blue(color)-Color.blue(center))>35)different++;
  }
  return different>=2;
 }
 private void captureIfReady(){
  if(released||preview==null)return;
  Bitmap capture=preview.capture();
  if(!hasMapDetail(capture)){
    if(capture!=null)capture.recycle();
    if(++captureAttempts<6)main.postDelayed(this::captureIfReady,750);
    return;
  }
  preview.close();preview=null;removeAllViews();loadingCover=null;
  worker.execute(()->{
    Bitmap blurred=blur(capture,EffectSettings.number(getContext(),"map_backdrop_blur",12,0,24));
    main.post(()->{
      if(released){capture.recycle();blurred.recycle();return;}
      sharp=capture;soft=blurred;invalidate();
    });
  });
 }
 void anchor(float screenX,float screenY){x=Math.max(0,Math.min(1,screenX));y=Math.max(.1f,Math.min(.85f,screenY));radial=null;invalidate();}
 private static Bitmap blur(Bitmap source,int radius){if(radius==0)return source.copy(Bitmap.Config.ARGB_8888,false);int w=source.getWidth(),h=source.getHeight();int[] a=new int[w*h],b=new int[w*h];source.getPixels(a,0,w,0,0,w,h);radius=Math.max(1,radius);for(int pass=0;pass<2;pass++){for(int row=0;row<h;row++){long r=0,g=0,bl=0;int count=radius*2+1;for(int xx=-radius;xx<=radius;xx++){int c=a[row*w+Math.max(0,Math.min(w-1,xx))];r+=Color.red(c);g+=Color.green(c);bl+=Color.blue(c);}for(int xx=0;xx<w;xx++){b[row*w+xx]=0xFF000000|((int)(r/count)<<16)|((int)(g/count)<<8)|(int)(bl/count);int old=a[row*w+Math.max(0,xx-radius)],next=a[row*w+Math.min(w-1,xx+radius+1)];r+=Color.red(next)-Color.red(old);g+=Color.green(next)-Color.green(old);bl+=Color.blue(next)-Color.blue(old);}}int tw=w;w=h;h=tw;for(int row=0;row<h;row++)for(int col=0;col<w;col++)a[row*w+col]=b[col*h+row];}return Bitmap.createBitmap(a,source.getWidth(),source.getHeight(),Bitmap.Config.ARGB_8888);}
 @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){return false;}
 @Override protected void onDraw(Canvas c){super.onDraw(c);if(sharp==null)return;float cx=x*getWidth(),cy=y*getHeight();float scale=Math.max(getWidth()/(float)sharp.getWidth(),getHeight()/(float)sharp.getHeight());transform.setScale(scale,scale);transform.postTranslate(cx-sharp.getWidth()*scale/2,cy-sharp.getHeight()*scale/2);
  ColorMatrix colors=new ColorMatrix();colors.setSaturation(0);ColorMatrix tone=new ColorMatrix(dark?new float[]{-.45f,0,0,0,135,0,-.45f,0,0,142,0,0,-.45f,0,150,0,0,0,1,0}:new float[]{.52f,0,0,0,116,0,.52f,0,0,116,0,0,.52f,0,116,0,0,0,1,0});colors.postConcat(tone);paint.setColorFilter(new ColorMatrixColorFilter(colors));c.drawBitmap(sharp,transform,paint);
  int save=c.saveLayer(0,0,getWidth(),getHeight(),null);c.drawBitmap(soft,transform,paint);if(radial==null){float radius=Math.max(getWidth(),getHeight())*EffectSettings.number(getContext(),"map_backdrop_spread",60,20,100)/100f;radial=new RadialGradient(cx,cy,Math.max(1,radius),new int[]{Color.TRANSPARENT,0xBBFFFFFF,Color.WHITE},new float[]{0,.55f,1},Shader.TileMode.CLAMP);}mask.setShader(radial);mask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));c.drawRect(0,0,getWidth(),getHeight(),mask);c.restoreToCount(save);paint.setColorFilter(null);
 }
 void release(){released=true;main.removeCallbacksAndMessages(null);if(preview!=null){preview.close();preview=null;}worker.shutdownNow();if(sharp!=null){sharp.recycle();sharp=null;}if(soft!=null){soft.recycle();soft=null;}}
 @Override protected void onDetachedFromWindow(){release();super.onDetachedFromWindow();}
}
