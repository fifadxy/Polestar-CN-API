package io.github.polestarcn.digitalkeylab;
public final class DigitalKeyCapabilityModelTest {
    public static void main(String[] args) {
        int runs = 0;
        for (int v=0; v<16; v++) {
            boolean ble=(v&1)!=0, owner=(v&2)!=0, vendor=(v&4)!=0, car=(v&8)!=0;
            DigitalKeyCapabilityModel.Status s=DigitalKeyCapabilityModel.evaluate(ble,owner,vendor,car);
            if (!ble && s!=DigitalKeyCapabilityModel.Status.NO_BLUETOOTH) throw new AssertionError("ble gate");
            if (ble && !owner && s!=DigitalKeyCapabilityModel.Status.OWNER_APPROVAL_REQUIRED) throw new AssertionError("owner gate");
            if (ble && owner && !vendor && s!=DigitalKeyCapabilityModel.Status.VENDOR_INTEGRATION_REQUIRED) throw new AssertionError("vendor gate");
            if (ble && owner && vendor && !car && s!=DigitalKeyCapabilityModel.Status.VEHICLE_PAIRING_REQUIRED) throw new AssertionError("vehicle gate");
            if (ble && owner && vendor && car && s!=DigitalKeyCapabilityModel.Status.READY_TO_VALIDATE) throw new AssertionError("validation gate");
            if (DigitalKeyCapabilityModel.canClaimWorkingDigitalKey(s)) throw new AssertionError("never assert digital key operation");
            runs++;
        }
        System.out.println("PASS: "+runs+" combinations; no operational claim");
    }
}
