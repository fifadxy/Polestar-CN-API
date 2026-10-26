package io.github.polestarcn.companion;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/** A tiny hardware-accelerated Polestar-orange ambient line for vehicle control pages. */
public final class ViAmbientGlowView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private ValueAnimator animator;
  private float pulse = .34f;

  public ViAmbientGlowView(Context context) {
    super(context);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  }

  @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    paint.setShader(new LinearGradient(
        0, 0, Math.max(1, w), 0,
        new int[]{Color.TRANSPARENT, Color.rgb(255, 107, 0), Color.TRANSPARENT},
        new float[]{0f, .5f, 1f}, Shader.TileMode.CLAMP));
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (animator != null) animator.cancel();
    animator = ValueAnimator.ofFloat(.24f, .78f, .24f);
    animator.setDuration(2600L);
    animator.setRepeatCount(ValueAnimator.INFINITE);
    animator.setInterpolator(new AccelerateDecelerateInterpolator());
    animator.addUpdateListener(a -> {
      pulse = (Float) a.getAnimatedValue();
      invalidate();
    });
    animator.start();
  }

  @Override protected void onDetachedFromWindow() {
    if (animator != null) {
      animator.cancel();
      animator = null;
    }
    super.onDetachedFromWindow();
  }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    paint.setAlpha((int) (255f * pulse));
    float d = getResources().getDisplayMetrics().density;
    float cy = getHeight() / 2f;
    canvas.drawRoundRect(0, cy - d, getWidth(), cy + d, d, d, paint);
  }
}
