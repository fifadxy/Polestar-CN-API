# Hisingen Polestar Global / China API 差异说明

> Repository: `fifadxy/Polestar-CN-API`  
> 文档状态：持续维护（Living Document）  
> 首次整理：2026-10-21  
> 对照基线：Hisingen upstream `v2.0.10`；中国极星 Android App `com.polestar.china.android 4.25.0-262`；本项目已完成的中国区运行时验证  
> 适用范围：Polestar 车辆发现、认证/Token、C3/PCCS/Data Portal、车辆状态获取、实时流、远程控制接口边界、Global 与 China 网络地址差异

---

## 0. 文档维护规则

本文件是本仓库关于 **Hisingen 海外版（Global）与中国版（China）后端差异的唯一总表**。

以后只要发生以下任一变化，应在**同一个代码提交**中同步更新本文件：

- 新发现或确认一个中国区 API / RPC / Host；
- Global Hisingen 上游升级，API、OIDC、C3、PCCS 或 Data Portal 行为改变；
- 中国区登录由“网页会话”升级为可直接获得车辆 API 会话；
- Token 类型、存储位置、刷新方式、有效期或 Audience 发生变化；
- 新增/删除状态字段、实时流、远程控制能力；
- C3 / PCCS / VOCMO 主备关系发生变化；
- Android App 版本升级导致类名、Proto、RPC、域名发生变化；
- 本仓库的 Hisingen China 实现从“未验证/实验”提升为“已验证”。

### 0.1 状态标记

| 标记 | 含义 |
| --- | --- |
| **VERIFIED** | 已通过 Hisingen 上游源码、真实 App 运行或本项目实际网络/运行时验证确认 |
| **STRONG-INFERENCE** | 多处证据一致，但尚未形成完整端到端验证 |
| **EXPERIMENTAL** | 已发现接口或结构，但稳定性/权限/适用车型尚未验证 |
| **MAPPING_REQUIRED** | 已知 Global 有对应能力，但中国区 RPC/权限映射仍未确认 |
| **NOT IMPLEMENTED** | 当前本仓库尚未实现 |
| **NOT APPLICABLE** | 该机制不适用于此区域/路径 |

> 重要：**Capability（能力标记）不等于实时车辆状态，也不等于远程命令一定可执行。**

---

# 1. 一页结论

Global 与 China 不是简单的“换域名”。

当前已经确认至少存在以下差异：

| 项目 | Global / 上游 Hisingen | China / 中国区 |
| --- | --- | --- |
| Polestar ID | 欧洲/Global OIDC | 中国 Polestar ID / 中国官网与中国 App 账户体系 |
| 登录入口 | `polestarid.eu.polestar.com` OIDC | `www.polestar.cn/zh-cn/auth/login/`；官方 App 另有中国移动端登录流程 |
| Hisingen 当前可用认证 | OIDC/PKCE + Refresh Token；另有 command-client 授权 | **当前仅集成 Official Web Sign-In UI；网页登录 ≠ 已取得车辆 API Token** |
| 车辆发现 | `mystar-v2` GraphQL `getConsumerCarsV2`；可选 VDMS enrichment | 中国 App 自有车辆账户链路；本项目尚未验证网页会话可直接换取同等车辆列表 |
| 主车辆状态 | GraphQL + C3 gRPC | **C3 gRPC 为已验证主路径** |
| C3 discovery | `https://cnepmob.volvocars.com` | `https://cnepmob.volvocars.com.cn` |
| PCCS | `https://api.pccs-prod.plstr.io:443` | `https://api.pccs-prod.origin.polestar.cn` |
| C3 运行时 Host | discovery 动态返回 `grpcHost/grpcPort` | 已观察到多个 `.volvocars.com.cn` C3 Host |
| 实时状态 | C3 gRPC stream + GetLatest 类 RPC | **C3 live 已验证；PCCS 未观察到承担 live 状态主流** |
| Remote command | Global 已有较完整 C3 Invocation / PCCS Chronos 映射 | 中国区不可直接照搬，当前多数仍为 **MAPPING_REQUIRED** |
| Data Act M2M | Hisingen 支持官方 EU Data Portal M2M | **不能当作中国 App Token / 中国 C3 Token 使用** |
| Token 可互换性 | Web token、command token、M2M token 本身就不是一类 | 中国 Token 与 Global Token **不得假设可互换** |

核心原则：

1. **Global 登录成功不能推导 China API 可用。**
2. **China 官网网页登录成功不能推导 C3/PCCS 车辆 API 已认证。**
3. **EU Data Portal M2M Client ID/Secret 不能替代中国移动 App Token。**
4. **Global C3/PCCS RPC 名称可作为中国区研究参考，但必须逐项验证权限、Host、Proto 与返回结构。**

---

# 2. 架构总览

## 2.1 Global / Hisingen upstream

