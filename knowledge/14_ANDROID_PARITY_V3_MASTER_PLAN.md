# 14｜Polestar Assistant Android Parity V3.0：正式复刻开发总计划

**计划状态**：`APPROVED_FOR_DOCUMENTATION`（用户已要求同步方案和建立重要主线）；**代码功能实施未由本次文档同步自动完成**。启动开发要按阶段交付，不把计划文字当作构建结果。

**长期 Android 主线**：`android-polestar-assistant-main`；**仓库原 `main` 保持不变，Mac/Hisingen 系列分支不受影响**。

## 1. 愿景、范围和不采用的路线

以用户实际测试可用的中国区第三方极星助手 `app-release.apk.1.1(1).1` 为行为基准，重建独立、可维护、可编译的 Android 应用：登录、Session/Token、车辆发现、SOC/续航、累计里程、开闭/充电/空调状态、远程控制、原版 UI/Widget 和后台刷新；增强 China / Global 双区域以及 Token 误报归因和状态反馈。

**不采用**：将反编译 Java/smali 直接拼装成生产工程；把第三方 APK 内置秘密或账号凭证复制到代码；将参考 APK 的所有静态方法推断为已实测可用；仅凭编译成功宣布车辆功能完成。

当前仓库 `android-app/` `v0.1.0-dev2` 是已编译成功的 **DEMO/OFFLINE** 基础：`CapabilityMatrix.ENABLE_NETWORK_VEHICLE_API=false`、`ENABLE_REMOTE_COMMANDS=false`。新主线初建时**不改动这些代码开关**，后续只有经用户授权、协议验证和单项设备测试后才逐功能开放。

## 2. 先复刻的成功基准

**参考 APK 已由用户实车反馈可用（`REFERENCE_USER_VERIFIED`）**：中国区账号登录、SOC 读取、里程读取、空调远程控制、车门解锁。用户还反馈：使用一段时间偶发 Token 错误提示，但实际功能可能仍可用。

**当前 Android 新工程状态**：以上真实能力未接入；UI/Widget 为演示数据，`OperationState`、`TelemetryStatus` 是纯模型而非联网实现。

因此第一轮核心交付应优先做到本人授权账号真实登录 + 本人车辆状态读数 + 逐项复现上述远控 + 正确区分 Token 误报与真实命令结果；不要求先实现 Global 或完整 C3 Live Stream 才开始中国区远控。

## 3. P0–P7 实施阶段与验收

| 阶段 | 内容和代码任务 | 具体交付 / 通过标准 |
|---|---|---|
| **P0 原 APK 完整方法级分析** | DEX、Manifest、资源、UI 创建流程、登录、Token、C3 gRPC、Protobuf 字段、控制 `invoke`、`refreshControlState`、Widget Worker；明确每条结论证据等级 | 完整业务方法表、调用图、HTTP/RPC 候选结构、界面清单、原始素材引用清单、未知项；不包含真实 Token/签名盐 |
| **P1 China 登录和 Session** | ChinaAuthRepository、SessionManager、TokenRefreshCoordinator、认证错误分类、认证上下文/区域隔离、车辆发现 | 本人授权账号能正常登入、获取本人车辆、正确恢复/退出/过期；刷新并发不乱序；不能用 Website Cookie 替代车 API Token |
| **P2 C3 只读状态** | 车辆发现、SOC/续航/里程/充电、Exterior/门窗/车锁/空调、单位/空值/时间戳/Proto 映射，缓存与状态源 | 在同一车辆和近似时间对照参考 APK/官方 App；数据真实、字段级追踪且区分 LIVE/CACHED/STALE |
| **P3 真实远程控制** | RemoteCommandGateway、C3InvocationClient、车型可用性、操作前确认、服务器响应、异步状态回读、Token 与控制结果分离 | 先验证用户已确认可用的空调和解锁，再逐项核对 lock、window、tailgate、honk/flash、precleaning 等；实车操作只在本人授权/手动触发下执行 |
| **P4 原版 UI 视觉/交互** | 按参考 APK 原生 UI/车辆俯视图/字体/颜色/间距/状态图/手势/下拉刷新/控制弹窗重建；保持 China 原版体验，双区域入口不侵入核心界面 | 同设备同状态截图对照、页面功能可达、不同分辨率显示、空值和异常反馈正常 |
| **P5 Widget 与后台任务** | 标准/紧凑 Widget 配置与刷新、车辆切换、主动快捷控制、WorkManager 节流和离线策略；露营模式单独评估可靠性 | 实机添加和更新正常；长时间锁屏/重启/网络切换不虚报实时；自动空调维护不能作为人宠安全保证 |
| **P6 China / Global 增强** | 保持中国区已验证路径；Global 对照 Hisingen OIDC/Discovery/C3/PCCS；区域独立 tokens、缓存、车单、命令权限；可选 C3 Live Stream | China 回归通过；Global 每项按自身账号授权实测，不能照搬中国/全球其他端 Token |
| **P7 编译与交付** | Java 模型测试、静态知识库校验、Android CI、设备布局/Widget/后台/异常测试、实车逐项验收；调试日志脱敏 | 独立源码、Android Debug APK、构建 SHA/日志、功能矩阵、已验证清单、未实现项和缺陷记录 |

