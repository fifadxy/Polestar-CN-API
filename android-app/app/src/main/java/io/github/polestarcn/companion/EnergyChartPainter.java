package io.github.polestarcn.companion;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Shared VI-color chart: actual observations only, smooth curves pass through reported bins. */
final class EnergyChartPainter {
  private EnergyChartPainter(){}
  private static final int ORANGE=PsaDesign.ORANGE, LIGHT=Color.rgb(255,163,85);
  static void draw(Canvas canvas,List<EnergyChartSeries.Bin> bins,int metric,int chartStyle,
      double perPercent,double tariff,int ink,int muted,int grouping,boolean backdrop,float density){
    float w=canvas.getWidth(),h=canvas.getHeight(),d=Math.max(0.5f,density);
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    float left=backdrop?4*d:43*d,right=backdrop?4*d:10*d,top=backdrop?8*d:21*d,bottom=backdrop?8*d:33*d;
    float chartW=Math.max(1,w-left-right),chartH=Math.max(1,h-top-bottom);
    if(!backdrop){p.setColor(muted);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d);
      canvas.drawLine(left,top,left,top+chartH,p);canvas.drawLine(left,top+chartH,left+chartW,top+chartH,p);p.setStyle(Paint.Style.FILL);}
    if(metric==3&&tariff<=0){if(!backdrop){p.setColor(muted);p.setTextSize(12*d);canvas.drawText("--",left+8*d,top+chartH/2,p);}return;}
    if(bins==null||bins.isEmpty()){if(!backdrop){p.setColor(muted);p.setTextSize(11*d);canvas.drawText("--",left+8*d,top+chartH/2,p);}return;}
    double[] numbers=new double[bins.size()];double max=0;
    for(int i=0;i<bins.size();i++){
      double value=bins.get(i).amount(metric,perPercent,tariff);
      numbers[i]=Double.isFinite(value)&&value>=0?value:Double.NaN;
      if(Double.isFinite(numbers[i]))max=Math.max(max,numbers[i]);
    }
    if(max<=0){if(!backdrop){p.setColor(muted);p.setTextSize(11*d);canvas.drawText("0",left+8*d,top+chartH/2,p);}return;}
    float scale=(float)(max*1.14),step=chartW/numbers.length;
    if(chartStyle!=EnergyChartSeries.CURVE){
      p.setStyle(Paint.Style.FILL);p.setColor(ORANGE);p.setAlpha(backdrop?48:chartStyle==EnergyChartSeries.BOTH?128:225);
      for(int i=0;i<numbers.length;i++){
        if(!Double.isFinite(numbers[i]))continue;
        float size=(float)(numbers[i]/scale*chartH);float x=left+i*step+step*.16f;
        canvas.drawRoundRect(new RectF(x,top+chartH-size,x+step*.68f,top+chartH),Math.min(3*d,step*.12f),Math.min(3*d,step*.12f),p);
      }
    }
    if(chartStyle!=EnergyChartSeries.BARS){
      p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
      p.setColor(backdrop?ORANGE:LIGHT);p.setAlpha(backdrop?95:255);p.setStrokeWidth(backdrop?2.2f*d:2.5f*d);
      Path path=new Path();int start=-1;
      for(int i=0;i<numbers.length;i++){
        if(!Double.isFinite(numbers[i])){if(start>=0){canvas.drawPath(path,p);path.reset();start=-1;}continue;}
        float x=left+(i+.5f)*step,y=top+chartH-(float)(numbers[i]/scale*chartH);
        if(start<0){path.moveTo(x,y);start=i;}
        else{
          float px=left+(i-.5f)*step,py=top+chartH-(float)(numbers[i-1]/scale*chartH);
          float midpoint=(px+x)*.5f;
          path.cubicTo(midpoint,py,midpoint,y,x,y);
        }
      }
      if(start>=0)canvas.drawPath(path,p);
    }
    if(!backdrop){
      p.setStyle(Paint.Style.FILL);p.setAlpha(255);p.setTextSize(10*d);p.setColor(muted);
      canvas.drawText(String.format(Locale.getDefault(),"%.1f",max),3*d,top+7*d,p);
      int[] positions={0,bins.size()/2,bins.size()-1};
      for(int n:positions){String label=new SimpleDateFormat(grouping==EnergyChartSeries.MONTHLY?"yyyy-MM":"MM-dd",Locale.getDefault()).format(new Date(bins.get(n).date));
        float x=left+(n+.5f)*step-p.measureText(label)/2;
        canvas.drawText(label,Math.max(left,Math.min(left+chartW-p.measureText(label),x)),top+chartH+19*d,p);
      }
    }
  }
}
