package io.github.polestarcn.companion;
import android.content.Context;import android.graphics.*;import android.widget.FrameLayout;
/** Live hardware map: WebGL tiles are never copied through a software Bitmap canvas. */
final class ParkingBackdrop extends FrameLayout {
 private ParkingWebPreview preview;private boolean released;private float x=.5f,y=.42f;
 public ParkingBackdrop(Context c){this(c,null);}
 ParkingBackdrop(Context c,LocationSnapshot point){super(c);setBackgroundColor(PsaDesign.canvas(AppPreferences.dark(c)));setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);if(point==null)return;preview=new ParkingWebPreview(c,point,AppPreferences.dark(c),()->{},true);addView(preview.view,new LayoutParams(-1,-1));preview.whenReady(()->{if(preview!=null)preview.styleBackdrop(x,y);});}
 void anchor(float x,float y){this.x=Math.max(0,Math.min(1,x));this.y=Math.max(.1f,Math.min(.85f,y));if(preview!=null)preview.styleBackdrop(this.x,this.y);}
 void pause(){if(preview!=null)preview.pause();}
 void resume(){if(preview!=null)preview.resume();}
 @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){return false;}
 void release(){if(released)return;released=true;if(preview!=null){preview.close();preview=null;}removeAllViews();}
 @Override protected void onDetachedFromWindow(){release();super.onDetachedFromWindow();}
 static boolean hasDetail(Bitmap b){if(b==null||b.getWidth()<16||b.getHeight()<16)return false;int center=b.getPixel(b.getWidth()/2,b.getHeight()/2),different=0;for(int y=1;y<5;y++)for(int x=1;x<5;x++){int v=b.getPixel(x*b.getWidth()/5,y*b.getHeight()/5);if(Math.abs(Color.red(v)-Color.red(center))+Math.abs(Color.green(v)-Color.green(center))+Math.abs(Color.blue(v)-Color.blue(center))>35)different++;}return different>=2;}
}
