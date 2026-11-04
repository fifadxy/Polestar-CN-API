package io.github.polestarcn.companion;

import android.content.Context;

public enum DisplayMetric {
  BATTERY("battery", "电量", "Battery", "%"), RANGE("range", "续航", "Range", " km"),
  ODOMETER("odometer", "总里程", "Odometer", " km"),
  TA_DISTANCE("ta_distance", "Last Trip · TA 距离", "Last Trip · TA distance", " km"),
  TA_SPEED("ta_speed", "TA 平均速度", "TA · AVG Speed", " km/h"),
  TA_CONSUMPTION("ta_consumption", "TA 百公里能耗", "TA · Average Consumption", " kWh/100 km"),
  TM_DISTANCE("tm_distance", "Since Reset · TM 距离", "Since Reset · TM distance", " km"),
  TM_SPEED("tm_speed", "TM 平均速度", "TM · AVG Speed", " km/h"),
  TM_CONSUMPTION("tm_consumption", "TM 百公里能耗", "TM · Average Consumption", " kWh/100 km"),
  TA_DURATION("ta_duration", "TA 行驶时间", "TA driving time", ""),
  TM_DURATION("tm_duration", "TM 行驶时间", "TM driving time", ""),
  SINCE_CHARGE_DISTANCE("since_charge_distance", "充电后行程距离", "Since charge distance", " km"),
  SINCE_CHARGE_SPEED("since_charge_speed", "充电后平均速度", "Since charge average speed", " km/h"),
  SINCE_CHARGE_CONSUMPTION("since_charge_consumption", "充电后平均能耗", "Since charge consumption", " kWh/100 km"),
  TEMPERATURE("temperature", "车内温度", "Cabin temperature", " °C"),
  TARGET_TEMPERATURE("target_temperature", "空调目标温度", "Climate target", " °C"),
  AQI("aqi", "空气质量 AQI", "Air quality · AQI", " AQI"),
  PM25("pm25", "PM2.5", "PM2.5", " µg/m³"),
  CHARGE_LIMIT("charge_limit", "充电上限", "Charge limit", "%"),
  SERVICE_CYCLE("service_cycle", "保养周期剩余", "Service cycle remaining", "%"),
  CHARGE_CONNECTION("charge_connection", "充电连接", "Charger connection", ""),
  CHARGE_TYPE("charge_type", "充电类型", "Charging type", ""),
  CHARGE_POWER("charge_power", "充电功率", "Charging power", " kW"),
  CHARGE_CURRENT("charge_current", "充电电流", "Charging current", " A"),
  CHARGE_ETA("charge_eta", "预计充满", "Time to full", ""),
  POWER_STATUS("power_status", "电源状态", "Power status", ""),
  BATTERY_12V("battery_12v", "12V 电池", "12 V battery", ""),
  BRAKE_FLUID("brake_fluid", "制动液", "Brake fluid", ""),
  COOLANT("coolant", "冷却液", "Coolant", ""),
  WASHER_FLUID("washer_fluid", "玻璃水", "Washer fluid", ""),
  OIL_STATUS("oil_status", "机油（适用时）", "Oil (if applicable)", ""),
  VEHICLE_STATUS("vehicle_status", "车况", "Vehicle status", ""),
  TIRE_STATUS("tire_status", "胎压状态", "Tire status", ""),
  SERVICE_HOURS_LEFT("service_hours_left", "保养剩余工时", "Service engine hours left", " h"),
  SERVICE_DAYS_LEFT("service_days_left", "保养剩余天数", "Service days left", " d"),
  SERVICE_KM_LEFT("service_km_left", "保养剩余里程", "Service km left", " km"),
  SERVICE_TIME_PERCENT("service_time_percent", "保养时间剩余", "Service time left", "%"),
  SERVICE_KM_PERCENT("service_km_percent", "保养里程剩余", "Service distance left", "%");
  public final String id, zh, en, unit;
  DisplayMetric(String id, String zh, String en, String unit) { this.id = id; this.zh = zh; this.en = en; this.unit = unit; }
  public String label(boolean english) { return english ? en : zh; }
  public String shortLabel(boolean english) {
    switch (this) {
      case BATTERY_12V:return "12V";case BRAKE_FLUID:return english?"Brake":"制动液";case COOLANT:return english?"Coolant":"冷却液";case WASHER_FLUID:return english?"Washer":"玻璃水";case OIL_STATUS:return english?"Oil":"机油";
      case TA_DURATION: return english ? "TA time" : "TA 时长";
      case TM_DURATION: return english ? "TM time" : "TM 时长";
      case SINCE_CHARGE_DISTANCE: return english ? "SC km" : "充电后 km";
      case SINCE_CHARGE_SPEED: return "SC km/h";
      case SINCE_CHARGE_CONSUMPTION: return "SC kWh/100km";
      case TA_DISTANCE: return "TA · km";
      case TM_DISTANCE: return "TM · km";
      case TA_SPEED: return "TA · km/h";
      case TM_SPEED: return "TM · km/h";
      case TA_CONSUMPTION: return "TA kWh/100km";
      case TM_CONSUMPTION: return "TM kWh/100km";
      case TEMPERATURE: return english ? "Cabin °C" : "车内 °C";
      case TARGET_TEMPERATURE: return english ? "Target °C" : "目标 °C";
      case AQI: return "AQI";
      case PM25: return "PM2.5 µg/m³";
      case BATTERY: return english ? "Battery %" : "电量 %";
      case RANGE: return english ? "Range km" : "续航 km";
      case ODOMETER: return english ? "ODO km" : "总里程 km";
      case CHARGE_POWER: return english ? "Charge kW" : "充电 kW";
      case CHARGE_CURRENT: return english ? "Current A" : "电流 A";
      case CHARGE_CONNECTION: return english ? "Plug" : "充电连接";
      case CHARGE_TYPE: return english ? "Type" : "充电类型";
      case CHARGE_ETA: return english ? "Full" : "充满时间";
      case POWER_STATUS: return english ? "Power" : "电源状态";
      case VEHICLE_STATUS: return english ? "Status" : "车况";
      case TIRE_STATUS: return english ? "Tires" : "胎压";
      case SERVICE_HOURS_LEFT: return english ? "Service h" : "保养工时";
      case SERVICE_DAYS_LEFT: return english ? "Service d" : "保养天数";
      case SERVICE_KM_LEFT: return english ? "Service km" : "保养里程";
      case SERVICE_TIME_PERCENT: return english ? "Time left %" : "时间剩余 %";
      case SERVICE_KM_PERCENT: return english ? "Distance left %" : "里程剩余 %";
      default: return label(english);
    }
  }
  /** Timestamp of this metric's source, independent of battery refresh time. */
  public long reportedAt(VehicleSnapshot s) {
    if (s == null) return 0;
    switch (this) {
      case BATTERY_12V: case BRAKE_FLUID: case COOLANT: case WASHER_FLUID: case OIL_STATUS:return s.health.reportedAt;
      case CHARGE_LIMIT:return s.chargingDetails.targetReportedAt;
      case SERVICE_CYCLE:return s.health.reportedAt;
      case VEHICLE_STATUS: return s.health.reportedAt;
      case TIRE_STATUS: return s.tires.reportedAt;
      case TEMPERATURE: case TARGET_TEMPERATURE: return s.environment.climateReportedAt;
      case AQI: case PM25: return s.environment.airReportedAt;
      case ODOMETER: case TA_DISTANCE: case TM_DISTANCE: case TA_SPEED: case TM_SPEED:
      case TA_DURATION: case TM_DURATION: case SINCE_CHARGE_DISTANCE: case SINCE_CHARGE_SPEED:
      return s.mileage.reportedAt;
      case SERVICE_KM_LEFT: case SERVICE_KM_PERCENT:return s.health.serviceKmLeft!=null?s.health.reportedAt:0;
      case TA_CONSUMPTION: case TM_CONSUMPTION: case SINCE_CHARGE_CONSUMPTION: return s.mileage.consumptionReportedAt;
      case SERVICE_HOURS_LEFT:return s.health.reportedAt;
      case SERVICE_DAYS_LEFT: case SERVICE_TIME_PERCENT: return s.health.serviceDaysLeft!=null?s.health.reportedAt:0;
      default: return s.timestampMillis;
    }
  }
  public String value(VehicleSnapshot s, boolean includeUnit) {
    return value(s, includeUnit, false);
  }
  public String value(VehicleSnapshot s, boolean includeUnit, boolean english) {
    return value(null, s, includeUnit, english);
  }
  public String value(Context c, VehicleSnapshot s, boolean includeUnit, boolean english) {
    if (s == null) return "--";
    int check=this==BATTERY_12V?s.health.lowVoltageBattery:this==BRAKE_FLUID?s.health.brakeFluid:this==COOLANT?s.health.coolant:this==WASHER_FLUID?s.health.washerFluid:this==OIL_STATUS?s.health.oil:Integer.MIN_VALUE;
    if(check!=Integer.MIN_VALUE)return check==VehicleHealthSnapshot.UNKNOWN?"--":VehicleHealthSnapshot.label(check,english);
    if(this==CHARGE_LIMIT)return ServiceReading.number(s.chargingDetails.targetPercent,includeUnit?unit:"");
    if(this==SERVICE_CYCLE)return ServiceReading.number(ServiceReading.cyclePercent(c,s),includeUnit?unit:"");
    if (this == VEHICLE_STATUS) return c==null?VehicleCondition.summary(s,english,true):VehicleCondition.summary(c,s,AppPreferences.visible(c,"show_tire_status",true));
    if (this == TIRE_STATUS) return s.tires.summary(english);
    if(this==SERVICE_HOURS_LEFT)return ServiceReading.number(s.health.serviceHoursLeft,includeUnit?unit:"");
    if(this==SERVICE_DAYS_LEFT||this==SERVICE_KM_LEFT||this==SERVICE_TIME_PERCENT||this==SERVICE_KM_PERCENT) {
      Integer value=this==SERVICE_DAYS_LEFT?ServiceReading.days(c,s):this==SERVICE_KM_LEFT?ServiceReading.km(c,s)
          :this==SERVICE_TIME_PERCENT?ServiceReading.timePercent(c,s):ServiceReading.distancePercent(c,s);
      return ServiceReading.number(value,includeUnit?unit:"");
    }
    switch (this) {
      case CHARGE_CONNECTION: return s.chargingDetails.connection(english);
      case CHARGE_TYPE: return s.chargingDetails.type(english);
      case CHARGE_POWER: return s.chargingDetails.power(includeUnit);
      case CHARGE_CURRENT: return s.chargingDetails.current(includeUnit);
      case CHARGE_ETA: return s.chargingDetails.timeToFull(english);
      case POWER_STATUS: return s.chargingDetails.powerStatus(english);
      default: break;
    }
    if (this == TEMPERATURE) return EnvironmentSnapshot.temperatureText(s.environment.temperatureC, includeUnit);
    if (this == TARGET_TEMPERATURE) return EnvironmentSnapshot.temperatureText(s.environment.targetTemperatureC, includeUnit);
    if (this == AQI) return s.environment.aqi == null ? "--" : s.environment.aqi + (includeUnit ? unit : "");
    if (this == PM25) return MileageSnapshot.decimalText(s.environment.pm25UgM3, includeUnit ? unit : "");
    if (this == BATTERY) return s.batteryPercent == null ? "--" : s.batteryPercent + (includeUnit ? unit : "");
    if (this == RANGE) return s.rangeKm == null ? "--" : s.rangeKm + (includeUnit ? unit : "");
    if (this == ODOMETER) return s.mileage.totalMeters == null ? "--" :
        (s.mileage.totalMeters / 1000L) + (includeUnit ? unit : "");
    if (this == TA_DURATION || this == TM_DURATION) return MileageSnapshot.durationText(
        this == TA_DURATION ? MileageSnapshot.estimatedMinutes(s.mileage.automaticKm,s.mileage.automaticAverageSpeed)
            : MileageSnapshot.estimatedMinutes(s.mileage.manualKm,s.mileage.manualAverageSpeed),english);
    Double value;
    switch (this) {
      case TA_DISTANCE: value = s.mileage.automaticKm; break;
      case TM_DISTANCE: value = s.mileage.manualKm; break;
      case TA_SPEED: value = s.mileage.automaticAverageSpeed; break;
      case TM_SPEED: value = s.mileage.manualAverageSpeed; break;
      case TA_CONSUMPTION: value = s.mileage.automaticConsumption; break;
      case SINCE_CHARGE_DISTANCE: value = s.mileage.sinceChargeKm; break;
      case SINCE_CHARGE_SPEED: value = s.mileage.sinceChargeAverageSpeed; break;
      case SINCE_CHARGE_CONSUMPTION: value = s.mileage.sinceChargeConsumption; break;
      default: value = s.mileage.manualConsumption;
    }
    return (this == TA_CONSUMPTION && value!=null && s.mileage.automaticConsumptionEstimated ? "~" : "")
        + MileageSnapshot.decimalText(value, includeUnit ? unit : "");
  }
}
