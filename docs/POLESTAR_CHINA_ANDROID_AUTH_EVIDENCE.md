# Polestar China Android 4.25.0 认证与车辆发现证据链

> 分析日期：2026-10-25  
> Android 包：`com.polestar.china.android`  
> App 版本：`4.25.0`，build `262`  
> 结论级别：静态链路已闭环；真实账号端到端请求仍需脱敏动态验证  
> 安全边界：本文不包含真实 Token、Cookie、密码、验证码或内置 secret 的值

## 1. 结论

官方中国 Android App 的首次车辆授权不是由 `polestar.cn` 网页 Cookie 直接提供，而是由中国区移动端账户 Gateway 完成：

```text
验证码滑块（官方 H5）
  → 短信验证码请求
  → 手机号 + 验证码登录
  → LoginToken
       ├── Local Access / Refresh / userUid
       └── GlobleToken（App 原拼写）
            ├── Global Access / Refresh / ID Token
            └── expiresIn / userUid / userName
  → Global Access Token
  → China C3 动态环境发现
  → 动态 c3.url/car-information/car
  → C3/PCCS gRPC
```

关键结论：

1. 手机验证码发送前必须完成官方阿里滑块，得到 `sessionId`、`sig`、`token`。
2. 首次手机登录的响应模型同时定义 Local Token 与嵌套 Global Token，成功处理会在同一步持久化两者；没有证据支持“先网页登录，再把 Cookie 换成车辆 Token”。
3. `gw.app.polestar-auto.com` 登录栈的公共头为 `Content-Type`、`Version`、`Device`、`X-User-Agent`；已有 Local Token 后再加 `Authorization` 和 `psoneid`。
4. App 中确有另一套带 nonce/JNI 签名的 `baselinelibrary` 网络栈，但它不在 `PscnAccountRepo` 登录 Retrofit 的 OkHttp 链中，不能把那套签名误加到登录请求。
5. C3 环境发现是未带认证的 `GET .`；车辆列表和后续 C3/PCCS 调用才通过同一 auth provider 使用 Global Access Token。
6. 静态分析已经把“缺少公共参数”从泛化猜测缩小到明确的请求头、滑块字段和正确 endpoint；服务器具体先拒绝了哪一项仍须一次脱敏抓包确认。

## 2. 输入文件与完整性

### 2.1 DEX 归档

| 输入 | 内容 | 解压后大小 | SHA-256 |
| --- | --- | ---: | --- |
| `Archive.zip` | `classes.dex`–`classes9.dex` | 59,891,756 bytes | `3965bcdcb197b4697dd86008fe460c4c442c749c21c8317860541303b6322595` |
| `Archive 2.zip` | `classes10.dex`–`classes16.dex`、`dump.txt` | 44,138,120 bytes | `6261800aceb63a5c7cc35e61089e011e6bd35d478f7fcebc9f81de613653eb60` |

合计 16 个 DEX。归档中没有 APK、JAR 或 SO；`dump.txt` 只标记 Application 为 `com.polestar.cn.App`。因此本轮可以判定 Java/Kotlin 网络链，但不能据此审计未提供的 native library。

| DEX | Size (bytes) | DEX | Size (bytes) |
| --- | ---: | --- | ---: |
| `classes.dex` | 10,948,840 | `classes9.dex` | 10,065,148 |
| `classes2.dex` | 1,708 | `classes10.dex` | 8,681,620 |
| `classes3.dex` | 4,841,828 | `classes11.dex` | 8,248,144 |
| `classes4.dex` | 6,369,108 | `classes12.dex` | 8,450,720 |
| `classes5.dex` | 3,560,780 | `classes13.dex` | 2,048,420 |
| `classes6.dex` | 9,313,900 | `classes14.dex` | 7,741,096 |
| `classes7.dex` | 7,645,528 | `classes15.dex` | 3,501,580 |
| `classes8.dex` | 7,144,916 | `classes16.dex` | 5,466,424 |

与本报告直接相关的 DEX：

