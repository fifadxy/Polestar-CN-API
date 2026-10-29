package io.github.polestarcn.companion;
import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;import android.widget.*;import java.util.concurrent.*;
/** Experimental one-shot official web-map capture; native progressive blur thereafter. No map polling/GPS. */
final class ParkingBackdrop extends FrameLayout {
 private final boolean dark;private final Handler main=new Handler(Looper.getMainLooper());private ParkingWebPreview preview;private Bitmap sharp,soft;private boolean released;private final Paint paint=new Paint(3),mask=new Paint(3);private final Matrix transform=new Matrix();private RadialGradient radial;private final PorterDuffXfermode dstIn=new PorterDuffXfermode(PorterDuff.Mode.DST_IN);private final ColorMatrixColorFilter grading;private float x=.5f,y=.42f;private final ExecutorService worker=Executors.newSingleThreadExecutor();
 public ParkingBackdrop(Context c){this(c,null);}
 ParkingBackdrop(Context c,LocationSnapshot point){super(c);dark=AppPreferences.dark(c);grading=grading(dark);setWillNotDraw(false);setBackgroundColor(PsaDesign.canvas(dark));setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
  if(point==null)return;
  String key=VehicleCoordinator.identity(point.latitude+","+point.longitude+":"+dark+":"+EffectSettings.number(c,"map_backdrop_zoom",16,10,19));cacheFile=new java.io.File(c.getCacheDir(),"map-bg-"+key+".png");
  worker.execute(()->{Bitmap cached=cacheFile.isFile()&&System.currentTimeMillis()-cacheFile.lastModified()<600000?BitmapFactory.decodeFile(cacheFile.getPath()):null;if(cached!=null&&hasDetail(cached)){install(cached);}else{if(cached!=null)cached.recycle();main.post(()->{if(!released)startPreview(c,point);});}});
 }
 private int attempts;private View cover;private java.io.File cacheFile;
 private void startPreview(Context c,LocationSnapshot point){
  preview=new ParkingWebPreview(c,point,dark,()->{},true);preview.view.setAlpha(1);addView(preview.view,new LayoutParams(-1,-1));
  cover=new View(c);cover.setBackgroundColor(PsaDesign.canvas(dark));addView(cover,new LayoutParams(-1,-1));
  preview.whenReady(()->main.postDelayed(this::captureReady,900));
  main.postDelayed(this::captureReady,6000);main.postDelayed(this::captureReady,14000);
  main.postDelayed(()->{if(preview!=null){preview.close();preview=null;removeAllViews();}},22000);
 }
 static boolean hasDetail(Bitmap b){if(b==null||b.getWidth()<16||b.getHeight()<16)return false;int center=b.getPixel(b.getWidth()/2,b.getHeight()/2),different=0;for(int y=1;y<5;y++)for(int x=1;x<5;x++){int v=b.getPixel(x*b.getWidth()/5,y*b.getHeight()/5);if(Math.abs(Color.red(v)-Color.red(center))+Math.abs(Color.green(v)-Color.green(center))+Math.abs(Color.blue(v)-Color.blue(center))>35)different++;}return different>=2;}
 private void captureReady(){if(released||preview==null)return;Bitmap capture=preview.capture();if(!hasDetail(capture)){if(capture!=null)capture.recycle();if(++attempts<6)main.postDelayed(this::captureReady,900);return;}
  preview.close();preview=null;removeAllViews();worker.execute(()->{try{java.io.File[] files=getContext().getCacheDir().listFiles((dir,name)->name.startsWith("map-bg-"));if(files!=null&&files.length>=8){java.util.Arrays.sort(files,java.util.Comparator.comparingLong(java.io.File::lastModified));for(int i=0;i<=files.length-8;i++)files[i].delete();}try(java.io.FileOutputStream out=new java.io.FileOutputStream(cacheFile)){capture.compress(Bitmap.CompressFormat.PNG,100,out);}}catch(java.io.IOException ignored){}install(capture);});
 }
 private void install(Bitmap capture){Bitmap blurred=blur(capture,EffectSettings.number(getContext(),"map_backdrop_blur",12,0,24));main.post(()->{if(released){capture.recycle();blurred.recycle();return;}sharp=capture;soft=blurred;invalidate();});}