```text
Polestar ID (Global OIDC)
        │
        ├── Web/Session access + refresh token
        │       │
        │       ├── pc-api.polestar.com / mystar-v2 GraphQL
        │       │       ├── vehicle discovery
        │       │       └── telematics baseline
        │       │
        │       ├── C3 discovery
        │       │       └── dynamic C3 gRPC host
        │       │               ├── battery
        │       │               ├── exterior
        │       │               ├── availability
        │       │               ├── health
        │       │               ├── odometer
        │       │               ├── climate
        │       │               └── live streams
        │       │
        │       └── PCCS / Chronos
        │               ├── target SoC
        │               ├── amp limit
        │               ├── charge timer
        │               └── climate timer / charge location
        │
        └── Separate command-client access + refresh token
                ├── C3 Invocation remote commands
                └── app-backend / VDMS enrichment

Official EU Data Act mode (separate provider path)
        │
        ├── Account ID / Client ID / Client Secret
        ├── M2M access token
        └── /data-portal/m2m/v1/... REST telemetry
```

## 2.2 China / 当前已验证路径

```text
China Polestar account
        │
        ├── Official China Website
        │       └── browser / WKWebView website session
        │               └── 当前不能视为 vehicle API session
        │
        └── Official China Android App
                ├── app session token family
                ├── C3 controller
                │       └── Volvo Connected Car Digital Twin gRPC
                │               ├── battery
                │               ├── exterior
                │               └── live/current state
                ├── PCCS China
                └── VOCMO secondary path

本项目 Hisingen China
        │
        ├── Global flow 保留
        ├── China Official Web Sign-In 已接入
        └── China vehicle API native session：尚未完成
```

---

# 3. Global：认证与 Token

## 3.1 Global OIDC

**VERIFIED — Hisingen v2.0.10 source**

OIDC Provider：

```text
https://polestarid.eu.polestar.com
```

Hisingen 从：

```text
/.well-known/openid-configuration
```

动态获取：

- authorization endpoint
- token endpoint
- userinfo endpoint
- revocation endpoint

### Global Session Token

用途：

- `mystar-v2` GraphQL；
- 车辆发现；
- GraphQL telematics；
- C3 状态读取；
- PCCS/Chronos 大部分读取和写入。

类型：

- access token
- refresh token

特征：

- access token 为短期；
- refresh token 用于恢复会话和刷新；
- 长期会话材料由 Hisingen 存入 macOS Keychain；
- access token 不应写入日志或 Git。

### Global Command Token

**VERIFIED — Global Hisingen 使用独立 command-client authorization**

Global 远程命令不是简单复用普通 web session token。

Hisingen 维护第二组：

- command access token
- command refresh token

用途主要包括：

- C3 `invocation.InvocationService`；
- VDMS / `app-backend` 车辆信息 enrichment。

上游代码已验证：普通 web-client token 能用于主要 read/discovery，但某些 Invocation 命令依赖另一 client audience；反向也不能简单用 command-client token 替代 `mystar-v2` 普通车辆发现。

因此：

> **Global 本身就已经是“双 Token Audience”架构。不要把“Polestar access token”理解为一个全能 Token。**

### Token 值的文档边界

本仓库只记录：

- Token 类型；
- 获取方式；
- Audience/用途；
- 刷新逻辑；
- 存储位置；
- API 依赖关系。

**不记录、不提交：**

- access token 实值；
- refresh token 实值；
- authorization code；
- PKCE verifier；
- 登录密码；
- Cookie；
- 短信验证码。

---

# 4. Global：主要网络地址

**基于 Hisingen upstream v2.0.10**

| 功能 | 地址 | 状态 |
| --- | --- | --- |
| Global Polestar OIDC | `https://polestarid.eu.polestar.com` | VERIFIED |
| Mystar GraphQL | `https://pc-api.polestar.com/eu-north-1/mystar-v2/` | VERIFIED |
| Public vehicle image API | `https://pc-api.polestar.com/eu-north-1/mystar-public/` | VERIFIED |
| Polestar App Backend / VDMS | `https://pc-api.polestar.com/eu-north-1/app-backend/api/graphql` | VERIFIED |
| C3 discovery | `https://cnepmob.volvocars.com` | VERIFIED |
| C3 vehicle gRPC | discovery 返回的 `grpcHost:grpcPort` | VERIFIED / dynamic |
| PCCS / Chronos | `https://api.pccs-prod.plstr.io:443` | VERIFIED |
| EU Data Portal M2M | `https://pc-api.polestar.com/eu-north-1/data-portal/m2m/` | VERIFIED |

### C3 discovery

Hisingen 对 discovery 使用类似：

```text
Accept: application/volvo.cloud.cnepmob.v2+json
or
Accept: application/volvo.cloud.cnepmob.v1+json
Authorization: Bearer <token>
```

返回 JSON 中读取：

```text
c3.grpcHost
c3.grpcPort
```

因此 C3 真正的 vehicle gRPC Host 不是永久硬编码。

---

# 5. Global：车辆发现与状态获取

## 5.1 Vehicle Discovery

主路径：

```text
POST https://pc-api.polestar.com/eu-north-1/mystar-v2/
Authorization: Bearer <global-session-access-token>
```

GraphQL：

