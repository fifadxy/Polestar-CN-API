package io.github.polestarcn.companion;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/** Gives native dialog buttons and choice rows the same app haptics as home controls. */
final class HapticDialogBuilder extends AlertDialog.Builder {
  private final Context context;
  HapticDialogBuilder(Context context) { super(context); this.context = context; }
  private DialogInterface.OnClickListener click(DialogInterface.OnClickListener listener) {
    return (d, which) -> { Haptics.tap(context); if (listener != null) listener.onClick(d, which); };
  }
  @Override public AlertDialog.Builder setPositiveButton(CharSequence text, DialogInterface.OnClickListener listener) {
    return super.setPositiveButton(text, click(listener));
  }
  @Override public AlertDialog.Builder setNegativeButton(CharSequence text, DialogInterface.OnClickListener listener) {
    return super.setNegativeButton(text, click(listener));
  }
  @Override public AlertDialog.Builder setItems(CharSequence[] items, DialogInterface.OnClickListener listener) {
    return super.setItems(items, click(listener));
  }
  @Override public AlertDialog.Builder setSingleChoiceItems(CharSequence[] items, int checked, DialogInterface.OnClickListener listener) {
    return super.setSingleChoiceItems(items, checked, click(listener));
  }
  @Override public AlertDialog.Builder setMultiChoiceItems(CharSequence[] items, boolean[] checked, DialogInterface.OnMultiChoiceClickListener listener) {
    return super.setMultiChoiceItems(items, checked, (d, which, on) -> {
      Haptics.tap(context); if (listener != null) listener.onClick(d, which, on);
    });
  }
}
