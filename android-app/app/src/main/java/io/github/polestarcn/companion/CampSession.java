package io.github.polestarcn.companion;

/** A bounded delegation to maintain only parking climate and optional air purification. */
public final class CampSession {
  public final String id, vin, vehicleName;
  public final long endsAt;
  public final int intervalMinutes, minimumBattery;
  public final boolean keepAir;
  public final float targetTemperatureC;
  public final boolean verifyClimateTarget;
  public CampSession(String id, String vin, String vehicleName, long endsAt,
      int intervalMinutes, int minimumBattery, boolean keepAir) {this(id,vin,vehicleName,endsAt,intervalMinutes,minimumBattery,keepAir,22f,false);}
  public CampSession(String id,String vin,String vehicleName,long endsAt,int intervalMinutes,int minimumBattery,boolean keepAir,float targetTemperatureC){this(id,vin,vehicleName,endsAt,intervalMinutes,minimumBattery,keepAir,targetTemperatureC,true);}
  public CampSession(String id,String vin,String vehicleName,long endsAt,int intervalMinutes,int minimumBattery,boolean keepAir,float targetTemperatureC,boolean verifyClimateTarget){
    this.verifyClimateTarget=verifyClimateTarget;
    if(!Float.isFinite(targetTemperatureC)||targetTemperatureC<18||targetTemperatureC>28)throw new IllegalArgumentException("Invalid experimental temperature");this.targetTemperatureC=targetTemperatureC;
    if (id == null || id.isEmpty() || vin == null || vin.isEmpty() || endsAt <= 0
        || intervalMinutes < 20 || intervalMinutes > 60 || minimumBattery < 5 || minimumBattery > 90)
      throw new IllegalArgumentException("Invalid camping configuration");
    this.id = id; this.vin = vin; this.vehicleName = vehicleName;
    this.endsAt = endsAt; this.intervalMinutes = intervalMinutes;
    this.minimumBattery = minimumBattery; this.keepAir = keepAir;
  }
  public static boolean batteryAllows(Integer battery, int minimum) {
    return battery != null && battery >= minimum && battery <= 100;
  }
}