```graphql
query GetConsumerCarsV2 {
  getConsumerCarsV2 {
    vin
    internalVehicleIdentifier
    modelName
    modelYear
    registrationNo
    pno34
    structureWeek
  }
}
```

可选补充：

```text
https://pc-api.polestar.com/eu-north-1/app-backend/api/graphql
```

用于 VDMS enrichment，该路径在 Hisingen 中使用 command-client token，而不是普通 web token。

## 5.2 Global Telematics

Hisingen 不是只依赖单一 API。

基础数据可能来自 GraphQL：

- battery
- odometer
- health

然后按功能并行补充 C3/PCCS：

- battery extras
- availability
- exterior
- health
- software
- charging schedules
- climate
- climate timers
- trip/odometer
- connectivity
- air quality
- weather
- location
- amp limit
- charge locations
- OTA capabilities

数据最终合并进入 Hisingen 统一 `VehicleState`。

---

# 6. Global：已知 C3 / PCCS Read RPC

以下是 Hisingen v2.0.10 已实现/引用的主要 RPC。它们是 Global 端的参考，不代表 China 一定同权限、同 Host、同 Proto。

## 6.1 C3 vehicle-state

| 状态 | RPC | Host |
| --- | --- | --- |
| Battery latest | `/services.vehiclestates.battery.BatteryService/GetLatestBattery` | C3 |
| Battery live | `/services.vehiclestates.battery.BatteryService/GetBattery` | C3 |
| Availability | `/services.vehiclestates.availability.AvailabilityService/GetLatestAvailability` | C3 |
| Exterior latest | `/services.vehiclestates.exterior.ExteriorService/GetLatestExterior` | C3 |
| Exterior stream/current | `/services.vehiclestates.exterior.ExteriorService/GetExterior` | C3 |
| Health | `/services.vehiclestates.health.HealthService/GetHealth` | C3 |
| Odometer | `/services.vehiclestates.odometer.OdometerService/GetOdometer` | C3 |
| Dashboard | `/services.vehiclestates.dashboard.DashboardService/GetLatestDashboard` | C3 |
| Parking climate | `/services.vehiclestates.parkingclimatization.ParkingClimatizationService/GetLatestParkingClimatization` | C3 |
| Pre-cleaning | `/services.vehiclestates.precleaning.PreCleaningService/GetPreCleaning` | C3 |
| Pre-cleaning latest | `/services.vehiclestates.precleaning.PreCleaningService/GetLatestPreCleaning` | C3 |
| Air quality | `/services.vehiclestates.airquality.AirQualityService/GetLatestAirQuality` | C3 |
| Air quality stream/current | `/services.vehiclestates.airquality.AirQualityService/GetAirQuality` | C3 |
| Location latest | `/services.vehiclestates.location.LocationService/GetLatestLocation` | C3 |
| Location current | `/services.vehiclestates.location.LocationService/GetLocation` | C3 |
| Weather | `/services.vehiclestates.weather.WeatherService/GetLatestWeather` | C3 |

## 6.2 PCCS / Chronos

| 状态 | RPC | Host |
| --- | --- | --- |
| Target SoC | `/pccs.chronos.services.v1.TargetSocService/GetTargetSoc` | PCCS |
| Amp limit | `/pccs.chronos.services.v1.AmpLimitService/GetAmpLimit` | PCCS |
| Global charge timer | `/pccs.chronos.services.v2.GlobalChargeTimerService/GetGlobalChargeTimerStream` | PCCS |
| Charge locations | `/pccs.chronos.services.v1.ChargeLocationService/GetChargeLocations` | PCCS |
| Parking climate timers | `/pccs.chronos.services.v1.ParkingClimateTimerService/GetTimers` | PCCS |
| Location fallback | `/pccs.chronos.services.v1.LocationService/GetLocation` | PCCS |

---

# 7. Global：Remote Command API

Global Hisingen 已实现的主要控制族：

## 7.1 C3 Invocation

Service：

```text
/invocation.InvocationService
```

已实现方法包括：

- `ClimatizationStart`
- `ClimatizationStop`
- `PreCleaning`
- `Lock`
- `Unlock`
- `TailgateControl`
- `WindowControl`
- `HonkFlash`

这些命令在 Global Hisingen 中需要 command-client authorization 的场景不能用普通 web token 强行替代。

## 7.2 PCCS / Chronos writes

主要包括：

- `TargetSocService/SetTargetSoc`
- `AmpLimitService/SetAmpLimit`
- `ChargeNowService/StartOverrideChargeTimer`
- `ChargeNowService/StopOverrideChargeTimer`
- `GlobalChargeTimerService/SetGlobalChargeTimer`
- `ParkingClimateTimerService/SetTimers`
- `ParkingClimateTimerService/DeleteTimer`
- Charge Location create/update/delete

## 7.3 OTA

C3：

```text
/ota_mobcache.SchedulerService
```

主要方法：

- `Schedule`
- `InstallNow`
- `CancelSchedule`

> China 区目前不得直接把以上 Global command mapping 标记为可用；必须逐条完成真实中国区验证后才能解除 `MAPPING_REQUIRED`。

---

# 8. Global：官方 EU Data Portal / Data Act M2M

