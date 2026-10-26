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
