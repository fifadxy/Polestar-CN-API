package io.github.polestarcn.companion;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

/** A short, user-configurable pulse for app controls, respecting the system haptics switch. */
public final class Haptics {
  private Haptics() {}
  public static void tap(Context context) { pulse(context,false); }
  public static void vehicle(Context context){pulse(context,true);}
  private static void pulse(Context context,boolean strong) {
    if (!AppPreferences.visible(context, "haptics", true)
        || Settings.System.getInt(context.getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 0) return;
    int strength = Math.min(100,Math.max(1, AppPreferences.slider(context, "haptic_strength", 35))*(strong?2:1));
    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    if (vibrator == null || !vibrator.hasVibrator()) return;
    try {
      boolean amplitude = vibrator.hasAmplitudeControl();
      vibrator.vibrate(VibrationEffect.createOneShot(amplitude ? (strong?32:16) : (strong?16:6) + strength / 5,
          amplitude ? Math.max(1, Math.round(255f * strength / 100f)) : VibrationEffect.DEFAULT_AMPLITUDE));
    } catch (SecurityException | UnsupportedOperationException ignored) {
      // Devices without app-controlled haptics still support every action.
    }
  }
}
