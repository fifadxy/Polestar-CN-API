# 12｜Android 分阶段验证总表（参考 APK 与新工程区分）

**重要**：参考 APK 用户已反馈 China 登录、SOC、里程、空调、解锁真实可用，标 `REFERENCE_USER_VERIFIED`；本工程尚无相应 API 实现与端到端实测，故不能标 `PROJECT_DEVICE_VERIFIED`。

## A. 现有工程历史验收

| 项目 | 核验依据 | 状态 |
|---|---|---|
| 原 APK ZIP/DEX/SHA、v2 签名、33 字段 | `01_APK_EVIDENCE` / `evidence` | `APK_CONFIRMED` |
| Java 纯模型 + 语义测试 | `scripts/test_models.sh`、`tests/ModelStateTest.java` | 既有源码单测通过 |
| Android 0.1.0-dev2 Debug 编译 | Actions run `40261229408`，APK SHA256 `0e8d417cd6354ecf55ac414492a466ce10d02778e40eb59aa850bdbac0767a40` | `CI_VERIFIED`，**仅旧版本** |
| 首次区域选择、页面和两个 Widget | 已有代码，但无本次新设备日志 | `IMPLEMENTED_DEMO / DEVICE_UNTESTED` |
| China 真实登录及车辆接口 | v0.3.0-alpha2 已实现协议与 UI，尚无本项目设备日志 | `IMPLEMENTED_NEEDS_DEVICE_VERIFICATION` |
| China 远控 | 空调启停和解锁已接手动确认通道，无自动写重试 | `IMPLEMENTED_NEEDS_DEVICE_VERIFICATION` |
| Global 完整网络 API | 现有代码没有 | `NOT_IMPLEMENTED` |

## B. P0–P7 分项新验收要求

| 阶段 | 主要验收 |
|---|---|
| P0 | 参考 APK 七业务类、33 字段、认证方法、RPC/Proto 字段、UI 组件、Widget Worker 形成可复现依据；静态存在不等于运行成功 |
| P1 | China 本人账号合法登录、Session/Token 刷新、闲置和并发场景、车辆发现及多车绑定 |
| P2 | China 真实 SOC/里程优先，其余 Range/Exterior/Charging/Climate 分项核对，采样时间、单位、缓存新鲜度明确 |
| P3 | 用户主动解锁、空调操作逐项验证，指令服务受理与车辆真实状态核对分开；未确认的指令不自动重发；其余控制按车型逐项开放 |
| P4 | 原 APK 与新 APK 的同设备截图和操作流程对照，页面/手势/字体/素材状态正确 |
| P5 | Widget 配置/刷新/主动快捷控制与主界面一致，Doze、重启、断网不虚报实时 |
| P6 | Global 独立认证/车表与 China 分离；C3 Live/PCCS 每个方法独立验收 |
| P7 | GitHub CI、调试 APK、SHA256、源代码、测试报告和未通过项；不能用旧 v0.1 CI 代表新版本 |

## C. 命令与认证验收底线

- 状态：`DISABLED`、`NOT_SENT`、`SENDING`、`SERVER_ACCEPTED`、`VEHICLE_CONFIRMED`、`FAILED`、`UNCONFIRMED`。
- `SERVER_ACCEPTED` ≠ `VEHICLE_CONFIRMED`；没有可靠回读不展示“车辆已执行”。
- 参考 APK 一段时间后 Token 误报属于用户观察事实，根因尚未确认。分类登录、刷新、Discovery、C3 Read、Remote Write 和结果回读异常，禁止单个错误覆盖其他已成功操作。
- 真实指令只能针对用户授权的本人车辆，在明确主动操作后发起。命令超时先只读回读，不自动重复写入。
- 不提交密码、Access/Refresh Token、短信验证码、Cookie、第三方签名密钥或完整 VIN。
- WorkManager 不能作为生命安全相关的精确定时器；露营/宠物功能不得承诺通过手机轮询保障车内安全。

## D. 长时间与设备回归

详尽的 T01–T12 测试用例见 `15_TOKEN_AND_REMOTE_VALIDATION.md`，原 APK/新 APK 实测对照格式见 `13_REFERENCE_APK_FIELD_VALIDATION.md`。新代码在确认并接通 API 之前必须继续标 DEMO/OFFLINE，不能隐藏真实数据源状态。

R8、混淆及非必要安全加固按 `06_DEFERRED_SECURITY_R8.md` 延期，不意味着远控功能应永久搁置。

## v0.4.0-beta1 验收增量

自动化回归覆盖首次启动不出现区域弹窗、真实凭证输入、常规/小屏/横屏控制布局、语言切换、气候与净化缓存恢复、通知内容与锁屏隐私、错误模板拒绝。真实手机号登录、Token 长时刷新、指纹成功/失败/取消、尾箱/解锁/空调/净化车端动作、厂商电池策略仍需在用户设备验证。


## v0.7.6 本地增量验证

新增回归检查真实文字布局高度与可见区域，不只检查外层按钮。覆盖小屏/横屏 130% 字号、两套皮肤/中英文/日夜、最小 2×1/4×1 Widget 时间戳、48dp 主控制触区、首页控件复用、流式首条取消/时限/缓存、Health 未知枚举与字段来源时间。APK 另核包内版本、Release debuggable 标记与签名。账号/实车/Doze/手表/耗电仍为待实机验证。


## 2026-10-27 · v0.7.7-beta1

新增设置可切换 NIOlike 日夜皮肤；移除动态光效；车图下方 Health 汇总与可隐藏胎压；充电仅在启动/进行时独立分组；SOC 横条与旁侧续航、系统字体和 Widget 同步。Debug/Preview 各 102 项项目回归、Lint 与 Release/Preview 编译通过，APK v2 签名验证通过；账号/实车/手表与耗电未实测。详见 [本版知识库](24_V077_NIOLIKE_VI_CHARGING.md) 和 [更新说明](../docs/Psa_v077_Update.md)。


## 2026-10-27 v0.7.8 当前增量

新增检查门槛：最新参考用户实测仅标 REFERENCE_USER_VERIFIED；123 项合成测试只验证模型/布局。必须单独实车核对油液缺失、保养剩余、地图位置、TA能量一致性；无真实 Key，不声称在线高德图已成功。 具体来源、字段和限制见 [v0.7.8 车况、位置与行程](25_V078_CONDITION_LOCATION_TRIP.md)。


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

v087协议编码/界面测试通过不代表实车通过；目标温度为默认关闭的试验，18–28°C为App防护。命令确认要求运行且新鲜目标回报匹配，否则未确认，禁止写重试。


## v0.8.8 当前增量 · 2026-10-28

v088原生布局检查要求所有可见文字无省略号、行宽和行高均在边界内；风扇图形不随状态变化，颜色/计时由快照决定。Android8/15的alias切换和完整签名包不等于OEM/实车通过。


## v0.9.2 当前增量

新增驻车/行驶/充电隔离、重复采样、TM复位和清空验证；无实际登录和车辆写入，能耗为采样估算。


## v0.9.4 当前增量

新增原生10组粒子/5组气流渲染、离线测试/生命周期/省电、容量、Alpha及字体断言。测试模拟不接入账号、车辆或位置；不声称实测热/冷字段、耗电或固定FPS。
