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
| Hisingen 当前可用认证 | OIDC/PKCE + Refresh Token；另有 command-client 授权 | 官方移动端 Gateway 协议已静态还原；**Hisingen 当前仍只集成 Official Web Sign-In UI，尚未实现 native vehicle session** |
| 车辆发现 | `mystar-v2` GraphQL `getConsumerCarsV2`；可选 VDMS enrichment | 先请求 China C3 discovery，再请求动态 `<c3.url>/car-information/car`；Hisingen 尚未实现 |
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
5. **China 车辆 REST/gRPC 使用登录响应中的 Global Access Token，不使用网页 Cookie、Local Access 或 Global ID Token。**

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
                ├── official captcha slider
                │       └── sessionId / sig / token
                ├── gw.app.polestar-auto.com
                │       └── phone/password login
                │               ├── Local Access / Refresh / userUid
                │               └── Global Access / Refresh / ID Token
                ├── China C3 discovery (no bearer)
                │       └── dynamic c3.url / grpcHost / grpcPort
                ├── car-information + C3/PCCS
                │       └── Bearer Global Access
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

**VERIFIED（static）— Android 4.25.0-262 的 model、登录处理与持久化调用已经闭环**

```text
LoginToken
  ├── accessToken / refreshToken / expiresIn / userUid
  └── globleToken
       ├── accessToken / refreshToken / idToken
       └── tokenType / expiresIn / userUid / userName
```

phone/password/onetouch 的 `LoginToken` response schema 同时定义两组 Token；成功处理会保存外层 Local 数据，并在 `globleToken != null` 时保存 Global 数据。App 的本地 key 对应：

```text
Local:   key_token / key_refresh_token / key_token_expire_in
Global:  globle_access_token / globle_refresh_token / globle_id_token / ...
```

职责边界：

| Token | 已确认用途 |
| --- | --- |
| Local Access | Pscn Gateway bearer；同时发送 `psoneid: <Local userUid>` |
| Local Refresh | `uc.app.polestar-auto.com/oauth/token` |
| Global Access | 动态 `car-information`、C3 gRPC、PCCS bearer |
| Global Refresh | Gateway `/user/api/v2/oauth/refresh-global-token` |
| Global ID Token | Remote Car auth state/身份信息；不是车辆 REST/gRPC bearer |

注意：

1. 这里只记录**key 名称**，不记录真实 token；
2. `globle` 是 App 内实际观察到的拼写，不在文档中自行纠正；
3. Audience 结论来自 interceptor/auth provider 调用链，不是仅根据 key 名猜测；
4. 真实 token、短信码、Cookie、内置 client secret、Digital Key 私钥不得进入 Git。

## 9.3 中国登录方式

官方中国 App 的手机号链路已经还原：

```text
官方 auth-slider H5
  → sessionId / sig / token
  → POST /user/api/v1/credcode/send
  → POST /user/api/v2/oauth/login/phone
  → LoginToken(Local + nested Global)
```

验证码发送 JSON：

```text
principal
aliScene = ic_login_h5
aliSessionId
aliSign
aliToken
clientId = polestar-app-android
type = SIGNIN_BY_PHONE
```

验证码登录 JSON：

```text
clientId = polestar-app-android
phone
credential
autocrt = true
```

另有已确认的密码登录：

```text
POST /user/api/v2/oauth/login/password
clientId / phone / password / autocrt
```

Retrofit interface 也声明了 `/user/api/v1/credcode/send/login`，但当前 `VerificationCodeLoginViewModel` 的已证明调用链使用 `/user/api/v1/credcode/send`，不能仅凭 endpoint 名称替换。

## 9.4 Pscn Gateway 公共 Header

对 `gw.app.polestar-auto.com`，官方 client 自动添加：

```text
Content-Type: application/json
Version: 4.25.0
Device: Android
X-User-Agent: <Android Build.MODEL>
```

已有 Local Access 后再添加：

```text
Authorization: Bearer <Local Access>
psoneid: <Local userUid>
```

Pscn Account Retrofit 的 OkHttp builder 没有 `SignInterceptor`。App 中另有一套带 `X-SIGNATURE`、`X-TIMESTAMP`、nonce 和设备头的 `baselinelibrary` client，但它不属于上述登录链。

因此旧版本的“缺少公共参数”至少涉及正确 endpoint、滑块字段、JSON 字段和这组固定 header；服务器具体先拒绝哪一项仍需脱敏动态验证。

## 9.5 Token 刷新

Local refresh：

```text
POST https://uc.app.polestar-auto.com/oauth/token
Content-Type: application/x-www-form-urlencoded
Retrofit @QueryMap: grant_type / client_id / client_secret / refresh_token
```

