package io.github.polestarcn.companion.digitalkey;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Read-only V2.1 evidence gate.
 *
 * This class intentionally models only evidence already observed by the app.
 * It never performs account login, network discovery, BLE pairing, certificate
 * enrolment, PassiveEntry provisioning, P319 pairing, or vehicle commands.
 */
public final class AuthorizedFlowAssessment {
  public enum Stage {
    ACCOUNT_LOGIN,
    VEHICLE_DISCOVERY,
    OEM_IDENTITY,
    SDK_CONFIGURATION,
    DEVICE_ENROLMENT,
    VEHICLE_PAIRING_APPROVAL,
    ECDHE_CONFIRMATION,
    CERTIFICATE_EXCHANGE,
    CATALOG_ENROLMENT,
    PASSIVE_ENTRY_PROVISIONING,
    P319_PAIRING,
    VEHICLE_VERIFIED
  }

  private final EnumSet<Stage> completed;

  private AuthorizedFlowAssessment(EnumSet<Stage> completed) {
    this.completed = completed.clone();
  }

  /**
   * Build a snapshot from non-sensitive signals. Later protected stages are
   * deliberately left incomplete until an authorized OEM provider supplies
   * independently verifiable evidence.
   */
  public static AuthorizedFlowAssessment observed(
      boolean accountSessionPresent,
      boolean vehicleSelected,
      boolean oemIdentityAuthorized,
      boolean nativeSdkReady) {
    EnumSet<Stage> done = EnumSet.noneOf(Stage.class);
    if (accountSessionPresent) done.add(Stage.ACCOUNT_LOGIN);
    if (accountSessionPresent && vehicleSelected) done.add(Stage.VEHICLE_DISCOVERY);
    if (accountSessionPresent && vehicleSelected && oemIdentityAuthorized) {
      done.add(Stage.OEM_IDENTITY);
      if (nativeSdkReady) done.add(Stage.SDK_CONFIGURATION);
    }
    return new AuthorizedFlowAssessment(done);
  }

  public boolean completed(Stage stage) {
    return completed.contains(stage);
  }

  public Set<Stage> completedStages() {
    return Collections.unmodifiableSet(completed);
  }

  public Stage firstBlockedStage() {
    for (Stage stage : Stage.values()) {
      if (!completed.contains(stage)) return stage;
    }
    return null;
  }

  public boolean protectedProvisioningAllowed() {
    return completed.contains(Stage.OEM_IDENTITY)
        && completed.contains(Stage.SDK_CONFIGURATION);
  }

  public boolean canClaimWorkingDigitalKey() {
    return completed.contains(Stage.VEHICLE_VERIFIED);
  }

  public static String label(Stage stage) {
    if (stage == null) return "COMPLETE";
    switch (stage) {
      case ACCOUNT_LOGIN: return "Account login / 账号登录";
      case VEHICLE_DISCOVERY: return "Vehicle discovery / 车辆发现";
      case OEM_IDENTITY: return "OEM identity / 厂商钥匙身份";
      case SDK_CONFIGURATION: return "SDK configuration / SDK 配置";
      case DEVICE_ENROLMENT: return "Device enrolment / 新设备注册";
      case VEHICLE_PAIRING_APPROVAL: return "Vehicle approval / 车辆配对确认";
      case ECDHE_CONFIRMATION: return "ECDHE confirmation / 加密会话确认";
      case CERTIFICATE_EXCHANGE: return "Certificate exchange / 证书交换";
      case CATALOG_ENROLMENT: return "Catalogue enrolment / 目录注册";
      case PASSIVE_ENTRY_PROVISIONING: return "PassiveEntry provisioning / 被动进入配置";
      case P319_PAIRING: return "P319 pairing / P319 配对";
      case VEHICLE_VERIFIED: return "Vehicle verified / 实车验证";
      default: throw new AssertionError(stage);
    }
  }
}
