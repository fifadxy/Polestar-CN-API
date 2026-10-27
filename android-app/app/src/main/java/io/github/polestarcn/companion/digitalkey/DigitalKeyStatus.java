package io.github.polestarcn.companion.digitalkey;

public final class DigitalKeyStatus {
  public enum Phase {
    SDK_CONFIGURATION,
    DEVICE_ENROLMENT,
    VEHICLE_DISCOVERY,
    ECDHE_CONFIRMATION,
    CERTIFICATE_EXCHANGE,
    VEHICLE_IDENTITY_STORED,
    CATALOG_ENROLMENT,
    PASSIVE_ENTRY_PROVISIONING,
    PAK_PAIRING,
    PAK_PAIRED,
    UNAVAILABLE
  }

  public final Phase phase;
  public final String detail;
  public final boolean vehicleVerified;

  public DigitalKeyStatus(Phase phase, String detail, boolean vehicleVerified) {
    if (phase == null) throw new IllegalArgumentException("phase");
    this.phase = phase;
    this.detail = detail == null ? "" : detail;
    this.vehicleVerified = vehicleVerified;
  }

  public boolean canClaimWorkingKey() {
    return phase == Phase.PAK_PAIRED && vehicleVerified;
  }

  public static DigitalKeyStatus researchOnly(String detail) {
    return new DigitalKeyStatus(Phase.UNAVAILABLE, detail, false);
  }
}