| DEX | 主要证据 |
| --- | --- |
| `classes3.dex` | Gateway host、Retrofit API、`PscnAccountRepo` |
| `classes5.dex` | 登录 UI、登录 ViewModel、滑块回调、请求 JSON 字段 |
| `classes11.dex` | Pscn OkHttp/Retrofit、公共头、Local Token 刷新、`JsApi` |
| `classes12.dex` | JSON `RequestBody` 构造器 |
| `classes13.dex` | `PolestarIdAuthImpl`、Global Token 读取/刷新 |
| `classes14.dex` | Remote Car Service、C3 discovery、车辆列表、认证拦截器 |
| `classes15.dex` | 生产环境 domain provider |
| `classes16.dex` | `LoginToken`、`RCSClient`、China Remote Car 配置 |

JADX 全量处理了 `classes.dex`–`classes9.dex` 的 18,725 个 class（62 个方法级错误）和 `classes10.dex`–`classes16.dex` 的 11,649 个 class（34 个方法级错误）。本报告使用的 Retrofit 注解、字段模型、拦截器和主要调用点均可读；全局关键字复查没有发现与本文链路冲突的第二套 Pscn 登录实现。`PolestarIdAuthImpl$refreshToken$2` 存在明显的条件反编译伪影，本文只采用其无歧义的输入、调用和持久化行为，不采用伪影条件本身。

### 2.2 StackTrace 归档

两份 `Polestar-China-StackTrace.zip` 的 SHA-256 都是：

```text
8d7658abe485026eabd620db6f57adbe6597f2e01104f389f03d7a269ec49e78
```

它们是同一文件的重复副本。解压后有 2,755 个文件、一个 `LayoutRes@21174` 树和一个 `StackTrace@21174` 树；`all_strings.txt` 为 4,436,363 bytes。

StackTrace 中能看到首页、商城和 `com.polestar.carcontrol.*` UI 类，但没有以下认证关键字：

```text
credcode
oauth/login
refresh-global-token
globleToken
psoneid
X-User-Agent
car-information
cnepmob
polestar-app-android
```

所以它可以继续证明首页/车辆状态运行时路径，不能证明首次登录协议或公共参数。认证结论来自 DEX。

## 3. 生产环境 Host 选择

`classes3.dex` 的 `com.polestar.repository.domain.cliif` 保存环境常量；`com.polestar.repository.domain.cliint` 以 flavor `"p"` 选择 `C0686cliint`；`classes15.dex` 的 `ga.clicatch` 固定创建 `new cliint("p")`。

生产映射如下：

| 方法语义 | Host |
| --- | --- |
| China web/resources | `https://prod.china.polestar.cn/` |
| Pscn Gateway | `https://gw.app.polestar-auto.com` |
| Local OAuth refresh | `https://uc.app.polestar-auto.com` |
| GWO | `https://gwo.app.polestar-auto.com` |
| H5/resources | `https://h5.app.polestar-auto.com` |
| Account | `https://account.app.polestar-auto.com` |

Pscn Account Retrofit 使用 `ga.clicatch.clisuper().cliif()`，即生产 Gateway。

## 4. “字符串 → DEX → 类 → 方法 → API → 参数名”总表

