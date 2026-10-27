# 08｜Android Polestar Assistant 主线开发路线图（V3.0）

主线 `android-polestar-assistant-main`，基线 `feature/android-companion-v0.1`；本轮同步**计划与知识库**，源码仍为 `v0.1.0-dev2 DEMO/OFFLINE`，旧版 CI Run `40261229408` 已通过。真车数据和远控在新项目均**未实现**。

| 优先级 | 阶段 | 工作内容 | 必须验收 | 状态 |
|---|---|---|---|---|
| P0 | 参考 APK 深度分析 | DEX/资源/动态 Java UI、HTTP/JSON、Proto/RPC、Token、远控、Widget、后台方法级核对 | 可追溯方法-字段-请求-界面映射，未知点清晰 | `PLANNED`（静态基础已入库） |
| P1 | 中国区真实登录 | ChinaAuth、TokenRefreshCoordinator、Session、Vehicle Discovery | 用户授权登录、自动续期、报错分类、车单匹配 | `NOT_IMPLEMENTED` |
| P2 | 中国区车辆只读 | SOC、里程、续航、充电、Exterior、状态新鲜度 | 实车对照和字段级单位/时间戳 | `DEMO_ONLY` |
| P3 | 远程控制 | 首先空调与车门解锁，接着逐项扩展上锁、车窗、尾门、鸣笛闪灯、净化 | 用户主动确认、服务端受理/车端执行分离、未确认不自动重发 | `DISABLED_IN_CURRENT_BUILD` / **核心计划** |
| P4 | UI 一比一复刻 | 车辆图、布局、VI、手势、状态卡片、设置、控制反馈 | 同设备截图对照和交互测试 | `DEMO_ONLY` |
| P5 | Widget/后台 | Widget 车辆配置、状态更新、快捷控制、WorkManager/Doze | 实机添加/锁屏/重启/省电场景 | `DEMO_ONLY` |
| P6 | 双区域与扩展 | China/Global 隔离、C3 Live、PCCS，逐车型能力 | 各区域自身授权账号、车型、RPC 独立回归 | `MAPPING_REQUIRED` |
| P7 | 构建与交付 | JVM 测试、知识库检查、CI 编译、运行实测、缺陷与日志 | APK SHA/commit/CI run/分项验收记录 | 旧版 CI 已通过，新版待做 |

**依赖**：P0/P4 可以并行；P1/P2 保障 P3 的正确性；P3 不等待 P6 完成；P7 每阶段持续执行。不得把第三方参考 APK 的 `REFERENCE_USER_VERIFIED` 写为本项目已实车验证。

**功能优先级**：首个可用里程碑聚焦 China 登录、SOC、里程、空调控制、车门解锁、Token 误报；再补完整 UI/Widget 和其他可用的远控操作；最后 Global/C3 Live/PCCS 拓展。

**持续约束**：R8、混淆、可选安全加固和正式发布适配 `DEFERRED`；但会话隔离、用户确认、敏感日志脱敏、不会重复下发不确定写命令是必需的正确性保障。参见 `14_ANDROID_PARITY_V3_MASTER_PLAN.md`、`15_TOKEN_AND_REMOTE_VALIDATION.md` 和 `16_ANDROID_BRANCH_GOVERNANCE.md`。


## 2026-10-27 v0.7.8 当前增量

本轮实现车况/保养、NIOlike2、字体与地图二级预览。下一验收：参考同一车辆对照 GetLatestHealth、剩余保养语义、停车坐标偏移、高德 Key/额度、TA 独立能量与时长估算；随后实测 OPPO Watch X2 及耗电。不得把未知字段补成正常。 具体来源、字段和限制见 [v0.7.8 车况、位置与行程](25_V078_CONDITION_LOCATION_TRIP.md)。
