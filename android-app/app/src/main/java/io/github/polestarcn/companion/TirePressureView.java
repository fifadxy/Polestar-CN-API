package io.github.polestarcn.companion;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Four-wheel tyre view using the same top-view vehicle artwork as the exterior panel. */
public final class TirePressureView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Drawable vehicle;
  private final TireSnapshot tyres;
  private final boolean english, dark;
  private final int ink, muted, surface, line, orange = Color.rgb(255, 107, 0);

  public TirePressureView(Context context, TireSnapshot tyres) {
    super(context);
    this.tyres = tyres == null ? TireSnapshot.unknown() : tyres;
    english = AppPreferences.english(context);
    dark = AppPreferences.dark(context);
    ink = PsaDesign.ink(dark);
    muted = PsaDesign.muted(dark);
    surface = PsaDesign.surface(dark);
    line = PsaDesign.line(dark);
    vehicle = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.vehicle_top);
    if (dark && vehicle != null) {
      ColorMatrix m = new ColorMatrix(new float[] {
          .4f,0,0,0,10, 0,.4f,0,0,10, 0,0,.4f,0,10, 0,0,0,1,0});
      vehicle.setColorFilter(new ColorMatrixColorFilter(m));
    }
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

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float w = getWidth(), h = getHeight(), d = getResources().getDisplayMetrics().density;
    float scale = d * Math.min(1.25f, getResources().getConfiguration().fontScale);
    float mid = w / 2f, cy = h * .51f;
    String summary = tyres.summary(english);
    word(canvas, summary, 13 * scale, mid, 18 * scale,
        tyres.hasProblem() || tyres.allGood() ? orange : muted, Paint.Align.CENTER);

    canvas.save(); canvas.translate(mid, cy); canvas.rotate(-90);
    int half = (int)Math.min(h * .45f, w * .50f);
    vehicle.setBounds(-half, (int)(-half * .5625f), half, (int)(half * .5625f));
    vehicle.draw(canvas); canvas.restore();

    float cw = w * .31f, ch = 58 * scale;
    for (int i = 0; i < 4; i++) {
      boolean left = i % 2 == 0, front = i < 2;
      float x = left ? 0 : w - cw, y = h * (front ? .22f : .59f);
      boolean warn = attention(i);
      paint.setStyle(Paint.Style.FILL); paint.setColor(surface);
      canvas.drawRoundRect(x, y, x + cw, y + ch, 8 * d, 8 * d, paint);
      paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(warn ? 2*d : d);
      paint.setColor(warn ? orange : line);
      canvas.drawRoundRect(x, y, x + cw, y + ch, 8*d, 8*d, paint);

      float anchorX = mid + (left ? -1 : 1) * half * .30f;
      float anchorY = cy + (front ? -.25f : .25f) * half;
      canvas.drawLine(left ? cw : w-cw, y + ch/2, anchorX, anchorY, paint);
      paint.setStyle(Paint.Style.FILL); paint.setColor(warn ? orange : line);
      canvas.drawCircle(anchorX, anchorY, 4*d, paint);

      word(canvas, position(i) + (warn ? " !" : ""), 10 * scale, x + 7*d, y + 17*scale,
          warn ? orange : muted, Paint.Align.LEFT);
      word(canvas, tyres.label(i, english), 11 * scale, x + 7*d, y + 40*scale,
          warn ? orange : ink, Paint.Align.LEFT);
    }
  }
}