这是 Hisingen 中与非公开 Mystar/C3 路线并存的**另一套认证和数据路径**。

Base：

```text
https://pc-api.polestar.com/eu-north-1/data-portal/m2m/
```

## 8.1 M2M credentials

配置项：

- Account ID
- Client ID
- Client Secret

Token：

```text
POST /token
Content-Type: application/json

{
  "clientId": "<client-id>",
  "clientSecret": "<client-secret>"
}
```

返回短期：

- access token
- expiresIn

特点：

- 没有使用 Global 普通用户 OIDC refresh token；
- 没有使用 China App 的 access/refresh token；
- access token 到期后重新通过 client credentials 获取；
- API 请求使用 `Authorization: Bearer <m2m-token>`；
- 同时发送 `x-client-id`，Hisingen 优先用 Account ID，否则回退 Client ID；
- Hisingen upstream 内部按 **10,000 calls/day** 做配额保护。

## 8.2 Data Portal REST

车辆：

```text
GET /v1/vehicles
```

已实现的主要资源：

```text
/v1/vehicles/{VIN}/telemetry/battery
/v1/vehicles/{VIN}/telemetry/exterior
/v1/vehicles/{VIN}/telemetry/health
/v1/vehicles/{VIN}/telemetry/availability
/v1/vehicles/{VIN}/telemetry/odometer
/v1/vehicles/{VIN}/telemetry/location
/v1/vehicles/{VIN}/telemetry/parking-climatization
/v1/vehicles/{VIN}/telemetry/pre-cleaning

/v1/vehicles/{VIN}/charging/target-soc
/v1/vehicles/{VIN}/charging/amp-limit
/v1/vehicles/{VIN}/charging/charge-locations
/v1/vehicles/{VIN}/charging/is-at-charge-location
/v1/vehicles/{VIN}/charging/global-charge-timer
/v1/vehicles/{VIN}/charging/parking-climate-timer
/v1/vehicles/{VIN}/charging/charge-now
```

### 与 China 最大区别

EU Data Portal 的：

```text
Account ID + Client ID + Client Secret
```

**不能替代：**

- 中国 Polestar ID；
- 中国 App access token；
- 中国 App refresh token；
- 中国 C3 vehicle session；
- 中国 PCCS session。

---

# 9. China：登录与 Token

## 9.1 官方中国网页登录

当前 Hisingen China 已加入：

```text
https://www.polestar.cn/zh-cn/auth/login/
```

通过 WKWebView / 官方网页完成登录。

当前定义：

> **Website session only. Vehicle-provider session not verified.**

也就是说：

```text
China website login success
        ≠
China C3/PCCS vehicle API authenticated
```

当前代码不得因为网页显示“已登录”就认为：

- 可以读 SOC；
- 可以读车辆位置；
- 可以调用 C3；
- 可以调用 PCCS；
- 可以执行远程命令。

## 9.2 中国 Android App Token 家族

**VERIFIED — 已在中国官方 App 静态/运行时分析中确认这些本地 key 名存在**

```text
globle_access_token
globle_refresh_token
globle_id_token
key_token
key_refresh_token
```

注意：

1. 这里只记录**key 名称**，不记录真实 token；
2. `globle` 是 App 内实际观察到的拼写，不在文档中自行纠正；
3. 当前还不能仅根据名称断言每个 token 的最终 Audience；
4. `key_token` / `key_refresh_token` 与 `globle_*` 的完整职责边界仍需后续动态验证；
5. 真实 token、短信码、Cookie、Digital Key 私钥不得进入 Git。

## 9.3 中国登录方式

官方中国 App 已观察到手机号/短信体系参与正常登录。

本项目原则：

- 不绕过短信/2FA；
- 不抓取或记录验证码；
- 不向仓库提交真实 session；
- 如果未来实现 native China OAuth/session，只使用官方允许的登录流程和已验证 callback/token exchange。

---

# 10. China：网络地址

## 10.1 已验证/已观察 Host

| 功能 | China 地址 | 状态 |
| --- | --- | --- |
| 官方网页登录 | `https://www.polestar.cn/zh-cn/auth/login/` | VERIFIED |
| C3 discovery / static | `https://cnepmob.volvocars.com.cn` | VERIFIED |
| C3 token-related host | `cepmobtoken.prod.c3.volvocars.com.cn` | VERIFIED host observed；method mapping incomplete |
| C3 LBS-related host | `cepmob.lbs.prod.c3.volvocars.com.cn` | VERIFIED host observed；method mapping incomplete |
| C3 signature-related host | `cepmobsig.prod.c3.volvocars.com.cn` | VERIFIED host observed；method mapping incomplete |
| PCCS China | `https://api.pccs-prod.origin.polestar.cn` | VERIFIED |
| China Android package | `com.polestar.china.android` | VERIFIED |

这些地址与 Global 端的 `.com` / `plstr.io` 后端不能直接互换。

---

# 11. China：车辆状态获取链路

## 11.1 Battery 实际运行链路

**VERIFIED — 中国官方 App 运行时追踪**

