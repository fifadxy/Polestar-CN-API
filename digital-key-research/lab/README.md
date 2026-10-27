# Pure-Java Research Gate

本目录只含**无网络、无蓝牙、无实车命令**的能力判定模型和 16 组测试。目标是把“有蓝牙”、“车主批准”、“厂商 SDK 授权”和“车端注册”严格分成不同前置条件。

运行：`sh digital-key-research/scripts/run_lab_tests.sh`（JDK 17）。

模型的 `READY_TO_VALIDATE` 只表示允许进行后续验证，**不代表钥匙已经有效**；`canClaimWorkingDigitalKey` 在没有另一个独立实车证据系统前始终为 false。