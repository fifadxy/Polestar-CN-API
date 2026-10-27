# 01｜极星中国 4.25.0 APK 静态证据

## 1. 样本与分析边界

- 上传文件：极星Polestar-com.polestar.china.android-4.25.0.apk
- **APK_CONFIRMED**：包名 `com.polestar.china.android`，版本名称 4.25.0，版本号 262。
- **APK_CONFIRMED**：文件 135,944,423 B；SHA-256 `99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef`。
- **APK_CONFIRMED**：7,319 个 ZIP 条目、16 个 DEX、17 个 `.so`，原生 ABI **仅 arm64-v8a**。
- 解析 DEX 的类型/方法/类定义和方法引用；静态工具对 4,144 个目标包类、22,358 个方法体检查，统计 65,029 个调用引用。统计是扫描规模，**不是证实的车辆动作次数**。
- 仿真与实车：**EMULATOR_PENDING / VEHICLE_PENDING**。

## 2. 极星 2 Phone-as-Key 主链

| 组件 | 可确认的作用与证据 |
| --- | --- |
| `com.polestar.carcontrol.ui.pairing.ps2.Ps2PairingFragment` | Polestar 2 专属配对界面类，DEX 类定义存在 |
| `...pairing.ps2.SuccessfulParingFragment` | 配对成功界面类，DEX 类定义存在 |
| `com.polestar.carcontrol.vm.PakPairViewModel` | 配对结果观察相关代码引用 PAK Vehicle/Manager |
| `com.polestar.carcontrol.ui.pak.icupdigitalkey.ICupDigitalKeyController` | DEX 中直接引用 PhoneAsKeyManager 与 PhoneAsKeyVehicle |
| `com.volvocars.phoneaskey.api.PhoneAsKeyRepository` | 接口暴露 `loginUser`、`addPassiveEntry`、`removePassiveEntry`、`containsVin` |
| `com.volvocars.phoneaskey.impl.PhoneAsKeyManager` | 启停、车辆集合、监听器、RSSI 配置相关方法 |
| `com.volvocars.phoneaskey.impl.PhoneAsKeyVehicle` | `pair`、`getPaired`、`getConnectionStatus`、`disconnect` |
| `com.volvocars.phoneaskey.impl.business.PhoneAsKeyManager` | DEX 方法引用底层 `pak.p319.sdk.PAKService` 和 `VehicleManager` |
| `com.volvocars.phoneaskey.impl.business.PhoneAsKeyVehicleImpl` | DEX 方法引用 `pak.p319.sdk.Vehicle` |
| `com.volvocars.phoneaskey.impl.business.PhoneAsKeyRepositoryImpl` | DEX 方法引用 `PAKService`、`VehicleManager` |
| `com.volvocars.vocmosdk.business.passiveentry.PassiveEntryRepositoryImpl` | DEX 方法引用 PhoneAsKeyRepository 的无感进入接口 |

**调用关系**（方法引用级，不是完整经动态验证的时序）：
```text
Polestar 2 Pairing UI / PAK ViewModels
   -> ICupDigitalKeyController
   -> Volvo PhoneAsKey Manager / Vehicle / Repository
   -> pak.p319.sdk.PAKService / VehicleManager / Vehicle
                   ^
            vocmosdk passive-entry integration
```

## 3. Manifest 独立证据

- 声明 `BLUETOOTH`、`BLUETOOTH_ADMIN`、`BLUETOOTH_SCAN`、`BLUETOOTH_CONNECT`、`BLUETOOTH_ADVERTISE`、`ACCESS_FINE_LOCATION` 和 `ACCESS_BACKGROUND_LOCATION`。
- 注册非导出 `pak.p319.sdk.MSDKService`。
- 注册 `com.volvocars.phoneaskey.impl.PhoneAsKeyBootCompleteReceiver`。
- 存在极星 2 的 `PakCarScanningActivity`、`PakSettingsActivity`。
- Manifest 部分另有 `pak.p319.sdk.ScanResultsReceiver` 与 `InitProvider`。

## 4. CCC 与其他技术路线必须分开

`com.polestar.carcontrol.ui.ccckey.CarCccKeyFragment` 调用了 `com.boxing.digitalkey.DigitalKeyFramework` 与 `com.ecarx.open.ccc` 相关接口。它们与极星 2 PAK 链路并列，**不能因为处于同一 APK 就等价于极星 2 的可用协议**。极星官方 FAQ 明确极星 2 主要通过蓝牙配合云连接提供数字钥匙，而不是基于 Wallet 激活的架构。

## 5. 不得误读的证据

- 包含 `PhoneAsKeyPairingError.InvalidLicense` 类型：说明 SDK 表面定义了许可失败类型，**不代表已确认在本车上验证了许可证阻断**。
- APK 包含 mock keystore 测试资源与证书资源：仅记录“存在”；**不导出、不运行、不复用任何密钥/口令/证书字节**，也不把测试数据当作正式配对材料。
- `libvocmo-lib.so` 存在：可确认本地库打包，仍未完成 JNI→PAK 真实调用链与加密实现的动态证明。
- 系统 Root 不能推出可以导出硬件绑定密钥、克隆已配对数字钥匙或绕过车辆认证。
- 当前的 DEX 调用图为自制宽度解析器得到的调用关系，还应与成熟反编译工具交叉核验后用于最终实现决策。

## 6. 结论

**APK_CONFIRMED**：不是“只有界面”，而是拥有完整分层的 Polestar 2 PAK SDK 结构。  
**UNVERIFIED_HYPOTHESIS**：其 APK 内部 SDK 可被未获 OEM 授权的第三方 App 独立初始化和配对。必须等待授权/许可、应用身份和车端注册的实证。