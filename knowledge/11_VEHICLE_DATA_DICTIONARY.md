# 11｜参考 APK 车辆字段字典（33 项）与新工程数据语义

**已独立验证：** `classes.dex` 内 `Lapp/c3garage/companion/PolestarApi$Vehicle;` 的 `field_ids` 表共有 **33 个不同字段**。脚本 `scripts/inspect_vehicle_schema.py`；原始提取 JSON 位于 `evidence/apk-vehicle-schema.json`。

字段名/静态声明类型仅说明 APK 中存在对应数据容器：**不代表已经核对实际中国区 Proto tag、RPC 响应、字段取值、单位或可用车型**。第二份外部报告“30 个字段”的计数不正确。

| 类别 | 字段（参考 APK 名称） | 数量 | 状态 |
| --- | --- | --- | --- |
| 车辆标识与时间 | `vin`, `model`, `modelName`, `modelYear`, `stateTimestamp` | 5 | `APK_CONFIRMED` |
| 电池 / 充电 | `battery`, `range`, `charging`, `chargingType`, `chargerConnection`, `chargerPowerStatus`, `powerWatts`, `currentAmps`, `timeToFullMinutes` | 9 | `APK_CONFIRMED` |
| 累计与行程 | `odometerKm`, `tripAutomaticKm`, `tripManualKm` | 3 | `APK_CONFIRMED` |
| 开闭与锁具 | `locked`, `doors`, `doorStates`, `windows`, `windowStates`, `tailgate`, `tailgateLock` | 7 | `APK_CONFIRMED` |
| 空调与空气 | `climate`, `climateRuntimeMinutes`, `climateTemperatureC`, `climateTargetTemperatureC`, `ventilation`, `air`, `airRuntimeMinutes`, `airQualityIndex`, `pm25UgM3` | 9 | `APK_CONFIRMED` |
| **合计** |  | **33** | **字段存在 ≠ API 已可用** |

### 本分支新工程的数据范围

`VehicleSnapshot` 当前仅有供 UI/Widget 使用的基础展示字段：名字、演示 ID、SOC、续航、累计里程、充电状态、门窗尾门、车锁、来源、采集时间、演示标志。真实温度/PM2.5/行程等 **尚未编译成真实 API 模型映射**，也不提供假装已连接的值。

`TelemetryStatus` 的显示分类：

| 分类 | 含义 | 条件（未来真实数据源） |
| --- | --- | --- |
| `DEMO` | 演示，不来自车辆 | `isDemo = true`；`timestampMillis = 0` |
| `LIVE` | 新近取得，仍须标注实际采集时间 | 来源标签为 LIVE、有效时间戳、未超过调用方设置的最大有效期 |
| `CACHED` | 缓存非实时值 | 明确来源 CACHED 且未超过设定有效期 |
| `STALE` | 已超过有效期 | **不应继续显示为实时** |
| `UNKNOWN` | 缺时间戳、时钟异常或来源不清 | 不能用 0 冒充未知值 |

> 当前 UI / Widget 只接入 `DemoRepository`，明确 `DEMO / OFFLINE`，车辆采集时间为 `0`。该分类仅为未来接口接入做准备，**没有任何车辆 API、后台任务或远控被打开**。

### 后续每个字段接入必须记录

- 数据源：China C3 / China PCCS / Global C3 / Global GraphQL / Cache；证据日期与对应官方 App 版本。
- 车型、年款、VIN 脱敏标识、调用 RPC/响应嵌套路径、Proto 字段定义或独立运行证据。
- 单位、空值、枚举值映射、最近更新戳与来源优先级；时间戳异常必须回退 UNKNOWN。
- 对照官方车辆 UI：实际电池、续航、里程、开闭状态；分层标记 `MAPPING_REQUIRED -> VERIFIED_READ`，不跳级。

当真实接口接入后，可将目前的字段群划分到多个模块，保持 Android / Mac 独立运行与各自区域 Token 边界。不能直接把 Global 的字段路径当 China 的 Proto 定义。


## 2026-10-27 v0.7.8 当前增量

旧 33 字段表仍是历史参考结构。扩展字典见 25：health 2/3/4=剩余小时/天/km，5保养、6制动液、7冷却液、8可选机油、13雨刮液、38低压电池、14–35/46灯光。c3Lbs 点位 longitude=1/latitude=2。Battery12 不能独立判 TA 能耗，TA/TM 时长是带 ~ 的估算。 具体来源、字段和限制见 [v0.7.8 车况、位置与行程](25_V078_CONDITION_LOCATION_TRIP.md)。


## 2026-10-27 v0.7.9 免 Key 地图增量

当前位置方案改为用户明确选择的内嵌高德官方 URI 网页，移除 staticmap Key 依赖并清理旧配置。参考 APK 仍证明 field1=经度、field2=纬度，未采用外部 AI 文本中的反序定义。c3Lbs 4秒截止、失败隔离及切车/退出结果丢弃已补强；网页需网络和系统 WebView，手机底图/坐标精度仍待实车验收。见 [v0.7.9 知识库](26_V079_KEYLESS_PARKING_WEB.md)。


## 2026-10-27 v0.8.3 当前增量

基于最新v0.8.2/9744759，恢复PS默认并保留VoLike新布局；自动保养只用Health剩余值，历史手动记录不再补充显示。地图共用精简网页，并新增默认关闭的手机前台位置授权；无后台定位。所有Widget改用原生文字/按钮与等比车图，新增PSeu、自选数据、统一粗体/字号及精确尺寸适配。按钮资源和后续迭代参考Psa图标规范v1。协议/实车证据标签不因此升级。详见 [v0.8.3统一知识](28_V083_UI_WIDGET_MAP_ICONS.md)。
