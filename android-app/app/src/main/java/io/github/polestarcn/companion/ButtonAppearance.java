package io.github.polestarcn.companion;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
/** Independent control sizes; baseline tags prevent repeated scaling on refresh. */
final class ButtonAppearance {
 static int iconPercent(Context c){return EffectSettings.number(c,"ui_icon_percent",100,50,250);}
 static int textPercent(Context c){return EffectSettings.number(c,"ui_button_text_percent",100,50,220);}
 static float iconScale(Context c){return iconPercent(c)/100f;}
 static float textScale(Context c){return textPercent(c)/100f;}
 static void apply(View v){
  if(v instanceof CenteredStatusButton){((CenteredStatusButton)v).refreshAppearance();return;}
  String tag=v.getTag() instanceof String?(String)v.getTag():"";
  if(v instanceof ImageView&&tag.startsWith("control_icon_")){
   android.view.ViewGroup.LayoutParams lp=v.getLayoutParams();if(lp==null||lp.width<=0||lp.height<=0)return;
   int[] base=(int[])v.getTag(R.id.psa_size_baseline);if(base==null){base=new int[]{lp.width,lp.height};v.setTag(R.id.psa_size_baseline,base);}
   float scale=iconScale(v.getContext());int limit=Math.round(68*v.getResources().getDisplayMetrics().density);
   int maxWidth=limit,maxHeight=limit;if(v.getParent() instanceof View){View parent=(View)v.getParent();android.view.ViewGroup.LayoutParams box=parent.getLayoutParams();if(box!=null){if(box.width>0)maxWidth=Math.min(maxWidth,box.width-parent.getPaddingLeft()-parent.getPaddingRight());if(box.height>0)maxHeight=Math.min(maxHeight,box.height-parent.getPaddingTop()-parent.getPaddingBottom());}}lp.width=Math.max(1,Math.min(maxWidth,Math.round(base[0]*scale)));lp.height=Math.max(1,Math.min(maxHeight,Math.round(base[1]*scale)));v.setLayoutParams(lp);
  }
  if(v instanceof TextView&&(tag.startsWith("control_title_")||tag.equals("volike_control_label")||tag.equals("shortcut_label"))){
   TextView t=(TextView)v;Float base=(Float)t.getTag(R.id.psa_size_baseline);if(base==null){base=t.getTextSize();t.setTag(R.id.psa_size_baseline,base);}
   androidx.core.widget.TextViewCompat.setAutoSizeTextTypeWithDefaults(t,0);t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,base*textScale(t.getContext()));
  }
 }
}
