package io.github.polestarcn.companion;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
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
  // Geometry is testable without Canvas. Rectangles sharing only an edge
  // do not count as occluding each other.
  static boolean intersects(float ax0,float ay0,float ax1,float ay1,
      float bx0,float by0,float bx1,float by1){
    return ax0<bx1&&ax1>bx0&&ay0<by1&&ay1>by0;
  }
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
    if(!backdrop&&labelMode==EnergyChartDisplay.ALL&&focusedBar>=0
        &&focusedBar<values.length&&EnergyChartDisplay.valid(values[focusedBar]))
      drawReference(canvas,p,left,top,chartW,chartH,cell,focusedBar,
          values[focusedBar],axisMax,d);
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
      // LATEST displays the last truly observed bucket. ALL keeps a label per
      // observed bar and reserves enough horizontal space in Trend.resize().
      int[] marked=EnergyChartDisplay.annotated(values,labelMode,focusedBar);
      p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
      p.setTextSize(9*d);
      RectF previous=null;
      for(int index:marked){
        float x=left+(index+.5f)*cell;
        float topOfBar=top+chartH-(float)(values[index]/axisMax*chartH);
        String value=EnergyChartDisplay.value(metric,values[index],Locale.getDefault());
        float width=p.measureText(value);
        float labelX=Math.max(left,Math.min(left+chartW-width,x-width*.5f));
        float baseline=Math.max(12*d,topOfBar-12*d);
        RectF label=new RectF(labelX-2*d,baseline-10*d,labelX+width+2*d,baseline+3*d);
        if(previous!=null&&RectF.intersects(label,previous)){
          // Fallback lanes live above the plot; no bar or prior label hides them.
          baseline=(index%2==0?13f:28f)*d;
          label.set(labelX-2*d,baseline-10*d,labelX+width+2*d,baseline+3*d);
        }
        boolean barOverlap=false;
        if(chartStyle!=EnergyChartSeries.CURVE){
          for(int j=0;j<values.length;j++){
            if(!EnergyChartDisplay.valid(values[j])||values[j]<=0)continue;
            float hBar=(float)(values[j]/axisMax*chartH),barX=left+(j+.15f)*cell;
            if(intersects(label.left,label.top,label.right,label.bottom,
                barX,top+chartH-hBar,barX+cell*.7f,top+chartH)){barOverlap=true;break;}
          }
        }
        // A dark opaque pill + white text is readable above *any* VI orange
        // bar. Non-overlapping labels use the specified VI orange.
        p.setStyle(Paint.Style.STROKE);p.setColor(ORANGE);p.setAlpha(255);p.setStrokeWidth(1.1f*d);
        if(baseline+3*d<topOfBar-2*d)canvas.drawLine(x,baseline+3*d,x,topOfBar-2*d,p);
        p.setStyle(Paint.Style.FILL);
        if(barOverlap){
          p.setColor(Color.rgb(32,35,39));
          canvas.drawRoundRect(label,2*d,2*d,p);
          p.setColor(Color.WHITE);
        }else p.setColor(ORANGE);
        canvas.drawText(value,labelX,baseline,p);
        previous=label;
      }
      p.setTypeface(null);
    }
  }

  private static void placeholder(Canvas c,Paint p,int muted,float left,float top,float height,float d){
    p.setStyle(Paint.Style.FILL);p.setColor(muted);p.setTextSize(12*d);
    c.drawText("--",left+8*d,top+height*.5f,p);
  }

  /** The moving plot clips at the pinned axis gutter. Transparent chart and
   * scroller surfaces let the secondary dialog glass show through unchanged. */
  static void drawScrollableWithFloatingAxis(Canvas canvas,List<EnergyChartSeries.Bin> bins,
      int metric,int chartStyle,double perPercent,double tariff,int ink,int muted,
      int grouping,float density,int labelMode,int focusedBar,int scrollX,int viewportWidth){
    float d=Math.max(.5f,density);
    int scroll=Math.max(0,scrollX);
    int width=viewportWidth>0?viewportWidth:canvas.getWidth();
    canvas.save();
    canvas.clipRect(scroll+plotLeft(d),0,scroll+width,canvas.getHeight());
    draw(canvas,bins,metric,chartStyle,perPercent,tariff,ink,muted,grouping,false,
        density,labelMode,focusedBar);
    canvas.restore();
    drawFloatingYAxis(canvas,bins,metric,perPercent,tariff,muted,density,scroll);
  }
  static void drawFloatingYAxis(Canvas canvas,List<EnergyChartSeries.Bin> bins,int metric,
      double perPercent,double tariff,int muted,float density,int scrollX){
    float d=Math.max(.5f,density),left=plotLeft(d),top=35*d,bottom=39*d;
    float chartH=Math.max(1f,canvas.getHeight()-top-bottom);
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    canvas.save();canvas.translate(scrollX,0);
    // No opaque rectangle: the dialog's own translucent gradient is visible.
    if(bins==null||bins.isEmpty()||(metric==3&&tariff<=0)){canvas.restore();return;}
    double max=0;boolean observed=false;
    for(EnergyChartSeries.Bin bin:bins){
      if(bin==null)continue;
      double value=bin.amount(metric,perPercent,tariff);
      if(EnergyChartDisplay.valid(value)){observed=true;max=Math.max(max,value);}
    }
    if(observed){
      double interval=EnergyChartDisplay.tickStep(max);
      p.setColor(muted);p.setAlpha(255);p.setTextSize(9*d);
      p.setTextAlign(Paint.Align.LEFT);
      canvas.drawText(EnergyChartDisplay.unit(metric),4*d,16*d,p);
      p.setTextAlign(Paint.Align.RIGHT);
      for(int n=0;n<=4;n++){
        float y=top+chartH-n*chartH/4f;
        canvas.drawText(EnergyChartDisplay.tick(interval*n,interval,Locale.getDefault()),left-6*d,y+3*d,p);
      }
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.7f*d);p.setAlpha(130);
      canvas.drawLine(left,top,left,top+chartH,p);
    }
    canvas.restore();
  }
  /** Crosshair guide through the selected bucket, behind the numeric labels. */
  private static void drawReference(Canvas canvas,Paint p,float left,float top,float width,
      float height,float cell,int index,double value,double max,float d){
    float x=left+(index+.5f)*cell,y=top+height-(float)(value/max*height);
    p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.1f*d);
    p.setColor(ORANGE);p.setAlpha(165);
    p.setPathEffect(new DashPathEffect(new float[]{4*d,3*d},0));
    canvas.drawLine(x,top,x,top+height,p);
    canvas.drawLine(left,y,left+width,y,p);
    p.setPathEffect(null);p.setStyle(Paint.Style.FILL);p.setAlpha(255);
    canvas.drawCircle(x,y,2.7f*d,p);
  }

  /** Scrolling horizontal grid and date labels; vertical ticks use the pinned gutter. */
  private static void drawAxes(Canvas canvas,Paint p,List<EnergyChartSeries.Bin> bins,
      int grouping,int metric,int muted,float d,float left,float top,float chartW,float chartH,double interval){
    p.setColor(muted);
    for(int n=0;n<=4;n++){
      float y=top+chartH-n*chartH/4f;
      p.setAlpha(n==0?150:55);p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(n==0?1f*d:.65f*d);
      canvas.drawLine(left,y,left+chartW,y,p);
    }
    p.setStyle(Paint.Style.FILL);p.setColor(muted);p.setAlpha(255);p.setTextSize(9*d);
    p.setTextAlign(Paint.Align.LEFT);
    // Labels are sampled from actual bins. Throttle density to keep adjacent labels apart.
    int count=bins.size();
    // Sample dates by physical spacing, not a fixed total count: long daily
    // history remains identifiable as the user pans across the chart.
    int stride=Math.max(1,(int)Math.ceil(68*d/Math.max(1f,chartW/count)));
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