实施原则：P0 研究与 P4 UI 骨架可以并行；P1/P2 是 P3 实际安全、准确执行的功能依赖；P3 不等待 P6 的 Live/Global 完整实现；所有 P7 测试应随各阶段持续进行。

## 4. 目标工程分层

```text
android-app/app/src/main/java/io/github/polestarcn/companion/
  auth/        ChinaAuthRepository, GlobalAuthRepository,
               SessionManager, TokenRefreshCoordinator, AuthErrorClassifier
  api/         ChinaAccountClient, C3DiscoveryClient,
               C3VehicleStateClient, C3InvocationClient, PCCSClient
  vehicle/     VehicleRepository, VehicleStateMapper, VehicleCapabilityResolver
  remote/      RemoteCommandGateway, CommandResultTracker,
               CommandStateVerifier, RemoteControlViewModel
  ui/          dashboard, login, vehicle, control, settings, diagnostics
  widget/      provider, configure, control, renderer
  worker/      refresh worker; camp mode research, guarded
  diagnostics/ sanitized network and vehicle-state event log
```

- 继续采用原生 Android Java / Android SDK，非 Python Bridge。对照原 APK 的动态 Java UI，但整理为功能分层，并复用数据仓库。
- 引入的 C3/PCCS Proto schema 必须有字段/编号/类型/请求证据；不根据 `Vehicle` 字段名想象 Proto tags。
- 不将复杂协议字段或第三方 Client Signature 的固定私密材料以“猜测可用”的方式写入源码；只实现经过授权和验证的路径。
- China 和 Global 各自的 Session 命名空间/Token Audience/主机/车表/可用指令必须隔离；Global Hisingen 代码仅作为参考。

## 5. 完整功能表原则

以 `04_FEATURE_MATRIX.md` 维护功能、参考 APK 实测证据、APK 静态结构证据、**独立 Android 编译/模拟器/实车结果**，不要折叠为一个“完成”列。

- 第一优先级：China 登录、SOC、里程、空调控制、车门解锁、Token 误报修复。
- 第二优先级：续航/充电/Exterior、锁车、车窗/尾门、鸣笛闪灯/净化、完整 UI、Widget 快捷控制。
- 第三优先级：Global 全套兼容、流式状态、PCCS、后台复杂自动化与发布适配。

## 6. 关键质量门槛

