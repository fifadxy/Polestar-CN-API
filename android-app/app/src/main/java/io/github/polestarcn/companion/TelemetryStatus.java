package io.github.polestarcn.companion;

/** Presentation-only state of a vehicle snapshot. Not an API or a storage layer. */
public enum TelemetryStatus {
    DEMO, LIVE, CACHED, STALE, UNKNOWN;

    public static TelemetryStatus classify(boolean isDemo, String source, long capturedAt,
                                           long now, long maxAgeMillis) {
        if (isDemo) return DEMO;
        if (capturedAt <= 0 || now < capturedAt || maxAgeMillis <= 0) return UNKNOWN;
        if (now - capturedAt > maxAgeMillis) return STALE;
        if ("LIVE".equalsIgnoreCase(source)) return LIVE;
        if ("CACHED".equalsIgnoreCase(source)) return CACHED;
        return UNKNOWN;
    }

    public String description() {
        switch (this) {
            case DEMO: return "演示数据 · 不代表实际车辆状态";
            case LIVE: return "已采集车辆状态 · 请核对采集时间";
            case CACHED: return "缓存数据 · 非实时";
            case STALE: return "数据已过期 · 不得当作实时状态";
            default: return "来源或采集时间待确认";
        }
    }
}
