package io.github.polestarcn.companion;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.text.*;
import android.widget.TextView;
/** One centered icon/title group and a separately bounded feedback line. */
final class UtilityButton extends TextView {
 private final TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);private String feedback="";
 UtilityButton(Context c){super(c);setIncludeFontPadding(false);}
 void feedback(String value){feedback=AppPreferences.visible(getContext(),"show_button_feedback",true)?value:"";invalidate();}
 @Override protected void onDraw(Canvas c){
  float d=getResources().getDisplayMetrics().density,w=getWidth()-8*d;boolean status=!feedback.isEmpty();float lower=status?Math.min(getHeight()*.35f,17*d):0,h=getHeight()-lower;
  Drawable icon=getCompoundDrawablesRelative()[0];float iconSize=icon==null?0:Math.min(icon.getBounds().width(),h-6*d),gap=icon==null?0:4*d;
  paint.setTypeface(Typeface.create(getTypeface(),AppPreferences.visible(getContext(),"ui_button_bold",true)?Typeface.BOLD:Typeface.NORMAL));paint.setTextSize(Math.min(getTextSize(),h*.52f));paint.setColor(getCurrentTextColor());paint.setTextAlign(Paint.Align.LEFT);
  while(paint.getTextSize()>6*d&&paint.measureText(getText().toString())>w-iconSize-gap)paint.setTextSize(paint.getTextSize()-.5f*d);
  String title=TextUtils.ellipsize(getText(),paint,Math.max(0,w-iconSize-gap),TextUtils.TruncateAt.END).toString();float x=(getWidth()-iconSize-gap-paint.measureText(title))/2;
  if(icon!=null){icon.setTint(getCurrentTextColor());int top=Math.round((h-iconSize)/2);icon.setBounds(Math.round(x),top,Math.round(x+iconSize),Math.round(top+iconSize));icon.draw(c);}
  Paint.FontMetrics fm=paint.getFontMetrics();c.drawText(title,x+iconSize+gap,(h-fm.ascent-fm.descent)/2,paint);
  if(status){paint.setTypeface(ButtonFeedback.typeface(getContext()));paint.setTextSize(Math.min(ButtonFeedback.sp(getContext())*getResources().getDisplayMetrics().scaledDensity,lower*.72f));paint.setTextAlign(Paint.Align.CENTER);String fitted=TextUtils.ellipsize(feedback,paint,Math.max(0,w),TextUtils.TruncateAt.END).toString();fm=paint.getFontMetrics();c.drawText(fitted,getWidth()/2f,h+(lower-fm.ascent-fm.descent)/2,paint);}
 }
}
