package io.github.polestarcn.companion;
/** Transparent trip-reset TM cost estimate, never a charging meter or a live power read. */
public final class TmCost {
  private TmCost() {}
  public static Double kwh(Double distanceKm, Double kwhPer100Km) {
    if(distanceKm==null||kwhPer100Km==null||!Double.isFinite(distanceKm)||!Double.isFinite(kwhPer100Km)
        ||distanceKm<0||distanceKm>1000000||kwhPer100Km<0||kwhPer100Km>100)return null;
    return distanceKm*kwhPer100Km/100.0;
  }
  public static Double kwh(VehicleSnapshot vehicle,long now) {
    if(vehicle==null||vehicle.isDemo||!"LIVE".equals(vehicle.source))return null;
    if(vehicle.timestampMillis<=0||now-vehicle.timestampMillis>15*60_000L||vehicle.timestampMillis>now+300_000L)return null;
    MileageSnapshot trip=vehicle.mileage;
    if(trip==null||trip.reportedAt<=0||trip.consumptionReportedAt<=0
        ||Math.abs(trip.reportedAt-trip.consumptionReportedAt)>120_000L)return null;
    return kwh(trip.manualKm,trip.manualConsumption);
  }
  public static double cost(double kwh,double currencyPerKwh) {
    if(!Double.isFinite(kwh)||kwh<0||!Double.isFinite(currencyPerKwh)||currencyPerKwh<0)return 0;
    return kwh*currencyPerKwh;
  }
}
