package io.github.polestarcn.companion;

import android.content.Context;
import android.graphics.*;
import android.widget.ImageView;

/**
 * Polestar 2 vehicle view. VoLike uses the project's true top-down Polestar art,
 * rotated to portrait without distorting its original proportions; NIO keeps
 * its existing front three-quarter image.
 */
final class VehicleHeroView extends ImageView {
  private final Bitmap roof;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

  VehicleHeroView(Context context) {
    super(context);
    setImageResource(R.drawable.vehicle_hero);
    setScaleType(ScaleType.FIT_CENTER);
    setAdjustViewBounds(false);
    roof = BitmapFactory.decodeResource(getResources(),R.drawable.vehicle_top);
  }

  @Override protected void onDraw(Canvas canvas) {
    if (UiSkin.nioLayout(getContext()) || roof == null) {
      super.onDraw(canvas);
      return;
    }
    int innerW=getWidth()-getPaddingLeft()-getPaddingRight();
    int innerH=getHeight()-getPaddingTop()-getPaddingBottom();
    if (innerW <= 0 || innerH <= 0) return;
    float scale=Math.min(innerW/(float)roof.getHeight(),innerH/(float)roof.getWidth());
    // Keep the same original aspect ratio. The car's front, at image right,
    // points toward the upper edge of a portrait phone after -90 degrees.
    canvas.save();
    canvas.translate(getPaddingLeft()+innerW/2f,getPaddingTop()+innerH/2f);
    canvas.rotate(-90);
    RectF dest=new RectF(-roof.getWidth()*scale/2,-roof.getHeight()*scale/2,
        roof.getWidth()*scale/2,roof.getHeight()*scale/2);
    paint.setAlpha(255);
    canvas.drawBitmap(roof,null,dest,paint);
    canvas.restore();
  }
}
