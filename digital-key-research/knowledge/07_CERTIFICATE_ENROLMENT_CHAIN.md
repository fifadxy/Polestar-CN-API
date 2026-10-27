# 07｜证书注册、车辆配对与数字钥匙 Provisioning 主链

**样本：用户上传的极星中国 4.25.0 APK。**  
本页记录可移植架构和状态顺序；不记录真实密钥、认证报文、账号令牌、VIN 或可重放材料。

## 1. 结论

直接解析原 APK 后，Polestar 2 数字钥匙并不是单一 BLE 协议，而是至少四层依赖：

```text
VOCMO SDK configuration / UCC + USEC enrolment
                 ↓
Vehicle pairing session (BLE or remote)
ECDHE + explicit user approval + certificate exchange
                 ↓
Catalogue enrolment
                 ↓
CarAccess PassiveEntry provisioning
                 ↓
P319 Phone-as-Key pairing
```

当前最重要的可移植性信号：

- `Vocmo.initialize(Context, Environment, Manufacturer, PhoneAsKeyRepository)` **显式接受可注入的 PhoneAsKeyRepository**。
- `Authentication` 对 SDK 暴露的接口只有 `getToken(): String`。
- 当前相关 VOCMO/PAK 静态路径中没有找到 Android package/signing certificate、Play Integrity 或 SafetyNet 校验。
- UCC/USEC、Actor、Near-Field 等设备身份均存在“新设备自行生成密钥对/CSR，再经正常流程取得证书”的路径。
- 因此研究目标应是**新 App 生成并注册自己的身份**，而不是迁移官方 App 已配好的私钥。

这仍不等于第三方 App 已获 OEM 授权；服务端策略、SDK 分发权和实车认可仍需验证。

## 2. SDK 初始化：PhoneAsKeyRepository 是注入项

原 APK 中方法签名：

```text
Vocmo.initialize(
  Context,
  VocmoEnvironment,
  Manufacturer,
  PhoneAsKeyRepository
) -> Vocmo
```

`Vocmo.init(...)` 随后接受三个字符串身份参数、`Authentication` 与配置开关；Polestar 的 `VocmoInitializationImpl` 对它提供相同的三类字符串身份与 Authentication。

`Vocmo.init` 内部会调用：

```text
PhoneAsKeyRepository.loginUser(...)
```

而上一轮已确认 Polestar PAK user identity 来自当前 `UserModel.identifier`，不是直接把 access token 当成蓝牙钥匙。

## 3. SDK 基础证书：UCC / USEC

`ConfiguratorImpl.configure` 的顺序包括：

1. 获取 SDK entry point。
2. 获取 certificate-enrolment config。
3. 调用 `EnrolmentRepository.getEnrolment(...)`。
4. 获取或更新 UCC / USEC certificate。
5. 使用 UCC private key 创建后续 HTTPS/TLS 运行配置。
6. 获取 Feature Discovery / App Settings / Catalogue / MQTT 等配置。

`EnrolmentMessageComposerImpl.composePkiMessage`：

- 生成 UCC key pair；
- 生成 USEC key pair；
- 构造 CRMF/CMP certificate request；
- 请求消息由本机新生成 key pair 签名。

`EnrolmentImpl.performEnrolment` 通过配置提供的 certificate-enrolment URL 发送 PKI 请求并解析证书响应。

这说明在车辆配对之前，VOCMO SDK 自身已经有一层**设备/SDK证书注册**。

## 4. 密钥生成与本地存储

`VocKeyPairUtilsImpl.createKeyPair` 使用标准 Java `KeyPairGenerator`：

- 支持 RSA / ECDSA；
- `SecureRandom`；
- 生成后读取编码后的 private/public key；
- 封装为 SDK 的 `VocKeyPair`；
- 交给 `Cache.store`。

这说明这些 SDK key pair 并不是只能在官方 APK 中使用的固定硬件私钥。

同时，SDK Koin 模块提供两种 Cache：

- 普通 SharedPreferences：`Vocmo_Shared_Preferences`
- `EncryptedSharedPreferences`：`Vocmo_Secret_Shared_Preferences`

两者都由 `CacheImpl` 封装。**当前静态分析尚未最终解析 VocKeyPairUtils 注入的是哪一个 qualifier，因此不能写成“私钥明文存储”。**

证书本身由 `CertificateStorageImpl` 通过 Java `KeyStore` 保存/读取 X.509 certificate。

## 5. 车辆配对：BLE 与 Remote 共用 PairingSession

两条路径最终进入同一个 `PairingSession`：

### 本地 BLE

`BlePairingService.pairWithVehicle`：

- 已扫描并连接目标 BLE 设备；
- 取得本机 Bluetooth device name；
- 构造 `PairingSessionOptions(deviceName, pairingInfo, userId)`；
- `PairingSession.start(...)`。

### Remote pairing

`RemotePairingRepositoryImpl.initiatePairingSession`：

- 构造 MQTT `PairingInfo`；
- 同样构造 `PairingSessionOptions`；
- 调用同一个 `PairingSession.start(...)`。

因此证书交换/身份建立并不绑定“只有某一种 BLE UI 才能完成”。

## 6. PairingSession 的真实安全阶段

`PairingSession.start`：

1. reset session；
2. 初始化 `CertificateExchange`；
3. 使用 Diffie-Hellman API 生成临时 key pair；
4. 发送 `ECDHE_REQUEST`。

