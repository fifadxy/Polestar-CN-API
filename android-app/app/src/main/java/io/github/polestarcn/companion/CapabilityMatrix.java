package io.github.polestarcn.companion;

public final class CapabilityMatrix {
    public enum Status { DEMO_ONLY, MAPPING_REQUIRED, DEVICE_VERIFICATION, VERIFIED_LIVE, DISABLED }
    public static final boolean ENABLE_NETWORK_VEHICLE_API = true;
    public static final boolean ENABLE_REMOTE_COMMANDS = true;
    private CapabilityMatrix() {}
    public static Status vehicleDiscovery(Region region) { return china(region); }
    public static Status battery(Region region) { return china(region); }
    public static Status exterior(Region region) { return china(region); }
    public static Status odometer(Region region) { return china(region); }
    public static Status liveStream(Region region) { return Status.MAPPING_REQUIRED; }
    public static Status remoteCommands(Region region) { return region == Region.CHINA ? Status.DEVICE_VERIFICATION : Status.DISABLED; }
    public static Status windows(Region region) { return region == Region.CHINA ? Status.DEVICE_VERIFICATION : Status.DISABLED; }
    public static Status honkFlash(Region region) { return region == Region.CHINA ? Status.DEVICE_VERIFICATION : Status.DISABLED; }
    public static Status maintenance(Region region) { return region == Region.CHINA ? Status.VERIFIED_LIVE : Status.DISABLED; }
    private static Status china(Region region) { return region == Region.CHINA ? Status.DEVICE_VERIFICATION : Status.MAPPING_REQUIRED; }
}
