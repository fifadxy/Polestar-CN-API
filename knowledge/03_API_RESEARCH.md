## v0.7.5 Health / 车辆状态交叉核对

### Global / Europe Hisingen 源码

对 `NicolasKheirallah/Hisingen` commit `5301353665cf890b350c6e437b04c088833a88f1` 的
`PolestarGRPCCapabilities.parseHealth` 重新核对：

| 能力 | Health 字段 | 上游语义 |
|---|---:|---|
| 左前/右前/左后/右后胎压告警 | 9 / 10 / 11 / 12 | iTPMS/胎压告警状态 |
| 玻璃水 | 13 | 字段存在即已上报，值 > 1 为告警；**不是液位值** |
| 12V / Low-voltage battery | 38 | 值 2 为告警 |
| 四轮压力研究值 | 39–42 | Hisingen 可解析；Polestar 2 不保证提供，Android 主 UI 不展示 |
| 保养剩余天数 / 距离 | 3 / 4 | 上游 Health 映射 |
| 发动机小时保养输入 | 2 | 上游 schema 字段，即使 BEV 也可能上报 |

### China

- 本 Android 分支已通过 China C3 host 调用 `/services.vehiclestates.health.HealthService/GetHealth`。
- v0.7.5 将同一次响应同时映射为 12V、玻璃水和胎压告警；仍需用户授权 Polestar 2 实车验证 China 返回值语义。
- 历史中国官方 Android App 静态/StackTrace 证据能定位 `com.polestar.carcontrol.ui.main.CarMainFragment`、`CarMainTipsProvider` 等车辆状态 UI，但当前可访问材料**未直接证明**官方 App 对 13/38/9–12 的具体 RPC 映射。
- 用户提供的参考模板 APK `app.c3garage.companion` 也未发现 HealthService，因此不能用它替代官方 China App 证据。

### UI 决策

- 主界面只显示一个“车辆状态”聚合按钮。
- 12V 与玻璃水仅显示 Normal / Check / Unavailable，不显示虚构电压或液位百分比。
- 四轮 iTPMS 详情默认隐藏；实验开关只恢复告警视图，不把 39–42 kPa 当成中国 Polestar 2 已验证能力。


## v0.7.4 参考 APK 空调字段复核

对当前模板 APK 的 DEX 方法 `PolestarApi.readClimateFields` 与 `MainActivity.climateMetricText` 做字节码级复核：

- ParkingClimatization field **2**：状态（未知 / 开启 / 关闭 / 处理中）
- field **3**：`climateRuntimeMinutes`，模板 UI 将其作为**剩余运行分钟**
- field **7**：`climateTemperatureC`（车内温度）
- field **8**：`climateTargetTemperatureC`（目标温度）
- 仅当状态为“开启”且 runtimeMinutes > 0 时显示“剩余 …”；否则显示“剩余时间暂无”

Android v0.7.4 已按此证据优先显示车辆 field 3，本地 30 分钟计时仅作为带“估算”标记的回退。


# 03｜China / Global API 研究映射（Android Parity V3.0）

> 证据分层：APK 静态 / 用户参考 APK 实车反馈 / 官方 China App 与 Hisingen 历史研究 / 新 Android 已实现能力，各自独立。详见 `13_REFERENCE_APK_FIELD_VALIDATION.md`。

| 领域 | 参考 APK 的静态依据 | 用户反馈与上游研究 | 本项目当前代码 | V3 开发 |
|---|---|---|---|---|
| China 登录 | `gw.app.polestar-auto.com`、`/user/api/v2/oauth/login/password`、`login` | 参考 APK `REFERENCE_USER_VERIFIED` | 已实现；设备验证待完成 | P0/P1 优先复刻与验证 |
| China Token refresh | `/user/api/v2/oauth/refresh-global-token`、`refreshIfNeeded`、`SESSION_REFRESH_LOCK` | 用户反馈偶发 Token 报错，功能仍可能可用 | Keystore + single-flight；设备验证待完成 | P1：并发、过期、错误分类 |
| China 车辆发现 | `discoverAndRead`、`collectVehicles`、`ensureC3Host` | 参考客户端实际能操作本人车辆 | 已实现递归解析与多车选择；设备验证待完成 | P1/P2 映射 |
| China C3 discovery | `cnepmob.volvocars.com.cn` | 真实 Host 需发现，不能硬编码假设 | 已实现 v1 Accept discovery；设备验证待完成 | P1/P2 验证 |
| C3 电池 | `BatteryService`、`GetLatestBattery`、`Vehicle.battery` | 参考 APK SOC 已由用户实测 | 已实现 Proto 映射；设备验证待完成 | P2 真实读数 |
| 续航/充电 | `Vehicle.range`、`charging` 等 | 尚无明确用户逐字段实测 | DEMO / 未覆盖 | P2 逐字段确认 |
| C3 里程 | `OdometerService`、`GetLatestOdometer` | 参考 APK 累计里程已由用户实测 | DEMO | P2 真实读数 |
| C3 Health / 胎压 | **v0.7.3 复核：当前模板 APK 仅出现 Battery/Exterior/Odometer/ParkingClimatization/PreCleaning 服务，未发现 HealthService/GetHealth 或 TPMS/iTPMS 字符串**；Mac/Hisingen 另有 `tyreAndWarnings` | 上游 Hisingen：Health field 9–12 报警、39–42 压力字段；但这不能证明中国 Polestar 2 会返回可展示的四轮 kPa | 已实现 Health 告警读取；UI 按 iTPMS 状态展示，不显示未验证 kPa | P2 重点核对中国区 iTPMS 告警语义 |
| C3 Exterior | `ExteriorService`、`GetLatestExterior` | 中国官方 App 另有 C3 运行证据；不等同第三方所有字段实测 | DEMO | P2 状态回读，P3 解锁验证 |
| 远控空调 | `control`、`ClimatizationStart` / `ClimatizationStop` 线索 | 参考 APK 空调远控已由用户实测 | 手动确认、无自动重试；设备验证待完成 | **P3 核心复刻** |
| 远控解锁 | `control`、`Unlock` 线索 | 参考 APK 车门解锁已由用户实测 | 手动确认、无自动重试；设备验证待完成 | **P3 核心复刻** |
| 其他远控 | `Lock`、`WindowControl`、`TailgateControl`、`HonkFlash`、`PreCleaning` 线索 | 未经用户逐项确认 | 未实现 | P3 逐项确认车型支持 |
| PCCS China | 当前参考 APK 尚未独立确认实际 PCCS 主机 | 既有官方 China App/Hisingen 文档：`api.pccs-prod.origin.polestar.cn` | 未实现 | P6 后续按车型确认 |
| C3 Live Stream | 第三方 APK 的持续 Stream 尚未实证 | 官方中国 App 研究记录 C3 Live；不同软件，不能混为同一结论 | 未实现 | P6；不阻塞 P3 |
| Global | 参考 APK 以 China 为主 | 上游 Hisingen Global 有 OIDC、Vehicle、C3/PCCS 对照 | 区域选择 DEMO | P6 独立 Session 与接口 |