Global refresh：

```text
POST https://gw.app.polestar-auto.com/user/api/v2/oauth/refresh-global-token
Authorization: Bearer <Local Access>
psoneid: <Local userUid>
JSON: refreshToken = <Global Refresh>
```

Remote Car auth provider 在 Global Access 剩余有效期不超过 60 秒时刷新。

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
| 官方滑块 | `https://h5.app.polestar-auto.com/account-components/auth-slider/index.html` | VERIFIED（static） |
| Pscn Gateway | `https://gw.app.polestar-auto.com` | VERIFIED（static） |
| Local OAuth refresh | `https://uc.app.polestar-auto.com/oauth/token` | VERIFIED（static） |
| C3 discovery | `https://cnepmob.volvocars.com.cn` | VERIFIED（static）；请求本身不带 bearer |
| C3 vehicle REST | discovery 返回的动态 `c3.url` | VERIFIED（static） |
| C3 vehicle gRPC | discovery 返回的动态 `c3.grpcHost:c3.grpcPort` | VERIFIED（static） |
| C3 token-related host | `cepmobtoken.prod.c3.volvocars.com.cn` | VERIFIED host observed；method mapping incomplete |
| C3 LBS-related host | `cepmob.lbs.prod.c3.volvocars.com.cn` | VERIFIED host observed；method mapping incomplete |
| C3 signature-related host | `cepmobsig.prod.c3.volvocars.com.cn` | VERIFIED host observed；method mapping incomplete |
| PCCS China | `https://api.pccs-prod.origin.polestar.cn` | VERIFIED |
| China Android package | `com.polestar.china.android` | VERIFIED |

这些地址与 Global 端的 `.com` / `plstr.io` 后端不能直接互换。

---

# 11. China：车辆状态获取链路

## 11.1 Vehicle Discovery

官方 Remote Car Service 是两阶段发现：

```text
GET https://cnepmob.volvocars.com.cn/
Retrofit annotation: @GET(".")
Accept: application/volvo.cloud.cnepmob.v1+json
User-Agent: <RCS user agent>
（该 discovery client 静态构造为 auth = null）
        ↓
response.c3.url / grpcHost / grpcPort / grpcKeepAliveTime
response.c3Lbs.url / grpcHost / grpcPort / grpcKeepAliveTime
        ↓
GET <c3.url>/car-information/car
Authorization: Bearer <Global Access>
User-Agent: <RCS user agent>
```

按 VIN 的有限信息接口为：

```text
GET <c3.url>/car-information/car/{vin}
```

因此不能把 `https://cnepmob.volvocars.com.cn/car-information/car` 当成固定地址。`cnepmob` 负责环境发现，车辆列表 base URL 来自动态 `c3.url`。

REST `TokenInterceptor` 与 gRPC `GrpcCredentials` 都调用同一个 Remote Car auth provider；该 provider 最终返回 fresh Global Access Token。

## 11.2 Battery 实际运行链路

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

## 11.3 ChargeStatusResponseModel 已确认字段

项目运行时分析确认以下字段承载：

| 字段 | 含义 |
| --- | --- |
| `cliif` | SOC |
| `clicatch` | km range |
| `cliclass` | miles range |
| `cliint` | average consumption |
| `clido` | timestamp |

这些是逆向后的混淆字段名，后续 App 升级可能改变。

## 11.4 Layout Inspect 已确认的车辆控制页边界

**VERIFIED — 真机 Layout Inspect View 创建栈；STATIC-VERIFIED — 官方 APK 4.25.0-262**

车辆控制页由 `CarMainFragment` 和多 Provider 列表构造。已捕获 header/charging progress、command item、control command、climate/switch、Bluetooth Key 与 tips 模块；`CarcontrolMain*Binding`、`CarCommandButton`、`CarSwitchButton` 和 `CarChargingProgressBar` 可作为跨混淆分析入口。

