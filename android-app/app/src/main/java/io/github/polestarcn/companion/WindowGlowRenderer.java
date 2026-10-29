package io.github.polestarcn.companion;
import android.graphics.*;
/** Per-view glass polygons, cached in image coordinates. No source image is changed. */
final class WindowGlowRenderer {
 private final Path glass=new Path();private final Matrix local=new Matrix();private final Paint paint=new Paint(3);private final RectF old=new RectF();private int oldArt,color;private LinearGradient glow;
 private void polygon(RectF b,float... xy){glass.moveTo(b.left+xy[0]*b.width(),b.top+xy[1]*b.height());for(int i=2;i<xy.length;i+=2)glass.lineTo(b.left+xy[i]*b.width(),b.top+xy[i+1]*b.height());glass.close();}
 private void geometry(RectF b,int art){if(old.equals(b)&&oldArt==art)return;old.set(b);oldArt=art;glass.rewind();
  if(art==R.drawable.vehicle_top){polygon(b,.61f,.335f,.68f,.35f,.70f,.65f,.61f,.69f);polygon(b,.20f,.34f,.27f,.36f,.27f,.65f,.20f,.67f);polygon(b,.34f,.295f,.58f,.295f,.58f,.32f,.34f,.32f);polygon(b,.34f,.71f,.58f,.71f,.58f,.735f,.34f,.735f);}
  else if(art==R.drawable.psa_vehicle_top_white||art==R.drawable.psa_vehicle_top_dark||art==R.drawable.psa_vehicle_top_silver||art==R.drawable.psa_vehicle_top_blue){polygon(b,.25f,.26f,.34f,.29f,.34f,.70f,.25f,.73f);polygon(b,.66f,.29f,.75f,.29f,.75f,.70f,.66f,.70f);polygon(b,.37f,.22f,.64f,.22f,.64f,.26f,.37f,.26f);polygon(b,.37f,.74f,.64f,.74f,.64f,.78f,.37f,.78f);}
  else if(art==R.drawable.official_p2_side_snow||art==R.drawable.official_p2_side_space){polygon(b,.31f,.26f,.41f,.14f,.61f,.16f,.70f,.30f,.54f,.35f,.29f,.35f);}
  else if(art==R.drawable.psa_vehicle_front_silver){polygon(b,.38f,.45f,.58f,.45f,.62f,.53f,.34f,.53f);}
  else if(art==R.drawable.vehicle_hero){polygon(b,.34f,.26f,.43f,.15f,.70f,.15f,.75f,.31f);polygon(b,.77f,.18f,.82f,.25f,.85f,.36f,.80f,.34f);}
  else {polygon(b,.28f,.26f,.40f,.22f,.57f,.25f,.66f,.42f,.34f,.40f);polygon(b,.20f,.31f,.27f,.27f,.30f,.40f,.20f,.40f);}
 }
 void draw(Canvas c,RectF b,int art,float seconds,EffectStateController.Mode mode,int strength){
  draw(c,b,art,seconds,mode,strength,1f);
 }
 void draw(Canvas c,RectF b,int art,float seconds,EffectStateController.Mode mode,int strength,float speed){
  geometry(b,art);int tint=AirflowRenderer.color(mode);if(glow==null||color!=tint){color=tint;glow=new LinearGradient(0,0,1,0,new int[]{tint&0xFFFFFF,tint,tint&0xFFFFFF},new float[]{0,.5f,1},Shader.TileMode.MIRROR);paint.setShader(glow);}float pulse=.5f+.5f*(float)Math.sin(seconds*speed*.9f);local.setScale(b.width()*.75f,b.height());local.postTranslate(b.left+b.width()*(seconds*speed*.09f%1f-.5f),b.top);glow.setLocalMatrix(local);paint.setAlpha((int)((20+40*pulse)*strength/100f));c.drawPath(glass,paint);}
}
