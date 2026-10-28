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


## 2026-10-27 v0.7.9 免 Key 地图增量

当前位置方案改为用户明确选择的内嵌高德官方 URI 网页，移除 staticmap Key 依赖并清理旧配置。参考 APK 仍证明 field1=经度、field2=纬度，未采用外部 AI 文本中的反序定义。c3Lbs 4秒截止、失败隔离及切车/退出结果丢弃已补强；网页需网络和系统 WebView，手机底图/坐标精度仍待实车验收。见 [v0.7.9 知识库](26_V079_KEYLESS_PARKING_WEB.md)。


## 2026-10-27 v0.8.3 当前增量

基于最新v0.8.2/9744759，恢复PS默认并保留VoLike新布局；自动保养只用Health剩余值，历史手动记录不再补充显示。地图共用精简网页，并新增默认关闭的手机前台位置授权；无后台定位。所有Widget改用原生文字/按钮与等比车图，新增PSeu、自选数据、统一粗体/字号及精确尺寸适配。按钮资源和后续迭代参考Psa图标规范v1。协议/实车证据标签不因此升级。详见 [v0.8.3统一知识](28_V083_UI_WIDGET_MAP_ICONS.md)。


## v0.8.4 当前增量 · 2026-10-27

[KB29](29_V084_ICONS_SERVICE_TARGETSOC_READBACK.md)与[更新说明](../docs/Psa_v084_Update.md)记录新母版/昼夜通知、30000km与合并保养周期、C3 TargetSoC只读字段、首次协议、5/10/15/20秒四次回读和非阻塞等待。历史20000km、单次15秒回读与黑武士启动器约定被本轮授权替代。实车TargetSoC/平台桌面昼夜刷新仍待验收。


## v0.8.5 当前更新 · 2026-10-27

[KB30](30_V085_INLINE_CONDITION_STANDALONE.md)记录统一车况入口、页内展开、已报告API优先、设置7组与移除运行时模板依赖。[独立构建](../docs/Psa_Standalone_Build.md)区分完整私有签名包和无配置的公开CI验证包；不把客户端配置值写入公开源码。旧导入流程不再是用户安装步骤。真实账号/实车仍待设备验收。


## v0.8.6 当前增量

参见[KB31](31_V086_UI_WIDGET_REFRESH_POLICIES.md)和[更新说明](../docs/Psa_v086_Update.md)：VoLike合并切换、车况/反馈/字体配置、拖动排序、5×1与自选Widget按钮、Health状态选择、独立刷新策略。车辆c3Lbs与手机定位分开；手机定位只在二级预览按需获取。运行时间h/m无~，计算来源未改变。此前与上述行为不同的说明作为历史记录。完整发行包本地编译签名，无需其他APK。


## v0.8.7 当前增量 · 2026-10-28

下一步在授权车辆核验实验温度及新鲜回报，尤其supportsParkingClimatizationDirectStartSettings=false车型；继续真机/OEM大字/昼夜显示验收。无实车证据前不宣称任意温度支持。


## v0.8.8 当前增量 · 2026-10-28

后续优先OEM启动器缓存/系统主题图标/旧固定快捷方式与真机Widget尺寸字体验收；不因图标增加后台轮询，不因空调运行而伪造剩余时间。
