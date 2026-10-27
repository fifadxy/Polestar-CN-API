package io.github.polestarcn.digitalkeylab;

/**
 * Evidence-oriented, non-operational model recovered from the user-provided
 * Polestar China 4.25.0 APK. It never carries key bytes, BLE payloads, tokens,
 * VINs, or vehicle commands.
 */
public final class ProvisioningStateMachine {
  public enum Stage {
    SDK_CONFIGURATION,
    DEVICE_ENROLMENT,
    VEHICLE_DISCOVERY,
    ECDHE_CONFIRMATION,
    CERTIFICATE_EXCHANGE,
    VEHICLE_IDENTITY_STORED,
    CATALOG_ENROLMENT,
    PASSIVE_ENTRY_PROVISIONING,
    PAK_PAIRING,
    PAK_PAIRED
  }

  private Stage stage = Stage.SDK_CONFIGURATION;

  public Stage stage() { return stage; }

  public boolean advance(Stage expectedCurrent, Stage next) {
    if (stage != expectedCurrent || next.ordinal() != stage.ordinal() + 1) return false;
    stage = next;
    return true;
  }

  /**
   * The lab observes simulated phases only; reaching PAK_PAIRED is not
   * evidence that any real vehicle accepted a key.
   */
  public boolean reachedTerminalLabStage() {
    return stage == Stage.PAK_PAIRED;
  }

  public boolean canClaimWorkingDigitalKey() {
    return false;
  }

  public void reset() {
    stage = Stage.SDK_CONFIGURATION;
  }

  public static String evidenceFor(Stage stage) {
    switch (stage) {
      case SDK_CONFIGURATION: return "Vocmo.initialize + configurable PhoneAsKeyRepository";
      case DEVICE_ENROLMENT: return "UCC/USEC enrolment via Configurator/EnrolmentRepository";
      case VEHICLE_DISCOVERY: return "BLE or remote pairing service discovers target vehicle";
      case ECDHE_CONFIRMATION: return "PairingSession ECDHE request and explicit shared-secret approval";
      case CERTIFICATE_EXCHANGE: return "Actor/Near-Field CSR and vehicle certificate exchange";
      case VEHICLE_IDENTITY_STORED: return "Actor/Near-Field/Car certificates and BLE identity stored";
      case CATALOG_ENROLMENT: return "CA-CAT-030/031 catalogue enrolment";
      case PASSIVE_ENTRY_PROVISIONING: return "CarAccess passive-key response converted to PassiveEntryInfo";
      case PAK_PAIRING: return "ConnectionInfo registered with P319 and Vehicle.pair invoked";
      case PAK_PAIRED: return "Requires separate authorized real-vehicle verification";
      default: throw new AssertionError(stage);
    }
  }
}
