package io.github.polestarcn.companion;

import io.github.polestarcn.companion.api.ProtoWire;
import java.util.Map;

/**
 * Warning-only C3 Health snapshot.
 *
 * Global Hisingen source mapping:
 * field 13 = washer-fluid warning, field 38 = low-voltage/12 V warning,
 * fields 9-12 = tyre/iTPMS warnings. These mappings are source-verified globally; China
 * execution remains vehicle-verification dependent.
 */
public final class VehicleHealthSnapshot {
  public static final int UNKNOWN = -1;
  public static final int OK = 1;
  public static final int WARNING = 2;

  public final int lowVoltageBattery;
  public final int washerFluid;
  public final int tirePressure;
  public final long reportedAt;
  public final int maintenance,brakeFluid,coolant,oil,exteriorLights;
  public final Integer serviceHoursLeft,serviceDaysLeft,serviceKmLeft;

  public VehicleHealthSnapshot(
      int lowVoltageBattery, int washerFluid, int tirePressure, long reportedAt) {
    this(lowVoltageBattery,washerFluid,tirePressure,reportedAt,UNKNOWN,UNKNOWN,UNKNOWN,UNKNOWN,UNKNOWN,null,null,null);
  }
  public VehicleHealthSnapshot(int lowVoltageBattery,int washerFluid,int tirePressure,long reportedAt,
      int maintenance,int brakeFluid,int coolant,int oil,int exteriorLights,
      Integer serviceHoursLeft,Integer serviceDaysLeft,Integer serviceKmLeft) {
    this.maintenance=normalize(maintenance);this.brakeFluid=normalize(brakeFluid);this.coolant=normalize(coolant);
    this.oil=normalize(oil);this.exteriorLights=normalize(exteriorLights);
    this.serviceHoursLeft=counter(serviceHoursLeft);this.serviceDaysLeft=counter(serviceDaysLeft);this.serviceKmLeft=counter(serviceKmLeft);
    this.lowVoltageBattery = normalize(lowVoltageBattery);
    this.washerFluid = normalize(washerFluid);
    this.tirePressure = normalize(tirePressure);
    this.reportedAt = Math.max(0L, reportedAt);
  }

  public static VehicleHealthSnapshot unknown() {
    return new VehicleHealthSnapshot(UNKNOWN, UNKNOWN, UNKNOWN, 0L);
  }

  public static VehicleHealthSnapshot fromData(
      Map<Integer, Object> health, TireSnapshot tires) {
    Integer washerRaw = ProtoWire.integer(health, 13);
    Integer lowVoltageRaw = ProtoWire.integer(health, 38);
    int washer = washerRaw == null || washerRaw == 0 ? UNKNOWN : washerRaw == 1 ? OK : washerRaw == 2 ? WARNING : UNKNOWN;
    int lowVoltage =
        lowVoltageRaw == null ? UNKNOWN : lowVoltageRaw == 2 ? WARNING : lowVoltageRaw == 1 ? OK : UNKNOWN;
    int tyre =
        tires == null || !tires.hasAnyReport()
            ? UNKNOWN
            : tires.hasProblem() ? WARNING : tires.allGood() ? OK : UNKNOWN;
    return new VehicleHealthSnapshot(
        lowVoltage,washer,tyre,ProtoWire.timestampMillis(health,1),state(ProtoWire.integer(health,5),11),
        state(ProtoWire.integer(health,6),3),state(ProtoWire.integer(health,7),2),state(ProtoWire.integer(health,8),8),
        lights(health),ProtoWire.integer(health,2),ProtoWire.integer(health,3),ProtoWire.integer(health,4));
  }

  private static Integer counter(Integer value){return value==null||value<0?null:value;}
  private static int state(Integer raw,int maximum) {
    return raw==null||raw<1||raw>maximum?UNKNOWN:raw==1?OK:WARNING;
  }
  private static int lights(Map<Integer,Object> data) {
    boolean reported=false,unknown=false;
    for(int field=14;field<=35;field++) {
      if(field==29)continue;Integer raw=ProtoWire.integer(data,field);if(raw==null)continue;
      if(raw==2)return WARNING;if(raw==1)reported=true;else unknown=true;
    }
    for(Object value:ProtoWire.nested(data,46).values())if(value instanceof Number) {
      int raw=((Number)value).intValue();if(raw==2)return WARNING;if(raw==1)reported=true;else unknown=true;
    }
    return reported&&!unknown?OK:UNKNOWN;
  }
  private int[] otherStates(){return new int[]{maintenance,brakeFluid,coolant,oil,exteriorLights};}
  public boolean serviceOverdue(){return serviceHoursLeft!=null&&serviceHoursLeft<0||serviceDaysLeft!=null&&serviceDaysLeft<0||serviceKmLeft!=null&&serviceKmLeft<0;}
  private static int normalize(int value) {
    return value == OK || value == WARNING ? value : UNKNOWN;
  }

  public boolean hasAnyReport() { return hasAnyReport(true); }
  public boolean hasProblem() { return hasProblem(true); }
  public boolean allGood() { return allGood(true); }
  public int issueCount() { return issueCount(true); }
  public String summary(boolean english) { return summary(english, true); }

  public boolean hasAnyReport(boolean includeTires) {
    for(int state:otherStates())if(state!=UNKNOWN)return true;
    return lowVoltageBattery != UNKNOWN || washerFluid != UNKNOWN || includeTires && tirePressure != UNKNOWN;
  }
  public boolean hasProblem(boolean includeTires) {
    for(int state:otherStates())if(state==WARNING)return true;
    return serviceOverdue()||lowVoltageBattery == WARNING || washerFluid == WARNING || includeTires && tirePressure == WARNING;
  }
  public boolean allGood(boolean includeTires) {
    if(hasProblem(includeTires))return false;
    return lowVoltageBattery == OK && washerFluid == OK && (!includeTires || tirePressure == OK);
  }
  public int issueCount(boolean includeTires) {
    int extra=0;for(int state:otherStates())if(state==WARNING)extra++;
    return extra+(lowVoltageBattery == WARNING ? 1 : 0) + (washerFluid == WARNING ? 1 : 0)
        + (includeTires && tirePressure == WARNING ? 1 : 0);
  }
  public String summary(boolean english, boolean includeTires) {
    if (hasProblem(includeTires)) return english ? "Needs attention · " + issueCount(includeTires)
        : "需检查 · " + issueCount(includeTires) + "项";
    if (allGood(includeTires)) return "ALL GOOD";
    return hasAnyReport(includeTires) ? (english ? "Partially reported" : "部分已上报")
        : (english ? "Unavailable" : "未上报");
  }

  public static String label(int state, boolean english) {
    if (state == OK) return english ? "Normal" : "正常";
    if (state == WARNING) return english ? "Check" : "需检查";
    return english ? "Unavailable" : "未上报";
  }
}
