package io.github.polestarcn.companion;
import android.graphics.*;
import java.util.Random;
/** Solid native VI particles: deterministic pool, no frame allocations and no GIF. */
final class ChargingParticleRenderer {
 private static final float TAU=6.2831853f;
 private final float[] phase=new float[150],x=new float[150],size=new float[150],rate=new float[150],drift=new float[150];
 private final Paint core=new Paint(3),halo=new Paint(3);
 private final Matrix transform=new Matrix();
 private final RadialGradient glow=new RadialGradient(0,0,1,new int[]{0x55FF6B00,0x00FF6B00},null,Shader.TileMode.CLAMP);
 ChargingParticleRenderer(){
  Random r=new Random(23093);
  for(int i=0;i<150;i++){
   phase[i]=r.nextFloat();x[i]=r.nextFloat();
   size[i]=.5f+3.5f*(float)Math.pow(r.nextFloat(),3);
   rate[i]=.65f+r.nextFloat()*.8f;drift[i]=r.nextFloat()*2-1;
  }
  halo.setShader(glow);core.setColor(PsaDesign.ORANGE);
 }
 void draw(Canvas c,RectF b,float portX,float portY,float seconds,float density,EffectSettings p,int budget,float fade){
  draw(c,b,portX,portY,seconds,density,p,budget,fade,0);
 }
 void draw(Canvas c,RectF b,float portX,float portY,float seconds,float density,EffectSettings p,int budget,float fade,int art){
  boolean soft=p.style==1||p.style==3;
  boolean top=art==R.drawable.vehicle_top||art==R.drawable.psa_vehicle_top_white
      ||art==R.drawable.psa_vehicle_top_dark||art==R.drawable.psa_vehicle_top_blue
      ||art==R.drawable.psa_vehicle_top_silver;
  boolean right=VehicleArt.frontAtRight(art);
  int count=Math.min(p.particles,budget);
  float range=p.spawnRange/100f,curvature=p.arcAmount/100f;
  float scale=p.particleScale/100f;
  for(int i=0;i<count;i++){
   float t=(seconds*.25f*p.chargeSpeed*rate[i]+phase[i])%1f;
   float px,py,radius=size[i]*density*scale;
   boolean straight=((i*37+13)%100)<p.straightPercent;
   float arc=straight?0:curvature;
   float wobble=(float)Math.sin(t*TAU+phase[i]*TAU)*drift[i];
   if(p.absorb){
    float angle=(x[i]+phase[i]*.17f)*TAU;
    // Spawn inside the visible overlay, never from an invisible region beyond the frame.
    float sx=b.centerX()+(b.width()*.43f*Math.min(1f,range))*(float)Math.cos(angle);
    float sy=b.centerY()+(b.height()*.42f*Math.min(1f,range))*(float)Math.sin(angle);
    float tx=portX,ty=portY;
    if(top){
      // Four-sided absorption underneath the vehicle silhouette for top-down graphics.
      float cosine=(float)Math.cos(angle),sine=(float)Math.sin(angle);
      if(Math.abs(cosine)>Math.abs(sine)){
       tx=b.centerX()+Math.signum(cosine)*b.width()*.29f;
       ty=b.centerY()+sine*b.height()*.19f;
      }else{
       tx=b.centerX()+cosine*b.width()*.20f;
       ty=b.centerY()+Math.signum(sine)*b.height()*.28f;
      }
    }else{
      // Most particles enter the underbody; some enter from the front fascia.
      if(i%4==0){tx=b.left+b.width()*(right?.82f:.18f);ty=b.top+b.height()*.52f;}
      else{tx=b.left+b.width()*(.24f+.50f*x[i]);ty=b.top+b.height()*.78f;}
    }
    float u=t*t*(3f-2f*t);
    px=sx+(tx-sx)*u;
    py=sy+(ty-sy)*u;
    if(!straight){
      float curve=(float)Math.sin(Math.PI*t)*arc*(p.style==4?1.35f:1f);
      px+=drift[i]*b.width()*curve*.35f;
      py+=wobble*b.height()*curve*.4f;
    }
    radius*=Math.max(.12f,1f-.90f*t*t);
   }else{
    px=b.left+b.width()*(.08f+.84f*x[i]);
    py=b.bottom-b.height()*(.16f+.72f*t);
    if(!straight){
      px+=drift[i]*b.width()*arc*(float)Math.sin(Math.PI*t)*(p.style==4?.7f:.38f);
      py+=(float)Math.sin(t*TAU+phase[i]*TAU)*arc*b.height()*.08f;
    }
   }
   float edge=Math.min(1f,Math.min(t*9f,(1f-t)*9f));
   int alpha=Math.min(255,Math.max(0,(int)(255f*edge*fade)));
   if(alpha<2||radius<.1f*density)continue;
   core.setAlpha(alpha);halo.setAlpha(alpha);
   if(soft){
    float r=radius*2.8f;
    transform.setScale(r,r);transform.postTranslate(px,py);
    glow.setLocalMatrix(transform);c.drawCircle(px,py,r,halo);
   }
   c.drawCircle(px,py,radius,core);
  }
 }
}
