package io.github.polestarcn.companion;

/** JVM-only logic checks; does not require Android SDK, credentials or a vehicle. */
public final class ModelStateTest {
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
    public static void main(String[] ignored) {
        check(TelemetryStatus.classify(true, "DEMO / OFFLINE", 0L, 100L, 10L) == TelemetryStatus.DEMO, "demo");
        check(TelemetryStatus.classify(false, "LIVE", 90L, 100L, 20L) == TelemetryStatus.LIVE, "live");
        check(TelemetryStatus.classify(false, "LIVE", 90L, 100L, 5L) == TelemetryStatus.STALE, "stale");
        check(TelemetryStatus.classify(false, "CACHED", 90L, 100L, 20L) == TelemetryStatus.CACHED, "cached");
        check(TelemetryStatus.classify(false, "LIVE", 110L, 100L, 20L) == TelemetryStatus.UNKNOWN, "future timestamp");
        check(!OperationState.SERVER_ACCEPTED.isVehicleSuccess(), "RPC accepted != vehicle success");
        check(OperationState.VEHICLE_CONFIRMED.isVehicleSuccess(), "confirmation");
        for (OperationState state: OperationState.values()) {
            check(!state.canAutomaticallyRetry(), "no automatic write retry");
        }
        check(VehicleSnapshot.metric(null, "%").equals("--"), "null is unknown, not zero");
        System.out.println("PASS: pure-Java data provenance and operation state invariants");
    }
}
