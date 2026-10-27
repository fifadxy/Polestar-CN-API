package io.github.polestarcn.companion;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/** Gives native dialog buttons and choice rows the same app haptics as home controls. */
final class HapticDialogBuilder extends AlertDialog.Builder {
  private final Context context;
  HapticDialogBuilder(Context context) { super(context); this.context = context; }
  @Override public AlertDialog create() {
    AlertDialog dialog = super.create();
    dialog.setOnShowListener(ignored -> {
      android.widget.ListView list=dialog.getListView();
      if(list!=null) {
        list.setOnHierarchyChangeListener(new android.view.ViewGroup.OnHierarchyChangeListener() {
          public void onChildViewAdded(android.view.View parent,android.view.View child){ style(child); }
          public void onChildViewRemoved(android.view.View parent,android.view.View child){}
        });
        style(list);
      }
    });
    return dialog;
  }
  private void style(android.view.View view) {
    if(view instanceof android.widget.TextView) ((android.widget.TextView)view).setTypeface(PsaDesign.regular());
    if(view instanceof android.widget.CheckedTextView) {
      android.widget.CheckedTextView choice=(android.widget.CheckedTextView)view;
      choice.setCheckMarkDrawable(io.github.polestarcn.companion.R.drawable.vi_checkbox);
      choice.setCheckMarkTintList(android.content.res.ColorStateList.valueOf(PsaDesign.accentText(AppPreferences.dark(context))));
    }
    if(view instanceof android.view.ViewGroup) {
      android.view.ViewGroup group=(android.view.ViewGroup)view;
      for(int i=0;i<group.getChildCount();i++)style(group.getChildAt(i));
    }
  }
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
