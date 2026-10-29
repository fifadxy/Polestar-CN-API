package io.github.polestarcn.companion;
import android.content.Context;import android.graphics.RectF;
/** Immutable per-effect geometry. Shared budget across depth passes; no frame allocations. */
final class ParticleField {
 final int range,top,bottom,left,right,behind,focusX,focusY;final boolean customFocus,customX,customY;
 ParticleField(Context c,String key,int legacyRange,int legacyTop,int legacyBottom,int legacyLeft,int legacyRight){range=EffectSettings.number(c,key+"range",legacyRange,25,400);top=EffectSettings.number(c,key+"top",legacyTop,0,300);bottom=EffectSettings.number(c,key+"bottom",legacyBottom,0,300);left=EffectSettings.number(c,key+"left",legacyLeft,0,300);right=EffectSettings.number(c,key+"right",legacyRight,0,300);behind=EffectSettings.number(c,key+"behind",33,0,100);focusX=EffectSettings.number(c,key+"focus_x",50,0,100);focusY=EffectSettings.number(c,key+"focus_y",50,0,100);customX=c.getSharedPreferences("preferences",0).contains(key+"focus_x");customY=c.getSharedPreferences("preferences",0).contains(key+"focus_y");customFocus=c.getSharedPreferences("preferences",0).contains(key+"focus_x")||c.getSharedPreferences("preferences",0).contains(key+"focus_y");}
 int sum(){return top+bottom+left+right;}
 int count(int base,int budget){return Math.min(budget,Math.round(base*sum()/400f));}
 int side(float fraction){float n=fraction*sum();if(n<top)return 0;n-=top;if(n<bottom)return 1;n-=bottom;if(n<left)return 2;return 3;}
 boolean back(int index){return (index*37+11)%100<behind;}
 float x(RectF b,float fallback){return customX?b.left+b.width()*focusX/100f:fallback;}
 float y(RectF b,float fallback){return customY?b.top+b.height()*focusY/100f:fallback;}
 void start(RectF b,int i,float lane,float fraction,float[] out){float scale=range/100f;int side=side(lane);if(side<2){out[0]=b.centerX()+(fraction-.5f)*b.width()*1.2f*scale;out[1]=b.centerY()+(side==0?-1:1)*b.height()*.7f*scale;}else{out[0]=b.centerX()+(side==2?-1:1)*b.width()*.65f*scale;out[1]=b.centerY()+(fraction-.5f)*b.height()*1.3f*scale;}}
}
