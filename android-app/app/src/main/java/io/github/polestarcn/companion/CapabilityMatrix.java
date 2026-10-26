package io.github.polestarcn.companion;

public final class CapabilityMatrix {
    public enum Status { DEMO_ONLY, MAPPING_REQUIRED, VERIFIED_LIVE, DISABLED }
    public static final boolean ENABLE_NETWORK_VEHICLE_API = false;
    public static final boolean ENABLE_REMOTE_COMMANDS = false;
    private CapabilityMatrix() {}
    public static Status vehicleDiscovery(Region region) { return Status.MAPPING_REQUIRED; }
    public static Status battery(Region region) { return Status.DEMO_ONLY; }
    public static Status exterior(Region region) { return Status.DEMO_ONLY; }
    public static Status odometer(Region region) { return Status.DEMO_ONLY; }
    public static Status liveStream(Region region) { return Status.MAPPING_REQUIRED; }
    public static Status remoteCommands(Region region) { return Status.DISABLED; }
}