收到远端 ECDHE request 后：

- 创建 remote key；
- 计算 shared secret；
- 计算 derivative；
- 派生 AES encryption key；
- 把共享确认信息交给 listener 显示给用户。

用户确认后：

- `ECDHE_APPROVAL`；
- 双端都确认后进入 certificate exchange。

拒绝则发送 `ECDHE_DECLINE`。

## 7. Actor / Near-Field 证书交换

第一阶段：

`CertificateExchangeImpl.createActorCsr`

- 创建新的 Actor key pair；
- 读取车辆 UCC common name；
- 构造 Actor CSR；
- `PairingSession.exchangeCertificates` 加密并通过 `AES_CSR_ACTOR` 发送。

随后 PairingSession 处理车辆发回的证书类 frame，并交给 `CertificateExchange.handle`。

之后：

`CertificateExchangeImpl.createNearFieldCsr`

- 创建独立 Near-Field key pair；
- 生成 Near-Field CSR；
- 加密后以 `AES_CSR_NEARFIELD` 发送。

APK 中明确存在的 pairing frame 类型包括：

- `ECDHE_REQUEST`
- `ECDHE_APPROVAL`
- `ECDHE_DECLINE`
- `AES_CSR_ACTOR`
- `AES_CERT_ACTOR`
- `AES_CSR_NEARFIELD`
- `AES_CERT_NEARFIELD`
- `AES_CAR_ACTOR_CERT`
- `AES_BLE_CAR_INFO`
- `AES_CA`

## 8. 何时认为车辆身份完整

`CertificateExchangeImpl.storeCertificatesIfNeeded` 同时要求：

- ActorCertificate
- NearFieldCertificate
- CarRootCertificate
- CarActorCertificate
- BLE key

全部到齐后才构造 `PairingCertificatesDto`，保存 VehicleData，并回调：

`onCertificatesExchanged`

日志中存在：

`All certificates are exchanged and stored`

因此“BLE 已连接”与“车辆身份已经配好”是两个完全不同的状态。

## 9. Catalogue enrolment 位于车辆身份建立之后

`VocmoVehicleImpl.load()` 调用：

`CatalogRepository.performEnrolment(...)`

而 `CatalogRepositoryImpl.enrolCatalogBlocking` 会读取：

- ActorCertificate
- USEC Certificate
- UCC Certificate
- Actor key pair
- USEC key pair
- UCC key pair

构造 `CA-CAT-030`，再发往配置中的 Catalogue URI。

只有收到并验证 `CA-CAT-031` 后，Catalog enrolment 才成为 accepted 状态。

`VocmoVehicleImpl.getLoaded()` 在配置要求 CAT030 时直接依赖 `CatalogRepository.getEnrolled()`。

因此最合理的静态顺序是：

```text
Vehicle certificate exchange
        ↓
Actor identity becomes available
        ↓
VocmoVehicle.load
        ↓
Catalogue enrolment
        ↓
Vehicle services considered loaded
```

## 10. PassiveEntry → P319

车辆 VOCMO 通道建立后：

`PassiveEntryRepositoryImpl.onConnectionStatusChanged`

检查：

`PhoneAsKeyRepository.containsVin(VIN)`

若不存在：

`requestKeys()`

车辆返回的 PassiveKeyResponse 转成 PassiveEntryInfo，再调用：

`PhoneAsKeyRepository.addPassiveEntry(...)`

随后生成 P319 `ConnectionInfo` 并注册 P319 Vehicle，最终才进入：

`PhoneAsKeyVehicle.pair()`

## 11. 对“能否移植”的最新判断

### 技术可行性：明显上升

目前静态证据已经支持一条 clean new-device 路线：

1. 自研 App 初始化 VOCMO。
2. 通过正常账户 Authentication 和用户 identifier 完成 SDK enrolment。
3. 生成新的 UCC/USEC key pair 并取得证书。
4. 本人车辆进入正常 pairing 状态。
5. 自研 App 生成新的 ECDHE / Actor / Near-Field 身份。
6. 与车辆正常确认共享信息并交换证书。
7. 完成 catalogue enrolment。
8. 通过 CarAccess 请求该新设备自己的 PassiveEntry。
9. 创建 P319 ConnectionInfo。
10. 调用正常 PAK pairing。

### 尚不能跳过的验证

- 中国区 enrolment backend 是否对官方 app/client identity 有服务器侧限制。
- 新包名是否能正常完成 UCC/USEC enrolment。
- Catalogue 服务是否接受新宿主 App 产生的身份。
- SDK 重新分发/集成许可。
- 实车端是否还有未出现在 Android 静态代码里的白名单或车型软件限制。

## 12. Root AVD 动态验证应如何使用这份主链

后续 Root AVD 不以“拿到钥匙”为第一目标，而是逐级确认：

```text
SDK_CONFIGURED
DEVICE_ENROLLED
VEHICLE_DISCOVERED
ECDHE_CONFIRMATION
CERTIFICATE_EXCHANGE
VEHICLE_IDENTITY_STORED
CATALOG_ENROLLED
PASSIVE_ENTRY_PROVISIONED
PAK_PAIRING
PAK_PAIRED
```

研究分支中的 `ProvisioningStateMachine` 用这 10 个状态对动态日志做无敏感数据的阶段判定。