| 字符串/能力 | DEX | 类与方法 | API | 参数/结果 |
| --- | --- | --- | --- | --- |
| `account-components/auth-slider` | 15 / 11 / 5 | `ga.clicatch.cliclass()` → base constant provider → `VerificationCodeLoginFragment`；`JsApi.postMessage()` | `https://h5.app.polestar-auto.com/account-components/auth-slider/index.html` | H5 回传 `sessionId`、`sig`、`token` |
| `credcode` | 5 → 3 | `VerificationCodeLoginViewModel$getVerifyCode$1.invokeSuspend()` → `PscnAccountRepo.clifinal()` → `ra.clicatch.clicase()` | `POST /user/api/v1/credcode/send` | `principal`、`aliScene`、`aliSessionId`、`aliSign`、`aliToken`、`clientId`、`type` |
| `oauth/login/phone` | 5 → 3 | `VerificationCodeLoginViewModel$loginWithVerificationCode$1` → `PscnAccountRepo.clishort()` → `ra.clicatch.clidouble()` | `POST /user/api/v2/oauth/login/phone` | `clientId`、`phone`、`credential`、`autocrt` |
| `oauth/login/password` | 5 → 3 | `VerificationCodeLoginViewModel$loginWithPassword$1` → `PscnAccountRepo.clithrow()` → `ra.clicatch.cliwhile()` | `POST /user/api/v2/oauth/login/password` | `clientId`、`phone`、`password`、`autocrt` |
| `oauth/login/onetouch` | 5 → 3 | `PscnLoginViewModel$loginWithOperator$1` → `PscnAccountRepo.clisuper()` → `ra.clicatch.clilong()` | `POST /user/api/v1/oauth/login/onetouch` | `clientId`、`loginToken` |
| `oauth/wechat/wxmp/code` | 5 → 3 | `PscnLoginViewModel$loginWithWxCode$1` → `PscnAccountRepo.cliwhile()` → `ra.clicatch.clibreak()` | `POST /user/api/v2/oauth/wechat/wxmp/code` | `WxLoginRequestBean(clientId, "ps", code)` |
| `refresh-global-token` | 13 → 3 | `PolestarIdAuthImpl$refreshToken$2` → `PscnAccountRepo.cliimport()` → `ra.clicatch.clipublic()` | `POST /user/api/v2/oauth/refresh-global-token` | JSON `refreshToken`；返回 `LoginToken.GlobleToken` |
| Local refresh | 11 | `com.polestar.base.di.clishort.clitry()` → `clisuper.clido()` | `POST https://uc.app.polestar-auto.com/oauth/token` | `grant_type`、`client_id`、`client_secret`、`refresh_token` |
| 公共头 | 11 | `com.polestar.base.di.clielse.cligoto()` | Pscn Gateway client | `Content-Type`、`Version`、`Device`、`X-User-Agent`；登录后增加 `Authorization`、`psoneid` |
| `LoginToken` | 16 | `LoginToken`、`LoginToken.GlobleToken` | phone/password/onetouch login response | Local 与 Global 两组 Token |
| Token 持久化 | 5 | `PscnLoginViewModel.h0(LoginToken)` | 本地 preferences | Local access/refresh/expiry/userUid + Global access/refresh/id/type/expiry/timestamp/userUid/userName |
| China C3 discovery | 16 → 14 | `p405o6.clifor.clinew()` → `network.cliint.clido()` → `C3HostManagerImpl.clido()` | `GET https://cnepmob.volvocars.com.cn/`（Retrofit `@GET(".")`） | `Accept: application/volvo.cloud.cnepmob.v1+json`；返回 `c3`/`c3Lbs` |
| `car-information/car` | 14 | `network.clido.clido()` → `C3CarInformationManager.clinew()` | `GET <c3.url>/car-information/car` | `Authorization: Bearer <Global Access>`、`User-Agent`；返回车辆列表 |
| C3/PCCS gRPC auth | 14 / 16 / 13 | `GrpcCredentials` → `RCSClient.cliif.cliif()` → `AuthClientImpl.cliif()` → `PolestarIdAuthImpl.clibyte()` | 动态 C3 gRPC / PCCS | `Authorization: Bearer <Global Access>`；按调用可附 `vin` metadata |

### 4.1 一个重要的 endpoint 区分

Retrofit interface 同时声明了：

```text
POST /user/api/v1/credcode/send
POST /user/api/v1/credcode/send/login
```

但当前登录页的已证明调用链是：

```text
VerificationCodeLoginViewModel$getVerifyCode$1
  → PscnAccountRepo.clifinal
  → service.clicase
  → POST /user/api/v1/credcode/send
```

`/credcode/send/login` 的存在不能证明当前登录页在使用它。0.19.4 不应继续根据 endpoint 名称猜测，应按调用链使用 `/credcode/send`。

## 5. 验证码滑块与登录请求

### 5.1 滑块回调

`VerificationCodeLoginFragment`：

