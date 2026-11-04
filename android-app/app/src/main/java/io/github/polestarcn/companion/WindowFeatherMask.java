package io.github.polestarcn.companion;
import android.graphics.*;
/** Cached inward alpha feather. Geometry work occurs on change, never in the animation loop. */
final class WindowFeatherMask {
 private Bitmap mask;private int[] sharp,output;private float[] distance;private int width,height,last=-1;
 void invalidate(){mask=null;sharp=null;output=null;distance=null;last=-1;}
 Bitmap get(Path glass,RectF bounds,int feather){if(mask==null)build(glass,bounds);if(last==feather)return mask;last=feather;float radius=Math.max(.01f,Math.min(width,height)*.08f*feather/100f);for(int i=0;i<output.length;i++){float t=Math.min(1,distance[i]/radius);float a=t*t*(3-2*t);output[i]=((int)(Color.alpha(sharp[i])*a)<<24)|0xFFFFFF;}mask.setPixels(output,0,width,0,0,width,height);return mask;}
 private void build(Path glass,RectF b){float scale=Math.min(1,384f/Math.max(1,Math.max(b.width(),b.height())));width=Math.max(1,(int)Math.ceil(b.width()*scale));height=Math.max(1,(int)Math.ceil(b.height()*scale));mask=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(mask);c.scale(scale,scale);c.translate(-b.left,-b.top);Paint white=new Paint(Paint.ANTI_ALIAS_FLAG);white.setColor(Color.WHITE);c.drawPath(glass,white);sharp=new int[width*height];output=new int[sharp.length];distance=new float[sharp.length];mask.getPixels(sharp,0,width,0,0,width,height);
  for(int y=0;y<height;y++)for(int x=0;x<width;x++){int i=y*width+x;if(Color.alpha(sharp[i])==0){distance[i]=0;continue;}float d=x==0||y==0||x==width-1||y==height-1?1:100000;if(x>0)d=Math.min(d,distance[i-1]+1);if(y>0){d=Math.min(d,distance[i-width]+1);if(x>0)d=Math.min(d,distance[i-width-1]+1.414214f);if(x+1<width)d=Math.min(d,distance[i-width+1]+1.414214f);}distance[i]=d;}
  for(int y=height-1;y>=0;y--)for(int x=width-1;x>=0;x--){int i=y*width+x;float d=distance[i];if(x+1<width)d=Math.min(d,distance[i+1]+1);if(y+1<height){d=Math.min(d,distance[i+width]+1);if(x>0)d=Math.min(d,distance[i+width-1]+1.414214f);if(x+1<width)d=Math.min(d,distance[i+width+1]+1.414214f);}distance[i]=d;}
 }
}
