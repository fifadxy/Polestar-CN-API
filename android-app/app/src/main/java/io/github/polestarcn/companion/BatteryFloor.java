package io.github.polestarcn.companion;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import java.util.Locale;

/** Opt-in floor battery indicator. Uses existing vehicle telemetry; never sends commands. */
final class BatteryFloor {
  static final String PREFIX="battery_floor_";
  static final String VI="#FF6B00", GREEN="#21E57B";
  static final String[] CORNERS={"back_left","back_right","front_left","front_right"};

  static final class Settings {
    final boolean enabled,idle,status,grid;
    final int color,opacity,gridOpacity,glow,density,length,height;
    final int[] x=new int[4],y=new int[4];
    Settings(Context c){
      enabled=AppPreferences.visible(c,PREFIX+"enabled",false);
      idle=AppPreferences.visible(c,PREFIX+"idle",false);
      status=AppPreferences.visible(c,PREFIX+"status",true);
      grid=AppPreferences.visible(c,PREFIX+"grid",true);
      String preset=AppPreferences.get(c,PREFIX+"color_preset","vi");
      color=parseColor("green".equals(preset)?GREEN:"custom".equals(preset)
          ?AppPreferences.get(c,PREFIX+"custom_color",VI):VI);
      opacity=EffectSettings.number(c,PREFIX+"opacity",80,0,100);
      gridOpacity=EffectSettings.number(c,PREFIX+"grid_opacity",24,0,100);
      glow=EffectSettings.number(c,PREFIX+"glow",1,0,2);
      density=EffectSettings.number(c,PREFIX+"density",1,0,2);
      length=EffectSettings.number(c,PREFIX+"length",100,80,120);
      height=EffectSettings.number(c,PREFIX+"height",100,70,130);
      for(int i=0;i<4;i++){
        x[i]=EffectSettings.number(c,corner(i,"x"),0,-25,25);
        y[i]=EffectSettings.number(c,corner(i,"y"),0,-25,25);
      }
    }
    boolean visible(boolean charging,Integer soc){
      // Charging may be confirmed before the battery percentage is available.
      // Show an empty floor with --%, never fabricate 0% or drop the effect.
      return enabled&&(charging||(idle&&soc!=null&&soc>=0&&soc<=100));
    }
  }

