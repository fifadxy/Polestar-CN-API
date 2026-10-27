# 06｜用户提供的极星中国 4.25.0 APK 深度分析

**样本来源：用户上传原 APK。**  
包名 `com.polestar.china.android`，versionName 4.25.0，versionCode 262。  
MD5 `d66b01f6b6cf8cdbda0d226ae0926c60`  
SHA-256 `99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef`

> 本文只记录结构、调用关系、字段语义和可移植性结论，不保存/提交真实用户凭据、数字钥匙字节、证书私钥、车辆认证报文或已配对密钥。

## 1. 新增结论摘要

这轮直接解析用户提供的 APK 后，Polestar 2 的数字钥匙链路可以进一步还原为：

```text
Polestar account UserModel.identifier
        │
        ▼
PhoneAsKeyRepository.loginUser(identifier)
        │
        ▼
RCS/VOCMO BLE secure channel
        │
        ├─ 检查本地是否已经 containsVin(VIN)
        │
        └─ 未找到 → composePassiveKeyRequest()
                       │
                       ▼
                  车辆返回 PassiveKeyResponse
                       │
                       ▼
PassiveEntryConverter → PassiveEntryInfo
        │
        ▼
PhoneAsKeyRepository.addPassiveEntry(...)
        │
        ▼
P319 ConnectionInfo + VehicleManager
        │
        ▼
PhoneAsKeyVehicle.pair()
        │
        ▼
P319 PairingManager → PAKM BLE pairing
```

因此，真正的移植核心不是“复刻一个 BLE 扫描器”，而是**在车主授权且车辆处于允许配对的状态下，通过受信任的 VOCMO/CarAccess 通道为这个新手机实例取得新的 PassiveEntry 材料，再交给 P319 完成近场配对**。

## 2. PAK 用户身份不是远程 API Token

在 `CarMainViewModel` 和 `CarControlMainViewModel` 中均找到：

- 从当前登录用户模型读取一个字符串；
- 日志为 `PakUserId was null` / `Login with PAK id ...`；
- 调用 `PhoneAsKeyRepository.loginUser(String)`。

继续追踪后，该字符串来自 `UserModel.identifier`。也就是说，静态证据支持：

**PAK loginUser 使用当前 Polestar 用户的 identifier，而不是把 access token / refresh token 直接作为 PAK ID。**

`PhoneAsKeyRepositoryImpl.loginUser` 的行为：

1. 读取 P319 当前保存的 `pakUserId`；
2. 首次用户写入；
3. 同一用户不重复处理；
4. 用户变化时更新 PAK ID，并清空旧的 Phone-as-Key 车辆对象。

这对独立实现是积极信号：现有中国区登录链若已经能稳定得到相同的用户 identifier，可以复用这一身份层，不需要把云端 Token 当作 BLE 密钥。

## 3. PassiveEntry 材料由车辆/VOCMO 链路提供

`PassiveEntryRepositoryImpl.onConnectionStatusChanged` 在 BLE 连接建立后：

- 调用 `PhoneAsKeyRepository.containsVin(VIN)`；
- 如果本地还没有该车，调用 `requestKeys()`。

`requestKeys()`：

- 检查 `PassiveEntryConfiguration.supported`；
- 构造 `composePassiveKeyRequest()`；
- 通过 VOCMO ChannelRepository 发送；
- 静态字符串中可见 CarAccess OID `1.3.6.1.4.1.37916.3.8.13.0.0.1`。

收到 `PassiveEntryMessage` 后，`PassiveEntryConverter` 从 `PassiveKeyResponse` 读取并转换：

- PAKM UUID
- PAKM Beacon ID
- TCAM Beacon ID
- PAKM Hello Key
- Long-term Access Key

然后调用：

`PhoneAsKeyRepository.addPassiveEntry(PassiveEntryInfo, userId, tcamUuid, VIN)`

**这说明关键密钥材料来自合法配对过程中的车辆/VOCMO 消息，而不是 APK 内置一个通用数字钥匙。**

## 4. userId 和 TCAM UUID 的来源

`PassiveEntryUtils.getUserId()`：

- 如果配置显式提供 UID，就使用配置值；
- 否则从本机保存的 **Actor Certificate** 读取证书；
- 取证书 Subject Common Name；
- 调用 `vocmo PassiveEntryApi.calculateUserIdForPhoneAsKey(...)` 生成 Phone-as-Key user id。

`PassiveEntryUtils.tcamUuid(vehicleData)`：

- 如果配置显式提供 TCAM UID，就使用配置值；
- 否则读取车辆 **Car Actor Certificate**；
- 取证书 Subject Key Identifier；
- 使用命名空间 UUID + UUID v5 算法生成 TCAM UUID。

因此，独立 App 的另一个关键前置条件是：**必须能建立/复用 VOCMO 的合法设备与车辆证书体系。**

## 5. P319 ConnectionInfo 已完整恢复字段语义

`PhoneAsKeyRepositoryImpl.addPassiveEntry` 最终创建：

```text
ConnectionInfo(
  UID,
  PAKMUUID,
  PAKMBeaconUUID,
  TCAMUUID,
  TCAMBeaconUUID,
  startTime,
  endTime,
  LAK,
  PAKMHelloKey,
  VIN
)
```

APK 中 `ConnectionInfo` 构造函数的参数名称也直接保留了上述语义。

随后：

- 创建 `pak.p319.sdk.Vehicle(ConnectionInfo, ...)`
- 加入 `PAKService.VehicleManager`

这一步将 VOCMO/CarAccess 提供的 PassiveEntry 材料转换成 P319 可以用于 BLE PAKM 的本地车辆对象。

## 6. 对 “InvalidLicense” 的重要修正

