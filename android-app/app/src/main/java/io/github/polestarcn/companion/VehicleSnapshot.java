package io.github.polestarcn.companion;

public final class VehicleSnapshot {
  public final String name, id, source, charging, doors, windows, tailgate, lock;
  public final Integer batteryPercent, rangeKm, odometerKm;
  public final String climate, air;
  public final long timestampMillis;
  public final boolean isDemo;

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
    this.climate = climate;
    this.air = air;
    this.name = name;
    this.id = id;
    this.batteryPercent = batteryPercent;
    this.rangeKm = rangeKm;
    this.odometerKm = odometerKm;
    this.charging = charging;
    this.doors = doors;
    this.windows = windows;
    this.tailgate = tailgate;
    this.lock = lock;
    this.timestampMillis = timestampMillis;
    this.source = source;
    this.isDemo = isDemo;
  }

  public static String metric(Integer value, String unit) {
    return value == null ? "--" : value + unit;
  }
}
