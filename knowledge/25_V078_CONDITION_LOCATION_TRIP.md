# Psa Android v0.7.8 车况 停车位置与行程

2026-10-27；版本 0.7.8-beta1 / 78。此文件覆盖历史知识中 Health 读取、保养显示及 TA field 12 的当前解释，不改写历史验证记录。

## 最新参考证据

用户上传 `app-release.apk.1.1(3).1`，包名 `app.c3garage.companion`，版本 `0.1.20261027.081805` / 215223485；5,011,635 bytes，SHA-256 `f468c76eae50cfe753d62d6efd4a4706efb084a9bceb7ec26e0fa0fca6a161c6`。证书 SHA-256 `a64bcf2456d3a2e920a7620e899e7da74df7bcc39d53f517f43461eaf1166bc7`。minSdk 26，targetSdk 34。这是用户确认中国区工作的参考程序（REFERENCE_USER_VERIFIED），不是本项目实车验收。

JADX 方法级复核 `PolestarApi` 的 GetLatestHealth 与 GetLastParkedLocation；仅提交独立映射及元数据，不上传参考 APK/反编译业务源码。原 33 字段 JSON 是旧样本历史证据，保留；新增 evidence/reference-20261027-mapping.json。

## Health 当前协议

`services.vehiclestates.health.HealthService/GetLatestHealth`，UNARY；VIN 请求 field 2，响应 payload field 3，报告时间 payload field 1。响应显式 VIN 在 field 2，field 1 可能是请求 ID，不据此拒绝响应。4 秒截止，仅 UNIMPLEMENTED 时回退 SERVER_STREAMING GetHealth，收到首条取消。缓存 5 分钟，失败退避 1 分钟；空回复不覆盖先前状态，不新增长期订阅。

| Payload field | 语义 | 显示约束 |
|---|---|---|
| 2 / 3 / 4 | 剩余小时 / 天 / km | 不解释为已用；负哨兵值未知 |
| 5 | 保养状态 | 0 未提供，1 正常，2–11 警示/到期状态 |
| 6 | 制动液 | 1 正常，2/3 偏低 |
| 7 | 冷却液 | 1 正常，2 偏低 |
| 8 | 机油 | Mac/上游字段，最新参考没有打印；缺失未知 |
| 9–12 | 左前/右前/左后/右后胎压告警 | 1 正常，2 严重低，3 低，4 高；0/未知枚举未提供 |
| 13 / 38 | 雨刮液 / 12V | 1 正常，2 偏低 |
| 14–35，46 嵌套 | 外部灯光 | 识别正常/警示，未知不假定正常 |
| 39–42 | 可选 kPa | 未获 China 数值实测，仍实验性 |

车况汇总由 VehicleCondition 合并健康、可选胎压、门/窗/尾门。至少一项已报告且没有已知异常时 All Good!；这不是“每项均已测得正常”，未知详情保留。用户明确接受部分上报；旧严格全已知汇总不再用于首页。

ServiceReading 优先采用报告的剩余值，无值时用已配置的本地记录。百分比 = clamp(剩余天数/配置天数，剩余 km/配置 km，0–100%)，默认周期 365 天/20000 km，属于估算。condition_service_time 与 condition_service_distance 默认关闭，四套首页车况按钮及较大 Widget 共用开关。小时字段不强行转换为日历天数。

## c3Lbs 与高德

Discovery 的 c3Lbs.grpcHost 独立于 c3Host，限定官方 .volvocars.com.cn 主机。RPC `dtlinternet.DtlInternetService/GetLastParkedLocation`，UNARY，请求 field 1 VIN；响应 payload 2，点位 1=longitude、2=latitude、3={1:epochSeconds}；响应 field 3 位置可能过期。验证有限数及经纬度范围，不记录精确坐标、不写磁盘。

四套皮肤页眉入口按需读取，弹出二级位置预览及上报时间；切车/退出后丢弃旧结果。地图跳转沿用参考 androidamap viewMap/dev=0 路径，未证明另一个 CRS，不做臆测转换。高德未安装尝试 geo 系统地图。

StaticMapPreview 为可选 Web Service 静态图：HTTPS restapi.amap.com/v3/staticmap，location 经度在前，zoom 15，640*360，scale 1，橙色 marker。用户自己的 Web 服务 Key 用 SecureMapPreferences/Keystore 加密保存；开关与 Key 缺一不请求。设置告知坐标发往高德及额度消耗。仅打开弹窗请求一张图，单线程、连接 6s/读取 8s、2MiB 上限、取消与失败兜底；不增加地图 SDK、GPS 权限、持续定位或车辆轮询。签名校验型 Key 未适配 sig；没有实 Key 和实车坐标，在线成功/位置偏移待验收。