## 已确认的 API 研究边界

- China 与 Global 不只差一个域名。China 官网网页会话不能直接视为官方移动 App 的车云 Token；Global Data Portal M2M Token 不适用于 China C3。
- 参考 APK 的 `Session.access/refresh/expiresAt/c3Host`、`signedUrl`、`refreshIfNeeded` 只证明存在相应代码结构；动态 Token Audience、Proto 字段编号和服务端返回仍须授权测试。
- `SIGN_SALT`、`signedUrl` 等标识可研究方法职责，但**不在仓库复制第三方固定签名秘密、验证码或登录凭证**。
- 远控状态 `SERVER_ACCEPTED` 不等于车辆已执行。控制超时后只读回读，不自动重复高影响写入。
- 第三方报告 A/B 的分析已在 `10_TWO_REPORTS_RECONCILED.md` 逐条校对；不同技术线索不得合并成“已实车验证”。

## 开发顺序

`方法级拆解(P0) → 授权 China 登录+Token(P1) → Vehicle Discovery/C3 Read(P2) → 用户已证实空调与解锁(P3) → 其他远控逐项验证(P3) → UI/Widget(P4/P5) → Global/Stream/PCCS(P6)`。

每取得新的 RPC/响应字段证据，同时更新 `docs/HISINGEN_GLOBAL_CN_API_COMPARISON.md` 和 `11_VEHICLE_DATA_DICTIONARY.md`；每个实测结论必须说明测试目标是**参考 APK**还是**本项目 APK**。

## v0.4.0-beta1 方法级增量

参考模板 `PolestarApi.control`：`TailgateControl` 请求 VIN 包裹在 field 1/1，field 2=1 打开/2 关闭；`Lock` field 2=0；`PreCleaning` field 2=1 开启/0 关闭。`ParkingClimatization.GetLatestParkingClimatization` payload 3 内 field 2 与 `PreCleaning.GetLatestPreCleaning` payload 3 内 field 6，枚举 Unknown/On/Off/Pending。以上新增独立实现，保持 NEEDS_DEVICE_VERIFICATION。控制先通过系统强生物识别，调用一次，最多四次只读回读，不自动重发。


## v0.7.0-beta1 Health 增量

Android 已新增 `HealthService/GetHealth` 只读调用。四轮顺序按左前、右前、左后、右后，报警 1=正常、2=严重偏低、3=偏低、4=偏高；压力字段 39–42 以 kPa 显示。该映射来自当前 Hisingen 源码并与 Mac China `tyreAndWarnings` 能力对齐，代码已实现但仍需用户车辆完成 `PROJECT_DEVICE_VERIFIED`。


## v0.7.6 传输与时间来源

Health/GetHealth 修正为 SERVER_STREAMING，首条返回后取消订阅，4 秒截止时间；读失败不影响其他车况。枚举 0/未知值不映射为正常；来源定义与中国区边界见 knowledge/23。空调分钟和原报告时间共同用于展示倒计时，推算不写回车辆。


## 2026-10-27 v0.7.8 当前增量

当前 Health 主路径为 GetLatestHealth UNARY；只有 UNIMPLEMENTED 才回退 GetHealth SERVER_STREAMING。最新参考已证实 China 该主路径；旧“参考未出现 Health”的结论只适用于旧样本。独立 c3Lbs/GetLastParkedLocation 已加入按需读取；停车预览可选高德静态 API。Battery12 与上游容量解释冲突，禁止独立作为 TA 能耗。 具体来源、字段和限制见 [v0.7.8 车况、位置与行程](25_V078_CONDITION_LOCATION_TRIP.md)。


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

两版模板ClimatizationStart字段3=fixed32 float，所有调用固定22°C；ParkingClimatization字段8回报目标温度。历史能力false，Mac unsupported。v087实验18–28°C仅App保护，未实车验证；无写重试。


## v0.8.8 当前增量 · 2026-10-28

本轮无新RPC或写入参数。Widget空调剩余时间仍取ParkingClimatization字段3及原报告时间；缺失时仅沿用已确认本地会话估计，不能把运行状态推导为30分钟新计时。
