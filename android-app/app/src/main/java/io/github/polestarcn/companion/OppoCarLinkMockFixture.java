package io.github.polestarcn.companion;

/** Synthetic vehicle used only by the beta34 Mock OCS transport. Never sends a vehicle write. */
final class OppoCarLinkMockFixture {
  static final String VIN="TST0POLESTAR2M001";
  private static String lock="已锁",climate="关闭",tailgate="已关闭",windows="全部关闭";
  private static long updatedAt=System.currentTimeMillis();
  private OppoCarLinkMockFixture(){}

  static synchronized VehicleSnapshot snapshot(){
    return new VehicleSnapshot("Polestar 2 TestLab","mock-ocs",66,280,12345,"空闲",
        "全部关闭",windows,tailgate,lock,updatedAt,"MOCK_OCS",false,climate,"关闭");
  }

  static boolean canCommand(String action){
    if(action==null)return false;
    switch(action){
      case "lock":case "unlock":case "climate-start":case "climate-stop":
      case "trunk-open":case "trunk-close":case "windows-open":case "windows-close":
      case "honk":case "flash":case "honk-flash":return true;
      default:return false;
    }
  }

  static synchronized void apply(String action){
    if(!canCommand(action))return;
    switch(action){
      case "lock":lock="已锁";break;
      case "unlock":lock="未锁";break;
      case "climate-start":climate="开启";break;
      case "climate-stop":climate="关闭";break;
      case "trunk-open":tailgate="已打开";break;
      case "trunk-close":tailgate="已关闭";break;
      case "windows-open":windows="已打开";break;
      case "windows-close":windows="全部关闭";break;
      default:break;
    }
    updatedAt=System.currentTimeMillis();
  }

  static synchronized void touch(){updatedAt=System.currentTimeMillis();}

  static synchronized void reset(){
    lock="已锁";climate="关闭";tailgate="已关闭";windows="全部关闭";updatedAt=System.currentTimeMillis();
  }
}
