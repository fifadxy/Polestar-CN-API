package io.github.polestarcn.companion;
import android.graphics.*;import java.util.Random;
/** A1 vectors and A2 three primary laminar paths. Entire car is crossed at full PNG opacity. */
final class AirflowRenderer {
 private final Paint paint=new Paint(3);private final Path[] paths=new Path[9];private final PathMeasure[] measures=new PathMeasure[9];private final Path segment=new Path();private final float[] phase=new float[120],lane=new float[120],rate=new float[120],length=new float[120];
 private float oldW,oldH,oldL,oldT;private boolean oldRight;
 AirflowRenderer(){paint.setStrokeCap(Paint.Cap.ROUND);paint.setStyle(Paint.Style.STROKE);Random r=new Random(930);for(int i=0;i<120;i++){phase[i]=r.nextFloat();lane[i]=r.nextFloat();rate[i]=.8f+.4f*r.nextFloat();length[i]=6+4*r.nextFloat();}for(int i=0;i<9;i++){paths[i]=new Path();measures[i]=new PathMeasure();}}
 private void geometry(RectF b,boolean right){if(oldW==b.width()&&oldH==b.height()&&oldL==b.left&&oldT==b.top&&oldRight==right)return;oldW=b.width();oldH=b.height();oldL=b.left;oldT=b.top;oldRight=right;float start=right?b.right+b.width()*.12f:b.left-b.width()*.12f,end=right?b.left-b.width()*.12f:b.right+b.width()*.12f;
  for(int i=0;i<9;i++){float y=b.top+b.height()*(i<3?.22f+i*.28f:.12f+(i-3)*.15f);paths[i].rewind();paths[i].moveTo(start,y);paths[i].cubicTo(start+(end-start)*.32f,y-b.height()*.07f,start+(end-start)*.7f,y+b.height()*.07f,end,y);measures[i].setPath(paths[i],false);}}
 void draw(Canvas c,RectF b,boolean frontRight,float seconds,float density,EffectSettings p,EffectStateController.Mode mode,int budget,float fade){geometry(b,frontRight);
  int color=mode==EffectStateController.Mode.COOLING?0xFF46B0FA:mode==EffectStateController.Mode.HEATING?PsaDesign.ORANGE:mode==EffectStateController.Mode.NEUTRAL?0xFF9EAFB6:0xFF42D39A;
  paint.setColor(color);boolean laminar=mode==EffectStateController.Mode.VENTILATION||mode==EffectStateController.Mode.PURIFICATION;
  if(laminar){int n=3+Math.min(6,Math.min(p.lines,budget)/20);for(int i=0;i<n;i++){float total=measures[i].getLength(),t=(seconds*.22f*p.airSpeed+phase[i])%1f;for(int j=0;j<4;j++){float end=t*total-j*total*.045f,start=Math.max(0,end-total*.045f);if(end<=0)continue;segment.rewind();measures[i].getSegment(start,Math.min(total,end),segment,true);float tail=p.fade?Math.min(1,(1-t)*5):1;paint.setAlpha((int)((.53f-j*.13f)*p.strength*2*tail*fade));paint.setStrokeWidth(density*(i<3?1.7f:.8f));c.drawPath(segment,paint);}}}
  else{int count=Math.min(p.lines,budget);float start=frontRight?b.right+b.width()*.12f:b.left-b.width()*.12f,end=frontRight?b.left-b.width()*.12f:b.right+b.width()*.12f;
   for(int i=0;i<count;i++){float t=(seconds*.24f*p.airSpeed*rate[i]+phase[i])%1f,x=start+(end-start)*t,y=b.top+b.height()*(.05f+lane[i]*.9f)+(float)Math.sin(t*6.28f+i)*b.height()*.018f;float alpha=p.fade?Math.min(1,(1-t)*6)*Math.min(1,t*8):1;paint.setAlpha((int)(p.strength*1.6f*alpha*fade));paint.setStrokeWidth(2*density);float dx=length[i]*density*(frontRight?-1:1);c.drawLine(x,y,x+dx,y,paint);}
  }
 }
}
