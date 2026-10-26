package io.github.polestarcn.companion;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * Larger, low-cost Polestar ambient-light band.
 * Only this small view invalidates; the dashboard itself is never continuously redrawn.
 */
public final class ViAmbientGlowView extends View {
  private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint core = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Matrix matrix = new Matrix();
  private Shader haloShader;
  private Shader coreShader;
  private ValueAnimator animator;
  private float pulse = .32f;
  private float phase;

  public ViAmbientGlowView(Context context) {
    super(context);
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    setLayerType(LAYER_TYPE_HARDWARE, null);
  }

  @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    int orange = Color.rgb(255, 107, 0);
    haloShader = new LinearGradient(
        -w * .25f, 0, w * 1.25f, 0,
        new int[]{Color.TRANSPARENT, orange, orange, Color.TRANSPARENT},
        new float[]{0f, .35f, .65f, 1f}, Shader.TileMode.CLAMP);
    coreShader = new LinearGradient(
        0, 0, Math.max(1, w), 0,
        new int[]{Color.TRANSPARENT, orange, Color.TRANSPARENT},
        new float[]{0f, .5f, 1f}, Shader.TileMode.CLAMP);
    halo.setShader(haloShader);
    core.setShader(coreShader);
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (animator != null) animator.cancel();
    animator = ValueAnimator.ofFloat(0f, 1f);
    animator.setDuration(3600L);
    animator.setRepeatCount(ValueAnimator.INFINITE);
    animator.setInterpolator(new AccelerateDecelerateInterpolator());
    animator.addUpdateListener(a -> {
      phase = (Float) a.getAnimatedValue();
      pulse = .30f + .42f * (float) Math.sin(Math.PI * phase);
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
    if (haloShader == null || coreShader == null) return;
    boolean dark = AppPreferences.dark(getContext());
    float shift = (phase - .5f) * getWidth() * .18f;
    matrix.reset();
    matrix.setTranslate(shift, 0);
    haloShader.setLocalMatrix(matrix);
    coreShader.setLocalMatrix(matrix);

    float d = getResources().getDisplayMetrics().density;
    float cy = getHeight() / 2f;
    halo.setAlpha((int) (255f * pulse * (dark ? .58f : .34f)));
    core.setAlpha((int) (255f * (.42f + pulse * .40f) * (dark ? .92f : .72f)));
    canvas.drawRoundRect(0, d, getWidth(), getHeight() - d, getHeight() / 2f,
        getHeight() / 2f, halo);
    canvas.drawRoundRect(
        getWidth() * .10f, cy - 1.25f * d, getWidth() * .90f, cy + 1.25f * d,
        2f * d, 2f * d, core);
  }
}
