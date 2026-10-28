package io.github.polestarcn.companion;

import io.github.polestarcn.companion.api.ProtoWire;
import java.util.Locale;
import java.util.Map;

/** Reference APK: GetLatestOdometer response 3; total metres 2, TM km 3, TA km 4. */
public final class MileageSnapshot {
  public final Long totalMeters;
  public final Double manualKm, automaticKm;
  public final Double manualAverageSpeed, automaticAverageSpeed, manualConsumption, automaticConsumption;
  public final long reportedAt, consumptionReportedAt;
  public final Double sinceChargeKm, sinceChargeAverageSpeed, sinceChargeConsumption;
  public final boolean automaticConsumptionEstimated;


  public MileageSnapshot(Long totalMeters, Double manualKm, Double automaticKm) {
    this(totalMeters, manualKm, automaticKm, null, null, null, null, 0, 0);
  }
  public MileageSnapshot(Long totalMeters, Double manualKm, Double automaticKm,
      Double manualAverageSpeed, Double automaticAverageSpeed, Double manualConsumption,
      Double automaticConsumption, long reportedAt, long consumptionReportedAt) {
    this(totalMeters,manualKm,automaticKm,manualAverageSpeed,automaticAverageSpeed,manualConsumption,
        automaticConsumption,reportedAt,consumptionReportedAt,null,null,null,false);
  }
  public MileageSnapshot(Long totalMeters, Double manualKm, Double automaticKm,
      Double manualAverageSpeed, Double automaticAverageSpeed, Double manualConsumption,
      Double automaticConsumption, long reportedAt, long consumptionReportedAt,
      Double sinceChargeKm, Double sinceChargeAverageSpeed, Double sinceChargeConsumption,
      boolean automaticConsumptionEstimated) {
    this.sinceChargeKm=valid(sinceChargeKm); this.sinceChargeAverageSpeed=valid(sinceChargeAverageSpeed);
    this.sinceChargeConsumption=valid(sinceChargeConsumption);
    this.automaticConsumptionEstimated=automaticConsumptionEstimated;
    this.totalMeters = totalMeters != null && totalMeters >= 0 ? totalMeters : null;
    this.manualKm = valid(manualKm);
    this.automaticKm = valid(automaticKm);
    this.manualAverageSpeed = valid(manualAverageSpeed);
    this.automaticAverageSpeed = valid(automaticAverageSpeed);
    this.manualConsumption = valid(manualConsumption);
    this.automaticConsumption = valid(automaticConsumption);
    this.reportedAt = Math.max(0, reportedAt);
    this.consumptionReportedAt = Math.max(0, consumptionReportedAt);
  }

  public static MileageSnapshot unknown() {
    return new MileageSnapshot(null, null, null);
  }

  public static MileageSnapshot fromReply(Map<Integer, Object> reply) {
    Map<Integer, Object> data = ProtoWire.nested(reply, 3);
    Object rawTotal = data.get(2);
    // The reference uses an integer metre counter, not a floating-point km value.
    Long meters = rawTotal instanceof Long || rawTotal instanceof Integer
        ? ((Number) rawTotal).longValue() : null;
    return new MileageSnapshot(meters, ProtoWire.decimal(data, 3), ProtoWire.decimal(data, 4),
        ProtoWire.decimal(data, 5), ProtoWire.decimal(data, 6), null, null,
        ProtoWire.timestampMillis(data, 1), 0, ProtoWire.decimal(data,7), ProtoWire.decimal(data,8),null,false);
  }

  /**
   * Legacy battery field 12 is TA consumption; Hisingen v2.0.10 observes capacity there.
   * Accept it only when independent TA energy (field 15) and same-report distance corroborate
   * its meaning. Never substitute capacity or since-charge consumption for TA.
   */
  public MileageSnapshot withConsumption(Map<Integer, Object> battery) {
    long at=ProtoWire.timestampMillis(battery,1);
    Double raw=valid(ProtoWire.decimal(battery,12)), energy=valid(ProtoWire.decimal(battery,15));
    boolean aligned=at>0 && reportedAt>0 && at<=System.currentTimeMillis()+300_000L && reportedAt<=System.currentTimeMillis()+300_000L && Math.abs(at-reportedAt)<=120_000L;
    Double calculated=aligned && energy!=null && automaticKm!=null && automaticKm>0
        ? valid(energy/(automaticKm*10.0)) : null;
    Double automatic=null; boolean estimated=false;
    if(calculated!=null) {
      if(raw!=null && Math.abs(raw-calculated)<=Math.max(.5,calculated*.05)) automatic=raw;
      else if(raw==null) { automatic=calculated; estimated=true; }
    }
    return new MileageSnapshot(totalMeters,manualKm,automaticKm,manualAverageSpeed,
        automaticAverageSpeed,ProtoWire.decimal(battery,3),automatic,reportedAt,at,
        sinceChargeKm,sinceChargeAverageSpeed,ProtoWire.decimal(battery,13),estimated);
  }

  /** Distance / mean speed is an estimate for that trip period, not a reported driving timer. */
  public static Double estimatedMinutes(Double distance, Double speed) {
    return valid(distance)!=null && valid(speed)!=null && speed>0 ? valid(distance/speed*60.0) : null;
  }
  public static String durationText(Double minutes, boolean english) {
    if(valid(minutes)==null || minutes>Integer.MAX_VALUE) return "--";
    long rounded=Math.round(minutes),hours=rounded/60,remainder=rounded%60;
    return (hours>0 ? hours+"h " : "")+remainder+"m";
  }

  public Integer wholeKm() {
    if (totalMeters == null || totalMeters / 1000L > Integer.MAX_VALUE) return null;
    return (int) (totalMeters / 1000L);
  }

  public String totalText() {
    return totalMeters == null ? "--" : (totalMeters / 1000L) + " km";
  }

  public static String tripText(Double value) {
    return decimalText(value, " km");
  }

  public static String decimalText(Double value, String unit) {
    Double valid = valid(value);
    return valid == null ? "--" : String.format(Locale.getDefault(), "%.1f", valid) + unit;
  }

  private static Double valid(Double value) {
    return value != null && Double.isFinite(value) && value >= 0 ? value : null;
  }
}
