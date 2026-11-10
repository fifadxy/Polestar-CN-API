package io.github.polestarcn.companion;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Four-wheel tyre view using the same top-view vehicle artwork as the exterior panel. */
public final class TirePressureView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Drawable vehicle;
  private final TireSnapshot tyres;
  private final boolean english, dark;
  private final int ink, muted, surface, line, orange = Color.rgb(255, 107, 0);

 public TirePressureView(Context context){this(context,TireSnapshot.unknown());}
  public TirePressureView(Context context, TireSnapshot tyres) {
    super(context);
    this.tyres = tyres == null ? TireSnapshot.unknown() : tyres;
    english = AppPreferences.english(context);
    dark = AppPreferences.dark(context);
    ink = PsaDesign.ink(dark);
    muted = PsaDesign.muted(dark);
    surface = PsaDesign.surface(dark);
    line = PsaDesign.line(dark);
    vehicle = androidx.core.content.ContextCompat.getDrawable(context, VehicleArt.conditionTopDrawable(context));
    paint.setTypeface(PsaDesign.regular());
    setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    StringBuilder a = new StringBuilder(english ? "Tire status. " : "胎压状态。");
    for (int i = 0; i < 4; i++) a.append(position(i)).append(" ").append(this.tyres.label(i, english)).append(". ");
    setContentDescription(a.toString());
  }

  private String position(int i) {
    return (english ? new String[]{"Front left", "Front right", "Rear left", "Rear right"}
        : new String[]{"左前", "右前", "左后", "右后"})[i];
  }

  private boolean attention(int i) {
    int w = tyres.warning(i);
    return w == TireSnapshot.VERY_LOW || w == TireSnapshot.LOW || w == TireSnapshot.HIGH;
  }

  private void word(Canvas c, String text, float size, float x, float y, int color, Paint.Align align) {
    paint.setColor(color); paint.setStyle(Paint.Style.FILL); paint.setTextAlign(align);
    paint.setTextSize(size); c.drawText(text, x, y, paint);
  }
  private void fittedWord(Canvas c,String value,float maxWidth,float size,float x,float y,int color) {
    paint.setTextSize(size);
    String label=android.text.TextUtils.ellipsize(value,new android.text.TextPaint(paint),
        Math.max(1f,maxWidth),android.text.TextUtils.TruncateAt.END).toString();
    word(c,label,size,x,y,color,Paint.Align.LEFT);
  }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float w = getWidth(), h = getHeight(), d = getResources().getDisplayMetrics().density;
    float scale = d * Math.min(1.25f, getResources().getConfiguration().fontScale);
    float mid = w / 2f, cy = h * .50f;
    float cw = w * .31f, ch = 58 * scale;
    float ratio=vehicle!=null&&vehicle.getIntrinsicHeight()>0&&vehicle.getIntrinsicWidth()>0
        ?vehicle.getIntrinsicHeight()/(float)vehicle.getIntrinsicWidth():.53f;
    float half=ExteriorCalloutLayout.carHalfLength(w,h,d,ratio,cw,scale);
    String summary = tyres.summary(english);
    word(canvas, summary, 13 * scale, mid, 18 * scale,
        tyres.hasProblem() || tyres.allGood() ? orange : muted, Paint.Align.CENTER);

    canvas.save();canvas.translate(mid,cy);canvas.rotate(VehicleArt.conditionTopRotationDegrees());
    if(vehicle!=null){
      int length=Math.round(half),width=Math.round(half*ratio);
      vehicle.setBounds(-length,-width,length,width);
      vehicle.draw(canvas);
    }
    canvas.restore();
    for (int i = 0; i < 4; i++) {
      boolean left = i % 2 == 0, front = i < 2;
      float x = left ? 0 : w - cw, y = h * (front ? .22f : .59f);
      boolean warn = attention(i);
      paint.setStyle(Paint.Style.FILL); paint.setColor(surface);
      canvas.drawRoundRect(x, y, x + cw, y + ch, 8 * d, 8 * d, paint);
      paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(warn ? 2*d : d);
      paint.setColor(warn ? orange : line);
      canvas.drawRoundRect(x, y, x + cw, y + ch, 8*d, 8*d, paint);

      float anchorX = mid + (left ? -1 : 1) * half * ratio * .72f;
      float anchorY = cy + (front ? -.40f : .40f) * half;
      canvas.drawLine(left ? cw : w-cw, y + ch/2, anchorX, anchorY, paint);
      paint.setStyle(Paint.Style.FILL); paint.setColor(warn ? orange : line);
      canvas.drawCircle(anchorX, anchorY, 4*d, paint);

      fittedWord(canvas,position(i) + (warn ? " !" : ""),cw-14*d,10*scale,x+7*d,y+17*scale,
          warn?orange:muted);
      fittedWord(canvas,tyres.label(i,english),cw-14*d,11*scale,x+7*d,y+40*scale,
          warn?orange:ink);
    }
  }
}