1. 给 WebView 开启 JavaScript；
2. 注册名为 `chromium` 的 `JsApi`；
3. H5 调用 `chromium.postMessage(...)`；
4. `JsApi` 解码消息并转给 fragment handler；
5. handler 从 JSON 读取 `sessionId`、`sig`、`token`；
6. 三个值写入 ViewModel，再触发短信验证码请求。

因此这三项不是可静态伪造的“公共常量”，而是每次滑块验证产生的短期值。

### 5.2 短信验证码请求

```http
POST https://gw.app.polestar-auto.com/user/api/v1/credcode/send
Content-Type: application/json
Version: 4.25.0
Device: Android
X-User-Agent: <Android Build.MODEL>
```

```json
{
  "principal": "<phone>",
  "aliScene": "ic_login_h5",
  "aliSessionId": "<slider sessionId>",
  "aliSign": "<slider sig>",
  "aliToken": "<slider token>",
  "clientId": "polestar-app-android",
  "type": "SIGNIN_BY_PHONE"
}
```

首次登录时本地尚无 Local Access Token，拦截器不会添加 `Authorization` 或 `psoneid`。

### 5.3 手机验证码登录

```http
POST https://gw.app.polestar-auto.com/user/api/v2/oauth/login/phone
Content-Type: application/json
Version: 4.25.0
Device: Android
X-User-Agent: <Android Build.MODEL>
```

```json
{
  "clientId": "polestar-app-android",
  "phone": "<phone>",
  "credential": "<sms code>",
  "autocrt": true
}
```

`credential` 才是短信验证码字段名；不是 `code`。

### 5.4 密码登录

```http
POST https://gw.app.polestar-auto.com/user/api/v2/oauth/login/password
```

```json
{
  "clientId": "polestar-app-android",
  "phone": "<phone>",
  "password": "<password>",
  "autocrt": true
}
```

所有上述 map 都由 `classes12.dex` 的 `C3158d1.clido(Object)` 序列化为 `application/json` RequestBody。

## 6. 公共参数与签名边界

### 6.1 Pscn 登录网络栈实际添加的 Header

`com.polestar.base.di.clielse.cligoto()` 对 Gateway 请求添加：

| 条件 | Header |
| --- | --- |
| 始终 | `Content-Type: application/json` |
| URL 不是 `prod.china.polestar.cn` | `Version: 4.25.0` |
| 同上 | `Device: Android` |
| 同上 | `X-User-Agent: <Build.MODEL>` |
| Local Access 非空 | `Authorization: Bearer <Local Access>` |
| Local Access 非空 | `psoneid: <Local userUid>` |

Pscn Retrofit 使用 `@com.polestar.repository.di.p` 的 Gateway client。该 OkHttp builder 的拦截器链为日志、上述 header interceptor、token response/refresh、Chucker 和统计拦截器；没有 `com.baselinelibrary.sign.SignInterceptor`。

### 6.2 另一套签名网络栈不属于登录链

`BaseOkHttpClientUtil` / `CustomOkHttpClientUtil` 的确会添加多组 `X-*` 设备头、`X-api-signature-version`、nonce，并把 `SignInterceptor` 注册为 network interceptor；`SignInterceptor` 最终添加 `X-SIGNATURE` 和 `X-TIMESTAMP`。

但这套 client 与 Pscn Account Retrofit 是两套独立网络栈。当前 DEX 证据不支持为 `/user/api/v1/credcode/send` 或 `/user/api/v2/oauth/login/*` 增加 nonce、JNI signature 或整套 `X-DEVICE-*`。

### 6.3 对“缺少公共参数”的可证明结论

旧请求如果仅发送手机号，至少缺少：

- 正确的 `/credcode/send` 调用路径；
- 滑块产生的 `aliSessionId`、`aliSign`、`aliToken`；
- `aliScene`、`clientId`、`type`；
- 官方 client 固定添加的 `Version`、`Device`、`X-User-Agent`。

静态分析不能证明服务器错误具体指其中哪一个字段，也不能证明校验顺序。动态验证应比较“字段是否存在和格式是否一致”，而不是记录真实验证码或 Token。

## 7. Token 模型与刷新

### 7.1 首次登录响应

`LoginToken` 外层字段：

```text
accessToken
refreshToken
tokenType
expiresIn
userUid
userName
globleToken (nullable)
```