完整 APK 将点击回调继续追踪到闪灯、鸣笛、车锁、后备箱、车窗、空调启停和空气净化的 Interactor/controller。充电口 type `2` 只存在于通用扩展模型；**Polestar 2 的能力配置不包含该按钮，且 C3/PCCS/VOCMO 三类 controller 均返回 unsupported**。详细证据、资源映射和限制见 [极星中国 App Layout Inspect 运行时证据](CHINA_LAYOUT_INSPECTOR_RUNTIME_EVIDENCE.md)。

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
| 车辆列表 | Mystar `getConsumerCarsV2` + optional VDMS | dynamic `<c3.url>/car-information/car` + Global Access | IMPLEMENTED_UNVERIFIED |
| SOC | GraphQL battery + C3 BatteryService | C3 battery live/current | IMPLEMENTED_UNVERIFIED |
| 续航 | GraphQL/C3 battery | `ChargeStatusResponseModel` 已确认 km/miles | IMPLEMENTED_UNVERIFIED |
| 充电状态 | C3 battery + GraphQL merge | C3 model 已确认 `Unplugged` 等状态 | IMPLEMENTED_UNVERIFIED |
| 平均能耗 | Global telemetry/domain mapping | China model `cliint` 已确认 | IMPLEMENTED_UNVERIFIED |
| Exterior | C3 `ExteriorService` | C3 live 已观察 | IMPLEMENTED_UNVERIFIED |
| Availability | C3 | China C3 family 已确认；真实响应语义待验 | IMPLEMENTED_UNVERIFIED |
| Health | GraphQL + C3 | China C3 family 已确认；真实响应语义待验 | IMPLEMENTED_UNVERIFIED |
| Odometer | GraphQL + C3 | China C3 family 已确认；单位/时间戳待验 | IMPLEMENTED_UNVERIFIED |
| Climate status | C3 | 已确认 China CarInformation / climate capability 部分信息 | IMPLEMENTED_UNVERIFIED |
| Target SoC | PCCS Chronos | China PCCS host/proto 已确认 | IMPLEMENTED_UNVERIFIED（read only） |
| Amp limit | PCCS Chronos | China PCCS host/proto 已确认 | IMPLEMENTED_UNVERIFIED（read only） |
| Location | C3 / PCCS fallback | C3 LBS host 已观察 | IMPLEMENTED_UNVERIFIED；必须脱敏 GPS |
| Software/OTA | C3 OTA services | 尚未完成 China 端到端 | MAPPING_REQUIRED |
| Remote lock/unlock | C3 Invocation | `lock` / `unlock` 与 Proto 已静态确认 | DISABLED |
| Remote climate | C3 Invocation | `climatizationStart` / `climatizationStop` 与 Proto 已静态确认 | DISABLED |

## 13.1 China 远程命令静态 RPC 映射

**STATIC-VERIFIED — 官方 APK `4.25.0-262`；Mac 0.19.5 仍全部 fail-closed**

| 功能 | C3 Invocation 方法 | 请求模型 |
| --- | --- | --- |
| 车锁/解锁 | `lock` / `unlock` | `LockRequest` / `UnlockRequest` |
| 只解锁后备箱 | `unlock` | `UnlockRequest(UNLOCK_TYPE_TRUNK_ONLY)` |
| 车窗全开/全关 | `windowControl` | `WindowControlRequest(OPEN_ALL/CLOSE_ALL)` |
| 尾门打开/关闭 | `tailgateControl` | `TailgateControlRequest(OPEN_TAILGATE/CLOSE_TAILGATE)` |
| 鸣笛/闪灯 | `honkFlash` | `HonkFlashRequest` |
| 空调启动/停止 | `climatizationStart` / `climatizationStop` | `ClimatizationStartRequest` / `ClimatizationStopRequest` |
| 空气净化 | `preCleaning` | `PreCleaningRequest` |

方法和 Proto 存在不等于账号具备 command audience，也不等于服务端 ACK 或车辆完成动作。只有逐命令验证权限、请求、ACK 和后续新鲜车辆状态后才能提升状态。

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
| Polestar web/session access token | OIDC session，Mystar/C3/PCCS reads | `globle_access_token`；已确认供 car-information/C3/PCCS 使用 | **不可跨区域互换** |
| Polestar web/session refresh token | Global Keychain session restore | `globle_refresh_token`；经 Pscn Gateway 刷新 | **不可跨区域互换** |
| ID token | Global 并非 Hisingen 车辆状态主授权核心 | `globle_id_token`；不是车辆 REST/gRPC bearer | 否 |
| Command access token | Global 独立 command audience | 中国区对应关系待验证 | MAPPING_REQUIRED |
| Command refresh token | Global 独立 browser authorization 后持久化 | 中国区对应关系待验证 | MAPPING_REQUIRED |
| `key_token` | NOT APPLICABLE / no direct same-name mapping | China Local Access；Pscn Gateway bearer | 否 |
| `key_refresh_token` | NOT APPLICABLE / no direct same-name mapping | China Local Refresh；`uc.../oauth/token` | 否 |
| EU Data Portal M2M access token | Client ID/Secret grant | 不是中国 App token | **否** |
| Data Portal Client Secret | EU Data Act M2M | 中国 C3 登录不使用此机制 | **否** |
| Public image API key | 非用户级静态 key | China 是否同资源策略未确认 | 不作为用户授权 |