```text
CarMainFragment
    ↓
CarMainViewModel.requestUpdate
    ↓
CarBatteryInteractorImpl.requestBatteryStatus
    ↓
hybrid client layer
    ↓
C3BatteryController
    ↓
Volvo Connected Car Digital Twin battery gRPC
    ↓
GetBatteryResponse
    ↓
ChargeStatusResponseModel
    ↓
UI
```

这证明中国 App 的首页实时电池信息不是简单从本地 Room 缓存读取。

一次实际验证中：

```text
Live UI / model:
SOC                  72.0 %
estimated range      300 km
average consumption  19.1
charging state       Unplugged
timestamp            1792472861622

stale local Room sample:
SOC                  44 %
range                180 km
```

因此：

> **分析中国状态时必须区分 Live C3 model 与本地持久化缓存。**

## 11.2 ChargeStatusResponseModel 已确认字段

项目运行时分析确认以下字段承载：

| 字段 | 含义 |
| --- | --- |
| `cliif` | SOC |
| `clicatch` | km range |
| `cliclass` | miles range |
| `cliint` | average consumption |
| `clido` | timestamp |

这些是逆向后的混淆字段名，后续 App 升级可能改变。

---

# 12. China：C3 / VOCMO / PCCS 主备关系

当前证据：

- **C3 = primary**
- **VOCMO = secondary / fallback-related**
- **PCCS = charging/control-related backend；当前未观察到其承担首页实时状态主流**

Stage 12 运行时验证中：

- C3 exterior live：观察到有效调用；
- PCCS live：未观察到对应实时主流；
- Battery 首页数据通过 C3 路径更新。

因此 China Hisingen 实现时的优先顺序应是：

```text
C3 live/current state
    ↓
C3 latest state
    ↓
其他已验证 backend fallback
    ↓
local cache only as stale/fallback presentation
```

而不是：

```text
local database
    ↓
guess current vehicle state
```

---

# 13. Global vs China：状态源详细对照

| 数据 | Global Hisingen | China 已验证 | China Hisingen 当前状态 |
| --- | --- | --- | --- |
| 车辆列表 | Mystar `getConsumerCarsV2` + optional VDMS | 中国 App 自有账户链路 | NOT IMPLEMENTED |
| SOC | GraphQL battery + C3 BatteryService | C3 battery live/current | MAPPING REQUIRED |
| 续航 | GraphQL/C3 battery | `ChargeStatusResponseModel` 已确认 km/miles | MAPPING REQUIRED |
| 充电状态 | C3 battery + GraphQL merge | C3 model 已确认 `Unplugged` 等状态 | MAPPING REQUIRED |
| 平均能耗 | Global telemetry/domain mapping | China model `cliint` 已确认 | MAPPING REQUIRED |
| Exterior | C3 `ExteriorService` | C3 live 已观察 | MAPPING REQUIRED |
| Availability | C3 | 尚未完成 China 独立字段验证 | MAPPING REQUIRED |
| Health | GraphQL + C3 | 尚未完成完整 China 映射 | MAPPING_REQUIRED |
| Odometer | GraphQL + C3 | 尚未完成完整 China 映射 | MAPPING_REQUIRED |
| Climate status | C3 | 已确认 China CarInformation / climate capability 部分信息 | MAPPING_REQUIRED |
| Target SoC | PCCS Chronos | China PCCS host 已确认，方法级待核 | MAPPING_REQUIRED |
| Amp limit | PCCS Chronos | 方法级待核 | MAPPING_REQUIRED |
| Location | C3 / PCCS fallback | C3 LBS host 已观察 | EXPERIMENTAL |
| Software/OTA | C3 OTA services | 尚未完成 China 端到端 | MAPPING_REQUIRED |
| Remote lock/unlock | C3 Invocation | 中国区权限和 RPC 尚未完成验证 | MAPPING_REQUIRED |
| Remote climate | C3 Invocation | 中国请求模型已发现，但需端到端验证 | MAPPING_REQUIRED |

---

# 14. Climate：Polestar 2 China 已确认边界

针对当前项目验证车辆：

```text
parkingClimatizationMaxRuntime = 30
supportsParkingClimatizationDirectStartSettings = false
```

已分析的 PCCS/Volvo `ClimatizationStartRequest` 中可见：

- invocation request
- start
- temperature
- seat heating
- steering-wheel heating

但**没有发现用户自定义 duration 字段**。

因此本项目当前策略：

- 不添加自定义 5–30 分钟 climate duration 控件；
- 保留原生 start / stop；
- 可以展示 provider 实际返回的状态/剩余时间（若可验证）；
- 不把 `maxRuntime=30` 错误解释为“API 支持任意 1–30 分钟参数”。

---

# 15. Token 对照总表

