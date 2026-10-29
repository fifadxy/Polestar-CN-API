# v0.9.5-beta2 露营托管 Hotfix 知识条目
2026-10-29。以 `docs/Psa_v095_beta2_Hotfix_Camping_Recovery.md` 为完整规范。

- 远程停止与本地停止必须分离，失联时本地立即停止，远程仍显示待核实。
- 退出登录不等待网络；清空会话时保留加密 pending-stop VIN，取消无会话的调度。新登录必须同 VIN 才可核实。
- 恢复互联网后先判断有效登录、读取实时车辆状态（LIVE、未过期、VIN 一致），然后才决定是否需要处理空调/净化停止。
- 离线网络错误不能等于 Token 拒绝；服务器 401/403 或明确拒绝刷新后必须重新认证，不规避服务端会话规则。
- 托管超时、低电自动结束不得丢弃尚未确认的车端停止责任；`remote_stop_pending` 只能在可靠回读成功后关闭。
- 恢复 UI 两个步骤：`RECOVERING_REFRESH`（1/2）、`RECOVERING_VERIFY`（2/2）。
- 通过 `CampRecoveryGateTest`、`CampRecoveryHotfixTest`、`CampEngineTest` 约束逻辑；真实中国区登录、实车远控和系统后台 Doze 必须另外验证。
