package io.github.polestarcn.companion;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.text.*;
import android.text.style.*;
import android.view.Gravity;
import android.widget.TextView;

/** Native glyph/text layout: no custom text painting or single-line scrolling on device canvases. */
class CenteredStatusButton extends androidx.appcompat.widget.AppCompatTextView {
 private String title="";private boolean composing,slot,iconOnly;private String feedback="";private Drawable glyph;private boolean serviceGlyph;private Integer bar;
 private int desiredGlyphSize;private int previewIcon=-1,previewLabel=-1;
 final void previewAppearance(int icon,int label){if(icon>=0)previewIcon=icon;if(label>=0)previewLabel=label;refreshAppearance();}
 private float iconScale(){return previewIcon>=0?previewIcon/100f:UiDisplayMode.minimal(getContext())?MinimalIconAppearance.sizePercent(getContext())/100f:ButtonAppearance.iconScale(getContext());}
 private float labelScale(){return previewLabel<0?ButtonAppearance.textScale(getContext()):previewLabel/100f;}
 private final Paint track=new Paint(Paint.ANTI_ALIAS_FLAG);
 CenteredStatusButton(Context c){super(c);setIncludeFontPadding(false);}
 @Override public void setText(CharSequence value,BufferType type){if(!composing)title=value==null?"":value.toString().split("\n",2)[0];super.setText(value,type);}
 final String title(){return title;}
 final void glyph(int resource){serviceGlyph=resource==R.drawable.ic_service_reference;glyph=resource==0?null:MinimalIconAppearance.drawable(getContext(),resource);compose();}
 private void tintGlyph(int color){
  if(!serviceGlyph){glyph.setTint(color);return;}
  // Preserve the reference gauge's orange sector; recolor only its white car/needle.
  glyph.setTintList(null);glyph.setColorFilter(new ColorMatrixColorFilter(new float[]{0,0,(Color.red(color)-255)/255f,0,255,0,0,(Color.green(color)-107)/255f,0,107,0,0,Color.blue(color)/255f,0,0,0,0,0,1,0}));
 }
 final void reserveIconRow(boolean value){slot=value;compose();}
 final void contents(String detail,Integer progress){feedback=detail==null?"":detail;bar=progress;compose();}
 @Override public void setTextColor(int color){super.setTextColor(color);if(glyph!=null)tintGlyph(color);for(Drawable d:getCompoundDrawablesRelative())if(d!=null)d.setTint(color);}
 final void refreshAppearance(){compose();requestLayout();invalidate();}
 private void compose(){
  float d=getResources().getDisplayMetrics().density;boolean icon=glyph!=null;boolean minimal=UiDisplayMode.minimal(getContext())&&icon;String shownTitle=minimal?"":title;String visibleFeedback=minimal?"":feedback;iconOnly=minimal;
  setSingleLine(false);setHorizontallyScrolling(false);setGravity(minimal?Gravity.CENTER:slot?Gravity.TOP|Gravity.CENTER_HORIZONTAL:Gravity.CENTER);setEllipsize(TextUtils.TruncateAt.END);
  boolean second=!shownTitle.isEmpty()&&(!visibleFeedback.isEmpty()||slot);setMaxLines(second?2:1);setPadding(Math.round(4*d),Math.round(2*d),Math.round(4*d),Math.round((slot&&!minimal?6:bar!=null?6:2)*d));
  setTypeface(PsaDesign.regular());androidx.core.widget.TextViewCompat.setAutoSizeTextTypeWithDefaults(this,androidx.core.widget.TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
  SpannableStringBuilder value=new SpannableStringBuilder(shownTitle);if(AppPreferences.visible(getContext(),"ui_button_bold",true))value.setSpan(new StyleSpan(Typeface.BOLD),0,value.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  if(second||shownTitle.isEmpty()&&!visibleFeedback.isEmpty()){if(second)value.append('\n');int start=value.length();value.append(visibleFeedback.isEmpty()?"\u00a0":visibleFeedback);value.setSpan(new ForegroundColorSpan(AppPreferences.dark(getContext())?Color.WHITE:Color.rgb(115,115,115)),start,value.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);value.setSpan(new RelativeSizeSpan(Math.min(.85f,ButtonFeedback.sp(getContext())/11f)),start,value.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);if(AppPreferences.visible(getContext(),"feedback_bold",false))value.setSpan(new StyleSpan(Typeface.BOLD),start,value.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}
  if(icon){tintGlyph(getCurrentTextColor());int base=slot?(getResources().getConfiguration().screenHeightDp<=360?12:18):16;int px=Math.round((minimal?(getResources().getConfiguration().screenHeightDp<=360?24:32):base)*d*iconScale());desiredGlyphSize=px;glyph.setBounds(0,0,px,px);}
  if(glyph!=null||slot)setCompoundDrawablesRelative(iconOnly||slot?null:glyph,!iconOnly&&slot?glyph:null,null,null);setCompoundDrawablePadding(Math.round(2*d));
  composing=true;super.setText(value,BufferType.SPANNABLE);composing=false;scrollTo(0,0);
 }
 @Override protected void onMeasure(int widthSpec,int heightSpec){
  // Use TextView's real layout, including fallback-font metrics, feedback spans
  // and compound icons. A separate StaticLayout can underestimate CJK height.
  float scale=getResources().getDisplayMetrics().scaledDensity;
  if(glyph!=null){int width=MeasureSpec.getSize(widthSpec)-getPaddingLeft()-getPaddingRight();int height=MeasureSpec.getSize(heightSpec)-getPaddingTop()-getPaddingBottom();
   if(slot&&!iconOnly)height-=Math.round(getMaxLines()*5*scale)+getCompoundDrawablePadding();
   int px=desiredGlyphSize;if(MeasureSpec.getMode(widthSpec)!=MeasureSpec.UNSPECIFIED)px=Math.min(px,Math.max(1,width));if(MeasureSpec.getMode(heightSpec)!=MeasureSpec.UNSPECIFIED)px=Math.min(px,Math.max(1,height));glyph.setBounds(0,0,px,px);
  }
  int low=5,high=Math.round(11*labelScale()),best=5;
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
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(iconOnly&&glyph!=null){canvas.save();canvas.translate((getWidth()-glyph.getBounds().width())/2f,(getHeight()-glyph.getBounds().height())/2f);glyph.draw(canvas);canvas.restore();}if(bar==null||UiDisplayMode.minimal(getContext()))return;float d=getResources().getDisplayMetrics().density,width=Math.max(0,Math.min(getWidth()-10*d,96*d)),left=(getWidth()-width)/2f;track.setColor(PsaDesign.line(AppPreferences.dark(getContext())));canvas.drawRoundRect(left,getHeight()-4*d,left+width,getHeight()-2*d,d,d,track);track.setColor(PsaDesign.ORANGE);canvas.drawRoundRect(left,getHeight()-4*d,left+width*bar/100f,getHeight()-2*d,d,d,track);}
}