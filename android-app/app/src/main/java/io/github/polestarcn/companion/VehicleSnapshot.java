package io.github.polestarcn.companion;

public final class VehicleSnapshot {
  public final String name, id, source, charging, doors, windows, tailgate, lock;
  public final Integer batteryPercent, rangeKm, odometerKm;
  public final String climate, air;
  public final long timestampMillis;
  public final boolean isDemo;
  public final MileageSnapshot mileage;
  public final ExteriorSnapshot exterior;
  public final EnvironmentSnapshot environment;
  public final ChargingSnapshot chargingDetails;
  public final TireSnapshot tires;
  public final VehicleHealthSnapshot health;

  public VehicleSnapshot(
      String name,
      String id,
      Integer batteryPercent,
      Integer rangeKm,
      Integer odometerKm,
      String charging,
      String doors,
      String windows,
      String tailgate,
      String lock,
      long timestampMillis,
      String source,
      boolean isDemo) {
    this(
        name,
        id,
        batteryPercent,
        rangeKm,
        odometerKm,
        charging,
        doors,
        windows,
        tailgate,
        lock,
        timestampMillis,
        source,
        isDemo,
        "未知",
        "未知");
  }

  public VehicleSnapshot(
      String name,
      String id,
      Integer batteryPercent,
      Integer rangeKm,
      Integer odometerKm,
      String charging,
      String doors,
      String windows,
      String tailgate,
      String lock,
      long timestampMillis,
      String source,
      boolean isDemo,
      String climate,
      String air) {
    this(name, id, batteryPercent, rangeKm, odometerKm, charging, doors, windows, tailgate,
        lock, timestampMillis, source, isDemo, climate, air,
        new MileageSnapshot(odometerKm == null ? null : odometerKm.longValue() * 1000L,
            null, null), ExteriorSnapshot.unknown(), EnvironmentSnapshot.unknown(), ChargingSnapshot.unknown(),
        TireSnapshot.unknown(), VehicleHealthSnapshot.unknown());
  }

  private VehicleSnapshot(
      String name, String id, Integer batteryPercent, Integer rangeKm, Integer odometerKm,
      String charging, String doors, String windows, String tailgate, String lock,
      long timestampMillis, String source, boolean isDemo, String climate, String air,
      MileageSnapshot mileage, ExteriorSnapshot exterior, EnvironmentSnapshot environment, ChargingSnapshot chargingDetails,
      TireSnapshot tires, VehicleHealthSnapshot health) {
    this.mileage = mileage == null ? MileageSnapshot.unknown() : mileage;
    this.exterior = exterior == null ? ExteriorSnapshot.unknown() : exterior;
    this.environment = environment == null ? EnvironmentSnapshot.unknown() : environment;
    this.chargingDetails = chargingDetails == null ? ChargingSnapshot.unknown() : chargingDetails;
    this.tires = tires == null ? TireSnapshot.unknown() : tires;
    this.health = health == null ? VehicleHealthSnapshot.unknown() : health;
    this.climate = climate;
    this.air = air;
    this.name = name;
    this.id = id;
    this.batteryPercent = batteryPercent;
    this.rangeKm = rangeKm;
    this.odometerKm = this.mileage.wholeKm();
    this.charging = charging;
    this.doors = doors;
    this.windows = windows;
    this.tailgate = tailgate;
    this.lock = lock;
    this.timestampMillis = timestampMillis;
    this.source = source;
    this.isDemo = isDemo;
  }

  public VehicleSnapshot withMileage(MileageSnapshot value) {
    return new VehicleSnapshot(name, id, batteryPercent, rangeKm, odometerKm, charging,
        doors, windows, tailgate, lock, timestampMillis, source, isDemo, climate, air, value, exterior, environment, chargingDetails, tires, health);
  }

  public VehicleSnapshot withExterior(ExteriorSnapshot value) {
    return new VehicleSnapshot(name, id, batteryPercent, rangeKm, odometerKm, charging,
        value.summary(false), value.summary(true), tailgate, lock, timestampMillis, source,
        isDemo, climate, air, mileage, value, environment, chargingDetails, tires, health);
  }

  public VehicleSnapshot withEnvironment(EnvironmentSnapshot value) {
    return new VehicleSnapshot(name, id, batteryPercent, rangeKm, odometerKm, charging, doors,
        windows, tailgate, lock, timestampMillis, source, isDemo, value.climate, value.air,
        mileage, exterior, value, chargingDetails, tires, health);
  }

  public VehicleSnapshot withCharging(ChargingSnapshot value) {
    return new VehicleSnapshot(name, id, batteryPercent, rangeKm, odometerKm, charging, doors,
        windows, tailgate, lock, timestampMillis, source, isDemo, climate, air,
        mileage, exterior, environment, value, tires, health);
  }

  public VehicleSnapshot withTires(TireSnapshot value) {
    return new VehicleSnapshot(name, id, batteryPercent, rangeKm, odometerKm, charging, doors,
        windows, tailgate, lock, timestampMillis, source, isDemo, climate, air,
        mileage, exterior, environment, chargingDetails, value, health);
  }

  public VehicleSnapshot withHealth(VehicleHealthSnapshot value) {
    return new VehicleSnapshot(name, id, batteryPercent, rangeKm, odometerKm, charging, doors,
        windows, tailgate, lock, timestampMillis, source, isDemo, climate, air,
        mileage, exterior, environment, chargingDetails, tires, value);
  }

  public static String metric(Integer value, String unit) {
    return value == null ? "--" : value + unit;
  }
}