| Token / Credential | Global | China | 是否可互换 |
| --- | --- | --- | --- |
| Polestar web/session access token | OIDC session，Mystar/C3/PCCS reads | 中国 App 存在 `globle_access_token` 类本地项 | **不可假设** |
| Polestar web/session refresh token | Global Keychain session restore | `globle_refresh_token` 已观察 | **不可假设** |
| ID token | Global 并非 Hisingen 车辆状态主授权核心 | `globle_id_token` 已观察 | 否 |
| Command access token | Global 独立 command audience | 中国区对应关系待验证 | MAPPING_REQUIRED |
| Command refresh token | Global 独立 browser authorization 后持久化 | 中国区对应关系待验证 | MAPPING_REQUIRED |
| `key_token` | NOT APPLICABLE / no direct same-name mapping | China App 已观察 | Unknown |
| `key_refresh_token` | NOT APPLICABLE / no direct same-name mapping | China App 已观察 | Unknown |
| EU Data Portal M2M access token | Client ID/Secret grant | 不是中国 App token | **否** |
| Data Portal Client Secret | EU Data Act M2M | 中国 C3 登录不使用此机制 | **否** |
| Public image API key | 非用户级静态 key | China 是否同资源策略未确认 | 不作为用户授权 |

---

# 16. 当前 Hisingen China 实现状态

截至本文件首次提交：

## 已完成

- Global Hisingen 原有登录逻辑保留；
- 增加 `Polestar Sign-In Region…`；
- 增加：
  - `Global Polestar ID`
  - `China Polestar ID · Official Web Sign-In`
- China web 模式打开官方：
  `https://www.polestar.cn/zh-cn/auth/login/`
- WKWebView 使用持久 `WKWebsiteDataStore.default()`；
- 不主动读取表单密码；
- 不主动提取 Cookie；
- 不把网页登录结果伪装成车辆 API 登录成功。

## 尚未完成

- China native vehicle API token exchange；
- China C3 authenticated session；
- China 车辆列表；
- China Hisingen SOC/续航实时显示；
- China C3 live stream；
- China PCCS method mapping；
- China remote commands；
- China token refresh lifecycle；
- China session → macOS Keychain 的正式安全模型。

---

# 17. China Hisingen 后续实现建议

优先级：

### P0 — Read-only 状态

1. 获取合法、已验证的 China vehicle API session；
2. 车辆发现；
3. C3 discovery；
4. Battery；
5. Exterior；
6. Availability；
7. Odometer/Health；
8. 统一映射到 Hisingen `VehicleState`。

### P1 — Live stream

- battery live
- exterior confirmation/live
- reconnect/backoff
- timestamp / freshness

### P2 — Charging / climate settings

- PCCS China RPC method verification；
- Target SoC；
- Amp limit；
- climate state；
- timers。

### P3 — Remote control

必须逐条验证：

- endpoint；
- token audience；
- command permission；
- request proto；
- result semantics；
- vehicle-side confirmation。

在完成验证前一律保持：

```text
MAPPING_REQUIRED
```

---

# 18. 安全与仓库规则

本仓库为公开代码库时，**绝对禁止提交：**

- access token；
- refresh token；
- ID token；
- command token；
- SMS/2FA code；
- Cookie；
- Authorization header；
- Digital Key secret/private key；
- Client Secret 实值；
- 完整 VIN；
- 手机号；
- 精确车辆位置；
- 未脱敏抓包；
- 未脱敏 Frida / logcat 输出。

允许提交：

- Host；
- path；
- RPC 名称；
- Proto 字段结构（不含真实用户数据）；
- token 类型名称；
- SharedPreferences key 名；
- 假值/fixture；
- 已脱敏日志。

示例占位：

```text
<access-token-redacted>
<refresh-token-redacted>
<VIN-redacted>
<phone-redacted>
```

---

# 19. 关键区别：不要混淆的四组概念

## 19.1 Website login vs Vehicle API login

```text
www.polestar.cn 登录成功
!=
C3/PCCS 授权完成
```

## 19.2 Capability vs State

```text
supportsParkingClimatization = true
!=
climate is currently running
```

## 19.3 Provider ACK vs Vehicle physical state

```text
command accepted
!=
vehicle has completed the action
```

远程动作完成后应以新鲜车辆状态再次确认。

## 19.4 Cache vs Live

```text
Room / local cached SOC
!=
C3 current SOC
```

China 官方 App 已实际出现过：

```text
local cache 44 %
live C3     72 %
```

因此 UI 必须保留 freshness/timestamp 概念。

---

# 20. 版本追踪

| 日期 | 文档版本 | Hisingen 基线 | China App 基线 | 变更 |
| --- | --- | --- | --- | --- |
| 2026-10-21 | 1.0 | v2.0.10 | 4.25.0-262 | 首次建立 Global / China API、Token、Host、状态获取完整差异表 |

---

# 21. 后续每次更新的检查清单

修改 China API / Hisingen 集成时检查：

- [ ] 是否出现新 Host？
- [ ] 是否出现新的 OIDC/token audience？
- [ ] 是否改变 token refresh 规则？
- [ ] 是否改变 C3 discovery？
- [ ] 是否新增/删除 RPC？
- [ ] 是否改变 PCCS Host 或服务版本？
- [ ] 是否改变 vehicle discovery？
- [ ] 是否改变 Battery/Exterior/Availability 主数据源？
- [ ] 是否新增 live stream？
- [ ] 是否改变字段映射？
- [ ] 是否改变命令权限？
- [ ] 是否确认 command ACK 与真实状态？
- [ ] 是否清除文档中的敏感值？
- [ ] 是否更新“版本追踪”表？

