package io.github.polestarcn.companion;
/** Pure polling policy; parking LBS calls never trigger general vehicle/remote-control reads. */
final class ParkingRefreshStrategy {
  private ParkingRefreshStrategy(){}
  static int intervalMinutes(String mode,int fixedMinutes,int unchanged,int failures){
    if("manual".equals(mode))return 0;
    int base="fixed".equals(mode)?fixedMinutes:unchanged>=3?60:unchanged>=1?30:15;
    if(failures>0)base=Math.max(base,Math.min(120,15*(1<<Math.min(3,failures))));
    return Math.min(120,Math.max(5,base));
  }
  static boolean same(LocationSnapshot a,LocationSnapshot b){
    return a!=null&&b!=null&&a.valid()&&b.valid()
        &&Math.abs(a.latitude-b.latitude)<.0001&&Math.abs(a.longitude-b.longitude)<.0001;
  }
}