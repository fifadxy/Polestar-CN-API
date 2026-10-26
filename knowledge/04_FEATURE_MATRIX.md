# 04｜Android 功能矩阵（V3.0：参考 APK / 本项目分开验收）

`REFERENCE_USER_VERIFIED`=用户亲测参考 APK，`APK_CONFIRMED`=静态代码存在，`DEMO_ONLY`=新工程仅演示，`DISABLED_IN_CURRENT_BUILD`=新工程当前不执行，`PLANNED`=未实现但已纳入路线；这些概念不能相互替代。

| 功能 | 参考 APK 可用性证据 | 当前 Android v0.3.0-alpha2 | V3 计划/验收 |
|---|---|---|---|
| China / Global 首次选择 | 参考 APK 以 China 为主；双区域是本项目增强 | 区域选择 UI（待设备验收） | P4/P6 完整区域隔离 |
| China 账号登录 | **REFERENCE_USER_VERIFIED** | `IMPLEMENTED_NEEDS_DEVICE_VERIFICATION` | **P1 高优先级，实际授权登录** |
| Token 保存/刷新 | 静态方法存在 + 用户报告误报 | `IMPLEMENTED_NEEDS_DEVICE_VERIFICATION`；Keystore + single-flight lock | **P1 高优先级，并发/过期/误报诊断** |
| China Vehicle Discovery | 有 `discoverAndRead` 等 | `IMPLEMENTED_NEEDS_DEVICE_VERIFICATION` | P1/P2 实际车单与多车验证 |
| SOC 电量 | **REFERENCE_USER_VERIFIED** | `IMPLEMENTED_NEEDS_DEVICE_VERIFICATION`；首页/Widget 使用明确标记的缓存 | **P2 真实 SOC** |
| 续航与充电 | 字段和服务线索；未逐项实测 | `IMPLEMENTED_NEEDS_DEVICE_VERIFICATION` | P2 单位/时间戳/状态 |
| 累计里程 | **REFERENCE_USER_VERIFIED** | `IMPLEMENTED_NEEDS_DEVICE_VERIFICATION`；米→公里换算 | **P2 实际里程** |
| 车身门窗/尾门状态 | APK_CONFIRMED | `IMPLEMENTED_NEEDS_DEVICE_VERIFICATION` | P2 状态回读 |
| 空调启动/停止 | **REFERENCE_USER_VERIFIED：空调控制可用**，未拆分每一操作的独立证据 | `IMPLEMENTED_NEEDS_DEVICE_VERIFICATION`；手动确认、无自动重试 | **P3 重建并逐项验证** |
| 车门解锁 | **REFERENCE_USER_VERIFIED** | `IMPLEMENTED_NEEDS_DEVICE_VERIFICATION`；手动确认、无自动重试 | **P3 重建并确认车辆状态** |
| 车门上锁 | APK_CONFIRMED | DISABLED_IN_CURRENT_BUILD | P3 单项验证 |
| 车窗/尾门控制 | APK_CONFIRMED | DISABLED_IN_CURRENT_BUILD | P3 按车型与 API 验证 |
| 鸣笛/闪灯/空气净化 | APK_CONFIRMED | DISABLED_IN_CURRENT_BUILD | P3 按车型与 API 验证 |
| 命令状态与 Token 错误区分 | 参考 APK 有状态回读相关方法；用户报告误报 | `SENDING/SERVER_ACCEPTED/FAILED/UNCONFIRMED` 已接 UI；车辆确认待设备回读 | P1/P3 `SERVER_ACCEPTED` 和 `VEHICLE_CONFIRMED` 分开 |
| 原版 UI/手势/车辆图 | MainActivity 动态 UI 与资源线索 | 自研简洁 DEMO UI | P4 同设备截图和手势对照 |
| 标准/紧凑 Widget | APK 有 Provider/Configure/Control/Refresh 类 | China 使用缓存快照，Global 为 DEMO；无后台联网 Worker | P5 添加、刷新、快捷控制实测 |
| WorkManager 后台刷新 | 原 APK WorkManager 线索 | 未联网、无持久定时同步 | P5 后台/Doze/缓存效期 |
| 露营模式 Worker | APK_CONFIRMED | 未实现 | P5 单独评估与验证；不作为人宠安全保障 |
| C3 Live Stream | 参考 APK 未确认 | 未实现 | P6；不阻塞 P3 |
| Global 登录与车辆数据 | 本项目扩展，对照 Hisingen | 只有区域 DEMO UI | P6 分区域独立验证 |
| 独立源码与构建 | 不适用（参考 APK 是二进制） | v0.3.0-alpha2 `LOCAL_BUILD_VERIFIED`；v2 签名和 ZIP 完整性通过 | P7 后续新版本分别构建/验收 |

**V3.0 是开发总计划版本，不是已经完成的 APK 版本号。** `knowledge/13` 记录参考 APK 用户实测，`knowledge/15` 记录 Token/车控验收，`knowledge/14` 为完整开发总计划。

## v0.4.0-beta1 增量（覆盖上表当前实现列）

区域弹窗已移除，默认中国区真实登录/车控；手机号密码请求沿用真实服务协议，模板支持一次性 SAF 导入并本机加密。空调启停/门锁/尾箱/净化均实现并需系统强生物识别。新增中英文、日夜模式、英文 Widget、可选常驻通知及 JobScheduler 三十分钟刷新；上述接口仍为 IMPLEMENTED_NEEDS_DEVICE_VERIFICATION，未提升为实车已验证。
