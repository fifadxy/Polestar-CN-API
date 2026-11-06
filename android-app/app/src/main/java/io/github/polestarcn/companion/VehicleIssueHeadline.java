package io.github.polestarcn.companion;

import android.content.Context;

/**
 * A concise, source-backed issue title for the three main UI views.
 * Only explicit warnings/open states are named; unknown telemetry is never a fault.
 */
final class VehicleIssueHeadline {
  private VehicleIssueHeadline(){}

  static String headline(Context context,VehicleSnapshot snapshot,boolean includeTires) {
    if(!VehicleCondition.problem(snapshot,includeTires))
      return AppPreferences.tr(context,"请检查车辆","Check vehicle");
    String item=issueName(context,snapshot,includeTires);
    if(item.isEmpty())return AppPreferences.tr(context,"请检查车辆","Check vehicle");
    return AppPreferences.tr(context,"请检查 ","Check ")+item;
  }

  static String issueName(Context c,VehicleSnapshot s,boolean includeTires){
    if(s==null)return "";
    boolean en=AppPreferences.english(c);
    VehicleHealthSnapshot h=s.health;
    // Put safety-relevant warnings first; prioritization affects presentation only.
    if(h.lowVoltageBattery==VehicleHealthSnapshot.WARNING)
      return en?"12V battery":"12V 蓄电池";
    if(h.brakeFluid==VehicleHealthSnapshot.WARNING)
      return en?"brake fluid":"制动液";
    if((includeTires&&h.tirePressure==VehicleHealthSnapshot.WARNING)
        ||(includeTires&&s.tires.hasProblem()))
      return en?"tyre pressure":"胎压";
    if(h.washerFluid==VehicleHealthSnapshot.WARNING)
      return en?"washer fluid":"玻璃水";
    if(h.coolant==VehicleHealthSnapshot.WARNING)
      return en?"coolant":"冷却液";
    if(h.oil==VehicleHealthSnapshot.WARNING)
      return en?"oil system":"机油";
    if(h.exteriorLights==VehicleHealthSnapshot.WARNING)
      return en?"exterior lights":"车灯";
    if(h.maintenance==VehicleHealthSnapshot.WARNING||h.serviceOverdue())
      return en?"vehicle service":"保养";
    if("已打开".equals(s.tailgate)||"未关严".equals(s.tailgate))
      return en?"tailgate":"尾门";
    for(int i=0;i<4;i++){
      int door=s.exterior.state(false,i);
      if(door==1||door==3)return en?"doors":"车门";
      int glass=s.exterior.state(true,i);
      if(glass==1||glass==3)return en?"windows":"车窗";
    }
    if(open(s.doors))return en?"doors":"车门";
    if(open(s.windows))return en?"windows":"车窗";
    return "";
  }
  private static boolean open(String text){
    return text!=null&&(text.contains("个开启")||text.equals("已打开")||text.equals("未关严"));
  }
}