官方接口：[高德静态地图](https://lbs.amap.com/api/webservice/guide/api/staticmaps)。

## Mac 与 TA 能耗冲突

对照 `integrate/hisingen-china-0.19.5` commit `f50820bd9c34c3bc56d18b51543c60ddcc6bde91` 的 ChinaPolestarAPI.swift 与上游 Hisingen v2.0.10 PolestarGRPC.swift / PolestarGRPCCapabilities.swift。Mac China 传递 TA/TM 距离、平均速度，没有提供可直接复用的 TA 能耗或原始行驶时长。

旧 polestar-js battery.proto（commit 3387cd13297882bf4b7d8126951574442a09352a）把 field 12 当自动行程平均能耗，field 15 当自动行程总 Wh；当前 Hisingen 把 12 当观察到的电池容量 kWh。仅凭 12 得到 78 等值不能显示为 TA kWh/100km。

现在只在同次电池独立能量 15、Odometer TA 距离 4 和报告时间（相差不超过 2 分钟、不能明显在未来）对齐时印证：Wh/(10×km)。与 raw12 相差不超过 max(0.5,5%) 才接受；缺 raw12 时可显示计算结果并标 ~；冲突或缺少字段则 --。缓存 consumptionSchema <2 的 TA 能耗失效。TM 保留已验证的既有字段 3；Battery 13 / Odometer 7、8 增加充电后能耗、距离与平均速度。

TA/TM 时长没有已证实的独立字段，因此按同一 Odometer 距离/均速估算分钟，标 ~；速度为零、未知时不算。用户称 ra 的项目没有独立 schema 证据，仍按 TA 自动行程命名，不新增虚构字段。所有新增显示均可选入首页/Widget，不加复位或修改车辆请求。

## UI 与验证范围

四皮肤中 NIOlike 与 NIOlike2 共用几何布局，后者只改 VI 配色；统一圆圈快捷入口、紧凑温度/净化、单行数据与刷新图标。UiTypography 按皮肤保存 80–120%，Widget 独立设置，原生 Dialog 也叠加系统字号。Widget 从 applicationContext 计算，避免 Activity 皮肤字体与插件倍率叠加。HapticDialogBuilder 包含列表、正负/中性按钮及可滚动的详情容器。

本轮 Debug/Preview 123 项合成测试及原生布局/RemoteViews、Lint、纯模型检查见更新说明。无本项目 China 账号/车辆/高德 Key/手表实机成功证据，状态为实现与构建验证，不升级为 PROJECT_DEVICE_VERIFIED。


## 2026-10-27 v0.7.9 免 Key 地图增量

当前位置方案改为用户明确选择的内嵌高德官方 URI 网页，移除 staticmap Key 依赖并清理旧配置。参考 APK 仍证明 field1=经度、field2=纬度，未采用外部 AI 文本中的反序定义。c3Lbs 4秒截止、失败隔离及切车/退出结果丢弃已补强；网页需网络和系统 WebView，手机底图/坐标精度仍待实车验收。见 [v0.7.9 知识库](26_V079_KEYLESS_PARKING_WEB.md)。


## 2026-10-27 v0.8.3 当前增量

基于最新v0.8.2/9744759，恢复PS默认并保留VoLike新布局；自动保养只用Health剩余值，历史手动记录不再补充显示。地图共用精简网页，并新增默认关闭的手机前台位置授权；无后台定位。所有Widget改用原生文字/按钮与等比车图，新增PSeu、自选数据、统一粗体/字号及精确尺寸适配。按钮资源和后续迭代参考Psa图标规范v1。协议/实车证据标签不因此升级。详见 [v0.8.3统一知识](28_V083_UI_WIDGET_MAP_ICONS.md)。


## v0.8.6 当前增量

参见[KB31](31_V086_UI_WIDGET_REFRESH_POLICIES.md)和[更新说明](../docs/Psa_v086_Update.md)：VoLike合并切换、车况/反馈/字体配置、拖动排序、5×1与自选Widget按钮、Health状态选择、独立刷新策略。车辆c3Lbs与手机定位分开；手机定位只在二级预览按需获取。运行时间h/m无~，计算来源未改变。此前与上述行为不同的说明作为历史记录。完整发行包本地编译签名，无需其他APK。
