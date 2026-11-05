package io.github.polestarcn.companion;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

/** Only confirmed warning enum values are translated; never invent unverified tyre pressure. */
public final class OppoVehicleIssues {
  private OppoVehicleIssues(){}
  public static List<String> collect(Context c,VehicleSnapshot s){
    List<String> issues=new ArrayList<>();if(s==null||s.isDemo)return issues;
    boolean en=AppPreferences.english(c);
    final String[] zh={"左前","右前","左后","右后"};
    final String[] english={"Front left","Front right","Rear left","Rear right"};
    for(int i=0;i<4;i++){
      int t=s.tires.warning(i);
      if(t==TireSnapshot.LOW||t==TireSnapshot.VERY_LOW||t==TireSnapshot.HIGH)
        issues.add((en?english[i]:zh[i])+ (en?" tyre ":"轮胎")+s.tires.label(i,en));
      int d=s.exterior.state(false,i);
      if(d==1||d==3)issues.add((en?english[i]:zh[i])+(en?" door open":"车门未关闭"));
      int w=s.exterior.state(true,i);
      if(w==1||w==3)issues.add((en?english[i]:zh[i])+(en?" window open":"车窗未关闭"));
    }
    if("已打开".equals(s.tailgate)||"未关严".equals(s.tailgate))issues.add(en?"Tailgate not closed":"尾箱未关闭");
    VehicleHealthSnapshot h=s.health;
    if(h.lowVoltageBattery==VehicleHealthSnapshot.WARNING)issues.add(en?"12V battery warning":"12V 蓄电池异常");
    if(h.washerFluid==VehicleHealthSnapshot.WARNING)issues.add(en?"Washer fluid low":"玻璃水不足");
    if(h.tirePressure==VehicleHealthSnapshot.WARNING&&!s.tires.hasProblem())issues.add(en?"Tyre pressure warning":"胎压异常");
    if(h.maintenance==VehicleHealthSnapshot.WARNING||h.serviceOverdue())issues.add(en?"Service inspection due":"保养检查到期");
    if(h.brakeFluid==VehicleHealthSnapshot.WARNING)issues.add(en?"Brake fluid warning":"制动液警告");
    if(h.coolant==VehicleHealthSnapshot.WARNING)issues.add(en?"Coolant warning":"冷却液警告");
    if(h.oil==VehicleHealthSnapshot.WARNING)issues.add(en?"Oil system warning":"油液检查提醒");
    if(h.exteriorLights==VehicleHealthSnapshot.WARNING)issues.add(en?"Lighting check needed":"外部照明异常");
    return issues;
  }
  public static String detail(List<String> items,int limit){StringBuilder out=new StringBuilder();for(int i=0;i<Math.min(items.size(),limit);i++){if(i>0)out.append(" · ");out.append(items.get(i));}if(items.size()>limit)out.append(" · +").append(items.size()-limit);return out.toString();}
}
