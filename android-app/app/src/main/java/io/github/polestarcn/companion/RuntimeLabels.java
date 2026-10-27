package io.github.polestarcn.companion;

import android.content.Context;

/** Presentation-only remaining times. A zero counter never proves a vehicle has stopped. */
public final class RuntimeLabels {
  private RuntimeLabels() {}
  private static String value(Context c,long millis,boolean estimated) {
    long minutes=Math.max(0,(millis+59_999L)/60_000L);
    boolean english=AppPreferences.english(c);
    if(minutes==0)return english?"Refresh to verify":"请刷新确认";
    String amount=minutes>=60?(minutes/60)+(english?" h ":"小时")+(minutes%60)+(english?" m":"分"):
        minutes+(english?" min":"分钟");
    return (estimated?"~":"")+(english?"Left ":"剩余")+amount;
  }
  public static String camp(Context c) {
    if(!CampModeManager.active(c))return "";
    CampSession s=CampModeManager.session(c);
    return s==null?"":value(c,s.endsAt-System.currentTimeMillis(),false);
  }
  public static String air(Context c,VehicleSnapshot s) {
    if(s==null||!"开启".equals(s.air))return "";
    Integer minutes=s.environment.airRuntimeMinutes;
    if(minutes==null)return AppPreferences.tr(c,"剩余 --","Left --");
    long elapsed=s.environment.airReportedAt>0?Math.max(0,System.currentTimeMillis()-s.environment.airReportedAt):0;
    return value(c,minutes*60_000L-elapsed,elapsed>60_000L);
  }
  public static String climate(Context c,VehicleSnapshot s,String id) {
    if(s==null||!"开启".equals(s.climate))return "";
    String label=ClimateRuntime.shortLabel(c,s,id,AppPreferences.english(c));
    if(label.equals("运行中")||label.equals("Running"))
      return AppPreferences.tr(c,"剩余时间暂无","Remaining unavailable");
    return label;
  }
}