 void anchor(float screenX,float screenY){x=Math.max(0,Math.min(1,screenX));y=Math.max(.1f,Math.min(.85f,screenY));rebuildMask();invalidate();}
 private static ColorMatrixColorFilter grading(boolean dark){ColorMatrix colors=new ColorMatrix();colors.setSaturation(0);ColorMatrix tone=new ColorMatrix(dark?new float[]{-.45f,0,0,0,135,0,-.45f,0,0,142,0,0,-.45f,0,150,0,0,0,1,0}:new float[]{.52f,0,0,0,116,0,.52f,0,0,116,0,0,.52f,0,116,0,0,0,1,0});colors.postConcat(tone);return new ColorMatrixColorFilter(colors);}
 private void rebuildMask(){float radius=Math.max(getWidth(),getHeight())*EffectSettings.number(getContext(),"map_backdrop_spread",60,20,100)/100f;radial=new RadialGradient(x*getWidth(),y*getHeight(),Math.max(1,radius),new int[]{Color.TRANSPARENT,0xBBFFFFFF,Color.WHITE},new float[]{0,.55f,1},Shader.TileMode.CLAMP);}
 @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);rebuildMask();}
 private static Bitmap blur(Bitmap source,int radius){if(radius==0)return source.copy(Bitmap.Config.ARGB_8888,false);int w=source.getWidth(),h=source.getHeight();int[] a=new int[w*h],b=new int[w*h];source.getPixels(a,0,w,0,0,w,h);radius=Math.max(1,radius);for(int pass=0;pass<2;pass++){for(int row=0;row<h;row++){long r=0,g=0,bl=0;int count=radius*2+1;for(int xx=-radius;xx<=radius;xx++){int c=a[row*w+Math.max(0,Math.min(w-1,xx))];r+=Color.red(c);g+=Color.green(c);bl+=Color.blue(c);}for(int xx=0;xx<w;xx++){b[row*w+xx]=0xFF000000|((int)(r/count)<<16)|((int)(g/count)<<8)|(int)(bl/count);int old=a[row*w+Math.max(0,xx-radius)],next=a[row*w+Math.min(w-1,xx+radius+1)];r+=Color.red(next)-Color.red(old);g+=Color.green(next)-Color.green(old);bl+=Color.blue(next)-Color.blue(old);}}int tw=w;w=h;h=tw;for(int row=0;row<h;row++)for(int col=0;col<w;col++)a[row*w+col]=b[col*h+row];}return Bitmap.createBitmap(a,source.getWidth(),source.getHeight(),Bitmap.Config.ARGB_8888);}
 @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){return false;}
 @Override protected void onDraw(Canvas c){super.onDraw(c);if(sharp==null)return;float cx=x*getWidth(),cy=y*getHeight();float scale=Math.max(getWidth()/(float)sharp.getWidth(),getHeight()/(float)sharp.getHeight());transform.setScale(scale,scale);transform.postTranslate(cx-sharp.getWidth()*scale/2,cy-sharp.getHeight()*scale/2);
  paint.setColorFilter(grading);c.drawBitmap(sharp,transform,paint);
  int save=c.saveLayer(0,0,getWidth(),getHeight(),null);c.drawBitmap(soft,transform,paint);mask.setShader(radial);mask.setXfermode(dstIn);c.drawRect(0,0,getWidth(),getHeight(),mask);c.restoreToCount(save);paint.setColorFilter(null);
 }
 void release(){released=true;main.removeCallbacksAndMessages(null);if(preview!=null){preview.close();preview=null;}worker.shutdownNow();if(sharp!=null){sharp.recycle();sharp=null;}if(soft!=null){soft.recycle();soft=null;}}
 @Override protected void onDetachedFromWindow(){release();super.onDetachedFromWindow();}
}
