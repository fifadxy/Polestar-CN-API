package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import java.util.Calendar;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class EnergyTrendWindowV133Test {
 @Test public void autoTrendFollowsSelectedMonthRangeAndAllUsesLastMonth(){
  Calendar date=Calendar.getInstance();date.clear();date.set(2026,10,12,12,0,0);
  for(int range=0;range<4;range++){
   EnergyTrendWindow.Window w=EnergyTrendWindow.period(date.getTimeInMillis(),range,"auto");
   int months=range==0?1:range==1?3:range==2?6:1;
   Calendar expected=Calendar.getInstance();expected.setTimeInMillis(date.getTimeInMillis());
   expected.add(Calendar.MONTH,-months);assertEquals(expected.getTimeInMillis(),w.start);
   expected.add(Calendar.MONTH,-months);assertEquals(expected.getTimeInMillis(),w.previousStart);
   assertEquals(date.getTimeInMillis(),w.end);
   assertTrue(w.previousStart<w.start&&w.start<w.end);
  }
 }
 @Test public void manualSevenAndThirtyDayWindowsAreAdjacent(){
  long now=System.currentTimeMillis();
  for(String mode:new String[]{"rolling7","rolling30"}){
   EnergyTrendWindow.Window w=EnergyTrendWindow.period(now,3,mode);
   assertEquals(now,w.end);assertTrue(w.previousStart<w.start);
   assertTrue(now-w.start>=6L*86400000L);
  }
 }
}
