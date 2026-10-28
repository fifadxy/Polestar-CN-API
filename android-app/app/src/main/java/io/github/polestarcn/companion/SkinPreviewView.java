package io.github.polestarcn.companion;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Real drawable-based preview for each switchable control skin; no screenshot assets or network. */
public final class SkinPreviewView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
  private final Bitmap car;
  private final String skin;
  private boolean selected;

  public SkinPreviewView(Context context, String skin) {
    super(context);
    this.skin = skin;
    this.car = BitmapFactory.decodeResource(getResources(),
        VehicleArt.drawable(VehicleArt.key(context),AppPreferences.dark(context),
          UiSkin.PS_DEFAULT.equals(skin),
          UiSkin.NIO_LIKE.equals(skin)||UiSkin.NIO_LIKE_2.equals(skin)));
    setTag("skin_preview");
    setContentDescription(UiSkin.label(context, skin) + " preview");
  }

  public String skin() { return skin; }
  public void setSelectedSkin(boolean selected) { this.selected = selected; invalidate(); }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float sx = getWidth() / 300f, sy = getHeight() / 164f;
    canvas.save(); canvas.scale(sx, sy);

    boolean dark = AppPreferences.dark(getContext());
    boolean nio = UiSkin.NIO_LIKE.equals(skin) || UiSkin.NIO_LIKE_2.equals(skin);
    boolean green = UiSkin.NIO_LIKE.equals(skin);
    boolean psDefault=UiSkin.PS_DEFAULT.equals(skin);
    boolean polestar = UiSkin.VOLIKE_POLESTAR.equals(skin)||psDefault;
    boolean detailed = UiSkin.DETAILED.equals(skin) || polestar;

    int base = dark ? 0xFF141618 : nio ? (green ? 0xFFF1F8FA : 0xFFF3F3F1)
        : polestar ? 0xFFF4F4F2 : 0xFFF3EEE8;
    int card = dark ? 0xFF292B2C : 0xFFFFFFFF;
    int text = dark ? Color.WHITE : 0xFF151718;
    int muted = dark ? 0xFFA6ABAE : 0xFF777C80;
    int accent = polestar || UiSkin.NIO_LIKE_2.equals(skin) ? PsaDesign.ORANGE
        : green ? 0xFF20C479 : 0xFF5E8D7A;

    paint.setColor(base); canvas.drawRoundRect(0,0,300,164,17,17,paint);

    if(psDefault){
      drawText(canvas,"Polestar 2",14,12,20,text,true);drawText(canvas,"78%",22,12,47,text,true);drawText(canvas,"312 km",17,192,47,text,false);
      paint.setColor(accent);canvas.drawRect(12,53,220,56,paint);
      if(car!=null){float h=65,w=h*car.getWidth()/(float)car.getHeight();canvas.drawBitmap(car,null,new RectF(150-w/2,61,150+w/2,126),paint);}
      drawText(canvas,"All Good！",12,116,139,accent,false);
      for(int i=0;i<4;i++){paint.setColor(card);float x=8+i*73;canvas.drawRoundRect(x,145,x+67,160,4,4,paint);paint.setColor(accent);canvas.drawCircle(x+33,152,3,paint);}
    }else if (nio) {
      paint.setColor(card);canvas.drawRoundRect(8,8,145,155,14,14,paint);
      paint.setColor(card);canvas.drawRoundRect(153,8,292,155,14,14,paint);
      drawText(canvas,"Polestar 2",15,18,27,text,true);
      drawText(canvas,"78%",26,18,62,text,true);
      drawText(canvas,"312 km",13,84,60,muted,false);
      paint.setColor(dark ? 0xFF424647 : 0xFFDFE0E1);canvas.drawRoundRect(18,68,141,73,3,3,paint);
      paint.setColor(accent);canvas.drawRoundRect(18,68,111,73,3,3,paint);
      if(car!=null){
        float cw=112,ch=Math.min(93,cw/(car.getWidth()/(float)car.getHeight()));
        canvas.drawBitmap(car,null,new RectF(20,88,20+cw,88+ch),paint);
      }
      paint.setColor(accent);canvas.drawRoundRect(160,27,282,32,2,2,paint);
      drawText(canvas,"CONTROL",13,166,55,text,true);
      for(int i=0;i<4;i++){
        float x=184+(i%2)*68, y=80+(i/2)*42;
        paint.setColor(dark?0xFF353A40:0xFFF5F5F5);canvas.drawCircle(x,y,16,paint);
        paint.setColor(accent);canvas.drawCircle(x,y,3.8f,paint);
      }
    } else {
      drawText(canvas,"Polestar 2",14,14,20,text,true);

      paint.setColor(card);canvas.drawRoundRect(10,28,290,61,11,11,paint);
      drawText(canvas,"78%",18,18,49,text,true);
      drawText(canvas,"312 km",15,90,49,text,true);
      drawText(canvas,"READY",8,236,48,muted,true);
      paint.setColor(dark?0xFF424647:0xFFDFE0E1);canvas.drawRoundRect(18,54,281,58,2,2,paint);
      paint.setColor(accent);canvas.drawRoundRect(18,54,222,58,2,2,paint);

      paint.setColor(card);canvas.drawRoundRect(10,66,290,151,14,14,paint);

      for(int i=0;i<4;i++){
        float x=36,y=80+i*18.5f;
        paint.setColor(dark?0xFF353A40:0xFFF3F3F1);canvas.drawCircle(x,y,9.5f,paint);
        paint.setColor(i==1?accent:muted);canvas.drawCircle(x,y,2.6f,paint);
      }

      if(car!=null){
        canvas.save();
        canvas.translate(detailed?151:176,107);
        canvas.rotate(VehicleArt.portraitRotation(getContext()));
        float width=detailed?88:110;
        float height=width*car.getHeight()/(float)car.getWidth();
        canvas.drawBitmap(car,null,new RectF(-width/2,-height/2,width/2,height/2),paint);
        canvas.restore();
      }

      if(detailed){
        paint.setColor(dark?0xFF222526:0xFFF3F3F1);canvas.drawRoundRect(222,72,284,145,10,10,paint);
        drawText(canvas,"STATUS",8,228,86,muted,true);
        drawText(canvas,"TIME 21%",8,228,101,text,false);
        paint.setColor(dark?0xFF45494A:0xFFD9DADD);canvas.drawRoundRect(228,106,278,109,1.5f,1.5f,paint);
        paint.setColor(accent);canvas.drawRoundRect(228,106,239,109,1.5f,1.5f,paint);
        drawText(canvas,"KM 75%",8,228,123,text,false);
        paint.setColor(dark?0xFF45494A:0xFFD9DADD);canvas.drawRoundRect(228,128,278,131,1.5f,1.5f,paint);
        paint.setColor(accent);canvas.drawRoundRect(228,128,266,131,1.5f,1.5f,paint);
        drawText(canvas,"23°C",9,228,141,text,true);
      }

      paint.setColor(accent);canvas.drawCircle(18,145,3,paint);
      drawText(canvas,"STATUS",8,26,148,muted,true);
    }

    if(selected){
      paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3.2f);paint.setColor(accent);
      canvas.drawRoundRect(2,2,298,162,16,16,paint);paint.setStyle(Paint.Style.FILL);
    }
    canvas.restore();
  }

  private void drawText(Canvas canvas,String text,float size,float x,float y,int color,boolean bold){
    paint.setShader(null);paint.setColor(color);paint.setTextSize(size);
    paint.setTypeface(bold?PsaDesign.display():PsaDesign.regular());
    paint.setTextAlign(Paint.Align.LEFT);canvas.drawText(text,x,y,paint);
  }
}
