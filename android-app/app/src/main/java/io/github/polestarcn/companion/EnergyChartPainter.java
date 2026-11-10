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

/** VI chart renderer. Unobserved periods stay gaps; axes and callouts are display-only. */
final class EnergyChartPainter {
  private EnergyChartPainter(){}
  private static final int ORANGE=PsaDesign.ORANGE, LIGHT=Color.rgb(255,163,85);
  static float plotLeft(float density){return 59f*Math.max(.5f,density);}
  static float plotRight(float density){return 13f*Math.max(.5f,density);}

  /** Background visualizations keep their original lightweight, annotation-free mode. */
  static void draw(Canvas canvas,List<EnergyChartSeries.Bin> bins,int metric,int chartStyle,
      double perPercent,double tariff,int ink,int muted,int grouping,boolean backdrop,float density){
    draw(canvas,bins,metric,chartStyle,perPercent,tariff,ink,muted,grouping,backdrop,density,
        backdrop?EnergyChartDisplay.HIDDEN:EnergyChartDisplay.LATEST,-1);
  }

  static void draw(Canvas canvas,List<EnergyChartSeries.Bin> bins,int metric,int chartStyle,
      double perPercent,double tariff,int ink,int muted,int grouping,boolean backdrop,float density,
      int labelMode,int focusedBar){
    float w=canvas.getWidth(),h=canvas.getHeight(),d=Math.max(.5f,density);
    float left=backdrop?4*d:plotLeft(d),right=backdrop?4*d:plotRight(d);
    float top=backdrop?8*d:35*d,bottom=backdrop?8*d:39*d;
    float chartW=Math.max(1,w-left-right),chartH=Math.max(1,h-top-bottom);
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    if(metric==3&&tariff<=0){if(!backdrop)placeholder(canvas,p,muted,left,top,chartH,d);return;}
    if(bins==null||bins.isEmpty()){if(!backdrop)placeholder(canvas,p,muted,left,top,chartH,d);return;}
    double[] values=new double[bins.size()];double max=0;
    for(int i=0;i<bins.size();i++){
      double raw=bins.get(i).amount(metric,perPercent,tariff);
      values[i]=EnergyChartDisplay.valid(raw)?raw:Double.NaN;
      if(EnergyChartDisplay.valid(values[i]))max=Math.max(max,values[i]);
    }
    // Preserve the distinction between zero consumption and no observations.
    if(EnergyChartDisplay.annotated(values,EnergyChartDisplay.ALL,-1).length==0){
      if(!backdrop)placeholder(canvas,p,muted,left,top,chartH,d);return;
    }
    double unit=EnergyChartDisplay.tickStep(max),axisMax=unit*4d;
    if(!backdrop)drawAxes(canvas,p,bins,grouping,metric,muted,d,left,top,chartW,chartH,unit);
    float cell=chartW/values.length;
    if(chartStyle!=EnergyChartSeries.CURVE){
      p.setStyle(Paint.Style.FILL);p.setColor(ORANGE);
      p.setAlpha(backdrop?48:chartStyle==EnergyChartSeries.BOTH?128:225);
      for(int i=0;i<values.length;i++){
        if(!EnergyChartDisplay.valid(values[i]))continue;
        float size=(float)(values[i]/axisMax*chartH),x=left+i*cell+cell*.15f;
        canvas.drawRoundRect(new RectF(x,top+chartH-size,x+cell*.7f,top+chartH),
            Math.min(3*d,cell*.12f),Math.min(3*d,cell*.12f),p);
      }
    }
    if(chartStyle!=EnergyChartSeries.BARS){
      p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
      p.setColor(backdrop?ORANGE:LIGHT);p.setAlpha(backdrop?95:255);
      p.setStrokeWidth(backdrop?2.2f*d:2.2f*d);
      Path path=new Path();int last=-1;
      for(int i=0;i<values.length;i++){
        if(!EnergyChartDisplay.valid(values[i])){if(last>=0){canvas.drawPath(path,p);path.reset();last=-1;}continue;}
        float x=left+(i+.5f)*cell,y=top+chartH-(float)(values[i]/axisMax*chartH);
        if(last<0){path.moveTo(x,y);last=i;}else{
          float px=left+(i-.5f)*cell,py=top+chartH-(float)(values[i-1]/axisMax*chartH);
          float midpoint=(px+x)*.5f;path.cubicTo(midpoint,py,midpoint,y,x,y);
        }
      }
      if(last>=0)canvas.drawPath(path,p);
    }
    if(!backdrop&&labelMode!=EnergyChartDisplay.HIDDEN){
      // Each label is a real value at its own bucket. A short VI leader makes it readable.
      int[] marked=EnergyChartDisplay.annotated(values,labelMode,focusedBar);
      p.setStyle(Paint.Style.FILL);p.setColor(ORANGE);p.setAlpha(255);
      p.setTextSize(9*d);p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
      for(int index:marked){
        float x=left+(index+.5f)*cell;
        float topOfBar=top+chartH-(float)(values[index]/axisMax*chartH);
        float textY=Math.max(11*d,topOfBar-17*d);
        float tipY=Math.max(textY+4*d,topOfBar-2*d);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.1f*d);
        canvas.drawLine(x,tipY,x,topOfBar,p);
        p.setStyle(Paint.Style.FILL);canvas.drawCircle(x,topOfBar,1.8f*d,p);
        String value=EnergyChartDisplay.value(metric,values[index],Locale.getDefault());
        float width=p.measureText(value);
        float labelX=Math.max(left,Math.min(left+chartW-width,x-width*.5f));
        canvas.drawText(value,labelX,textY,p);
      }
      p.setTypeface(null);
    }
  }

  private static void placeholder(Canvas c,Paint p,int muted,float left,float top,float height,float d){
    p.setStyle(Paint.Style.FILL);p.setColor(muted);p.setTextSize(12*d);
    c.drawText("--",left+8*d,top+height*.5f,p);
  }

  /** Horizontal 0/25/50/75/100% grid and labelled ticks, plus readable date buckets. */
  private static void drawAxes(Canvas canvas,Paint p,List<EnergyChartSeries.Bin> bins,
      int grouping,int metric,int muted,float d,float left,float top,float chartW,float chartH,double interval){
    p.setStyle(Paint.Style.FILL);p.setColor(muted);p.setTextSize(9*d);
    canvas.drawText(EnergyChartDisplay.unit(metric),4*d,16*d,p);
    for(int n=0;n<=4;n++){
      float y=top+chartH-n*chartH/4f;
      p.setColor(muted);p.setAlpha(n==0?150:55);p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(n==0?1f*d:.65f*d);
      canvas.drawLine(left,y,left+chartW,y,p);
      p.setStyle(Paint.Style.FILL);p.setAlpha(255);p.setTextAlign(Paint.Align.RIGHT);
      p.setTextSize(9*d);
      canvas.drawText(EnergyChartDisplay.tick(interval*n,interval,Locale.getDefault()),left-6*d,y+3*d,p);
    }
    p.setTextAlign(Paint.Align.LEFT);
    p.setStyle(Paint.Style.STROKE);p.setColor(muted);p.setAlpha(130);p.setStrokeWidth(.7f*d);
    canvas.drawLine(left,top,left,top+chartH,p);
    p.setStyle(Paint.Style.FILL);p.setAlpha(255);p.setTextSize(9*d);
    // Labels are sampled from actual bins. Throttle density to keep adjacent labels apart.
    int count=bins.size();
    int maxLabels=Math.max(2,Math.min(9,(int)(chartW/(54*d))));
    int stride=Math.max(1,(int)Math.ceil(Math.max(0,count-1)/(double)(maxLabels-1)));
    float previousRight=-1000f;
    for(int i=0;i<count;i++){
      if(i!=count-1&&i%stride!=0)continue;
      String format=grouping==EnergyChartSeries.MONTHLY?"yy/MM":"MM/dd";
      String label=new SimpleDateFormat(format,Locale.getDefault()).format(new Date(bins.get(i).date));
      float cell=chartW/count,x=left+(i+.5f)*cell;
      float width=p.measureText(label),labelX=Math.max(left,Math.min(left+chartW-width,x-width*.5f));
      // Give the latest interval priority over cramped intermediate labels.
      if(labelX<=previousRight+5*d&&i!=count-1)continue;
      canvas.drawText(label,labelX,top+chartH+20*d,p);
      previousRight=labelX+width;
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.6f*d);p.setAlpha(50);
      canvas.drawLine(x,top,x,top+chartH,p);
      p.setStyle(Paint.Style.FILL);p.setAlpha(255);
    }
  }
}
