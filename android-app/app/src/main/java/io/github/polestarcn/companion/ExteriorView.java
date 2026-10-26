package io.github.polestarcn.companion;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Original template vehicle art, front upward, with callouts at the actual body positions. */
public final class ExteriorView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Drawable vehicle;
  private final VehicleSnapshot state;
  private final boolean english, dark;
  private final int ink, muted, surface, line, orange = Color.rgb(255, 107, 0);
  public ExteriorView(Context context) { this(context, null); }
  public ExteriorView(Context context, VehicleSnapshot state) {
    super(context); this.state = state;
    english = AppPreferences.english(context); dark = AppPreferences.dark(context);
    ink = Color.parseColor(dark ? "#F4F4F2" : "#18191A");
    muted = Color.parseColor(dark ? "#AAAAA7" : "#626560");
    surface = Color.parseColor(dark ? "#292B2D" : "#FFFFFF");
    line = Color.parseColor(dark ? "#45484A" : "#DCDDD7");
    vehicle = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.vehicle_top);
    if (dark && vehicle != null) {
      ColorMatrix m = new ColorMatrix(new float[] {
          .14f,0,0,0,10, 0,.14f,0,0,10, 0,0,.14f,0,10, 0,0,0,1,0});
      vehicle.setColorFilter(new ColorMatrixColorFilter(m));
    }
    paint.setTypeface(getResources().getFont(R.font.lato_regular));
    StringBuilder description = new StringBuilder(english ? "Front at top. " : "车头朝上。");
    for (int i = 0; i < 4; i++) description.append(position(i)).append(english ? " door " : "门 ")
        .append(AppPreferences.status(context, ExteriorSnapshot.label(value(false, i))))
        .append(english ? ", window " : "，窗 ")
        .append(AppPreferences.status(context, ExteriorSnapshot.label(value(true, i)))).append("。 ");
    description.append(english ? "Tailgate " : "尾箱 ").append(state == null ? "--" : AppPreferences.status(context, state.tailgate));
    setContentDescription(description.toString());
    setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
  }
  public static boolean hasOpening(VehicleSnapshot state) {
    if (state == null) return false;
    if ("已打开".equals(state.tailgate) || "未关严".equals(state.tailgate)) return true;
    for (int i = 0; i < 4; i++) for (boolean window : new boolean[]{false, true}) {
      int value = state.exterior.state(window, i);
      if (value == 1 || value == 3) return true;
    }
    return false;
  }
  private int value(boolean window, int i) { return state == null ? -1 : state.exterior.state(window, i); }
  private String position(int i) {
    return (english ? new String[]{"Front left", "Front right", "Rear left", "Rear right"}
        : new String[]{"左前", "右前", "左后", "右后"})[i];
  }
  private String status(int value) {
    return value == 1 ? (english ? "Open" : "开启") : value == 2 ? (english ? "Closed" : "关闭")
        : value == 3 ? (english ? "Ajar" : "未关严") : "--";
  }
  private int color(int value) { return value == 1 || value == 3 ? orange : value == 2 ? ink : muted; }
  private void word(Canvas c, String text, float size, float x, float y, int color, Paint.Align align) {
    paint.setColor(color); paint.setStyle(Paint.Style.FILL); paint.setTextAlign(align);
    paint.setTextSize(size); c.drawText(text, x, y, paint);
  }
  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float w = getWidth(), h = getHeight(), d = getResources().getDisplayMetrics().density;
    float text = d * Math.min(1.25f, getResources().getConfiguration().fontScale);
    float mid = w / 2, cy = h * .51f;
    word(canvas, english ? "FRONT ↑" : "车头 ↑", 11 * text, mid, 18 * text, muted, Paint.Align.CENTER);
    canvas.save(); canvas.translate(mid, cy); canvas.rotate(-90);
    int half = (int) Math.min(h * .46f, w * .53f);
    vehicle.setBounds(-half, (int)(-half * .5625f), half, (int)(half * .5625f));
    vehicle.draw(canvas); canvas.restore();
    float cw = w * .30f, ch = 65 * text;
    for (int i = 0; i < 4; i++) {
      boolean left = i % 2 == 0, front = i < 2;
      float x = left ? 0 : w - cw, y = h * (front ? .22f : .57f);
      int door = value(false, i), window = value(true, i);
      boolean open = door == 1 || door == 3 || window == 1 || window == 3;
      paint.setStyle(Paint.Style.FILL); paint.setColor(surface);
      canvas.drawRoundRect(x, y, x + cw, y + ch, 8 * d, 8 * d, paint);
      paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(d); paint.setColor(open ? orange : line);
      canvas.drawRoundRect(x, y, x + cw, y + ch, 8 * d, 8 * d, paint);
      float anchorX = mid + (left ? -1 : 1) * half * .205f;
      float anchorY = cy + (front ? -.12f : .15f) * half;
      canvas.drawLine(left ? cw : w - cw, y + ch / 2, anchorX, anchorY, paint);
      paint.setStyle(Paint.Style.FILL); paint.setColor(open ? orange : line);
      canvas.drawCircle(anchorX, anchorY, 3 * d, paint);
      word(canvas, position(i) + (open ? " !" : ""), 10 * text, x + 7 * d, y + 16 * text, muted, Paint.Align.LEFT);
      word(canvas, (english ? "Door · " : "门 · ") + status(door), 11 * text, x + 7 * d, y + 36 * text, color(door), Paint.Align.LEFT);
      word(canvas, (english ? "Win · " : "窗 · ") + status(window), 11 * text, x + 7 * d, y + 54 * text, color(window), Paint.Align.LEFT);
    }
    boolean tailOpen = state != null && ("已打开".equals(state.tailgate) || "未关严".equals(state.tailgate));
    word(canvas, (english ? "Tailgate · " : "尾箱 · ") + (state == null ? "--" : AppPreferences.status(getContext(), state.tailgate)),
        12 * text, mid, h - 12 * text, tailOpen ? orange : ink, Paint.Align.CENTER);
  }
}
