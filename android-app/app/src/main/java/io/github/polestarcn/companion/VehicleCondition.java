package io.github.polestarcn.companion;

/** Visible-condition summary; partial reporting is explicitly described in the detail sheet. */
public final class VehicleCondition {
  private VehicleCondition() {}
  public static boolean problem(VehicleSnapshot v,boolean tyres) {
    if(v==null)return false;
    if(v.health.hasProblem(tyres)||tyres&&v.tires.hasProblem())return true;
    if("已打开".equals(v.tailgate)||"未关严".equals(v.tailgate))return true;
    for(int i=0;i<4;i++)for(boolean window:new boolean[]{false,true}) {
      int state=v.exterior.state(window,i);if(state==1||state==3)return true;
    }
    return opening(v.doors)||opening(v.windows);
  }
  private static boolean opening(String s){return s!=null&&(s.contains("个开启")||s.equals("已打开")||s.equals("未关严"));}
  public static boolean reported(VehicleSnapshot v,boolean tyres) {
    if(v==null)return false;
    if(v.health.hasAnyReport(tyres)||tyres&&v.tires.hasAnyReport())return true;
    for(int i=0;i<4;i++)for(boolean window:new boolean[]{false,true}) {
      int state=v.exterior.state(window,i);if(state>=1&&state<=3)return true;
    }
    return "全部关闭".equals(v.doors)||"全部关闭".equals(v.windows)||"已关闭".equals(v.tailgate)||problem(v,tyres);
  }
  public static String summary(VehicleSnapshot v,boolean english,boolean tyres) {
    return problem(v,tyres)?(english?"Please check vehicle status!":"请注意车辆状况！")
        :reported(v,tyres)?"All Good！":(english?"Unavailable":"未上报");
  }
}
