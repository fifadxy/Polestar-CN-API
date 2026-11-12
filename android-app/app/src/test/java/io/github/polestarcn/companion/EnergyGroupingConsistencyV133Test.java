package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.ContentValues;
import android.content.Context;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.List;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class EnergyGroupingConsistencyV133Test {
 private Context c;private EnergyLog log;
 @Before public void reset()throws Exception{
  c=RuntimeEnvironment.getApplication();
  Field f=EnergyLog.class.getDeclaredField("instance");f.setAccessible(true);
  EnergyLog old=(EnergyLog)f.get(null);if(old!=null)old.close();f.set(null,null);
  c.deleteDatabase("energy_history.db");c.getSharedPreferences("preferences",0).edit().clear().commit();
  log=EnergyLog.get(c);
 }
 private void sample(int daysAgo,double drive,double climate){
  Calendar date=Calendar.getInstance();date.add(Calendar.DAY_OF_YEAR,-daysAgo);
  ContentValues v=new ContentValues();v.put("vehicle","a");v.put("t",date.getTimeInMillis());
  v.put("soc",60);v.put("tm",1000.-daysAgo);v.put("cons",20);v.put("speed",25);
  v.put("odo",1000.-daysAgo);v.put("climate",0);v.put("charge",0);v.put("acvalid",0);
  v.put("ac",climate);v.put("drive",drive);log.getWritableDatabase().insertOrThrow("samples",null,v);
 }
 private double total(List<EnergyChartSeries.Bin> bins,int metric){
  double sum=0;for(EnergyChartSeries.Bin bin:bins)if(bin.observed()){
   double v=bin.amount(metric,.71,1.4);if(Double.isFinite(v))sum+=v;
  }return sum;
 }
 @Test public void allHistoryTotalsMatchDayWeekAndMonth(){
  sample(2,1.5,1);sample(8,2,2);sample(46,3,0);
  sample(104,4,3);sample(207,5,1);sample(399,6,2);
  EnergyLog.Report all=log.readRetention("a");
  assertEquals(6,all.count);assertEquals(21.5,all.drive,.001);
  assertEquals(9,all.ac,.001);assertEquals(6,all.points.size());
  for(int step=0;step<3;step++){
   List<EnergyChartSeries.Bin> bins=EnergyChartSeries.aggregate(all.points,step);
   assertEquals(all.drive,total(bins,0),.00001);
   assertEquals(all.ac*.71,total(bins,1),.00001);
   assertEquals((all.drive+all.ac*.71)*1.4,total(bins,3),.00001);
   assertEquals(all.distanceKm,total(bins,2),.00001);
  }
  assertEquals(2,log.readRecentMonths("a",1).count);
  assertEquals(3,log.readRecentMonths("a",3).count);
  assertEquals(4,log.readRecentMonths("a",6).count);
 }
 @Test public void longTrendWindowsDoNotPreAggregateToMonths(){
  sample(2,1,0);sample(68,2,0);sample(240,3,0);
  Calendar from=Calendar.getInstance();from.add(Calendar.MONTH,-9);
  EnergyLog.Report r=log.readBetween("a",from.getTimeInMillis(),System.currentTimeMillis());
  assertEquals(3,r.count);assertEquals(3,r.points.size());
 }
}
