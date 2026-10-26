package io.github.polestarcn.companion;

import io.github.polestarcn.companion.api.ProtoWire;

/** JVM-only logic checks; does not require Android SDK, credentials or a vehicle. */
public final class ModelStateTest {
  private static void check(boolean condition, String name) {
    if (!condition) throw new AssertionError(name);
  }

  private static String hex(byte[] b) {
    StringBuilder s = new StringBuilder();
    for (byte x : b) s.append(String.format("%02x", x & 255));
    return s.toString();
  }

  public static void main(String[] ignored) {
    check(
        TelemetryStatus.classify(true, "DEMO / OFFLINE", 0L, 100L, 10L) == TelemetryStatus.DEMO,
        "demo");
    check(TelemetryStatus.classify(false, "LIVE", 90L, 100L, 20L) == TelemetryStatus.LIVE, "live");
    check(TelemetryStatus.classify(false, "LIVE", 90L, 100L, 5L) == TelemetryStatus.STALE, "stale");
    check(
        TelemetryStatus.classify(false, "CACHED", 90L, 100L, 20L) == TelemetryStatus.CACHED,
        "cached");
    check(
        TelemetryStatus.classify(false, "LIVE", 110L, 100L, 20L) == TelemetryStatus.UNKNOWN,
        "future timestamp");
    check(!OperationState.SERVER_ACCEPTED.isVehicleSuccess(), "RPC accepted != vehicle success");
    check(OperationState.VEHICLE_CONFIRMED.isVehicleSuccess(), "confirmation");
    for (OperationState state : OperationState.values()) {
      check(!state.canAutomaticallyRetry(), "no automatic write retry");
    }
    check(VehicleSnapshot.metric(null, "%").equals("--"), "null is unknown, not zero");
    byte[] envelope = ProtoWire.bytesField(1, ProtoWire.stringField(1, "TEST"));
    check(hex(envelope).equals("0a060a0454455354"), "reference invocation VIN field 1/1");
    check(
        hex(ProtoWire.stringField(2, "TEST")).equals("120454455354"),
        "reference state VIN field 2");
    check(
        hex(ProtoWire.concat(envelope, ProtoWire.varintField(2, 1))).endsWith("1001"),
        "tailgate open enum=1");
    check(
        hex(ProtoWire.concat(envelope, ProtoWire.varintField(2, 2))).endsWith("1002"),
        "tailgate close enum=2");
    check(
        hex(ProtoWire.fixed32Field(3, 22f)).equals("1d0000b041"), "climate temperature float32 LE");
    try {
      ProtoWire.decode(new byte[] {18, 127});
      throw new AssertionError("truncated data accepted");
    } catch (IllegalArgumentException expected) {
    }
    check(
        ProtoWire.enumText(ProtoWire.decode(new byte[] {16, 99}), 2, "未知", "开启", "关闭").equals("未知"),
        "unknown enum is not off");
    VehicleSnapshot climate =
        new VehicleSnapshot(
            "test", "masked", null, null, null, "unknown", "unknown", "unknown", "unknown",
            "unknown", 0, "CACHED", false, "开启", "关闭");
    check(climate.climate.equals("开启") && climate.air.equals("关闭"), "climate/air independent");
    System.out.println("PASS: pure-Java data provenance and operation state invariants");
  }
}
