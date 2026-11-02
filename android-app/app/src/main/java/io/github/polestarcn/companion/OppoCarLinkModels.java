package io.github.polestarcn.companion;
import android.content.Context;
import com.oplus.ocs.carlink.model.*;
import java.util.*;
/** Cache-only mappings. Unsupported or unknown controls are omitted; no vehicle wake is performed. */
final class OppoCarLinkModels {
  static final Set<String> SUPPORTED=Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList("CAR-DOOR","LOCK-CAR","ALL-WINDOW","CAR-BOOT","AC-TEMP","CAR-HORN","DOUBLE-FLASH","CAR-HORN&&DOUBLE-FLASH")));
  static String carId(String vin){return "psa_"+VehicleCoordinator.identity(vin);}
  static CarInfo info(Context c,OppoCarLinkConfig config,VehicleSnapshot s,String vin){
    String account=c.getSharedPreferences("oppo-carlink-identity",0).getString("account","");
    if(account.isEmpty()){account=UUID.randomUUID().toString();c.getSharedPreferences("oppo-carlink-identity",0).edit().putString("account",account).apply();}
    CarInfo out=new CarInfo(account,carId(vin),s.name==null?"Polestar":s.name,true);
    out.setCompanyId(Integer.toString(config.companyId));out.setTypeCode(config.typeCode);out.setTypeName(config.typeName);out.setImage(config.image);out.setPowerType("2");return out;
  }
  static void account(Context c,String phone){c.getSharedPreferences("oppo-carlink-identity",0).edit().putString("account",VehicleCoordinator.identity("psa-oppo-account:"+phone.trim())).apply();}
  static boolean canReport(VehicleSnapshot s){return s!=null&&!s.isDemo&&s.timestampMillis>0&&s.batteryPercent!=null&&s.batteryPercent>=0&&s.batteryPercent<=100&&s.rangeKm!=null&&s.rangeKm>=0&&("已上锁".equals(s.lock)||"已锁".equals(s.lock)||"未锁".equals(s.lock));}
  static CarStatus status(VehicleSnapshot s,String vin){
    CarStatus out=new CarStatus(carId(vin));out.setTimestamp(s.timestampMillis);
    // This means the source is usable; it does not assert a BLE/digital-key connection.
    out.setActive(!s.isDemo&&EffectStateController.fresh(s,System.currentTimeMillis()));
    if(s.batteryPercent!=null)out.setBatteryLeftPercent(Math.max(0,Math.min(100,s.batteryPercent))/100f);
    if(s.rangeKm!=null){out.setBatteryEnduranceMileage(s.rangeKm);out.setTotalEnduranceMileage(s.rangeKm);}
    if(s.odometerKm!=null)out.setTotalMileage(s.odometerKm);out.setCharging(ChargingVisibility.shouldShow(s));
    ArrayList<ControlInstruction> controls=new ArrayList<>();
    out.setAllDoorStatus("已上锁".equals(s.lock)||"已锁".equals(s.lock));
    add(controls,out.getCarId(),"LOCK-CAR","未锁".equals(s.lock)?0:"已上锁".equals(s.lock)||"已锁".equals(s.lock)?1:-1);
    add(controls,out.getCarId(),"CAR-DOOR","未锁".equals(s.lock)?1:"已上锁".equals(s.lock)||"已锁".equals(s.lock)?0:-1);
    add(controls,out.getCarId(),"CAR-BOOT","已打开".equals(s.tailgate)||"未关严".equals(s.tailgate)?1:"已关闭".equals(s.tailgate)?0:-1);
    add(controls,out.getCarId(),"AC-TEMP","开启".equals(s.climate)?1:"关闭".equals(s.climate)?0:-1);
    if("开启".equals(s.climate)||"关闭".equals(s.climate))out.setAirConditionerStatus("开启".equals(s.climate)?1:0);
    add(controls,out.getCarId(),"ALL-WINDOW",s.windows!=null&&("已打开".equals(s.windows)||"未关严".equals(s.windows)||s.windows.contains("个开启"))?1:"全部关闭".equals(s.windows)||"已关闭".equals(s.windows)?0:-1);
    for(String key:new String[]{"CAR-HORN","DOUBLE-FLASH","CAR-HORN&&DOUBLE-FLASH"})add(controls,out.getCarId(),key,0);
    out.setAvailableControlInstructions(controls);return out;
  }
  private static void add(List<ControlInstruction> list,String car,String key,int state){if(state>=0)list.add(new ControlInstruction(car,key,state));}
}
