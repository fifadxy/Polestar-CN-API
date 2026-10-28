package io.github.polestarcn.companion;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.TextView;
import java.util.function.Supplier;
/** Centered condition group with optional percentage/bar, bounded to the same utility slot. */
public final class VehicleConditionButton extends TextView {
 private final Supplier<VehicleSnapshot> snapshot;
 private final android.text.TextPaint paint=new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
 private Integer percent;private String mode;private Drawable icon;
 public VehicleConditionButton(Context c,Supplier<VehicleSnapshot> source){super(c);snapshot=source;setIncludeFontPadding(false);}
 public void setConditionIcon(int res){icon=res==0?null:getContext().getDrawable(res).mutate();invalidate();}
 public static String displayMode(Context c){String v=AppPreferences.get(c,"condition_display","bar");return v.equals("percent")||v.equals("text")?v:"bar";}
 public void refreshService(){mode=displayMode(getContext());percent=mode.equals("text")?null:ServiceReading.cyclePercent(getContext(),snapshot.get());setPadding(0,0,0,0);
  setContentDescription(getText()+(percent==null?"":" · "+AppPreferences.tr(getContext(),"保养周期剩余 ","Service cycle remaining ")+percent+"%")+AppPreferences.tr(getContext()," · 点击展开车况"," · Expand vehicle condition"));invalidate();}
 @Override protected void onDraw(Canvas c){if(getWidth()<=0||getHeight()<=0)return;
  float d=getResources().getDisplayMetrics().density,w=getWidth()-10*d;boolean bar=percent!=null&&"bar".equals(mode),glyph=icon!=null&&"bar".equals(mode);float heading=getHeight()-(bar?15*d:0);
  paint.setTypeface(Typeface.create(getTypeface(),AppPreferences.visible(getContext(),"ui_button_bold",true)?Typeface.BOLD:Typeface.NORMAL));paint.setTextSize(Math.min(getTextSize(),heading*.56f));paint.setTextAlign(Paint.Align.LEFT);paint.setColor(getCurrentTextColor());
  String title=getText().toString()+(percent!=null&&!bar?" "+percent+"%":"");float size=glyph?Math.min(16*d,heading*.65f):0,gap=glyph?4*d:0;
  while(paint.getTextSize()>6*d&&paint.measureText(title)>w-size-gap)paint.setTextSize(paint.getTextSize()-.5f*d);
  title=TextUtils.ellipsize(title,paint,Math.max(0,w-size-gap),TextUtils.TruncateAt.END).toString();float x=(getWidth()-size-gap-paint.measureText(title))/2;
  if(glyph){icon.setTint(getCurrentTextColor());int top=Math.round((heading-size)/2);icon.setBounds(Math.round(x),top,Math.round(x+size),Math.round(top+size));icon.draw(c);}
  Paint.FontMetrics fm=paint.getFontMetrics();c.drawText(title,x+size+gap,(heading-fm.descent-fm.ascent)/2,paint);if(!bar)return;
  paint.setTypeface(ButtonFeedback.typeface(getContext()));paint.setTextSize(Math.min(ButtonFeedback.sp(getContext())*getResources().getDisplayMetrics().scaledDensity,10*d));paint.setTextAlign(Paint.Align.CENTER);
  String label=AppPreferences.tr(getContext(),"保养周期 ","Service cycle ")+percent+"%";while(paint.getTextSize()>6*d&&paint.measureText(label)>w)paint.setTextSize(paint.getTextSize()-.5f*d);c.drawText(label,getWidth()/2f,getHeight()-6*d,paint);
  float length=Math.max(0,Math.min(w,96*d)),left=(getWidth()-length)/2;paint.setColor(PsaDesign.line(AppPreferences.dark(getContext())));c.drawRoundRect(left,getHeight()-4*d,left+length,getHeight()-2*d,d,d,paint);paint.setColor(PsaDesign.ORANGE);c.drawRoundRect(left,getHeight()-4*d,left+length*percent/100f,getHeight()-2*d,d,d,paint);
 }
}
