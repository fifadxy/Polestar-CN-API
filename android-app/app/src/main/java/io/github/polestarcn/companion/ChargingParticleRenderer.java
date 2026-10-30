package io.github.polestarcn.companion;
import android.graphics.*;import java.util.Random;
/** Fixed pool and cached glow; no allocations in a frame. Five styles, two directions. */
final class ChargingParticleRenderer {
 private final float[] phase=new float[150],x=new float[150],size=new float[150],speed=new float[150],drift=new float[150];
 private final ParticleDepth depth=new ParticleDepth(size);
 private final Paint core=new Paint(3),halo=new Paint(3);private final Matrix transform=new Matrix();
 private final RadialGradient bead=new RadialGradient(0,0,1,new int[]{0xFFFF6B00,0xB0FF6B00,0x00FF6B00},new float[]{0,.4f,1},Shader.TileMode.CLAMP);private final float[] origin=new float[2];
 private final RadialGradient glow=new RadialGradient(0,0,1,new int[]{0x55FF6B00,0x00FF6B00},null,Shader.TileMode.CLAMP);
 ChargingParticleRenderer(){Random r=new Random(23093);for(int i=0;i<150;i++){phase[i]=r.nextFloat();x[i]=r.nextFloat();size[i]=.5f+3.5f*(float)Math.pow(r.nextFloat(),3);speed[i]=.65f+r.nextFloat()*.8f;drift[i]=r.nextFloat()*2-1;}halo.setShader(glow);core.setColor(PsaDesign.ORANGE);}
 void draw(Canvas c,RectF b,float portX,float portY,float seconds,float density,EffectSettings p,int budget,float fade){
  drawLayer(c,b,portX,portY,seconds,density,p,budget,fade,-1);}
 void drawLayer(Canvas c,RectF b,float portX,float portY,float seconds,float density,EffectSettings p,int budget,float fade,int layer){
  ParticleField field=p.chargeField;if(p.absorb&&field.sum()==0)return;portX=field.x(b,portX);portY=field.y(b,portY);
  boolean soft=p.style==1||p.style==3;int count=p.absorb?field.count(p.particles,Math.min(150,budget)):Math.min(p.particles,Math.min(150,budget));float margin=12*density;
  for(int i=0;i<count;i++){if(layer>=0&&(depth.behind(i,count,field.behind)?0:1)!=layer)continue;float t=(seconds*.25f*p.chargeSpeed*speed[i]+phase[i])%1f;
   float px,py,radius=size[i]*density*p.particleSizePercent/100f;
   if(p.absorb){field.start(b,i,x[i],phase[i],origin);float sx=origin[0],sy=origin[1];float u=t*t*(3-2*t);px=sx+(portX-sx)*u;py=sy+(portY-sy)*u;if(ParticleMotion.curved(i,p)){float dx=portX-sx,dy=portY-sy,dist=(float)Math.sqrt(dx*dx+dy*dy);float arc=ParticleMotion.offset(t,i+(p.style>=2?p.style*19:0))*Math.min(b.width(),b.height())*p.arcPercent/100f;if(dist>1){px-=dy/dist*arc;py+=dx/dist*arc;}}radius*=Math.min(1,(1-t)*3);}
   else{px=b.left+x[i]*b.width()+drift[i]*density*10*(float)Math.sin(t*6.28f);if(p.style>=2)px+=(b.centerX()-px)*.12f*(float)Math.sin(t*Math.PI);if(p.style==4)px+=drift[i]*24*density*(float)Math.sin(t*Math.PI);py=b.bottom+margin-(b.height()+2*margin)*t;}
   float edge=Math.min(1,t*8)*Math.min(1,(1-t)*7);core.setAlpha((int)(255*edge*fade*(layer==0?.7f:1)));halo.setAlpha((int)(255*edge*fade));radius*=fade;if(radius<.15f*density)continue;
   if(soft){float r=radius*2.8f;transform.setScale(r,r);transform.postTranslate(px,py);glow.setLocalMatrix(transform);c.drawCircle(px,py,r,halo);}
   transform.setScale(radius,radius);transform.postTranslate(px,py);bead.setLocalMatrix(transform);core.setShader(bead);c.drawCircle(px,py,radius,core);
  }
 }
}
