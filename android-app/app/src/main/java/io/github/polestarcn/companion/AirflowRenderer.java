package io.github.polestarcn.companion;
import android.graphics.*;import java.util.Random;
/** Cached unit gradients: a crisp leading head with a continuous fading trail, never uniform dots. */
final class AirflowRenderer {
 private final Paint paint=new Paint(3);private final Matrix matrix=new Matrix();
 private final float[] phase=new float[120],lane=new float[120],rate=new float[120];
 private final float[] transform={1,0,0,0,1,0,0,0,1};private LinearGradient streak;private int lastColor;
 AirflowRenderer(){paint.setStrokeCap(Paint.Cap.ROUND);Random r=new Random(930);for(int i=0;i<120;i++){phase[i]=r.nextFloat();lane[i]=r.nextFloat();rate[i]=.8f+.4f*r.nextFloat();}}
 static int color(EffectStateController.Mode m){return m==EffectStateController.Mode.COOLING?0xFF46B0FA:m==EffectStateController.Mode.HEATING?PsaDesign.ORANGE:m==EffectStateController.Mode.NEUTRAL?0xFF9EAFB6:0xFF42D39A;}
 void draw(Canvas c,RectF b,boolean right,float seconds,float density,EffectSettings p,EffectStateController.Mode mode,int budget,float fade){
  int color=color(mode);if(streak==null||lastColor!=color){lastColor=color;streak=new LinearGradient(0,0,1,0,new int[]{color&0x00FFFFFF,(color&0x00FFFFFF)|0x24000000,(color&0x00FFFFFF)|0x98000000,color},new float[]{0,.25f,.78f,1},Shader.TileMode.CLAMP);paint.setShader(streak);}
  float start=right?b.right+b.width()*.14f:b.left-b.width()*.14f,end=right?b.left-b.width()*.14f:b.right+b.width()*.14f;
  for(int i=0,n=Math.min(p.lines,budget);i<n;i++){
   float t=(seconds*.20f*p.airSpeed*rate[i]+phase[i])%1f;
   float sx=start,sy=b.top+b.height()*(.04f+lane[i]*.92f),ex=end,ey=sy;
   if(p.airAbsorb){sx=(i%2==0?b.left-b.width()*.12f:b.right+b.width()*.12f);ex=b.centerX();ey=b.centerY()+(lane[i]-.5f)*b.height()*.38f;}
   float x=sx+(ex-sx)*t,y=sy+(ey-sy)*t+(float)Math.sin(t*6.283f+i)*b.height()*.015f;
   float dx=ex-sx,dy=ey-sy,dist=(float)Math.sqrt(dx*dx+dy*dy);if(dist<1)continue;
   float len=b.width()*(.07f+.065f*lane[i]);float ux=dx/dist,uy=dy/dist,tx=x-ux*len,ty=y-uy*len;
   transform[0]=ux*len;transform[1]=-uy;transform[2]=tx;transform[3]=uy*len;transform[4]=ux;transform[5]=ty;matrix.setValues(transform);streak.setLocalMatrix(matrix);
   float edge=Math.min(1,t*8)*(p.fade||p.airAbsorb?Math.min(1,(1-t)*7):1);paint.setAlpha((int)(Math.min(255,p.strength*2.2f)*edge*fade));paint.setStrokeWidth(density*(.9f+lane[i]*.85f));c.drawLine(tx,ty,x,y,paint);
  }
 }
}
