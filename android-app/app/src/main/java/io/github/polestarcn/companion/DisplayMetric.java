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
  TEMPERATURE("temperature", "车内温度", "Cabin temperature", " °C"),
  TARGET_TEMPERATURE("target_temperature", "空调目标温度", "Climate target", " °C"),
  AQI("aqi", "空气质量 AQI", "Air quality · AQI", " AQI"),
  PM25("pm25", "PM2.5", "PM2.5", " µg/m³"),
  CHARGE_CONNECTION("charge_connection", "充电连接", "Charger connection", ""),
  CHARGE_TYPE("charge_type", "充电类型", "Charging type", ""),
  CHARGE_POWER("charge_power", "充电功率", "Charging power", " kW"),
  CHARGE_CURRENT("charge_current", "充电电流", "Charging current", " A"),
  CHARGE_ETA("charge_eta", "预计充满", "Time to full", ""),
  POWER_STATUS("power_status", "电源状态", "Power status", ""),
  VEHICLE_STATUS("vehicle_status", "车辆状态", "Vehicle status", ""),
  TIRE_STATUS("tire_status", "胎压状态", "Tire status", ""),
  SERVICE_DAYS_LEFT("service_days_left", "保养剩余天数", "Service days left", " d"),
  SERVICE_KM_LEFT("service_km_left", "保养剩余里程", "Service km left", " km"),
  SERVICE_TIME_PERCENT("service_time_percent", "保养时间剩余", "Service time left", "%"),
  SERVICE_KM_PERCENT("service_km_percent", "保养里程剩余", "Service distance left", "%");
  public final String id, zh, en, unit;
  DisplayMetric(String id, String zh, String en, String unit) { this.id = id; this.zh = zh; this.en = en; this.unit = unit; }
  public String label(boolean english) { return english ? en : zh; }
  public String shortLabel(boolean english) {
    switch (this) {
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
      case VEHICLE_STATUS: return english ? "Vehicle" : "车辆状态";
      case TIRE_STATUS: return english ? "Tires" : "胎压";
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
      case VEHICLE_STATUS: return s.health.reportedAt;
      case TIRE_STATUS: return s.tires.reportedAt;
      case TEMPERATURE: case TARGET_TEMPERATURE: return s.environment.climateReportedAt;
      case AQI: case PM25: return s.environment.airReportedAt;
      case ODOMETER: case TA_DISTANCE: case TM_DISTANCE: case TA_SPEED: case TM_SPEED:
      case SERVICE_KM_LEFT: case SERVICE_KM_PERCENT: return s.mileage.reportedAt;
      case TA_CONSUMPTION: case TM_CONSUMPTION: return s.mileage.consumptionReportedAt;
      case SERVICE_DAYS_LEFT: case SERVICE_TIME_PERCENT: return System.currentTimeMillis();
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
    if (this == VEHICLE_STATUS) return s.health.summary(english, c == null || AppPreferences.visible(c, "show_tire_status", true));
    if (this == TIRE_STATUS) return s.tires.summary(english);
    if (this == SERVICE_DAYS_LEFT || this == SERVICE_KM_LEFT
        || this == SERVICE_TIME_PERCENT || this == SERVICE_KM_PERCENT) {
      if (c == null) return "--";
      MaintenanceSchedule.Status status = MaintenanceSchedule.status(
          c, AppPreferences.get(c, "vehicle", ""), s);
      if (!status.configured) return "--";
      if (this == SERVICE_DAYS_LEFT) return status.remainingDays() + (includeUnit ? unit : "");
      if (this == SERVICE_KM_LEFT) {
        Integer v = status.remainingKm();
        return v == null ? "--" : v + (includeUnit ? unit : "");
      }
      int v = this == SERVICE_TIME_PERCENT
          ? status.remainingTimePercent() : status.remainingDistancePercent();
      return v < 0 ? "--" : v + (includeUnit ? unit : "");
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
    Double value;
    switch (this) {
      case TA_DISTANCE: value = s.mileage.automaticKm; break;
      case TM_DISTANCE: value = s.mileage.manualKm; break;
      case TA_SPEED: value = s.mileage.automaticAverageSpeed; break;
      case TM_SPEED: value = s.mileage.manualAverageSpeed; break;
      case TA_CONSUMPTION: value = s.mileage.automaticConsumption; break;
      default: value = s.mileage.manualConsumption;
    }
    return MileageSnapshot.decimalText(value, includeUnit ? unit : "");
  }
}