嵌套 `LoginToken.GlobleToken`：

```text
accessToken
refreshToken
idToken
tokenType
expiresIn
userUid (nullable)
userName (nullable)
```

`PscnLoginViewModel.h0(LoginToken)` 在同一次成功处理中持久化外层 Local 数据，并在 `globleToken != null` 时持久化嵌套 Global 数据。因此静态调用链给出的顺序是：

```text
phone/password login response
  → Local Session
  → nested Global Session
```

不是 `Local → 另一个首次交换 API → Global`。

### 7.2 Local Token 用途与刷新

Local Access 用于中国 Pscn Gateway 的认证请求。header interceptor 同时添加：

```text
Authorization: Bearer <Local Access>
psoneid: <Local userUid>
```

Local Refresh 走独立的 `uc.app.polestar-auto.com` client：

```text
POST /oauth/token?grant_type=refresh_token
                 &client_id=polestar-app-android
                 &client_secret=<APK embedded value; redacted>
                 &refresh_token=<Local Refresh>
Content-Type: application/x-www-form-urlencoded
```

Retrofit 声明使用 `@QueryMap`，所以这四项静态上是 URL query 参数；client 同时设置上述 Content-Type。

内置 client secret 的值不进入本文或 Git；实现时也不应写入日志。

### 7.3 Global Token 用途与刷新

Global Access 是 Remote Car Service 的 bearer token。Global ID Token 由独立 getter 使用，不是车辆列表/C3 bearer。

Global refresh：

```http
POST https://gw.app.polestar-auto.com/user/api/v2/oauth/refresh-global-token
Authorization: Bearer <Local Access>
psoneid: <Local userUid>
Content-Type: application/json
```

```json
{
  "refreshToken": "<Global Refresh>"
}
```

响应为新的 `LoginToken.GlobleToken`。`PolestarIdAuthImpl$getFreshAccessToken$2` 在剩余有效期不超过 60 秒时触发刷新。

`PolestarIdAuthImpl$signIn$2` 只是读取已存储的 `globleToken` 并更新 Remote Car Service 的 auth state；它不执行中国区首次登录。这是网页 OIDC/AppAuth 外观容易造成误判的地方。

## 8. 车辆发现与 C3/PCCS Token Audience

### 8.1 第一阶段：C3 环境发现

生产 China 配置返回：

```text
https://cnepmob.volvocars.com.cn
```

Retrofit service：

```http
GET .
Accept: application/volvo.cloud.cnepmob.v1+json
User-Agent: <RCS user agent>
```

该 service 的 Retrofit 构造调用显式使用默认的 `auth = null`，所以静态证据显示环境发现请求不带 bearer token。

响应模型：

```text
C3Response
  c3:
    url
    grpcHost
    grpcPort
    grpcKeepAliveTime
  c3Lbs:
    url
    grpcHost
    grpcPort
    grpcKeepAliveTime
```

`C3HostManagerImpl` 把 `c3.url` 用于后续 REST 车辆信息，把 `c3.grpcHost:grpcPort` 和 `c3Lbs.grpcHost:grpcPort` 转为 gRPC connection options。

### 8.2 第二阶段：车辆列表

```http
GET <c3.url>/car-information/car
Authorization: Bearer <Global Access>
User-Agent: <RCS user agent>
```

按 VIN 的有限信息接口为：

```http
GET <c3.url>/car-information/car/{vin}
```

不能把 `https://cnepmob.volvocars.com.cn/car-information/car` 永久硬编码为车辆列表地址；`cnepmob` 是 discovery base，真正的 REST base 是 discovery 返回的动态 `c3.url`。

### 8.3 bearer token 的调用链

```text
TokenInterceptor / GrpcCredentials
  → p441r8.clido.cliif()
  → RCSClient.cliif.cliif()
  → AuthClientImpl.cliif()
  → PolestarIdAuthImpl.clibyte()
  → getFreshAccessToken
  → stored Global Access（必要时先 refresh）
```

所以车辆列表、C3 gRPC 和 PCCS 使用 Global Access，而不是：

