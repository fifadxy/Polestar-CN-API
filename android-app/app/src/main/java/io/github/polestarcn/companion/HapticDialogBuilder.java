package io.github.polestarcn.companion;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ScrollView;
import android.widget.TextView;

/** Fixed-position, alpha-only secondary dialogs; no post-show geometry or typography changes. */
final class HapticDialogBuilder extends AlertDialog.Builder {
  private final Context context;
  private boolean mapCompact;
  HapticDialogBuilder(Context context){super(context);this.context=context;}
  HapticDialogBuilder compactMapWindow(){mapCompact=true;return this;}
  @Override public AlertDialog.Builder setView(View view){
    // Style once BEFORE first attachment/measurement to avoid font-induced layout jumps.
    UiTypography.applyTree(view);
    if(view instanceof android.widget.LinearLayout){
      ScrollView sc=new ScrollView(context);
      sc.setFillViewport(false);
      sc.setVerticalScrollBarEnabled(false);
      sc.setOverScrollMode(View.OVER_SCROLL_NEVER);
      sc.addView(view);
      return super.setView(sc);
    }
    return super.setView(view);
  }
  @Override public AlertDialog create(){
    AlertDialog dialog=super.create();
    Window window=dialog.getWindow();
    if(window!=null){
      boolean dark=AppPreferences.dark(context);
      GradientDrawable glass=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
          dark?new int[]{0xF02A3036,0xF2181D22}:new int[]{0xF8FFFFFF,0xF0EDF1F4});
      float density=context.getResources().getDisplayMetrics().density;
      glass.setCornerRadius(24*density);
      glass.setStroke(1,dark?0x557C8993:0xAAFFFFFF);
      window.setBackgroundDrawable(glass);
      window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
      window.setDimAmount(.24f);
      window.setWindowAnimations(R.style.PsaDialogFadeAnimation);
      window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
          |WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
      android.util.DisplayMetrics dm=context.getResources().getDisplayMetrics();
      int width=Math.min((int)(360*density),(int)(dm.widthPixels*.86f));
      int height=ViewGroup.LayoutParams.WRAP_CONTENT;
      if(mapCompact){
        int[] size=DialogWindowGeometry.map(dm.widthPixels,dm.heightPixels,density);
        width=size[0];height=size[1];
      }
      WindowManager.LayoutParams attrs=window.getAttributes();
      attrs.width=width;attrs.height=height;attrs.gravity=Gravity.CENTER;
      window.setAttributes(attrs);
    }
    return dialog;
  }
  private ArrayAdapter<CharSequence> stableAdapter(CharSequence[] items,boolean single){
    return new ArrayAdapter<CharSequence>(context,
        single?android.R.layout.select_dialog_singlechoice:android.R.layout.select_dialog_item,items){
      @Override public View getView(int position,View convertView,ViewGroup parent){
        View row=super.getView(position,convertView,parent);
        if(row instanceof TextView)((TextView)row).setTypeface(PsaDesign.regular());
        if(row instanceof CheckedTextView){
          CheckedTextView check=(CheckedTextView)row;
          check.setCheckMarkDrawable(R.drawable.vi_checkbox);
          check.setCheckMarkTintList(ColorStateList.valueOf(
              PsaDesign.accentText(AppPreferences.dark(context))));
        }
        UiTypography.applyTree(row);
        return row;
      }
    };
  }
  private DialogInterface.OnClickListener click(DialogInterface.OnClickListener listener){
    return (d,which)->{Haptics.tap(context);if(listener!=null)listener.onClick(d,which);};
  }
  @Override public AlertDialog.Builder setPositiveButton(CharSequence text,DialogInterface.OnClickListener listener){
    return super.setPositiveButton(text,click(listener));
  }
  @Override public AlertDialog.Builder setNegativeButton(CharSequence text,DialogInterface.OnClickListener listener){
    return super.setNegativeButton(text,click(listener));
  }
  @Override public AlertDialog.Builder setNeutralButton(CharSequence text,DialogInterface.OnClickListener listener){
    return super.setNeutralButton(text,click(listener));
  }
  @Override public AlertDialog.Builder setItems(CharSequence[] items,DialogInterface.OnClickListener listener){
    return super.setAdapter(stableAdapter(items,false),click(listener));
  }
  @Override public AlertDialog.Builder setSingleChoiceItems(CharSequence[] items,int checked,DialogInterface.OnClickListener listener){
    return super.setSingleChoiceItems(stableAdapter(items,true),checked,click(listener));
  }
  @Override public AlertDialog.Builder setMultiChoiceItems(CharSequence[] items,boolean[] checked,DialogInterface.OnMultiChoiceClickListener listener){
    return super.setMultiChoiceItems(items,checked,(d,which,on)->{
      Haptics.tap(context);if(listener!=null)listener.onClick(d,which,on);
    });
  }
}
