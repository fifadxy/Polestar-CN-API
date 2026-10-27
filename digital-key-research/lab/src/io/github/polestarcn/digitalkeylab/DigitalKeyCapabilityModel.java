package io.github.polestarcn.digitalkeylab;

/** Non-operational research state machine. No Bluetooth or vehicle commands. */
public final class DigitalKeyCapabilityModel {
    public enum Status {
        NO_BLUETOOTH, OWNER_APPROVAL_REQUIRED,
        VENDOR_INTEGRATION_REQUIRED, VEHICLE_PAIRING_REQUIRED,
        READY_TO_VALIDATE
    }
    private DigitalKeyCapabilityModel() {}
    public static Status evaluate(boolean ble, boolean ownerApproved,
                                  boolean vendorAuthorized, boolean vehicleRegistered) {
        if (!ble) return Status.NO_BLUETOOTH;
        if (!ownerApproved) return Status.OWNER_APPROVAL_REQUIRED;
        if (!vendorAuthorized) return Status.VENDOR_INTEGRATION_REQUIRED;
        if (!vehicleRegistered) return Status.VEHICLE_PAIRING_REQUIRED;
        return Status.READY_TO_VALIDATE;
    }
    /** Requires separate real-vehicle verification; never assert key availability from lab state. */
    public static boolean canClaimWorkingDigitalKey(Status status) { return false; }
}
