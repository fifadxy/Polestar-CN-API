# PsAssistant v0.9.5-beta2 — 露营托管、认证与网络恢复 Hotfix
日期：2026-10-29。基线：v0.9.5-beta1，开发分支：`hotfix/android-v095-beta2-20261029`。

## 问题和修复
1. **断网、服务器拒绝刷新导致托管无法停止**：手动停止仅改变安全持久化的本地状态，不等待网络；记录 `remote_stop_pending` 和加密 VIN。结束托管不等于车端空调已经停止。
2. **退出登录锁死**：`VehicleCoordinator.logout()` 不再等待远程网络任务。先停止本地托管、撤销当前会话，再清空普通车况。若远程停止待核实，保留其加密记录，取消未经认证的后台远程核实计划；必须重新登录原车辆才可能恢复。
3. **令牌刷新与会话保持**：`SecureSessionStore` 将 access / refresh token 加密保存；`ChinaPolestarApi.refreshIfNeeded` 在服务端允许时刷新。网络故障与 401/403 拒绝严格区分。服务端明确拒绝续期时要求用户再次登录；不绕过期限或重复提交密码。
4. **恢复网络流程**：联网验证 -> 登录会话验证 -> 重新读取同一辆车（VIN 必须匹配、读成功、`LIVE` 且新鲜）-> 核实托管状态 -> 在确有必要时处理车端关闭。每个远控动作都受只读核实及已尝试标记约束；不会自动无限重试。
5. **自动结束丢失未确认状态**：托管到期或电量低于下限时，`CampModeManager.deactivate` 保留远程停止待核实记录；只有成功回读确认后清除 pending 标志。
6. **恢复进度**：`RECOVERING_REFRESH`（1/2：刷新车况）与 `RECOVERING_VERIFY`（2/2：核实停止）；失败时转为 `WAITING_FOR_NETWORK`、`LOGIN_REQUIRED` 或未确认。

## 状态语义
| 本地状态 | 远程状态 | 允许操作 |
|---|---|---|
| ACTIVE | 待刷新/托管中 | 新鲜车况合格才检查或维持 |
| STOPPED + remote_stop_pending | 未知或可能运行 | 允许退出登录，不自动宣称车端已关闭 |
| STOPPED + remote_stop_pending + LOGIN_REQUIRED | 服务器会话无效 | 仅允许用户重新认证；不得发送远控 |
| STOPPED + remote_stop_pending + WAITING_FOR_NETWORK | 网络不可用 | 不做远程写入；保留加密数据 |
| STOPPED + remote_stop_pending + RECOVERING_REFRESH | 正在刷新真实车况 | 禁止重复写入 |
| STOPPED + remote_stop_pending + RECOVERING_VERIFY | 同一 VIN 已新鲜回读 | 按车端状态只处理尚未确认关闭的功能 |
| STOPPED + REMOTE_STOP_CONFIRMED | 真实车况确认关闭 | 取消重试任务 |

## 限制与安全约束
- 没有互联网时无法保证车端空调关闭；用户必须有使用车内物理控制或官方 App 检查的途径。
- Android JobScheduler 的联网任务在 Doze、系统重启及后台限制下可能延迟，并非实时保障。
- 车端所有写操作均可能在回包丢失时存在不确定性；不得按超时自动重复写同一指令。
- 重新登录后只接受**同一 VIN**且认证成功的实时车辆回读，绝不向其他车辆自动发原托管命令。
- 离线退出是本地操作，旧会话已清除；再次联网时不擅自复活被用户停止的托管任务。
- 开始新托管前必须先解决旧的 pending-stop 记录；防止覆盖未完成的停止义务。

## 验证记录
- 增加 `CampRecoveryGateTest`：同车实时读取允许恢复，异车、缓存、过期、演示数据、失效登录或读取失败均阻断。
- 既有 `CampRecoveryHotfixTest`：网络超时不触发登出，401/403 拒绝才提示重新认证。
- 既有 `CampEngineTest`：低电、到期、超时、取消等安全边界。
- 公共 CI 只能验证单元测试、Lint 和 APK 编译；中国区完整私有登录配置、实车联网/断网、远程空调停止仍需线下验收。

## 建议真机用例
1. 开启托管 -> 开启飞行模式 -> 点击结束托管；本地立即结束，提示远程未核实。
2. 断网托管 -> 退出登录；界面立即退出，密钥删除，待核实加密记录保留。
3. 重新登录其他 VIN；确保不发旧车指令。
4. 重新登录同一 VIN 后恢复联网；必须先新鲜回读，再进行单次必要的停止操作。
5. refresh token 被服务端拒绝；显示重新认证而不重复登录或自动提交密码。
6. 低电或到期同时断网；pending-stop 必须保留直至回读确认。
7. 复测多类并行指令；不能造成露营托管状态机竞争或登录界面死锁。
