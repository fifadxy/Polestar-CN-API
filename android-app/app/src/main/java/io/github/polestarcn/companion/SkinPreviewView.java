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
    this.car = BitmapFactory.decodeResource(getResources(), R.drawable.vehicle_hero);
    setTag("skin_preview");
    setContentDescription(UiSkin.label(context, skin) + " preview");
  }
  public String skin() { return skin; }
  public void setSelectedSkin(boolean selected) { this.selected = selected; invalidate(); }
  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float sx = getWidth() / 300f, sy = getHeight() / 164f;
    canvas.save();canvas.scale(sx,sy);
    boolean dark = AppPreferences.dark(getContext());
    boolean nio = UiSkin.NIO_LIKE.equals(skin) || UiSkin.NIO_LIKE_2.equals(skin);
    boolean green = UiSkin.NIO_LIKE.equals(skin);
    boolean polestar = UiSkin.VOLIKE_POLESTAR.equals(skin);
    int base = dark ? 0xFF141618 : nio ? (green ? 0xFFF1F8FA : 0xFFF3F3F1) : polestar ? 0xFFF3F3F1 : 0xFFF3EEE8;
    int card = dark ? 0xFF292B2C : 0xFFFFFFFF;
    int text = dark ? Color.WHITE : 0xFF151718;
    int muted = dark ? 0xFFA6ABAE : 0xFF777C80;
    int accent = polestar || UiSkin.NIO_LIKE_2.equals(skin) ? PsaDesign.ORANGE
        : green ? 0xFF20C479 : 0xFF5E8D7A;
    paint.setColor(base);canvas.drawRoundRect(0,0,300,164,17,17,paint);
    if(nio) {
      paint.setColor(card);canvas.drawRoundRect(8,8,145,155,14,14,paint);
      paint.setColor(card);canvas.drawRoundRect(153,8,292,155,14,14,paint);
    } else if(UiSkin.DETAILED.equals(skin) || polestar) {
      paint.setColor(card);canvas.drawRoundRect(8,8,292,155,14,14,paint);
    }
    drawText(canvas,"Polestar 2",15,18,27,text,true);
    drawText(canvas,"78%",26,18,62,text,true);
    drawText(canvas,"312 km",13,84,60,muted,false);
    paint.setColor(dark ? 0xFF424647 : 0xFFDFE0E1);
    canvas.drawRoundRect(18,68,141,73,3,3,paint);
    paint.setColor(accent);canvas.drawRoundRect(18,68,111,73,3,3,paint);
    if(car!=null) {
      float cw = nio ? 112 : UiSkin.SIMPLE.equals(skin) ? 158 : 132;
      float cx = nio ? 20 : UiSkin.SIMPLE.equals(skin) ? 125 : 149;
      float ratio = car.getWidth()/(float)car.getHeight();
      float ch=Math.min(93,cw/ratio);
      float cy=76 + (81-ch)/2f;
      paint.setAlpha(255);
      canvas.drawBitmap(car,null,new RectF(cx,cy,cx+cw,cy+ch),paint);
    }
    if(nio) {
      paint.setColor(accent);canvas.drawRoundRect(160,27,282,32,2,2,paint);
      drawText(canvas,"CONTROL",13,166,55,text,true);
      for(int i=0;i<4;i++){
        float x=184+(i%2)*68, y=80+(i/2)*42;
        paint.setColor(dark?0xFF353A40:0xFFF5F5F5);
        canvas.drawCircle(x,y,16,paint);
        paint.setColor(accent);canvas.drawCircle(x,y,3.8f,paint);
      }
    } else {
      for(int i=0;i<4;i++){
        float x=34+38*i, y=131;
        paint.setColor(dark?0xFF343637:0xFFFFFFFF);
        canvas.drawCircle(x,y,13,paint);
        paint.setColor(i==0?accent:muted);canvas.drawCircle(x,y,3.3f,paint);
      }
      if(!UiSkin.SIMPLE.equals(skin)) {
        drawText(canvas,"STATUS",10,215,105,muted,true);
        drawText(canvas,"21%  /  75%",11,202,122,text,true);
        paint.setColor(accent);canvas.drawRoundRect(202,132,278,135,2,2,paint);
      }
    }
    if(selected) {
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
