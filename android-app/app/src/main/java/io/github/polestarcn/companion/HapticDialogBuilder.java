package io.github.polestarcn.companion;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/** Gives native dialog buttons and choice rows the same app haptics as home controls. */
final class HapticDialogBuilder extends AlertDialog.Builder {
  private final Context context;
  private boolean previewAdded;private CharSequence heading;
  @Override public AlertDialog.Builder setTitle(CharSequence title){heading=title;return super.setTitle(title);}
  HapticDialogBuilder(Context context) { super(context); this.context = context; }
  @Override public AlertDialog.Builder setView(android.view.View view) {
    if(view instanceof android.widget.LinearLayout) {
      android.widget.LinearLayout panel=(android.widget.LinearLayout)view;previewAdded=panel.findViewWithTag("settings_live_preview")!=null;
      if(context instanceof MainActivity&&((MainActivity)context).showingSettings()&&panel.findViewWithTag("settings_live_preview")==null){panel.addView(new SettingsPreviewView(context),0);previewAdded=true;}
      android.widget.ScrollView scroll=new android.widget.ScrollView(context);scroll.addView(view);return super.setView(scroll);
    }
    return super.setView(view);
  }
  @Override public AlertDialog create() {
    if(!previewAdded&&context instanceof MainActivity&&((MainActivity)context).showingSettings()){android.widget.LinearLayout header=new android.widget.LinearLayout(context);header.setOrientation(android.widget.LinearLayout.VERTICAL);int pad=(int)(16*context.getResources().getDisplayMetrics().density);header.setPadding(pad,pad,pad,0);android.widget.TextView title=new android.widget.TextView(context);title.setText(heading);title.setTextSize(17);title.setTextColor(AppPreferences.dark(context)?android.graphics.Color.WHITE:android.graphics.Color.BLACK);header.addView(title);header.addView(new SettingsPreviewView(context));super.setCustomTitle(header);}
    AlertDialog dialog = super.create();
    dialog.setOnShowListener(ignored -> {
      android.view.Window window=dialog.getWindow();boolean dark=AppPreferences.dark(context);
      if(window!=null){android.graphics.drawable.GradientDrawable glass=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,dark?new int[]{0xF02A3036,0xF2181D22}:new int[]{0xF8FFFFFF,0xF0EDF1F4});glass.setCornerRadius(24*context.getResources().getDisplayMetrics().density);glass.setStroke(1,dark?0x557C8993:0xAAFFFFFF);window.setBackgroundDrawable(glass);window.setDimAmount(.24f);
        if(android.os.Build.VERSION.SDK_INT>=31){android.view.WindowManager wm=context.getSystemService(android.view.WindowManager.class);if(wm!=null&&wm.isCrossWindowBlurEnabled())window.setBackgroundBlurRadius(24);}
        android.view.View decor=window.getDecorView();UiTypography.applyTree(decor);decor.animate().cancel();decor.setAlpha(0f);decor.setScaleX(1f);decor.setScaleY(1f);decor.setTranslationX(0f);decor.setTranslationY(0f);window.setWindowAnimations(0);decor.animate().alpha(1f).setDuration(220).start();window.setLayout(Math.min((int)(360*context.getResources().getDisplayMetrics().density),(int)(context.getResources().getDisplayMetrics().widthPixels*.86f)),android.view.ViewGroup.LayoutParams.WRAP_CONTENT);decor.post(()->{int max=(int)(context.getResources().getDisplayMetrics().heightPixels*.78f);if(decor.getHeight()>max)window.setLayout(window.getAttributes().width,max);});}

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
    UiTypography.applyTree(view);view.setHapticFeedbackEnabled(false);
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
  @Override public AlertDialog.Builder setNeutralButton(CharSequence text,DialogInterface.OnClickListener listener) {
    return super.setNeutralButton(text,click(listener));
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
