package io.github.polestarcn.companion;
import android.graphics.*;import java.util.Random;
/** Fixed particle pool. Trails sample the actual past trajectory; no frame allocations. */
final class AirflowRenderer {
 private final Paint paint=new Paint(3),halo=new Paint(3);private final Matrix matrix=new Matrix();
 private final float[] phase=new float[120],lane=new float[120],rate=new float[120],head=new float[2],tail=new float[2],previous=new float[2];
 private final ParticleDepth depth=new ParticleDepth(lane);
 private RadialGradient glow,bead;private int lastColor;
 AirflowRenderer(){paint.setStrokeCap(Paint.Cap.ROUND);Random r=new Random(930);for(int i=0;i<120;i++){phase[i]=r.nextFloat();lane[i]=r.nextFloat();rate[i]=.8f+.4f*r.nextFloat();}}
 static int color(EffectStateController.Mode m){return m==EffectStateController.Mode.COOLING?0xFF46B0FA:m==EffectStateController.Mode.HEATING?PsaDesign.ORANGE:m==EffectStateController.Mode.NEUTRAL?0xFF9EAFB6:0xFF42D39A;}
 static int side(float fraction,EffectSettings p){float n=fraction*(p.topDensity+p.bottomDensity+p.leftDensity+p.rightDensity);if(n<p.topDensity)return 0;n-=p.topDensity;if(n<p.bottomDensity)return 1;n-=p.bottomDensity;if(n<p.leftDensity)return 2;return 3;}
 // Point at phase t: the exact same function is used for head and every trail sample.
 void position(RectF b,boolean right,int i,float t,EffectSettings p,float[] out){position(b,right,i,t,p,p.climateField,out);}
 void position(RectF b,boolean right,int i,float t,EffectSettings p,ParticleField field,float[] out){
  float sx=right?b.right+b.width()*.14f:b.left-b.width()*.14f,ex=right?b.left-b.width()*.14f:b.right+b.width()*.14f;
  float sy=b.top+b.height()*(.04f+lane[i]*.92f),ey=sy;
  if(p.airAbsorb){field.start(b,i,lane[i],phase[i],out);sx=out[0];sy=out[1];ex=field.x(b,b.centerX());ey=field.y(b,b.centerY());}

  float u=p.airAbsorb?t*t*(3-2*t):t,dx=ex-sx,dy=ey-sy;out[0]=sx+dx*u;out[1]=sy+dy*u;
  if(ParticleMotion.curved(i,p)){float dist=(float)Math.sqrt(dx*dx+dy*dy);if(dist>1){float arc=ParticleMotion.offset(t,i)*Math.min(b.width(),b.height())*p.arcPercent/100f;out[0]-=dy/dist*arc;out[1]+=dx/dist*arc;}}
 }
 void draw(Canvas c,RectF b,boolean right,float seconds,float density,EffectSettings p,EffectStateController.Mode mode,int budget,float fade){drawLayer(c,b,right,seconds,density,p,mode,budget,fade,-1);}
 void drawLayer(Canvas c,RectF b,boolean right,float seconds,float density,EffectSettings p,EffectStateController.Mode mode,int budget,float fade,int layer){
  int color=color(mode);paint.setShader(null);paint.setColor(color);if(glow==null||lastColor!=color){lastColor=color;glow=new RadialGradient(0,0,1,new int[]{(color&0xFFFFFF)|0x55000000,color&0xFFFFFF},null,Shader.TileMode.CLAMP);halo.setShader(glow);bead=new RadialGradient(0,0,1,new int[]{color,(color&0xFFFFFF)|0xB0000000,color&0xFFFFFF},new float[]{0,.4f,1},Shader.TileMode.CLAMP);}
  ParticleField field=(mode==EffectStateController.Mode.PURIFICATION||mode==EffectStateController.Mode.VENTILATION)?p.cleanField:p.climateField;if(p.airAbsorb&&field.sum()==0)return;
  int requested=p.airSpheres?p.particles:p.lines;if(p.airAbsorb)requested=field.count(requested,budget);int n=Math.min(120,Math.min(requested,budget));
  for(int i=0;i<n;i++){if(layer>=0&&(depth.behind(i,n,field.behind)?0:1)!=layer)continue;
   float t=(seconds*.20f*p.airSpeed*rate[i]+phase[i])%1f;position(b,right,i,t,p,field,head);
   float edge=Math.min(1,t*8)*(p.fade||p.airAbsorb?Math.min(1,(1-t)*7):1),scale=p.particleSizePercent/100f;
   int alpha=(int)(Math.min(255,p.strength*2.2f)*edge*fade*(layer==0?.7f:1));
   if(p.airSpheres){float radius=density*(.7f+3f*lane[i]*lane[i])*scale;
    matrix.setScale(radius*2.8f,radius*2.8f);matrix.postTranslate(head[0],head[1]);glow.setLocalMatrix(matrix);halo.setAlpha(alpha);c.drawCircle(head[0],head[1],radius*2.8f,halo);matrix.setScale(radius,radius);matrix.postTranslate(head[0],head[1]);bead.setLocalMatrix(matrix);paint.setShader(bead);paint.setAlpha(alpha);c.drawCircle(head[0],head[1],radius,paint);paint.setShader(null);
   }else{float span=(.09f+.07f*lane[i])*p.trailPercent/100f;position(b,right,i,Math.max(0,t-span),p,field,previous);paint.setStrokeWidth(density*(.9f+lane[i]*.85f)*scale);
    for(int j=1;j<=32;j++){float f=j/32f;position(b,right,i,Math.max(0,t-span*(1-f)),p,field,tail);paint.setAlpha((int)(alpha*f*f));c.drawLine(previous[0],previous[1],tail[0],tail[1],paint);previous[0]=tail[0];previous[1]=tail[1];}
   }
  }
 }
}
