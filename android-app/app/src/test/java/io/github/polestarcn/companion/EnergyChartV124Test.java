package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import java.util.Calendar;
import java.util.List;
import java.util.ArrayList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Pure time bucketing and trend comparator: no fabricated driving or energy points. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class EnergyChartV124Test {
  private static EnergyLog.Point point(int year,int month,int day,double kwh,double km){
    EnergyLog.Point p=new EnergyLog.Point();
    Calendar date=Calendar.getInstance();date.clear();date.set(year,month-1,day,12,0,0);
    p.t=date.getTimeInMillis();p.count=1;p.drive=kwh;p.distanceKm=km;return p;
  }
  @Test public void aggregationSupportsDailyWeeklyMonthlyAndGaps(){
    List<EnergyLog.Point> points=new ArrayList<>();
    points.add(point(2026,10,1,2,11));points.add(point(2026,10,3,3,15));
    points.add(point(2026,11,2,4,20));
    List<EnergyChartSeries.Bin> daily=EnergyChartSeries.aggregate(points,EnergyChartSeries.DAILY);
    assertTrue(daily.size()>=30);
    assertTrue(Double.isNaN(daily.get(1).amount(0,.71,1))); // Missing October 2 is a gap, not zero
    List<EnergyChartSeries.Bin> weekly=EnergyChartSeries.aggregate(points,EnergyChartSeries.WEEKLY);
    assertTrue(weekly.size()>=4);
    List<EnergyChartSeries.Bin> monthly=EnergyChartSeries.aggregate(points,EnergyChartSeries.MONTHLY);
    assertEquals(2,monthly.size());
    assertEquals(5d,monthly.get(0).amount(0,.71,1),.001);
    assertEquals(26d,monthly.get(0).amount(2,.71,1),.001);
    assertEquals(4d,monthly.get(1).amount(0,.71,1),.001);
  }
  @Test public void groupingBoundsFullRetentionToReadableBins(){
    assertEquals(EnergyChartSeries.MONTHLY,EnergyChartSeries.effectiveGranularity(0,730));
    assertEquals(EnergyChartSeries.WEEKLY,EnergyChartSeries.effectiveGranularity(0,90));
    assertEquals(EnergyChartSeries.DAILY,EnergyChartSeries.effectiveGranularity(0,31));
    assertEquals(EnergyChartSeries.MONTHLY,EnergyChartSeries.effectiveGranularity(2,7));
  }
  @Test public void comparisonUsesSelectedMetricAndValidBaseline(){
    EnergyLog.Report previous=new EnergyLog.Report();previous.count=3;previous.drive=2;previous.ac=1;previous.distanceKm=10;
    EnergyLog.Report current=new EnergyLog.Report();current.count=3;current.drive=3;current.ac=2;current.distanceKm=14;
    assertEquals(50.0,EnergyChartSeries.changeMetric(current,previous,0,.71,1),.001);
    assertEquals(100.0,EnergyChartSeries.changeMetric(current,previous,1,.71,1),.001);
    assertEquals(40.0,EnergyChartSeries.changeMetric(current,previous,2,.71,1),.001);
    assertEquals((3+2*.71-2-1*.71)/(2+1*.71)*100,EnergyChartSeries.changeMetric(current,previous,3,.71,1),.001);
    assertNull(EnergyChartSeries.changeMetric(current,previous,3,.71,0));
    previous.distanceKm=0;assertNull(EnergyChartSeries.changeMetric(current,previous,2,.71,1));
  }
}
