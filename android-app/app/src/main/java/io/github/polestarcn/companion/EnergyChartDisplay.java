package io.github.polestarcn.companion;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Presentation-only chart helpers; never invents telemetry or treats missing bins as zero. */
final class EnergyChartDisplay {
  static final int LATEST=0, ALL=1, HIDDEN=2;
  private EnergyChartDisplay(){}
  static int mode(String value){return "all".equals(value)?ALL:"off".equals(value)?HIDDEN:LATEST;}
  static String preference(int mode){return mode==ALL?"all":mode==HIDDEN?"off":"latest";}
  static int next(int mode){return mode==LATEST?ALL:mode==ALL?HIDDEN:LATEST;}
  /** Repeated bar taps: selected -> all + reference -> hidden. */
  static int afterBarTap(int mode,boolean focused){
    if(mode==HIDDEN)return LATEST;
    if(mode==LATEST&&!focused)return LATEST;
    return mode==LATEST?ALL:HIDDEN;
  }

  static int[] annotated(double[] amounts,int mode,int focusedIndex){
    if(amounts==null||amounts.length==0||mode==HIDDEN)return new int[0];
    List<Integer> indices=new ArrayList<>();
    if(mode==ALL){
      for(int i=0;i<amounts.length;i++)if(valid(amounts[i]))indices.add(i);
    } else if(focusedIndex>=0&&focusedIndex<amounts.length&&valid(amounts[focusedIndex])){
      indices.add(focusedIndex);
    } else {
      for(int i=amounts.length-1;i>=0;i--)if(valid(amounts[i])){indices.add(i);break;}
    }
    int[] out=new int[indices.size()];for(int i=0;i<out.length;i++)out[i]=indices.get(i);
    return out;
  }
  static boolean valid(double v){return Double.isFinite(v)&&v>=0;}
  /** Four equal, human-readable Y-axis intervals, always enclosing the highest valid value. */
  static double tickStep(double maximum){
    if(!Double.isFinite(maximum)||maximum<=0)return 0.25d;
    double raw=maximum/4d;
    double magnitude=Math.pow(10d,Math.floor(Math.log10(raw)));
    for(double v:new double[]{1d,2d,2.5d,5d,10d})if(v*magnitude>=raw-1e-12)return v*magnitude;
    return 10d*magnitude;
  }
  static int axisDigits(double tickStep){
    for(int digits=0;digits<=6;digits++){
      double scaled=tickStep*Math.pow(10d,digits);
      if(Math.abs(scaled-Math.rint(scaled))<1e-7)return digits;
    }
    return 6;
  }
  static String tick(double value,double step,Locale locale){
    return String.format(locale,"%."+axisDigits(step)+"f",value);
  }
  static String value(int metric,double amount,Locale locale){
    if(!valid(amount))return "--";
    String digits=amount>=1000?"%.0f":amount>=100?"%.1f":amount>=.01?"%.2f":amount>=.001?"%.3f":"%.4f";
    String number=String.format(locale,digits,amount);
    if(metric==2)return number+" km";
    if(metric==3)return "\u00a5"+number;
    return number+" kWh";
  }
  static String unit(int metric){return metric==2?"km":metric==3?"\u00a5":"kWh";}
}
