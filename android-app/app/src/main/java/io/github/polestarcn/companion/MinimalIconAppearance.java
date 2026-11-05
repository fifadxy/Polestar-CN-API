package io.github.polestarcn.companion;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.widget.ImageView;
import androidx.core.content.ContextCompat;
/** Independent per-skin minimal icon size and glyph weight; never modifies vehicle state. */
final class MinimalIconAppearance {
  private static final int SIDE=96;
  private static final LruCache<String,Bitmap> cache=new LruCache<>(64);
  private MinimalIconAppearance(){}
  static String sizeKey(Context c){return "minimal_icon_size_"+UiSkin.get(c);}
  static String weightKey(Context c){return "minimal_icon_weight_"+UiSkin.get(c);}
  static int sizePercent(Context c){return EffectSettings.number(c,sizeKey(c),UiSkin.PS_DEFAULT.equals(UiSkin.get(c))?125:100,65,175);}
  static int weightPercent(Context c){return EffectSettings.number(c,weightKey(c),100,70,150);}
  static Drawable drawable(Context c,int resource){
    Drawable original=ContextCompat.getDrawable(c,resource);
    if(original==null)return null;
    if(!UiDisplayMode.minimal(c)||weightPercent(c)==100||resource==R.drawable.ic_service_reference)return original.mutate();
    int weight=weightPercent(c);
    String key=resource+":"+weight+":"+c.getResources().getDisplayMetrics().densityDpi;
    Bitmap ready=cache.get(key);
    if(ready!=null&&!ready.isRecycled())return new BitmapDrawable(c.getResources(),ready);
    Bitmap input=Bitmap.createBitmap(SIDE,SIDE,Bitmap.Config.ARGB_8888);
    Canvas canvas=new Canvas(input);Drawable glyph=original.mutate();
    glyph.setTint(Color.WHITE);glyph.setBounds(8,8,SIDE-8,SIDE-8);glyph.draw(canvas);
    int[] source=new int[SIDE*SIDE],pixels=new int[SIDE*SIDE];input.getPixels(source,0,SIDE,0,0,SIDE,SIDE);
    int radius=Math.max(1,Math.min(3,Math.round(Math.abs(weight-100)/20f)));
    for(int y=0;y<SIDE;y++)for(int x=0;x<SIDE;x++){
      int opacity=weight>100?0:255;
      for(int dy=-radius;dy<=radius;dy++)for(int dx=-radius;dx<=radius;dx++){
        if(dx*dx+dy*dy>radius*radius)continue;
        int xx=x+dx,yy=y+dy;
        int neighbor=(xx>=0&&xx<SIDE&&yy>=0&&yy<SIDE)?Color.alpha(source[yy*SIDE+xx]):0;
        opacity=weight>100?Math.max(opacity,neighbor):Math.min(opacity,neighbor);
      }
      pixels[y*SIDE+x]=Color.argb(opacity,255,255,255);
    }
    input.recycle();Bitmap result=Bitmap.createBitmap(SIDE,SIDE,Bitmap.Config.ARGB_8888);
    result.setPixels(pixels,0,SIDE,0,0,SIDE,SIDE);cache.put(key,result);
    return new BitmapDrawable(c.getResources(),result);
  }
  static void apply(ImageView icon,int drawableId){icon.setImageDrawable(drawable(icon.getContext(),drawableId));icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);}
}