  static boolean replaces(Context c,VehicleSnapshot snapshot){
    return snapshot!=null&&ChargingVisibility.shouldShow(snapshot)
        &&new Settings(c).visible(true,snapshot.batteryPercent);
  }
  static String corner(int i,String axis){
    if(i<0||i>=4||(!"x".equals(axis)&&!"y".equals(axis)))throw new IllegalArgumentException("corner");
    return PREFIX+CORNERS[i]+"_"+axis;
  }
  static int parseColor(String raw){
    if(raw==null||!raw.matches("#[0-9a-fA-F]{6}"))return Color.rgb(255,107,0);
    return Color.parseColor(raw);
  }
  static void reset(Context c){
    SharedPreferences p=c.getSharedPreferences("preferences",0);SharedPreferences.Editor e=p.edit();
    for(String key:p.getAll().keySet())if(key.startsWith(PREFIX))e.remove(key);
    e.apply();
  }
  private static float clamp(float n,float low,float high){return Math.max(low,Math.min(high,n));}
  private static int tint(int rgb,int alpha){
    return Color.argb(Math.max(0,Math.min(255,alpha)),Color.red(rgb),Color.green(rgb),Color.blue(rgb));
  }
  private static void lerp(float[] out,float[] a,float[] b,float fraction){
    out[0]=a[0]+(b[0]-a[0])*fraction;out[1]=a[1]+(b[1]-a[1])*fraction;
  }
  static float[][] geometry(RectF car,Settings cfg,float viewBottom,float d){
    float height=Math.max(12*d,Math.min(48*d,car.height()*.19f*cfg.height/100f));
    // The old .73-.86 image-height band was hidden by opaque vehicle pixels.
    // Anchor a larger perspective strip at the vehicle ground line instead.
    float rear=car.top+car.height()*.78f,front=rear+height;
    float space=cfg.status?38*d:12*d;
    if(front>viewBottom-space){float move=front-(viewBottom-space);front-=move;rear-=move;}
    float[] originalX={.25f,.75f,.10f,.90f};
    float[][] p=new float[4][2];
    for(int i=0;i<4;i++){
      float px=.5f+(originalX[i]-.5f)*cfg.length/100f+cfg.x[i]/200f;
      p[i][0]=car.left+car.width()*clamp(px,.02f,.98f);
      p[i][1]=(i<2?rear:front)+height*cfg.y[i]/100f;
    }
    float gap=Math.min(p[2][1],p[3][1])-Math.max(p[0][1],p[1][1]);
    if(gap<height*.35f){float correction=height*.35f-gap;p[2][1]+=correction;p[3][1]+=correction;}
    // Respect the reserved caption area even at maximum corner offsets.
    float excess=Math.max(p[2][1],p[3][1])-(viewBottom-space);
    if(excess>0)for(int i=0;i<4;i++)p[i][1]-=excess;
    return p;
  }
  private static Path quad(float[] a,float[] b,float[] c,float[] d){
    Path path=new Path();path.moveTo(a[0],a[1]);path.lineTo(b[0],b[1]);
    path.lineTo(d[0],d[1]);path.lineTo(c[0],c[1]);path.close();return path;
  }
  static float draw(Canvas c,RectF car,Settings cfg,int soc,float density,boolean dark){
    float d=Math.max(.5f,density);
    float[][] p=geometry(car,cfg,c.getHeight(),d);
    Path floor=quad(p[0],p[1],p[2],p[3]);
    Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setColor(dark?0x2ABDC1C5:0x2A4C5054);c.drawPath(floor,paint);
    if(cfg.glow>0&&cfg.opacity>0){
      paint.setColor(tint(cfg.color,cfg.glow==1?23:42));paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth((cfg.glow==1?5f:9f)*d);c.drawPath(floor,paint);
      paint.setStyle(Paint.Style.FILL);
    }
    float fraction=clamp(soc/100f,0,1);
    if(fraction>0&&cfg.opacity>0){
      float[] rear=new float[2],front=new float[2];
      lerp(rear,p[0],p[1],fraction);lerp(front,p[2],p[3],fraction);
      int a=Math.round(255f*cfg.opacity/100f);
      float y1=(p[0][1]+p[1][1])*.5f,y2=(p[2][1]+p[3][1])*.5f;
      paint.setShader(new LinearGradient(0,y1,0,Math.max(y1+1f,y2),
          tint(cfg.color,Math.round(a*.42f)),tint(cfg.color,a),Shader.TileMode.CLAMP));
      c.drawPath(quad(p[0],rear,p[2],front),paint);paint.setShader(null);
    }
    if(cfg.grid&&cfg.gridOpacity>0){
      c.save();c.clipPath(floor);paint.setStyle(Paint.Style.STROKE);
      paint.setColor(tint(dark?Color.WHITE:Color.rgb(45,50,55),
          Math.round(115f*cfg.gridOpacity/100f)));
      paint.setStrokeWidth(.7f*d);
      int cols=cfg.density==0?8:cfg.density==2?16:12;
      int rows=cfg.density==0?2:cfg.density==2?4:3;
      float[] start=new float[2],end=new float[2];
      for(int i=1;i<cols;i++){
        float t=i/(float)cols;lerp(start,p[0],p[1],t);lerp(end,p[2],p[3],t);
        c.drawLine(start[0],start[1],end[0],end[1],paint);
      }
      for(int i=1;i<rows;i++){
        float t=i/(float)rows;lerp(start,p[0],p[2],t);lerp(end,p[1],p[3],t);
        c.drawLine(start[0],start[1],end[0],end[1],paint);
      }
      c.restore();
    }
    paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.75f*d);
    paint.setColor(tint(cfg.color,Math.round(110f*cfg.opacity/100f)));c.drawPath(floor,paint);
    return Math.max(p[2][1],p[3][1]);
  }

  private static String fitted(Paint paint,String value,float max){
    if(paint.measureText(value)<=max)return value;
    String s=value;while(s.length()>2&&paint.measureText(s+"…")>max)s=s.substring(0,s.length()-1);
    return s+"…";
  }
  static void status(Canvas c,float x,float edge,String title,String detail,float density,int color){
    if(title==null||title.isEmpty())return;
    Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    float d=Math.max(.5f,density);paint.setColor(color);paint.setTextAlign(Paint.Align.CENTER);
    paint.setTextSize(11*d);paint.setTypeface(Typeface.DEFAULT_BOLD);
    float base=Math.min(c.getHeight()-19*d,edge+16*d);
    float safeX=clamp(x,30*d,c.getWidth()-30*d);
    c.drawText(fitted(paint,title,c.getWidth()-14*d),safeX,base,paint);
    if(detail!=null&&!detail.isEmpty()){
      paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(9*d);
      c.drawText(fitted(paint,detail,c.getWidth()-14*d),safeX,
          Math.min(c.getHeight()-4*d,base+13*d),paint);
    }
  }
  static String[] message(Context c,VehicleSnapshot snap,Integer soc){
    String socLabel=soc==null||soc<0||soc>100?"--%":soc+"%";
    String first=AppPreferences.tr(c,"正在充电","Charging")+" · "+socLabel;
    if(snap==null||snap.chargingDetails==null)return new String[]{first,""};
    ChargingSnapshot state=snap.chargingDetails;
    String power=state.powerWatts!=null&&state.powerWatts>0
        ?String.format(Locale.getDefault(),"%.1f kW",state.powerWatts/1000d):"";
    String eta=state.minutesToFull==null?"":AppPreferences.tr(c,"剩余 ","Remaining ")
        +state.timeToFull(AppPreferences.english(c));
    return new String[]{first,power.isEmpty()?eta:eta.isEmpty()?power:power+" · "+eta};
  }
}
