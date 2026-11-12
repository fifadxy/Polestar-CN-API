package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.view.View;
import android.widget.LinearLayout;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.lang.reflect.Field;
import java.util.Calendar;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public final class EnergyHistoryRangeV124Test {
  private Context c;private EnergyLog log;
  @Before public void before()throws Exception{
    c=RuntimeEnvironment.getApplication();
    Field field=EnergyLog.class.getDeclaredField("instance");field.setAccessible(true);
    EnergyLog previous=(EnergyLog)field.get(null);if(previous!=null)previous.close();field.set(null,null);
    c.deleteDatabase("energy_history.db");c.getSharedPreferences("preferences",0).edit().clear().commit();log=EnergyLog.get(c);
  }
  private void sample(int daysAgo){
    Calendar date=Calendar.getInstance();date.add(Calendar.DAY_OF_YEAR,-daysAgo);
    SQLiteDatabase db=log.getWritableDatabase();
    db.execSQL("INSERT INTO samples(vehicle,t,soc,tm,cons,speed,odo,climate,charge,ac,drive,acvalid) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
        new Object[]{"a",date.getTimeInMillis(),70.,(double)daysAgo,20.,0.,(double)(1000-daysAgo),0,0,0.0,1.0,0});
  }
  @Test public void rollingAndCustomWindowsSelectOnlyRealStoredSamples(){
    sample(2);sample(6);sample(20);sample(36);
    assertEquals(1,log.readRecentDays("a",3).count);
    assertEquals(2,log.readRecentDays("a",7).count);
    assertEquals(3,log.readRecentDays("a",31).count);
    assertEquals(2,log.read("a",0).count); // legacy range still seven days
    assertEquals(3,log.read("a",1).count); // legacy range still 31 days
    Calendar start=Calendar.getInstance();start.add(Calendar.DAY_OF_YEAR,-10);
    assertEquals(2,log.readBetween("a",start.getTimeInMillis(),System.currentTimeMillis()).count);
    assertEquals(0,log.readBetween("a",System.currentTimeMillis()+86400000,System.currentTimeMillis()).count);
  }
  @Test public void calendarDateInputIsStrictAndNewRangeTabsAreVisible(){
    Calendar date=Calendar.getInstance();date.setTimeInMillis(EnergySummaryPanel.startOfDate(2026,11,10));
    assertEquals(2026,date.get(Calendar.YEAR));assertEquals(11,date.get(Calendar.MONTH)+1);assertEquals(10,date.get(Calendar.DAY_OF_MONTH));
    try{EnergySummaryPanel.startOfDate(2026,2,30);fail("Invalid February date accepted");}catch(IllegalArgumentException expected){}
    EnergySummaryPanel panel=new EnergySummaryPanel(c,null,null);
    LinearLayout ranges=(LinearLayout)panel.findViewWithTag("energy_range_periods");
    assertNotNull(ranges);assertEquals(4,ranges.getChildCount());
    assertFalse(ranges.getChildAt(0).isSelected());assertTrue(ranges.getChildAt(3).isSelected());
    assertEquals("1个月",((android.widget.TextView)ranges.getChildAt(0)).getText());
    assertEquals("3个月",((android.widget.TextView)ranges.getChildAt(1)).getText());
    assertEquals("半年",((android.widget.TextView)ranges.getChildAt(2)).getText());
    assertEquals("全部",((android.widget.TextView)ranges.getChildAt(3)).getText());
    for(int i:new int[]{0,1,2,3}){
      assertTrue(ranges.getChildAt(i).performClick());
      assertTrue(ranges.getChildAt(i).isSelected());
    }
  }
}
