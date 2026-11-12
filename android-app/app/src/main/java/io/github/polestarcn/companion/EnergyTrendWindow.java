package io.github.polestarcn.companion;
import java.util.Calendar;
/** Comparable adjacent observation windows (no fabricated prior history). */
final class EnergyTrendWindow {
  private EnergyTrendWindow(){}
  static final class Window {
    final long previousStart,start,end;
    Window(long previousStart,long start,long end){
      this.previousStart=previousStart;this.start=start;this.end=end;
    }
  }
  static Window period(long now,int range,String mode){
    if(now<=0)throw new IllegalArgumentException("Invalid current time");
    Calendar c=Calendar.getInstance();c.setTimeInMillis(now);
    if("rolling7".equals(mode)||"rolling30".equals(mode)){
      int days="rolling7".equals(mode)?7:30;
      c.add(Calendar.DAY_OF_YEAR,-days);long current=c.getTimeInMillis();
      c.add(Calendar.DAY_OF_YEAR,-days);
      return new Window(c.getTimeInMillis(),current,now);
    }
    // For full history, compare recent month to previous month; the previous
    // full retention interval does not exist within this database.
    int months=range==0?1:range==1?3:range==2?6:1;
    c.add(Calendar.MONTH,-months);long current=c.getTimeInMillis();
    c.add(Calendar.MONTH,-months);
    return new Window(c.getTimeInMillis(),current,now);
  }
}
