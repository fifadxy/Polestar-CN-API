package io.github.polestarcn.companion;
import android.content.Context;
import android.graphics.*;
import android.widget.TextView;
import java.util.function.Supplier;
/** Compact optional remaining-service bars inside the same condition button in every skin. */
public final class VehicleConditionButton extends TextView {
  private boolean showTime,showDistance;private Integer timePercent,distancePercent;
  private final Supplier<VehicleSnapshot> snapshot;private final android.text.TextPaint paint=new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
  public VehicleConditionButton(Context c,Supplier<VehicleSnapshot> source){super(c);setIncludeFontPadding(false);snapshot=source;paint.setTypeface(PsaDesign.regular());}
  private boolean time(){return showTime;}
  private boolean distance(){return showDistance;}
  public void refreshService(){
    timePercent=ServiceReading.timePercent(getContext(),snapshot.get());distancePercent=ServiceReading.distancePercent(getContext(),snapshot.get());
    showTime=AppPreferences.visible(getContext(),"condition_service_time",false)&&timePercent!=null;
    showDistance=AppPreferences.visible(getContext(),"condition_service_distance",false)&&distancePercent!=null;
    boolean bars=time()||distance();float d=getResources().getDisplayMetrics().density;
    setPadding((int)(3*d),(int)(bars?3*d:2*d),(int)(3*d),(int)(bars?21*d:2*d));
    setContentDescription(getText()+" · "+(time()?"Service time left "+ServiceReading.number(ServiceReading.timePercent(getContext(),snapshot.get()),"%")+" · ":"")
        +(distance()?"Service distance left "+ServiceReading.number(ServiceReading.distancePercent(getContext(),snapshot.get()),"%")+" · ":"")
        +AppPreferences.tr(getContext(),"比例按设置周期估算，点按查看完整车况","Percentages use configured service cycles. Tap for full condition."));invalidate();
  }
  @Override protected void onDraw(Canvas canvas){
    int count=(time()?1:0)+(distance()?1:0);
    float density=getResources().getDisplayMetrics().density,heading=count>0?getHeight()-21*density:getHeight();
    paint.setTypeface(getTypeface());paint.setTextSize(getTextSize());paint.setColor(getCurrentTextColor());paint.setTextAlign(Paint.Align.CENTER);
    Paint.FontMetrics metrics=paint.getFontMetrics();float height=metrics.descent-metrics.ascent;
    if(height>heading-4*density){paint.setTextSize(paint.getTextSize()*Math.max(.5f,(heading-4*density)/height));metrics=paint.getFontMetrics();}
    CharSequence title=android.text.TextUtils.ellipsize(getText(),paint,Math.max(0,getWidth()-8*density),android.text.TextUtils.TruncateAt.END);
    canvas.drawText(title.toString(),getWidth()/2f,(heading-metrics.descent-metrics.ascent)/2f,paint);
    if(count==0)return;
    float d=getResources().getDisplayMetrics().density,w=(getWidth()-8*d)/count;int index=0;
    if(time())bar(canvas,index++,w,timePercent,"d");
    if(distance())bar(canvas,index,w,distancePercent,"km");
  }
  private void bar(Canvas c,int index,float width,Integer percent,String unit){
    float d=getResources().getDisplayMetrics().density,left=4*d+index*width;
    paint.setTextSize(8*getResources().getDisplayMetrics().scaledDensity);paint.setTextAlign(Paint.Align.CENTER);paint.setColor(getCurrentTextColor());
    String label=(width<45*d?"":unit+" ")+ServiceReading.number(percent,"%");
    float measured=paint.measureText(label);if(measured>width-5*d)paint.setTextSize(paint.getTextSize()*(width-5*d)/measured);
    c.drawText(label,left+width/2,getHeight()-7*d,paint);
    paint.setColor(PsaDesign.line(AppPreferences.dark(getContext())));c.drawRect(left,getHeight()-4*d,left+width-3*d,getHeight()-2*d,paint);
    if(percent!=null){paint.setColor(VehicleCondition.problem(snapshot.get(),AppPreferences.visible(getContext(),"show_tire_status",true))?getCurrentTextColor():PsaDesign.ORANGE);c.drawRect(left,getHeight()-4*d,left+(width-3*d)*percent/100f,getHeight()-2*d,paint);}
  }
}
