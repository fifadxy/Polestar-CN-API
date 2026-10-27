package io.github.polestarcn.companion;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Scalable watch UI preview with switchable Psa and Volvo-inspired skins. */
public final class WatchPreviewView extends View {
  public static final int STYLE_PSA = 0;
  public static final int STYLE_VOLVO = 1;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final VehicleSnapshot snapshot;
  private final Bitmap car;
  private boolean round = true;
  private int style = STYLE_VOLVO;

  public WatchPreviewView(Context context, VehicleSnapshot snapshot) {
    super(context);
    this.snapshot = snapshot;
    paint.setTypeface(PsaDesign.regular());
    car = BitmapFactory.decodeResource(getResources(), R.drawable.vehicle_hero);
    setContentDescription("Switchable watch layout preview");
  }

  public void setRound(boolean value) { round = value; invalidate(); }
  public void setStyle(int value) { style = value == STYLE_PSA ? STYLE_PSA : STYLE_VOLVO; invalidate(); }
  public int getStyle() { return style; }

  private void word(Canvas canvas, String text, float size, float x, float y, int color, boolean bold) {
    paint.setShader(null); paint.setStyle(Paint.Style.FILL); paint.setColor(color); paint.setTextSize(size);
    paint.setTypeface(bold ? PsaDesign.display() : PsaDesign.regular());
    paint.setTextAlign(Paint.Align.CENTER); canvas.drawText(text, x, y, paint);
  }

  private void shell(Canvas canvas, int background) {
    paint.setShader(null); paint.setColor(background); paint.setStyle(Paint.Style.FILL);
    if (round) canvas.drawCircle(96,96,96,paint);
    else canvas.drawRoundRect(0,0,192,192,24,24,paint);
  }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float scale = Math.min(getWidth(), getHeight()) / 208f;
    canvas.save();
    canvas.translate((getWidth()-192*scale)/2f,(getHeight()-192*scale)/2f);
    canvas.scale(scale,scale);
    if(style==STYLE_VOLVO) drawVolvo(canvas); else drawPsa(canvas);
    canvas.restore();
  }

  private void drawPsa(Canvas canvas) {
    shell(canvas,Color.BLACK);
    word(canvas,"PsAssistant",12,96,35,Color.LTGRAY,false);
    word(canvas,snapshot==null?"--%":VehicleSnapshot.metric(snapshot.batteryPercent,"%"),34,96,77,Color.WHITE,true);
    word(canvas,snapshot==null?"-- km":VehicleSnapshot.metric(snapshot.rangeKm," km"),13,96,99,Color.LTGRAY,false);
    boolean en=AppPreferences.english(getContext());
    boolean unlocked=snapshot!=null&&"未锁".equals(snapshot.lock);
    boolean climate=snapshot!=null&&"开启".equals(snapshot.climate);
    paint.setColor(unlocked?Color.rgb(255,107,0):Color.WHITE);canvas.drawRoundRect(40,116,88,164,10,10,paint);
    paint.setColor(climate?Color.rgb(255,107,0):Color.WHITE);canvas.drawRoundRect(104,116,152,164,10,10,paint);
    word(canvas,en?"Lock":"门锁",11,64,145,Color.BLACK,false);
    word(canvas,en?"AC":"空调",11,128,145,Color.BLACK,false);
  }

  private void drawVolvo(Canvas canvas) {
    int bg=Color.rgb(16,20,21), green=Color.rgb(111,166,145), secondary=Color.rgb(166,174,172);
    shell(canvas,bg);
    paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(4);paint.setStrokeCap(Paint.Cap.ROUND);
    paint.setColor(Color.rgb(45,52,52));
    RectF ring=new RectF(8,8,184,184);canvas.drawArc(ring,-145,110,false,paint);
    paint.setColor(green);
    float soc=snapshot==null||snapshot.batteryPercent==null?0:Math.max(0,Math.min(100,snapshot.batteryPercent));
    canvas.drawArc(ring,-145,110*soc/100f,false,paint);paint.setStyle(Paint.Style.FILL);

    word(canvas,"POLESTAR 2",9,96,24,secondary,false);
    if(car!=null){
      Rect src=new Rect(0,0,car.getWidth(),car.getHeight());
      RectF dst=new RectF(49,35,143,83);
      paint.setAlpha(235);canvas.drawBitmap(car,src,dst,paint);paint.setAlpha(255);
    }
    word(canvas,snapshot==null?"--%":VehicleSnapshot.metric(snapshot.batteryPercent,"%"),28,96,109,Color.WHITE,true);
    word(canvas,snapshot==null?"-- km":VehicleSnapshot.metric(snapshot.rangeKm," km"),11,96,126,secondary,false);
    boolean charging=ChargingVisibility.shouldShow(snapshot);
    if(charging){
      word(canvas,AppPreferences.english(getContext())?"CHARGING":"充电中",9,96,139,green,true);
    }
    boolean en=AppPreferences.english(getContext());
    roundedAction(canvas,42,148,86,178,en?"LOCK":"门锁",snapshot!=null&&"未锁".equals(snapshot.lock)?Color.rgb(255,120,70):green);
    roundedAction(canvas,106,148,150,178,en?"CLIMATE":"空调",snapshot!=null&&"开启".equals(snapshot.climate)?green:Color.WHITE);
  }

  private void roundedAction(Canvas canvas,float l,float t,float r,float b,String label,int accent){
    paint.setColor(Color.rgb(34,40,40));canvas.drawRoundRect(l,t,r,b,15,15,paint);
    word(canvas,label,8,(l+r)/2f,t+19,accent,false);
  }
}
