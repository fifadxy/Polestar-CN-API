# PsAssistant Beta45：OPPO 系统兼容 · 流体云 & Indicator 全功能测试

版本：`v1.0.0-beta45`（159）｜ Android 主分支目标：`android-polestar-assistant-main`｜2026-11-05

## 1. 为什么区分 1.0 与 2.0

用户提供 OPPO 官方流体云页面：https://open.oppomobile.com/documentation/page/info?id=12658 。官方 OPPO Pantanal `IntelligentIntent` 需要业务/服务申请、正式 `launcher` / `fluidCloud` serviceId、client_id/secret 和有效意图场景许可，不能用自编本地通知伪造这种接入。在没有上述授权材料的情况下，本次提供两条**真实可执行的兼容实验路径**：

- **1.0 标准通知兼容**：Android 通知渠道 + 普通 ongoing 与短时事件通知，供 ColorOS 自行决定是否显示为胶囊；不是 OPPO 官方 1.0 私有模板接入。
- **2.0 Android 16 实时动态请求**：仅运行中的、由本程序发起并经车辆回读确认的空调/净化活动，以及明确启动的露营托管，尝试通过 Android 16 `Notification.Builder.setRequestPromotedOngoing` 和 `setShortCriticalText` 请求系统提升；`compileSdk=35` 使用 API 36 运行时反射，低版本自动降级为普通通知。系统可能拒绝；**没有 OPPO 平台审核，不能保证是真正的流体云 2.0**。
- **自动**：在 Android 16 上符合条件时尝试 2.0，否则降级为 1.0。
- **所有测试**：仅创建本机通知与预览，绝不通过模拟通知触发车辆联网、系统权限提升或远程控制。

官方参考：Android 实时动态通知 https://developer.android.com/develop/ui/views/notifications/live-update ，OPPO 官方服务页如上，第三方通道必要条件 https://help.aliyun.com/zh/document_detail/2997310.html 。

## 2. UI：设置 → OPPO 系统兼容

统一设置入口：`OPPO 系统兼容 · CarLink / LSPosed / 流体云 / Indicator`：

- **CarLink / 设备空间 / LSPosed**：保留原设备卡、系统绑定和 Hook 诊断；不得将虚拟会话等同官方授权。
- **OPPO Watch Indicator**：仍使用原 Beta44 手机实验通知和独立手表 APK；手表 Indicator 只有手表上的 App 发出符合条件的通知才能由表盘评估。
- **流体云主开关（默认关闭）**。
- **兼容通道（自动 / 1.0 / 2.0）**；**展示密度（简洁 VI / 全功能）**；**一级 / 二级**；**二级操作总开关（默认关闭）**。
- 11 类场景均可分别选择 **发送通知**、**尝试流体云展示**、**允许二级操作**（场景无操作则不展示操作开关）。「流体云」需要该场景通知开关先开启。
- 独立设置：上锁/解锁成功通知 5/10/15/20/30 秒，默认 10 秒；故障重复通知冷却 15/30/60/120/1440 分钟，默认 60 分钟。
- 一键关闭总开关可撤销所有本功能发布的通知，不影响用户常驻服务、远控和系统其他通知。

### 场景与测试

| 场景 ID | 实际通知触发规则 | 二级操作候选 | 模拟测试 |
|---|---|---|---|
| lock | 必须原远控结果回读 `VEHICLE_CONFIRMED` | 解锁 | ✓ |
| unlock | 必须原远控结果回读 `VEHICLE_CONFIRMED` | 上锁 | ✓ |
| climate | 已读取真实开启状态、运行时长，或本 App 已确认的本地估计 | 空调停止/启动 | ✓ |
| air | 已读取真实开启状态；倒计时采用本地计时 | 净化停止/启动 | ✓ |
| camp | 本 App 已明确启动托管；剩余时间来自受保护的本地任务 | 停止托管 | ✓ |
| charging | 充电状态来自真实车辆报告，默认关闭主动通知 | 无远程充电控制 | ✓ |
| fault | 只对实际回报的门窗、胎压、12V、保养等可识别异常通知；不猜测 kPa | 无 | ✓ |
| status | 电量/续航/车况快照，默认关闭主动通知 | 上锁/空调启动（需开启） | ✓ |
| trunk | 仅真实远控回读确认后的事件，默认关闭 | 开/关尾箱 | ✓ |
| windows | 仅真实远控回读确认后的事件，默认关闭 | 开/关车窗 | ✓ |
| find | 仅测试默认关闭；鸣笛/闪灯无车端确认字段，**不发送「已成功执行」通知** | 鸣笛/闪灯 | ✓ |

