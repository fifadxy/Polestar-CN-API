package io.github.polestarcn.companion;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Small charging-only energy pulse. It animates only while attached and active. */
public final class ChargingPulseView extends View {
  private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
  private ValueAnimator animator;
  private float phase;
  private int accent = Color.rgb(94, 141, 122);
  private boolean active = true;

  public ChargingPulseView(Context context) {
    super(context);
    setTag("charging_pulse");
    setContentDescription("Charging energy animation");
  }

  public void setAccentColor(int color) { accent = color; invalidate(); }

  public void setActive(boolean value) {
    active = value;
    setVisibility(value ? VISIBLE : GONE);
    if (value && isAttachedToWindow()) start();
    else stop();
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (active) start();
  }

  @Override protected void onDetachedFromWindow() {
    stop();
    super.onDetachedFromWindow();
  }

  private void start() {
    if (animator != null) return;
    animator = ValueAnimator.ofFloat(0f, 1f);
    animator.setDuration(1800L);
    animator.setRepeatCount(ValueAnimator.INFINITE);
    animator.setInterpolator(new LinearInterpolator());
    animator.addUpdateListener(a -> { phase = (Float) a.getAnimatedValue(); invalidate(); });
    animator.start();
  }

  private void stop() {
    if (animator != null) {
      animator.cancel();
      animator = null;
    }
  }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float d = getResources().getDisplayMetrics().density;
    float h = Math.max(3f*d, getHeight()*.42f);
    float top = (getHeight()-h)/2f, radius = h/2f;
    track.setColor(Color.argb(38, Color.red(accent), Color.green(accent), Color.blue(accent)));
    canvas.drawRoundRect(0, top, getWidth(), top+h, radius, radius, track);
    if (!active || getWidth() <= 0) return;
    float center = -getWidth()*.18f + phase*getWidth()*1.36f;
    float spread = Math.max(34f*d, getWidth()*.20f);
    glow.setShader(new LinearGradient(center-spread,0,center+spread,0,
        new int[]{Color.TRANSPARENT, Color.argb(70,Color.red(accent),Color.green(accent),Color.blue(accent)),
            accent, Color.argb(70,Color.red(accent),Color.green(accent),Color.blue(accent)), Color.TRANSPARENT},
        new float[]{0f,.28f,.5f,.72f,1f}, Shader.TileMode.CLAMP));
    canvas.drawRoundRect(0, top, getWidth(), top+h, radius, radius, glow);
    glow.setShader(null);
  }
}