---

# 22. 来源与证据边界

本文件综合以下证据：

1. **Hisingen upstream v2.0.10 source**
   - `PolestarAPI.swift`
   - `PolestarAPI+Authentication.swift`
   - `PolestarAPI+Telemetry.swift`
   - `PolestarGRPC.swift`
   - `PolestarGRPCCapabilities.swift`
   - `PolestarGRPCRemote.swift`
   - `PolestarDataPortalAPI.swift`
   - upstream `docs/api/*`

2. **本项目对中国官方 Android App 的静态与动态验证**
   - package: `com.polestar.china.android`
   - version: `4.25.0-262`
   - C3 controller / battery call chain
   - runtime ChargeStatusResponseModel
   - C3/PCCS China Host
   - token preference key names
   - C3 live observations

3. **当前仓库 Hisingen China patch**
   - China Official Web Sign-In
   - Global / China region selection

当某一项没有完整证据时，本文件必须明确写为：

```text
STRONG-INFERENCE
EXPERIMENTAL
MAPPING_REQUIRED
NOT IMPLEMENTED
```

而不是为了“看起来完整”而把推测写成事实。

---
## 2026-10-26 补充：第三方 Android Companion APK 静态研究（非官方 App）

本项目另取得第三方研究用 APK `app.c3garage.companion` v0.1.0（参考样本 SHA-256：
`8cae07a3b5c6050487443914ebb55b1d2f2f26e258f4b369837f2b88de052735`）。
**它不是**前文的极星中国官方 App `com.polestar.china.android`，也不是原生 Hisingen。
只将其当作 API 和 UI 结构的辅助线索，不能把该 APK 静态方法存在认定为官方 API 已经对本项目开放。

- 静态观察到 `https://gw.app.polestar-auto.com`、`/user/api/v2/oauth/login/password`、
  `/user/api/v2/oauth/refresh-global-token`、`https://cnepmob.volvocars.com.cn` 和 C3
  Battery/Exterior/Odometer/ParkingClimatization/PreCleaning 服务名。
  完整参数、Token audience、Proto 字段编号及实际成功率 **MAPPING_REQUIRED**。
- `PolestarApi$Vehicle` DEX 字段表独立计数为 **33 项**，只是模型字段，不等于验证了 33 项实时数据；
  如需字段列表，查 `knowledge/11_VEHICLE_DATA_DICTIONARY.md` 与证据 JSON。
- 该参考 APK 包含远控、Widget、WorkManager 的相关类/方法，但未经本项目真实账户/车辆运行验证。
  两份外部 AI 报告指出的潜在问题统一收录于 `knowledge/10_TWO_REPORTS_RECONCILED.md`。
- 2026-10-26 阶段决策：Android 独立工程先采用离线 DEMO，所有真实登录/写入远控暂不接入；
  R8、混淆、发行加固仍 **DEFERRED**。不向仓库写入账号、令牌、Cookie、真实 VIN 或客户端签名盐。

此附录的第三方 APK 证据，**不能覆盖**本文件对官方 China App C3 Live 的既有验证结论。

## Android v0.4.0-beta1 补充

Android 中国区新增模板方法体映射的 TailgateControl、Lock、PreCleaning 与 Climate/Air 只读状态、系统强生物识别门槛和可选后台通知。仅标 IMPLEMENTED_NEEDS_DEVICE_VERIFICATION。Mac/Hisingen 与 Global 当前实现和认证边界不受此 Android 分支改动影响。


## 10. v0.7 Android / China Health 对齐（2026-10-26）

- China Android 现在按 Mac/Hisingen 研究接入 `/services.vehiclestates.health.HealthService/GetHealth`。
- 当前 Hisingen 源码对 Polestar 5.11.1 Health 的解析：四轮胎压报警为 fields 9–12，实际压力为 fields 39–42（kPa），顺序左前/右前/左后/右后；43/44 是参考压力，不可误当四轮读数。
- Health field 3/4 对应剩余保养天数/里程研究值；Android v0.7 仍以用户指定的本地 365 天 / 20,000 km 规则为业务显示基准，等待 China 实车验证后再决定是否融合官方值。


## 20. Android v0.7.5 Health 聚合补充（2026-10-26）

Global Hisingen `PolestarGRPCCapabilities.parseHealth` 的源码级字段已经再次确认：

- washer fluid：Health field 13，值大于 1 为告警；这是**告警状态，不是液位测量值**；
- low-voltage / 12V battery：field 38，值 2 为告警；
- tyre/iTPMS warning：fields 9–12；
- tyre numeric research fields：39–42，但 Polestar 2 不保证返回，China Android UI 默认不展示；
- service days / distance：fields 3 / 4。

Android China 分支已使用同名 `HealthService/GetHealth`，v0.7.5 同一次只读调用会解析上述 12V/washer/tyre warning 为统一 `VehicleHealthSnapshot`。此处属于**Global 源码映射 + China 实现接入**，不是 China 官方 App 实车验证。

