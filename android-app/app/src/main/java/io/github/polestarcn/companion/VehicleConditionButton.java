package io.github.polestarcn.companion;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.widget.TextView;
import java.util.function.Supplier;
/** One aligned icon/title row plus separate remaining-service bars; no overlapping TextView draw. */
public final class VehicleConditionButton extends TextView {
 private final Supplier<VehicleSnapshot> snapshot;
 private final android.text.TextPaint paint=new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);

 private Integer timePercent,distancePercent;
 public VehicleConditionButton(Context c,Supplier<VehicleSnapshot> source){super(c);snapshot=source;setIncludeFontPadding(false);}
 public void refreshService(){
  timePercent=ServiceReading.cyclePercent(getContext(),snapshot.get());distancePercent=null;
  boolean bars=timePercent!=null||distancePercent!=null;float d=getResources().getDisplayMetrics().density;
  setPadding((int)(4*d),(int)(2*d),(int)(4*d),(int)((bars?24:2)*d));
  setContentDescription(getText()+" · "+(timePercent==null?"":AppPreferences.tr(getContext(),"保养周期剩余 ","Service cycle remaining ")+timePercent+"% · ")
      +(distancePercent==null?"":AppPreferences.tr(getContext(),"里程剩余 ","Distance remaining ")+distancePercent+"% · ")
      +AppPreferences.tr(getContext(),"参考周期比例，点按查看车辆上报值","Reference-cycle ratios. Tap for vehicle-reported values"));invalidate();
 }
 @Override protected void onDraw(Canvas canvas){
  float d=getResources().getDisplayMetrics().density;int count=(timePercent==null?0:1)+(distancePercent==null?0:1);
  float band=count==0?0:26*d,heading=Math.max(12*d,getHeight()-band),iconSize=0,gap=0;
  paint.setTypeface(getTypeface());paint.setTextSize(getTextSize());paint.setTextAlign(Paint.Align.LEFT);
  boolean warning=VehicleCondition.problem(snapshot.get(),AppPreferences.visible(getContext(),"show_tire_status",true));int color=warning?PsaDesign.accentText(AppPreferences.dark(getContext())):getCurrentTextColor();paint.setColor(color);
  Paint.FontMetrics fm=paint.getFontMetrics();float fontHeight=fm.descent-fm.ascent;
  if(fontHeight>heading-4*d){paint.setTextSize(paint.getTextSize()*(heading-4*d)/fontHeight);fm=paint.getFontMetrics();}
  CharSequence title=android.text.TextUtils.ellipsize(getText(),paint,Math.max(0,getWidth()-iconSize-gap-8*d),android.text.TextUtils.TruncateAt.END);
  float titleWidth=paint.measureText(title.toString()),left=Math.max(4*d,(getWidth()-iconSize-gap-titleWidth)/2);
  canvas.drawText(title.toString(),left+iconSize+gap,(heading-fm.descent-fm.ascent)/2,paint);
  if(count==0)return;float width=(getWidth()-10*d)/count;int i=0;
  if(timePercent!=null)bar(canvas,i++,width,timePercent,AppPreferences.tr(getContext(),"保养","Service"));
  if(distancePercent!=null)bar(canvas,i,width,distancePercent,"km");
 }
 private void bar(Canvas canvas,int index,float width,int percent,String label){
  float d=getResources().getDisplayMetrics().density,left=5*d+index*width;
  paint.setTypeface(PsaDesign.regular());paint.setTextAlign(Paint.Align.CENTER);paint.setColor(getCurrentTextColor());paint.setTextSize(10*getResources().getDisplayMetrics().scaledDensity);
  String value=label+" "+percent+"%";float measured=paint.measureText(value);if(measured>width-5*d)paint.setTextSize(paint.getTextSize()*(width-5*d)/measured);
  canvas.drawText(value,left+width/2,getHeight()-10*d,paint);paint.setColor(PsaDesign.line(AppPreferences.dark(getContext())));
  canvas.drawRoundRect(left,getHeight()-5*d,left+width-4*d,getHeight()-2*d,1*d,1*d,paint);paint.setColor(PsaDesign.ORANGE);
  canvas.drawRoundRect(left,getHeight()-5*d,left+(width-4*d)*percent/100f,getHeight()-2*d,1*d,1*d,paint);
 }
}
