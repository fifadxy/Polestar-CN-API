package io.github.polestarcn.companion;

/** These values are fictional demonstration data; they must never be presented as live telemetry. */
public final class DemoRepository implements VehicleRepository {
    @Override public VehicleSnapshot snapshot(Region region) {
        boolean china = region == Region.CHINA;
        return new VehicleSnapshot(
            china ? "Polestar 2 · CN DEMO" : "Polestar 2 · Global DEMO",
            china ? "DEMO-CN-0001" : "DEMO-GL-0001",
            china ? 72 : 81, china ? 300 : 356, china ? 12840 : 22360,
            "未连接充电枪（模拟）", "全部关闭（模拟）", "全部关闭（模拟）",
            "关闭（模拟）", "已上锁（模拟）", System.currentTimeMillis(),
            "DEMO / OFFLINE", true
        );
    }
    @Override public boolean usesLiveData() { return false; }
}