中国官方 App 的现有历史静态证据将车辆主页/提示集中到 `com.polestar.carcontrol`（如 `CarMainFragment`、`CarMainTipsProvider`），但当前可访问证据未直接定位 13/38/9–12 的官方 China App 调用，因此仍须保持 `MAPPING_REQUIRED / PROJECT_DEVICE_VERIFICATION` 边界。


## Android v0.7.6 读取策略更新

Health/GetHealth 按上游定义改为服务端流式读取首条即取消，4 秒截止时间；按车辆缓存 5 分钟并保留原时间。玻璃水/12V 未指定枚举不再视为正常。该修复只落实已核查的传输/字段定义，不表示中国极星实车验证完成。空调倒计时仅本地展示，不发送远程停止请求。


## 2026-10-27 Android v0.7.8 最新参考增量

对照 Mac China `integrate/hisingen-china-0.19.5` 的 f50820bd9c34c3bc56d18b51543c60ddcc6bde91 和上游 Hisingen v2.0.10；不修改 Mac 功能。最新用户参考 APK 0.1.20261027.081805 已确认 China 可用，现明确：

- Health 主路径 `GetLatestHealth` UNARY；旧 GetHealth 流式为 UNIMPLEMENTED 回退。Health payload 2/3/4 分别为**剩余**小时/天/km，新增健康项见 Android KB25；旧本地保养为唯一来源的记录已过时。
- 独立 c3Lbs.grpcHost → dtlinternet.DtlInternetService/GetLastParkedLocation，请求1 VIN，响应payload2，1经度/2纬度/3时间戳。Android 四套皮肤新增按需二级预览；高德是可选用户 Key 静态图，无后台定位。
- 旧公开 Battery12=TA平均能耗，与当前上游 Battery12=电池容量冲突。Android 只采纳独立 TA Wh/同源距离/时间可印证的数据，计算值标 ~，缺失/冲突 --。Mac China 当前没有原始 TA/TM 行驶时长，Android 距离/均速估算同样标 ~。
- REFERENCE_USER_VERIFIED 仅属于参考 APK；Android 合成测试与构建不代表新的实车协议验证。油液未上报、iTPMS数值、坐标系统/地图偏移和真实 TA 能量仍有验收边界。

映射与证据：[Android v0.7.8](../knowledge/25_V078_CONDITION_LOCATION_TRIP.md)。


## 2026-10-27 Android v0.7.9 地图显示层

按用户最新选择，用官方 https://uri.amap.com/marker 位置网页替代需 Key 的 staticmap；App 二级弹窗使用受限系统 WebView，不改变 Mac 工程或 China c3Lbs 坐标轴（1经度/2纬度）。读取失败不清会话，独立4秒 RPC 截止，按选中VIN读取并丢弃切车/退出后的旧结果。无地图 SDK、新定位权限或后台地图；网页开销/可用性和坐标偏移仍需实机验证。见 [v0.7.9](../knowledge/26_V079_KEYLESS_PARKING_WEB.md)。


## 2026-10-27 Android v0.8.3 显示层

基于9744759：保养公里/天/工时仅用GetLatestHealth已确认字段，不再用本地保养记录填补，百分比固定参考周期而非新发现的车辆百分比。地图仍由c3Lbs读取，网页覆盖层精简；新增默认关闭、明确系统授权的手机前台位置，仅向可信高德网页开放，无后台定位。新PSeu/Widget原生适配与公用图标不改变China远控协议。Mac功能代码不变。见 [Android知识28](../knowledge/28_V083_UI_WIDGET_MAP_ICONS.md)。


## Android v0.8.4 Target SoC补充证据

中国官方classes7.dex证实C3 `chronos.services.v1.TargetSocService/GetLatestTargetSoc`（非PCCS前缀），请求嵌套ChronosRequest(id1,vin2)，响应TargetSoc3目标百分比1、更新时间3，pending4不作为当前值。Android可选4秒只读实现与缺失限流见[KB29](../knowledge/29_V084_ICONS_SERVICE_TARGETSOC_READBACK.md)；静态映射通过，不冒充中国实车验收。最新模板保养剩余映射不变，参考里程按用户设定统一30000km。


## Android v0.8.5 独立安装与数据呈现

账户签名算法与车辆RPC不改，配置由私有构建注入自身资产，运行时不探测或导入其他APK。车况以异常/已报告项目优先、未知单独折叠；不把12V/制动液告警当电压/液位。见[KB30](../knowledge/30_V085_INLINE_CONDITION_STANDALONE.md)。


## v0.8.7 当前增量 · 2026-10-28

两份不同模板的ClimatizationStart字段3=float32，全部调用22.0f；目标回报ParkingClimatization字段8。历史China P2 supportsParkingClimatizationDirectStartSettings=false保持有效；Mac integrate/hisingen-china-0.19.5 f50820b远控unsupported。v087仅增加默认关闭的App实验范围18–28°C与回报校验，未证明车端能力。详见KB32及reference-climate-start-20261028.json。
