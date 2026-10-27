package io.github.polestarcn.companion;

import android.content.Context;
import android.graphics.*;
import android.widget.TextView;
import java.util.function.Supplier;

/** Compact vehicle-condition button with automatic remaining-service percentage bars. */
public final class VehicleConditionButton extends TextView {
  private boolean showTime, showDistance;
  private Integer timePercent, distancePercent;
  private final Supplier<VehicleSnapshot> snapshot;
  private final android.text.TextPaint paint = new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);

  public VehicleConditionButton(Context c, Supplier<VehicleSnapshot> source) {
    super(c);
    setIncludeFontPadding(false);
    snapshot = source;
    paint.setTypeface(PsaDesign.regular());
  }

  public void refreshService() {
    VehicleSnapshot vehicle = snapshot.get();
    timePercent = ServiceReading.timePercent(getContext(), vehicle);
    distancePercent = ServiceReading.distancePercent(getContext(), vehicle);
    showTime = timePercent != null;
    showDistance = distancePercent != null;
    boolean bars = showTime || showDistance;
    float d = getResources().getDisplayMetrics().density;
    setPadding((int) (4*d), (int) (bars ? 3*d : 2*d), (int) (4*d), (int) (bars ? 24*d : 2*d));
    StringBuilder description = new StringBuilder(String.valueOf(getText()));
    if (showTime) description.append(" · ")
        .append(AppPreferences.tr(getContext(), "保养时间 ", "Service time "))
        .append(ServiceReading.number(timePercent, "%"));
    if (showDistance) description.append(" · ")
        .append(AppPreferences.tr(getContext(), "保养里程 ", "Service distance "))
        .append(ServiceReading.number(distancePercent, "%"));
    description.append(" · ")
        .append(AppPreferences.tr(getContext(), "点按查看车况", "Tap for vehicle status"));
    setContentDescription(description.toString());
    invalidate();
  }

  @Override protected void onDraw(Canvas canvas) {
    int count = (showTime ? 1 : 0) + (showDistance ? 1 : 0);
    float d = getResources().getDisplayMetrics().density;
    float heading = count > 0 ? getHeight() - 31*d : getHeight();

    paint.setTypeface(getTypeface());
    paint.setTextSize(getTextSize());
    paint.setColor(getCurrentTextColor());
    paint.setTextAlign(Paint.Align.CENTER);
    Paint.FontMetrics metrics = paint.getFontMetrics();
    float height = metrics.descent - metrics.ascent;
    if (height > heading - 4*d) {
      paint.setTextSize(paint.getTextSize() * Math.max(.5f, (heading - 4*d) / height));
      metrics = paint.getFontMetrics();
    }
    CharSequence title = android.text.TextUtils.ellipsize(
        getText(), paint, Math.max(0, getWidth() - 10*d), android.text.TextUtils.TruncateAt.END);
    canvas.drawText(title.toString(), getWidth()/2f,
        (heading - metrics.descent - metrics.ascent)/2f, paint);
    if (count == 0) return;

    float width = (getWidth() - 10*d) / count;
    int index = 0;
    if (showTime) drawBar(canvas, index++, width, timePercent,
        AppPreferences.tr(getContext(), "时间", "Time"));
    if (showDistance) drawBar(canvas, index, width, distancePercent,
        AppPreferences.tr(getContext(), "里程", "Distance"));
  }

  private void drawBar(Canvas canvas, int index, float width, Integer percent, String labelName) {
    float d = getResources().getDisplayMetrics().density;
    float left = 5*d + index*width;
    paint.setTypeface(PsaDesign.regular());
    paint.setTextSize(8 * getResources().getDisplayMetrics().scaledDensity);
    paint.setTextAlign(Paint.Align.CENTER);
    paint.setColor(getCurrentTextColor());
    String label = labelName + " " + ServiceReading.number(percent, "%");
    float measured = paint.measureText(label);
    if (measured > width - 6*d) paint.setTextSize(paint.getTextSize() * (width - 6*d) / measured);
    canvas.drawText(label, left + width/2f, getHeight() - 13*d, paint);

    float trackRight = left + width - 4*d;
    float top = getHeight() - 8*d, bottom = getHeight() - 5*d;
    paint.setColor(PsaDesign.line(AppPreferences.dark(getContext())));
    canvas.drawRoundRect(left, top, trackRight, bottom, 1.5f*d, 1.5f*d, paint);
    if (percent != null) {
      paint.setColor(PsaDesign.ORANGE);
      float progressRight = left + (trackRight-left) * percent / 100f;
      canvas.drawRoundRect(left, top, progressRight, bottom, 1.5f*d, 1.5f*d, paint);
    }
  }
}