旧研究把 `PairingError.InvalidLicense` 当作“可能存在 SDK/OEM App 许可证”的线索。这一解释现在需要修正。

P319 中存在：

`Vehicle$LicenseState = ACTIVE / PENDING / EXPIRED / INVALID`

`Vehicle.getLicenseState` 的静态调用只读取：

- 当前时间；
- `ConnectionInfo.startTime`；
- `ConnectionInfo.endTime`。

`Vehicle.pair()` 在开始配对前检查该 `LicenseState`，不处于可用状态时映射成 `PairingError.InvalidLicense`。

**因此目前最强静态证据表明，这里的 License 是“这台车这份 ConnectionInfo 的时间有效性”，而不是 APK 签名许可。**

另外，对 `pak.p319.sdk`、`com.volvocars.phoneaskey`、`vocmosdk` 相关类扫描后，没有发现针对宿主 APK 的 `checkSignatures`、`SigningInfo`、APK certificate / Play Integrity / SafetyNet 等应用签名校验调用。

这不是“绝对证明没有任何应用身份限制”，但足以把“官方 APK 签名一定是 P319 的技术前置条件”从主要假设降级。

## 7. PAK Service 的真正启动门槛

P319 `PAKService` 静态分析明确看到以下 StartError：

- `BLENotSupported`
- `BluetoothDisabled`
- `MissingPermissions`
- `BatteryOptimisationsEnabled`

`PhoneAsKeyManager.start()`：

1. 先检查 BLE 支持；
2. 注册 PAK Service 观察；
3. 启动 P319 Service；
4. 成功后返回 `PhoneAsKeyResult.Success`。

这与已跑通的 Android 14 Root AVD 能力吻合：BLE/GATT、Android Keystore、后台服务、ADB root 均可在实验环境继续验证。

## 8. 配对时 VOCMO BLE 通道被显式开启

`PakPairViewModel$digitalKeyControllerPair$1` 中存在明确日志：

- `vocmoBleChannelEnabled set to true before pairing`
- 配对成功 / 错误 / 异常 / 超时后又设置为 false。

同时该 ViewModel：

- 通过 `RCSManager` 获取/激活车辆；
- 启动 BLE scan；
- 初始化 `ICupDigitalKeyController`；
- 调用 `PhoneAsKeyVehicle.pair()`；
- 轮询目标 VIN 的 `getPaired()`。

这进一步支持“双通道协同”模型：

**RCS/VOCMO BLE = 获取/维护受信任 CarAccess / PassiveEntry 材料；P319 PAK = 真正的 Phone-as-Key 近场连接与配对。**

## 9. Native 层证据

APK 仅包含 arm64-v8a 原生库。关键库包括：

- `libvocmo-lib.so`（约 21 MB）
- `libglble.so`

`libvocmo-lib.so` 导出/包含：

- Certificate API
- KeyPairFactory
- CertificateSignReq
- Diffie-Hellman
- AES encryption key derivation
- CMS certificate handling
- `PassiveEntryApi.calculateUserIdForPhoneAsKey`

`libglble.so` 含 AES / AES-GCM / mbedTLS 等原语，但目前不能仅凭存在这些函数判断它直接承担 P319 配对协议。

## 10. 当前移植可行性判断

### 技术上从“很不确定”提升为“有明确实现路径”

目前已能定义一个不依赖复制官方已配对钥匙的目标流程：

1. 自研 App 使用正常中国区账号登录，并取得同一 `UserModel.identifier`。
2. 初始化自研/获许可的 VOCMO/RCS 层。
3. 在本人车辆、正常车端配对流程和合法车辆证书体系下建立 CarAccess 通道。
4. 由车辆下发一份**属于这个新手机实例**的 PassiveEntry 材料。
5. 将材料写入 P319/兼容实现的 ConnectionInfo。
6. 启动 PhoneAsKeyManager。
7. 对目标 VIN 的 Vehicle 发起正常 `pair()`。
8. 实车确认近场连接、无感进入和驾驶授权。

### 仍未解决的决定性问题

- VOCMO 设备 Actor Certificate 如何由正常登录/账户链接流程为第三方 App 合法签发与持久化。
- RCS/VOCMO 的中国区后端初始化参数、设备注册及 Account Linking 是否对应用身份有额外限制。
- 原 P319/VOCMO SDK 的授权和重新分发许可。
- 实车配对过程是否存在 UI/车端确认之外的 OEM 服务端设备白名单。
- Android 自研 App 使用新包名时，能否走完整的新设备证书注册，而不是依赖官方包私有数据。

## 11. 下一步动态验证重点

在 Root AVD/真机中优先验证**新设备初始化链**，而不是导出已有钥匙：

1. 官方 APK 首次启动后 PAK/VOCMO 初始化的组件顺序。
2. 未登录 → 登录后 `pakUserId` 是否建立。
3. 没有车辆/没有证书时 PAK 启动是否仍可成功。
4. Actor Certificate 何时创建、由哪一条正常账户链产生。
5. `PassiveEntryConfiguration.supported` 的车型/地区来源。
6. 车辆激活后 VOCMO BLE channel 的连接状态变化。
7. 无 PassiveEntry 时是否准确进入 `requestKeys` 路径。
8. 全程只记录状态、类/错误码、时间序列，不导出密钥或认证报文。

## 12. 当前研究状态

- 原 APK 静态深度分析：**完成到 provisioning / pairing 主链**
- Root Android 14 Emulator / KVM / ADB root：**已验证**
- P319 App 签名校验：**未发现静态证据**
- VOCMO 设备证书签发链：**下一重点**
- 官方 APK 在远端 AVD 动态运行：**待解决原上传 APK 的安全传递**
- 实车新设备配对：**未执行**
