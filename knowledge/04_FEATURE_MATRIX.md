# 04｜Android 功能矩阵（V3.0：参考 APK / 本项目分开验收）

`REFERENCE_USER_VERIFIED`=用户亲测参考 APK，`APK_CONFIRMED`=静态代码存在，`DEMO_ONLY`=新工程仅演示，`DISABLED_IN_CURRENT_BUILD`=新工程当前不执行，`PLANNED`=未实现但已纳入路线；这些概念不能相互替代。

| 功能 | 参考 APK 可用性证据 | 当前 Android v0.1.0-dev2 | V3 计划/验收 |
|---|---|---|---|
| China / Global 首次选择 | 参考 APK 以 China 为主；双区域是本项目增强 | 区域选择 UI（待设备验收） | P4/P6 完整区域隔离 |
| China 账号登录 | **REFERENCE_USER_VERIFIED** | 未实现 | **P1 高优先级，实际授权登录** |
| Token 保存/刷新 | 静态方法存在 + 用户报告误报 | 未实现 | **P1 高优先级，并发/过期/误报诊断** |
| China Vehicle Discovery | 有 `discoverAndRead` 等 | 演示车单 | P1/P2 实际车单与多车验证 |
| SOC 电量 | **REFERENCE_USER_VERIFIED** | DEMO_ONLY | **P2 真实 SOC** |
| 续航与充电 | 字段和服务线索；未逐项实测 | DEMO_ONLY | P2 单位/时间戳/状态 |
| 累计里程 | **REFERENCE_USER_VERIFIED** | DEMO_ONLY | **P2 实际里程** |
| 车身门窗/尾门状态 | APK_CONFIRMED | DEMO_ONLY | P2 状态回读 |
| 空调启动/停止 | **REFERENCE_USER_VERIFIED：空调控制可用**，未拆分每一操作的独立证据 | DISABLED_IN_CURRENT_BUILD | **P3 重建并逐项验证** |
| 车门解锁 | **REFERENCE_USER_VERIFIED** | DISABLED_IN_CURRENT_BUILD | **P3 重建并确认车辆状态** |
| 车门上锁 | APK_CONFIRMED | DISABLED_IN_CURRENT_BUILD | P3 单项验证 |
| 车窗/尾门控制 | APK_CONFIRMED | DISABLED_IN_CURRENT_BUILD | P3 按车型与 API 验证 |
| 鸣笛/闪灯/空气净化 | APK_CONFIRMED | DISABLED_IN_CURRENT_BUILD | P3 按车型与 API 验证 |
| 命令状态与 Token 错误区分 | 参考 APK 有状态回读相关方法；用户报告误报 | OperationState 仅纯模型 | P1/P3 `SERVER_ACCEPTED` 和 `VEHICLE_CONFIRMED` 分开 |
| 原版 UI/手势/车辆图 | MainActivity 动态 UI 与资源线索 | 自研简洁 DEMO UI | P4 同设备截图和手势对照 |
| 标准/紧凑 Widget | APK 有 Provider/Configure/Control/Refresh 类 | DEMO_WIDGET，CI 已编译 | P5 添加、刷新、快捷控制实测 |
| WorkManager 后台刷新 | 原 APK WorkManager 线索 | 未联网、无持久定时同步 | P5 后台/Doze/缓存效期 |
| 露营模式 Worker | APK_CONFIRMED | 未实现 | P5 单独评估与验证；不作为人宠安全保障 |
| C3 Live Stream | 参考 APK 未确认 | 未实现 | P6；不阻塞 P3 |
| Global 登录与车辆数据 | 本项目扩展，对照 Hisingen | 只有区域 DEMO UI | P6 分区域独立验证 |
| 独立源码与构建 | 不适用（参考 APK 是二进制） | `CI_VERIFIED` 历史 v0.1.0-dev2 | P7 后续新版本分别构建/验收 |

**V3.0 是开发总计划版本，不是已经完成的 APK 版本号。** `knowledge/13` 记录参考 APK 用户实测，`knowledge/15` 记录 Token/车控验收，`knowledge/14` 为完整开发总计划。
