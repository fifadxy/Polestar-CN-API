package io.github.polestarcn.companion;

/** Proposed remote-command result vocabulary. No transport and no command execution. */
public enum OperationState {
    DISABLED("未开放"),
    NOT_SENT("未发送"),
    SENDING("发送中"),
    SERVER_ACCEPTED("服务端已接受，车辆未确认"),
    VEHICLE_CONFIRMED("车辆状态已确认"),
    FAILED("执行失败"),
    UNCONFIRMED("超时或结果未确认");

    public final String label;
    OperationState(String label) { this.label = label; }

    /** An RPC acknowledgement alone is not evidence that a vehicle acted. */
    public boolean isVehicleSuccess() {
        return this == VEHICLE_CONFIRMED;
    }

    /** Writes must never be retried automatically until idempotency is established. */
    public boolean canAutomaticallyRetry() {
        return false;
    }
}
