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

  public MileageSnapshot(Long totalMeters, Double manualKm, Double automaticKm) {
    this(totalMeters, manualKm, automaticKm, null, null, null, null, 0, 0);
  }
  public MileageSnapshot(Long totalMeters, Double manualKm, Double automaticKm,
      Double manualAverageSpeed, Double automaticAverageSpeed, Double manualConsumption,
      Double automaticConsumption, long reportedAt, long consumptionReportedAt) {
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
        ProtoWire.timestampMillis(data, 1), 0);
  }

  /** Optional C3 battery fields documented in global schemas; absent CN fields stay null. */
  public MileageSnapshot withConsumption(Map<Integer, Object> battery) {
    return new MileageSnapshot(totalMeters, manualKm, automaticKm, manualAverageSpeed,
        automaticAverageSpeed, ProtoWire.decimal(battery, 3), ProtoWire.decimal(battery, 12),
        reportedAt, ProtoWire.timestampMillis(battery, 1));
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