一级胶囊采用简洁 VI 橙色小图标与状态短语；二级卡片使用 Android 原生展开通知、详情及**受保护的 App 内控制按钮**，不依赖未获批的 OPPO 远控私有 SDK。实际是否成为 ColorOS 设备流体云胶囊由系统和官方接入许可决定。

## 3. 全项目实验测试

- **单项测试**：11 个场景各有按钮。会生成带「【模拟】」的通知，90 秒后结束；测试二级按钮只返回测试页并显示「未发送车辆指令」。
- **一键全部通知**：依次发出全部 11 项模拟通知，不修改真实状态、不申请刷新、不启动服务、不发任何车辆控制命令。
- **组合验证**：11 场景 × 2 路线（1.0/2.0）× 2 密度（简洁/全功能）× 2 层级（一级/二级） = **88 种构造测试**，本地检查必要字段、Action 和样式，不一次向通知栏推送 88 条内容。
- **诊断**：通知权限、Android 版本、Android 16 是否允许推广通知、当前路由、模式、层级、动作开关、最后一次模拟测试。兼容检测不代表系统实际上显示了胶囊。

## 4. 真车/权限安全

1. 对锁定和解锁事件，仅在 `CommandReadback` 实际回报且 `OperationState.VEHICLE_CONFIRMED` 时发送成功提示；`SERVER_ACCEPTED`、`UNCONFIRMED` 和模拟状态都不能标记成功。
2. 二级按钮 `PendingIntent` 只打开主程序。主程序总是再次显示确认对话框；随后执行原本的生物识别/确认策略、VIN 检查和指令结果回读；默认总开关和每场景操作均关闭。
3. 露营倒计时采用本地任务，停止后如远端状态未核实，显示「本地停止、车端待确认」。不使用普通车况缓存冒充真实停止结果。
4. 轮胎、蓄电池、门窗问题基于已有 `VehicleSnapshot` / C3 枚举，未上报或缺失数据不得猜测、不得生成实际故障；通知按故障签名和冷却时长去重。
5. 本通知系统仅读取已有 `VehicleSnapshotStore`，不新建额外车辆轮询或任何后台长连接；空调/净化剩余时间可使用 Android 系统原生倒计时展示，避免每秒唤醒。
6. 在 ColorOS 16/17 或 Android 16 实机检查真实状态栏胶囊、锁屏、下拉二级按钮及权限；没有官方批准的 `serviceId` 时不可宣称已打通 Pantanal 2.0。**本版本并未接入 OPPO 官方推送 API。**

## 5. 实施文件

`OppoFluidPolicy.java`、`OppoFluidScene.java`、`OppoFluidCloud.java`、`OppoVehicleIssues.java`、`OppoSystemCompatActivity.java`、`OppoFluidCloudBeta45Test.java`；变更 `MainActivity.java`、`VehicleCoordinator.java`、`VehicleSnapshotStore.java`、`CampModeManager.java`、`ResidentService.java`、`UpdateHistoryActivity.java`、Manifest、Gradle、CI。

未改变 Beta44 原有 LSPosed Hook / CarLink 业务协议、登录或授权材料。CI 编译并测试后才可交付 Debug 验证 APK；FULL 独立登录版仍需要复用私有签名与合法登录客户端配置，并单独校验。
