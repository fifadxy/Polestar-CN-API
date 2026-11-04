package io.github.polestarcn.companion;

/** Adapter seam: DemoRepository now; region-specific API adapters only after validation. */
public interface VehicleRepository {
    VehicleSnapshot snapshot(Region region);
    boolean usesLiveData();
}
