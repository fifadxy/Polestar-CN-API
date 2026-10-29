package io.github.polestarcn.companion;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.text.*;
import android.text.style.*;
import android.view.Gravity;
import android.widget.TextView;

/** Native glyph/text layout: no custom text painting or single-line scrolling on device canvases. */
class CenteredStatusButton extends TextView {
 private String title="";private boolean composing,slot;private String feedback="";private Drawable glyph;private Integer bar;
 private final Paint track=new Paint(Paint.ANTI_ALIAS_FLAG);
 CenteredStatusButton(Context c){super(c);setIncludeFontPadding(false);}
 @Override public void setText(CharSequence value,BufferType type){if(!composing)title=value==null?"":value.toString().split("\n",2)[0];super.setText(value,type);}
 final String title(){return title;}
 final void glyph(int resource){glyph=resource==0?null:getContext().getDrawable(resource).mutate();compose();}
 final void reserveIconRow(boolean value){slot=value;compose();}
 final void contents(String detail,Integer progress){feedback=detail==null?"":detail;bar=progress;compose();}
 @Override public void setTextColor(int color){super.setTextColor(color);if(glyph!=null)glyph.setTint(color);for(Drawable d:getCompoundDrawablesRelative())if(d!=null)d.setTint(color);}
 private void compose(){
  float d=getResources().getDisplayMetrics().density;boolean icon=glyph!=null;
  setSingleLine(false);setHorizontallyScrolling(false);setGravity(slot?Gravity.TOP|Gravity.CENTER_HORIZONTAL:Gravity.CENTER);setEllipsize(TextUtils.TruncateAt.END);
  boolean second=!title.isEmpty()&&(!feedback.isEmpty()||slot);setMaxLines(second?2:1);setPadding(Math.round(4*d),Math.round(2*d),Math.round(4*d),Math.round((slot?6:bar!=null?6:2)*d));
  setTypeface(PsaDesign.regular());setAutoSizeTextTypeWithDefaults(AUTO_SIZE_TEXT_TYPE_NONE);
  SpannableStringBuilder value=new SpannableStringBuilder(title);if(AppPreferences.visible(getContext(),"ui_button_bold",true))value.setSpan(new StyleSpan(Typeface.BOLD),0,value.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  if(second||title.isEmpty()&&!feedback.isEmpty()){if(second)value.append('\n');int start=value.length();value.append(feedback.isEmpty()?"\u00a0":feedback);value.setSpan(new ForegroundColorSpan(AppPreferences.dark(getContext())?Color.WHITE:Color.rgb(115,115,115)),start,value.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);value.setSpan(new RelativeSizeSpan(Math.min(.85f,ButtonFeedback.sp(getContext())/11f)),start,value.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);if(AppPreferences.visible(getContext(),"feedback_bold",false))value.setSpan(new StyleSpan(Typeface.BOLD),start,value.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}
  if(icon){glyph.setTint(getCurrentTextColor());int px=Math.round((slot?(getResources().getConfiguration().screenHeightDp<=360?12:18):16)*d);glyph.setBounds(0,0,px,px);}
  if(glyph!=null||slot)setCompoundDrawablesRelative(slot?null:glyph,slot?glyph:null,null,null);setCompoundDrawablePadding(Math.round(2*d));
  composing=true;super.setText(value,BufferType.SPANNABLE);composing=false;scrollTo(0,0);
 }
 @Override protected void onMeasure(int widthSpec,int heightSpec){
  // Use TextView's real layout, including fallback-font metrics, feedback spans
  // and compound icons. A separate StaticLayout can underestimate CJK height.
  float scale=getResources().getDisplayMetrics().scaledDensity;
  int low=5,high=11,best=5;
  while(low<=high){int sp=(low+high)/2;
   setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,sp*scale);
   super.onMeasure(widthSpec,heightSpec);
   Layout nativeLayout=getLayout();
   int available=getMeasuredHeight()-getCompoundPaddingTop()-getCompoundPaddingBottom();
   if(nativeLayout!=null&&nativeLayout.getHeight()<=available&&nativeLayout.getLineCount()<=getMaxLines()){best=sp;low=sp+1;}else high=sp-1;
  }
  setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,best*scale);
  super.onMeasure(widthSpec,heightSpec);
 }
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(bar==null)return;float d=getResources().getDisplayMetrics().density,width=Math.max(0,Math.min(getWidth()-10*d,96*d)),left=(getWidth()-width)/2f;track.setColor(PsaDesign.line(AppPreferences.dark(getContext())));canvas.drawRoundRect(left,getHeight()-4*d,left+width,getHeight()-2*d,d,d,track);track.setColor(PsaDesign.ORANGE);canvas.drawRoundRect(left,getHeight()-4*d,left+width*bar/100f,getHeight()-2*d,d,d,track);}
}