---

# 16. 当前 Hisingen China 实现状态

当前基线为 `integrate/hisingen-china-0.19.5`，上游 Hisingen `v2.0.10`。

## 已完成

- Global Hisingen 原有登录逻辑保留；
- 独立 native China 手机/SMS session、专用 Keychain 和 Local/Global token 刷新已实现；
- China/Global 区域选择会重建 provider，Global 账号与车辆缓存保持隔离；
- public C3 discovery、动态 `c3.url` 车辆列表、C3/PCCS read path 和 live stream wiring 已实现；
- 多种车辆列表 JSON 形态、VIN 去重、token 轮换兼容和可选 PCCS capability gate 已实现；
- Android/localhost Bridge 已从活动源码和构建产物中删除；
- China 远程命令在 UI capability 与 provider 执行两层保持 fail-closed；
- 完整解析 Android 4.25.0-262 的 16 个 DEX；
- 还原滑块 → SMS → phone/password login → Local/Global Token；
- 还原 Local/Global refresh；
- 确认动态 C3 discovery → car-information 两阶段车辆发现；
- 确认 car-information/C3/PCCS 使用 Global Access Token。

## 尚未完成

- 真实账号完成滑块、单次 SMS、Local + Global token 签发；
- Local/Global token 到期刷新、撤销重试和应用重启恢复；
- 真实账号的动态 C3 discovery、车辆列表与多车选择；
- Battery、Exterior、Availability、Odometer、Health 同分钟官方 App 对照；
- C3 live stream 重连和 PCCS read 的方法级授权验证；
- China 远程命令授权与车辆结果验证；在此之前继续禁用；
- Apple Developer ID 签名与 notarization。

---

# 17. China Hisingen 后续验证顺序

优先级：

### P0 — 账号与只读状态

1. 使用自有账号完成一次官方 auth-slider → SMS → login；
2. 只记录状态码、字段名、token family 是否存在与 expiry 行为；
3. 验证 Keychain restore、Local/Global refresh 和 sign-out 删除；
4. 验证 public discovery → 动态 `<c3.url>/car-information/car`；
5. 同分钟对照 Battery / Exterior / Availability / Odometer / Health；
6. 核对时间戳、单位、stale/unknown 和多车选择。

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

在完成逐命令验证前一律保持：

```text
DISABLED
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
| 2026-10-25 | 1.1 | v2.0.10 / China 0.19.3 设计基线 | 4.25.0-262 | 完成 16 DEX 认证、公共头、Token、动态车辆发现证据链；纠正 SMS endpoint 与固定 car-information 地址假设 |
| 2026-10-26 | 1.2 | v2.0.10 / China 0.19.5 | 4.25.0-262 | 核对全部远端分支；加入 APK 身份、云端 Java/UI 运行时、Layout Inspect、C3 Invocation 映射及 Polestar 2 无充电口按钮结论 |

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
   - APK 身份、签名、商店来源与 DEX 一致性：`docs/CHINA_ANDROID_APK_PROVENANCE.md`
   - 16 个 DEX：Login ViewModel / PscnAccountRepo / Retrofit / OkHttp / Token model / Remote Car Service
   - 认证与车辆发现完整证据链：`docs/POLESTAR_CHINA_ANDROID_AUTH_EVIDENCE.md`
   - 真机 Layout Inspect View 创建栈与 APK 控制器交叉验证：`docs/CHINA_LAYOUT_INSPECTOR_RUNTIME_EVIDENCE.md`
   - 云端 ADB/root、Frida、Java/UI 与 metadata-only 网络观察：`docs/ANDROID_CLOUD_RUNTIME_EVIDENCE.md`
   - C3 controller / battery call chain
   - runtime ChargeStatusResponseModel
   - C3/PCCS China Host
   - token preference key names
   - C3 live observations

3. **当前仓库 Hisingen China 0.19.5 集成线**
   - 分支盘点与选择性移植依据：`docs/BRANCH_INVENTORY_AND_0195_PLAN.md`
   - native 手机/SMS session、独立 Keychain 与 Local/Global token refresh
   - Global / China region selection、动态 C3 discovery、C3/PCCS read path
   - China remote commands fail-closed

当某一项没有完整证据时，本文件必须明确写为：

```text
SOURCE_VERIFIED
IMPLEMENTED_UNVERIFIED
DISABLED
STRONG-INFERENCE
EXPERIMENTAL
MAPPING_REQUIRED
NOT IMPLEMENTED
```

而不是为了“看起来完整”而把推测写成事实。
