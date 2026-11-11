package io.github.polestarcn.companion;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Chart-only bucketing of observed energy history; no fabricated points. */
final class EnergyChartSeries {
  static final int DAILY=0,WEEKLY=1,MONTHLY=2;
  static final int BARS=0,CURVE=1,BOTH=2;
  static final class Bin {
    final long date;
    int count;
    double drive,airPercent,distance;
    Bin(long date){this.date=date;}
    boolean observed(){return count>0;}
    double amount(int metric,double perPercent,double tariff){
      if(!observed())return Double.NaN; // A missing day/week/month is a gap, not zero consumption.
      double air=airPercent*perPercent;
      if(metric==0)return drive;
      if(metric==1)return air;
      if(metric==2)return distance;
      return tariff>0?(drive+air)*tariff:Double.NaN;
    }
    double energy(double perPercent){return drive+airPercent*perPercent;}
  }
  private EnergyChartSeries(){}
  static int granularity(String value){return "week".equals(value)?WEEKLY:"month".equals(value)?MONTHLY:DAILY;}
  static int style(String value){return "curve".equals(value)?CURVE:"both".equals(value)?BOTH:BARS;}
  /** Bound on-screen bins to ~31 slots for legible ALL-value labels and GPU safety.
   * Full retained history is monthly, medium custom ranges are weekly. */
  static int effectiveGranularity(int requested,long days){
    int minimum=days>210?MONTHLY:days>31?WEEKLY:DAILY;
    return Math.max(minimum,Math.max(DAILY,Math.min(MONTHLY,requested)));
  }
  static long key(long timestamp,int granularity){
    Calendar c=Calendar.getInstance();c.setTimeInMillis(timestamp);
    c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);
    if(granularity==WEEKLY){int offset=(c.get(Calendar.DAY_OF_WEEK)+5)%7;c.add(Calendar.DAY_OF_YEAR,-offset);}
    else if(granularity==MONTHLY)c.set(Calendar.DAY_OF_MONTH,1);
    return c.getTimeInMillis();
  }
  static long nextKey(long time,int step){
    Calendar date=Calendar.getInstance();date.setTimeInMillis(time);
    if(step==WEEKLY)date.add(Calendar.DAY_OF_YEAR,7);
    else if(step==MONTHLY)date.add(Calendar.MONTH,1);
    else date.add(Calendar.DAY_OF_YEAR,1);
    return date.getTimeInMillis();
  }
  static List<Bin> aggregate(List<EnergyLog.Point> source,int granularity){
    TreeMap<Long,Bin> sorted=new TreeMap<>();
    if(source==null)return new ArrayList<>();
    for(EnergyLog.Point p:source){
      if(p==null||p.count<=0)continue;
      long k=key(p.t,granularity);Bin bin=sorted.get(k);
      if(bin==null){bin=new Bin(k);sorted.put(k,bin);}
      bin.count+=p.count;
      if(Double.isFinite(p.drive)&&p.drive>=0)bin.drive+=p.drive;
      if(Double.isFinite(p.ac)&&p.ac>=0)bin.airPercent+=p.ac;
      if(Double.isFinite(p.distanceKm)&&p.distanceKm>=0)bin.distance+=p.distanceKm;
    }
    if(sorted.isEmpty())return new ArrayList<>();
    // Explicit null/empty gaps prevent a misleading smooth curve across missing observations.
    List<Bin> result=new ArrayList<>();long date=sorted.keySet().iterator().next();long until=sorted.lastKey();
    for(int n=0;n<1000&&date<=until;n++){
      Bin bin=sorted.get(date);result.add(bin==null?new Bin(date):bin);
      long next=nextKey(date,granularity);if(next<=date)break;date=next;
    }
    return result;
  }
  static double totalEnergy(EnergyLog.Report report,double perPercent){
    if(report==null||report.count==0)return Double.NaN;
    return Math.max(0,report.drive)+Math.max(0,report.ac)*perPercent;
  }
  static Double changeMetric(EnergyLog.Report latest,EnergyLog.Report prior,int metric,double perPercent,double tariff){
    if(latest==null||prior==null||latest.count<2||prior.count<2)return null;
    double recent=metricValue(latest,metric,perPercent,tariff);
    double before=metricValue(prior,metric,perPercent,tariff);
    if(!Double.isFinite(recent)||!Double.isFinite(before)||before<=0)return null;
    return (recent-before)/before*100d;
  }
  private static double metricValue(EnergyLog.Report report,int metric,double perPercent,double tariff){
    if(metric==0)return report.drive;
    if(metric==1)return report.ac*perPercent;
    if(metric==2)return report.distanceKm;
    return tariff>0?(report.drive+report.ac*perPercent)*tariff:Double.NaN;
  }
  /** Percent change from the immediately preceding equally long period; undefined for zero or missing baseline. */
  static Double change(EnergyLog.Report latest,EnergyLog.Report prior,double perPercent){
    if(latest==null||prior==null||latest.count<2||prior.count<2)return null;
    double a=totalEnergy(latest,perPercent),b=totalEnergy(prior,perPercent);
    if(!Double.isFinite(a)||!Double.isFinite(b)||b<=0)return null;
    return (a-b)/b*100d;
  }
}
