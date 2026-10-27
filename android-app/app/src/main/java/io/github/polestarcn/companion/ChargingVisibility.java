package io.github.polestarcn.companion;

import java.util.Locale;

/**
 * Presentation-only gate for the dedicated charging section.
 *
 * Battery charge state is authoritative. Connected cable, watts, amps or a supplying-power
 * reading alone do not establish charging: those fields can remain populated after charging.
 * A connected, initializing supply can represent the start handshake while the main state is
 * idle/unspecified. Explicit scheduled, paused, completed and failed states take precedence.
 * This class never changes telemetry, refreshes it or issues a vehicle command.
 */
public final class ChargingVisibility {
  static final long FRESHNESS_WINDOW_MILLIS = 15 * 60_000L;

  private ChargingVisibility() {}

  public static boolean shouldShow(VehicleSnapshot snapshot) {
    return shouldShow(snapshot, System.currentTimeMillis());
  }

  static boolean shouldShow(VehicleSnapshot snapshot, long now) {
    if (snapshot == null) return false;
    TelemetryStatus freshness = TelemetryStatus.classify(snapshot.isDemo, snapshot.source,
        snapshot.timestampMillis, now, FRESHNESS_WINDOW_MILLIS);
    if (freshness != TelemetryStatus.LIVE && freshness != TelemetryStatus.CACHED) return false;

    String state = snapshot.charging == null ? ""
        : snapshot.charging.trim().toLowerCase(Locale.ROOT);
    switch (state) {
      case "充电中":
      case "智能充电":
      case "charging":
      case "smart charging":
      case "开始充电":
      case "正在开始充电":
      case "充电启动中":
      case "正在启动充电":
      case "充电初始化中":
      case "starting charging":
      case "initializing charging":
        return true;
      case "":
      case "未知":
      case "未指定":
      case "空闲":
      case "unknown":
      case "unspecified":
      case "idle":
        ChargingSnapshot details = snapshot.chargingDetails;
        return Integer.valueOf(1).equals(details.connection)
            && Integer.valueOf(2).equals(details.powerStatus);
      default:
        // Includes scheduled/complete/paused/discharging/fault and unrecognized states.
        return false;
    }
  }

  public static boolean isChargingMetric(DisplayMetric metric) {
    if (metric == null) return false;
    switch (metric) {
      case CHARGE_CONNECTION:
      case CHARGE_TYPE:
      case CHARGE_POWER:
      case CHARGE_CURRENT:
      case CHARGE_ETA:
      case POWER_STATUS:
        return true;
      default:
        return false;
    }
  }
}