1. **认证**：测试“刚登录→正常读数→空闲一段时间→刷新→后台恢复→区域切换→重登录”；记录错误发生在哪一环节。参考 APK Token 误报属于 `USER_REPORTED`，真正原因 `HYPOTHESIS`。
2. **远控**：`NOT_SENT → SENDING → SERVER_ACCEPTED → VEHICLE_CONFIRMED | FAILED | UNCONFIRMED`；只有真车回读或可靠回执能标已执行。请求超时不能自动重复危险写指令。
3. **数据源**：没有真实数据时只能是 DEMO/OFFLINE；缓存不冒充实时；单位、空值、车型能力和采集时间均可追溯。
4. **界面**：同 Android 设备/分辨率截图比对，深色/浅色/字号/触摸区域/长文本/Widget 调整；保留参考素材的版权与授权边界。
5. **可构建**：CI `assembleDebug` + 纯 Java / KB 检查，提供准确 SHA256、Git commit、测试通过情况，不能把之前 v0.1 APK 当作新版本交付。
6. **范围**：R8/混淆、非必要加固继续 `DEFERRED`；基础会话隔离/权限确认/日志脱敏与不重复远控是必要的功能正确性，不因延期而取消。

## 7. 开发和验收材料

- P0：API 请求/响应结构清单、网络/Proto 方法来源表、UI 页面和控件清单、候选 RPC 方法级证据。
- P1–P3：按接口分项记录脱敏请求 ID、认证/命令/状态回读分类、时间戳、参考客户端与新客户端对照。
- P4–P5：页面和 Widget 截图差异表，后台时效与省电场景记录。
- P6–P7：区域回归矩阵、全部 APK 构建记录和剩余缺陷表。

每阶段完整记录源码改动、构建/实测证据、失败与待解决项到 `07_PROGRESS.md`，修改接口时同步 `03_API_RESEARCH.md`、`docs/HISINGEN_GLOBAL_CN_API_COMPARISON.md`。


## 2026-10-27 v0.7.8 当前增量

当前 Android v0.7.8 在 P2/P3 显示层加入新参考 Health/停车位置，保养百分比与时长估算明示来源；仍不实现远程复位。后续优先同车实测，而非用 Global 或旧公开 schema 推断 China 能力。 具体来源、字段和限制见 [v0.7.8 车况、位置与行程](25_V078_CONDITION_LOCATION_TRIP.md)。


## 2026-10-27 v0.8.3 当前增量

基于最新v0.8.2/9744759，恢复PS默认并保留VoLike新布局；自动保养只用Health剩余值，历史手动记录不再补充显示。地图共用精简网页，并新增默认关闭的手机前台位置授权；无后台定位。所有Widget改用原生文字/按钮与等比车图，新增PSeu、自选数据、统一粗体/字号及精确尺寸适配。按钮资源和后续迭代参考Psa图标规范v1。协议/实车证据标签不因此升级。详见 [v0.8.3统一知识](28_V083_UI_WIDGET_MAP_ICONS.md)。


## v0.8.4 当前增量 · 2026-10-27

[KB29](29_V084_ICONS_SERVICE_TARGETSOC_READBACK.md)与[更新说明](../docs/Psa_v084_Update.md)记录新母版/昼夜通知、30000km与合并保养周期、C3 TargetSoC只读字段、首次协议、5/10/15/20秒四次回读和非阻塞等待。历史20000km、单次15秒回读与黑武士启动器约定被本轮授权替代。实车TargetSoC/平台桌面昼夜刷新仍待验收。


## v0.8.5 当前更新 · 2026-10-27

[KB30](30_V085_INLINE_CONDITION_STANDALONE.md)记录统一车况入口、页内展开、已报告API优先、设置7组与移除运行时模板依赖。[独立构建](../docs/Psa_Standalone_Build.md)区分完整私有签名包和无配置的公开CI验证包；不把客户端配置值写入公开源码。旧导入流程不再是用户安装步骤。真实账号/实车仍待设备验收。


## v0.8.6 当前增量

参见[KB31](31_V086_UI_WIDGET_REFRESH_POLICIES.md)和[更新说明](../docs/Psa_v086_Update.md)：VoLike合并切换、车况/反馈/字体配置、拖动排序、5×1与自选Widget按钮、Health状态选择、独立刷新策略。车辆c3Lbs与手机定位分开；手机定位只在二级预览按需获取。运行时间h/m无~，计算来源未改变。此前与上述行为不同的说明作为历史记录。完整发行包本地编译签名，无需其他APK。
