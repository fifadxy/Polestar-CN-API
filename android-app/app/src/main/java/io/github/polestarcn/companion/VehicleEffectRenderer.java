package io.github.polestarcn.companion;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import java.util.Random;
/**
 * Real-time, low-allocation particles and air paths over an unchanged original PNG.
 * Uses Canvas, never a GIF / video. Core is solid; optional halo uses radial gradient.
 */
public final class VehicleEffectRenderer {
  private static final int ORANGE=Color.rgb(255,107,0),COOL=Color.rgb(70,176,250),GREEN=Color.rgb(66,211,154);
  private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Matrix glowMatrix=new Matrix();
  private final RadialGradient glow=new RadialGradient(0,0,24,
      new int[]{Color.argb(82,255,107,0),Color.argb(34,255,107,0),Color.TRANSPARENT},
      new float[]{0,.38f,1},Shader.TileMode.CLAMP);
  private final Particle[] particles=new Particle[150];
  private final Path path=new Path();
  private VehicleEffectSettings.Mode mode=VehicleEffectSettings.Mode.NONE;
  private String style="C1a";private boolean absorb;private int count=60,windCount=12,strength=65;
  private float speed=1f;private final float density;
  private long start=SystemClock.uptimeMillis();
  private static class Particle {float x,y,phase,r,drift,swing;boolean fore;}
  public VehicleEffectRenderer(Context context){
    density=context.getResources().getDisplayMetrics().density;
    Random random=new Random(294367);
    for(int i=0;i<particles.length;i++){
      Particle p=new Particle();p.x=.07f+random.nextFloat()*.86f;
      p.y=.10f+random.nextFloat()*.8f;p.phase=random.nextFloat();
      p.r=(float)(1.0+7*Math.pow(random.nextFloat(),2.0));p.drift=(random.nextFloat()-.5f)*.10f;
      p.swing=random.nextFloat()*6.283f;p.fore=random.nextInt(5)==0;particles[i]=p;
    }
    paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
  }
  public void configure(Context context) {
    style=VehicleEffectSettings.style(context);absorb=VehicleEffectSettings.absorption(context);
    count=VehicleEffectSettings.count(context);windCount=VehicleEffectSettings.airflow(context);
    strength=VehicleEffectSettings.strength(context);speed=VehicleEffectSettings.speed(context);
  }
  public void mode(VehicleEffectSettings.Mode next){
    if(next==null)next=VehicleEffectSettings.Mode.NONE;
    if(next!=mode){mode=next;start=SystemClock.uptimeMillis();}
  }
  public boolean active(){return mode!=VehicleEffectSettings.Mode.NONE;}
  public void draw(Canvas canvas,RectF rect,boolean foreground){
    if(!active()||rect.isEmpty())return;
    long tick=SystemClock.uptimeMillis()-start;
    canvas.save();canvas.translate(rect.left,rect.top);
    if(mode==VehicleEffectSettings.Mode.CHARGING)drawParticles(canvas,rect.width(),rect.height(),tick,foreground);
    else drawWind(canvas,rect.width(),rect.height(),tick,foreground);
    paint.setShader(null);canvas.restore();
  }
  private void drawParticles(Canvas canvas,float w,float h,long tick,boolean foreground){
    float base=Math.min(w,h),cycle=4000f;boolean halo=style.equals("C1d")||style.equals("C1+d");
    boolean arc=style.equals("C1+c"),natural=style.equals("C1a")||style.equals("C1d");
    for(int i=0;i<count;i++){
      Particle p=particles[i];if(p.fore!=foreground)continue;
      float t=fract(p.phase+tick*speed/(cycle*(.72f+.65f*fract(i*.618f))));
      float x,y,r=p.r*density;
      if(absorb){
        float u=t*t*(2f-t);
        float sx=p.x,sy=p.y;
        float tx=.28f,ty=.55f;
        float bend=arc?.18f:(natural?.065f:.11f);
        x=(1-u)*(1-u)*sx+2*(1-u)*u*(sx+tx)*.5f+u*u*tx
            + (float)Math.sin(u*Math.PI)*bend*(p.x<.5f?1:-1);
        y=(1-u)*(1-u)*sy+2*(1-u)*u*(sy+ty)*.5f+u*u*ty
            + (float)Math.sin(u*Math.PI)*bend*.25f;
        r*=1f-.85f*t*t;
      }else{
        x=p.x+p.drift*t+(float)Math.sin(t*6.28f+p.swing)*.012f;
        y=1.05f-1.18f*t+(p.y-.5f)*.18f;
        if(arc)x+=(float)Math.sin(t*Math.PI*2+p.swing)*.025f;
      }
      if(x<-.1f||x>1.1f||y<-.1f||y>1.1f)continue;
      float fade=Math.min(1f,Math.min(t*10,(1-t)*10));
      int alpha=(int)(255*fade*(foreground?.70f:.88f));
      float cx=x*w,cy=y*h;
      if(halo){
        float gr=Math.max(8f*density,r*3.4f);
        glowMatrix.reset();glowMatrix.setScale(gr/24f,gr/24f);glowMatrix.postTranslate(cx,cy);
        glow.setLocalMatrix(glowMatrix);paint.setShader(glow);
        paint.setAlpha((int)(alpha*.6f));paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx,cy,gr,paint);paint.setShader(null);
      }
      paint.setStyle(Paint.Style.FILL);paint.setColor(ORANGE);paint.setAlpha(alpha);
      canvas.drawCircle(cx,cy,Math.max(.7f*density,r*.5f),paint);
    }
  }
  private void drawWind(Canvas canvas,float w,float h,long tick,boolean foreground){
    boolean laminar=mode==VehicleEffectSettings.Mode.VENTILATION;
    int color=mode==VehicleEffectSettings.Mode.COOLING?COOL:mode==VehicleEffectSettings.Mode.HEATING?ORANGE:GREEN;
    paint.setShader(null);paint.setColor(color);paint.setStyle(Paint.Style.STROKE);
    int tracks=laminar?Math.max(3,Math.min(9,(windCount+3)/5)):windCount;
    float width=laminar?1.8f*density:2.25f*density;
    paint.setStrokeWidth(width*(.65f+strength/175f));
    for(int track=0;track<tracks;track++){
      boolean front=(track%4==0);
      if(front!=foreground)continue;
      float yBase=.25f+.5f*(track+.5f)/tracks;
      int pulses=laminar?3:2;
      for(int wave=0;wave<pulses;wave++){
        float phase=fract(tick*speed/(laminar?5000f:3400f)+track*.173f+wave/(float)pulses);
        float xHead=1.10f-phase*1.26f;
        float length=laminar?.28f:.085f;
        float xTail=Math.min(1.06f,xHead+length);
        float xEnd=Math.max(-.05f,xHead);
        if(xTail<xEnd)continue;
        float progress=Math.max(0,Math.min(1,(xHead+.06f)/.28f));
        int opacity=(int)((laminar?160:190)*(front?.55f:1f)*(strength/100f)*Math.min(1f,progress));
        paint.setAlpha(opacity);
        path.reset();
        int steps=laminar?14:5;
        for(int j=0;j<=steps;j++){
          float x=xEnd+(xTail-xEnd)*j/steps;
          float y=yBase+(float)Math.sin((x*2.5f+track*.19f)*Math.PI)*.035f;
          if(j==0)path.moveTo(x*w,y*h);else path.lineTo(x*w,y*h);
        }
        canvas.drawPath(path,paint);
      }
    }
    paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);
  }
  private static float fract(float value){return value-(float)Math.floor(value);}
}