- Local Access；
- Global ID Token；
- `polestar.cn` Cookie；
- `psoneid` 本身。

China PCCS 生产地址仍为：

```text
https://api.pccs-prod.origin.polestar.cn
```

## 9. 对 0.19.5 的直接设计约束

### 9.1 建议模块边界

```text
ChinaAuthorizationCoordinator
  ├── CaptchaSessionProvider
  │     └── 官方 auth-slider WKWebView + 严格 message handler
  ├── PscnGatewayClient
  │     ├── send verification code
  │     ├── phone/password login
  │     └── refresh global token
  ├── ChinaLocalTokenRefresher
  ├── ChinaTokenVault (Keychain)
  └── ChinaVehicleClient
        ├── C3 discovery
        ├── dynamic car-information
        └── C3/PCCS credentials
```

### 9.2 实现顺序

1. 在 WKWebView 中只承载官方滑块，取得短期 `sessionId/sig/token`；不读取密码或 Cookie。
2. 按本报告的 endpoint、JSON 字段和固定头发送短信。
3. 用用户输入的验证码完成 `/oauth/login/phone`，立即丢弃验证码。
4. 将 Local/Global 两组 Token 分开写入 Keychain，并保存各自 expiry/timestamp/userUid。
5. Global Access 剩余 60 秒前刷新；Global refresh 失败时不要错误清除 Global 以外的 provider 会话。
6. 先执行 C3 discovery，再以动态 `c3.url` 获取车辆列表。
7. Global 与 China 使用独立 Keychain namespace、VIN 和 cache。
8. 只完成 read-only Battery/Range/Odometer/Exterior 后再声明 China vehicle login functional。
9. China remote commands 继续保持禁用。

### 9.3 仍需动态确认的最小集合

- macOS `WKWebView` 是否能完整运行官方滑块并回传同样的三字段；
- Gateway 是否接受 macOS WebKit 环境下的滑块结果；
- `Version`/`Device`/`X-User-Agent` 的值约束，而不只是 header 名存在；
- 登录响应在真实账号上是否始终包含非空 `globleToken`；
- discovery 是否在生产环境始终保持无认证；
- `<c3.url>/car-information/car` 的实际状态码和返回模型。

本轮环境没有连接 Android 设备，也没有 ADB 流量入口，因此安装 Reqable 不会产生新的证据。进入上述动态验证时，可使用 Reqable 做脱敏请求结构对照；若 App 的证书固定阻断代理，则只用已有 Root/Frida metadata probe 记录 header 名、URL、状态码和 JSON key，不记录值。

## 10. 证据强度与当前阻塞

| 项目 | 状态 | 说明 |
| --- | --- | --- |
| 登录 endpoint 与 JSON key | VERIFIED（static） | ViewModel → Repo → Retrofit 注解闭环 |
| Pscn 公共 Header | VERIFIED（static） | OkHttp interceptor 直接可读 |
| 登录不使用 SignInterceptor | VERIFIED（static） | Pscn client builder 与 baselinelibrary client 分离 |
| Login response schema 支持 Local/Global 同时返回 | VERIFIED（static） | model + 条件持久化调用闭环；非空性待动态确认 |
| Global refresh | VERIFIED（static） | Repo/API/model/persistence 闭环；一个分支存在 JADX 伪影 |
| C3 discovery 无 bearer | VERIFIED（static） | Retrofit 构造显式传 `auth = null` |
| 车辆列表使用 Global Access | VERIFIED（static） | REST interceptor → auth provider → fresh Global Access 闭环 |
| 真实账号端到端登录 | NOT YET VERIFIED | 需要一次脱敏动态验证 |
| Hisingen 0.19.5 静态实现 | IMPLEMENTED_UNVERIFIED | 已合并完整 0.19.4 骨架并按本证据修正登录、刷新和动态车辆发现；需真实账号验证 |

静态分析与 0.19.5 合并实现已经完成。下一阶段不是继续猜参数，而是按 9.3 做最小脱敏动态验证；若需要代理对照，可使用 Reqable，但不得通过关闭 TLS 校验或记录敏感值来强行取证。
