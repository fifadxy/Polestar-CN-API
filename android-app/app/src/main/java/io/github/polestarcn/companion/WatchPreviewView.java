package io.github.polestarcn.companion;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/** Scalable 192dp concept preview; does not claim to be an installed watch application. */
public final class WatchPreviewView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final VehicleSnapshot snapshot;
  private boolean round = true;
  public WatchPreviewView(Context context, VehicleSnapshot snapshot) {
    super(context); this.snapshot = snapshot;
    setContentDescription("Watch layout preview, not a running watch application");
  }
  public void setRound(boolean value) { round = value; invalidate(); }
  private void word(Canvas canvas, String text, float size, float x, float y, int color) {
    paint.setStyle(Paint.Style.FILL); paint.setColor(color); paint.setTextSize(size);
    paint.setTextAlign(Paint.Align.CENTER); canvas.drawText(text, x, y, paint);
  }
  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float scale = Math.min(getWidth(), getHeight()) / 208f;
    canvas.save(); canvas.translate((getWidth() - 192 * scale) / 2, (getHeight() - 192 * scale) / 2);
    canvas.scale(scale, scale); paint.setColor(Color.BLACK); paint.setStyle(Paint.Style.FILL);
    if (round) canvas.drawCircle(96, 96, 96, paint);
    else canvas.drawRoundRect(0, 0, 192, 192, 22, 22, paint);
    word(canvas, "PsAssistant", 12, 96, 37, Color.LTGRAY);
    word(canvas, snapshot == null ? "--%" : VehicleSnapshot.metric(snapshot.batteryPercent, "%"), 36, 96, 78, Color.WHITE);
    word(canvas, snapshot == null ? "-- km" : VehicleSnapshot.metric(snapshot.rangeKm, " km"), 14, 96, 101, Color.LTGRAY);
    boolean en = AppPreferences.english(getContext());
    boolean unlocked = snapshot != null && "未锁".equals(snapshot.lock);
    boolean climate = snapshot != null && "开启".equals(snapshot.climate);
    paint.setColor(unlocked ? Color.rgb(255,107,0) : Color.WHITE);
    canvas.drawRoundRect(40, 116, 88, 164, 18, 18, paint);
    paint.setColor(climate ? Color.rgb(255,107,0) : Color.WHITE);
    canvas.drawRoundRect(104, 116, 152, 164, 18, 18, paint);
    word(canvas, en ? "Lock" : "门锁", 12, 64, 145, Color.BLACK);
    word(canvas, en ? "AC" : "空调", 12, 128, 145, Color.BLACK);
    canvas.restore();
  }
}
