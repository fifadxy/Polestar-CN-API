# 09｜Android 系统约束：BLE、后台与数字钥匙安全设计

**参考平台**：项目 minSdk 26 / targetSdk 34 / compileSdk 35；Root AVD 已验证 Android 14 userdebug x86_64+arm64-v8a native bridge。本文讨论 Android 能力，不等同于 Polestar OEM 授权。

## 一、权限分版本实施

| Android 版本 | 初始 BLE 发现/连接 | 前台/后台约束 | 本分支处理 |
| --- | --- | --- | --- |
| 8–11（API 26–30） | Manifest BLUETOOTH/BLUETOOTH_ADMIN，扫描需要 ACCESS_FINE_LOCATION 运行时授权 | 位置服务开关与后台扫描限制；后台位置有独立规则 | 只读自检，不扫描 |
| 12–13（API 31–33） | BLUETOOTH_SCAN、BLUETOOTH_CONNECT、BLUETOOTH_ADVERTISE 属于 Nearby Devices 权限 | 后台 FGS 启动有系统限制 | 由用户点击后申请权限 |
| 14（API 34） | 同上 | 需要正确的 FGS type 与权限 | 未来使用 connectedDevice，当前无 PAK FGS |
| 15+（API 35+） | 同上并继续核查新行为 | BOOT_COMPLETED 启动某些 FGS 受限制；不能简单强制常驻 | 未来按官方生命周期测试 |

注意：
- Android 12+ 访问 BluetoothAdapter 的一些状态接口可能需要 BLUETOOTH_CONNECT。不能像旧版自检那样先读状态再申请权限。
- BLUETOOTH_SCAN 的 neverForLocation 标记可能过滤一部分 BLE 信标。在没有确认 PAK 实际广播类型和隐私行为前，不要盲目设置；同时不因此默认申请持续位置访问。
- ACCESS_BACKGROUND_LOCATION 不是“所有数字钥匙都必须申请”的通用规则；应按实际扫描与位置使用行为、目标 SDK 和 Play 政策决定。
- 实车 PAK 需要哪些权限，应以合法取得的厂商 SDK 的运行时契约为准，而不是一概复制官方 Manifest。

Android 官方来源：
- https://developer.android.com/develop/connectivity/bluetooth/bt-permissions
- https://developer.android.com/develop/connectivity/bluetooth/ble/background
- https://developer.android.com/develop/connectivity/bluetooth/companion-device-pairing
- https://developer.android.com/about/versions/14/changes/fgs-types-required
- https://developer.android.com/about/versions/15/behavior-changes-15

## 二、前台连接与无感进入不是同一个状态

推荐生命周期（待授权 SDK）：

```text
User-visible initial pairing
    → OEM device identity ready
    → Vehicle trusted / paired
    → Local passive-entry service enabled by user
    → BLE monitoring (system-approved method)
    → Connected / out of range / reconnected
    → OEM key SDK determines key validity
```

Android BLE 文档明确：后台允许某些连接行为，但应用进程终止会关闭连接；另外从后台拉起 Activity / FGS 也有限制。设计中必须区分 PROCESS_RESTART、BLUETOOTH_OFF、NO_PERMISSION、OUT_OF_RANGE、SDK_REJECTED、PAIRED 与 KEY_VERIFIED。

推荐持久连接策略（**仅供未来合法 SDK 接入后实施**）：
1. 配对和初始化在用户可见界面完成，不在开机广播中偷偷授权。
2. Android 14+ 采用适用的 connectedDevice 前台服务权限/类型，或经过评估的 Companion Device Presence 机制。
3. 需要长期运行时展示真实可关闭通知，尊重电池优化、后台限制及用户明确关闭钥匙的选择。
4. 遇到系统杀后台时只尝试合法重连，不跳过设备重新验证或用户授权门槛。
5. 从 Chrome/Android 版本升级、系统省电模式、Root/非 Root 真机覆盖回归。

## 三、Root 与普通真机分工

| 平台 | 验证范围 | 不可推出 |
| --- | --- | --- |
| Root Android 14 AVD | 权限、应用启动/崩溃、Activity 生命周期、模拟 BLE、错误分支、Keystore 接口存在 | OEM 认证/实体车可用 |
| 普通无 Root 手机 | 后台生存、权限 UX、用户通知、真实 BLE 芯片/省电适配 | 仅凭连接=能启动车 |
| 本人授权 Polestar 2 | 正常配对、状态恢复、撤销、异常场景 | 对其他车辆或他人账号适用 |

在 AVD 中的 Android Keystore/KeyMint 服务存在，也不能推断具备与目标真机一致的 TEE、StrongBox、硬件密钥证明或远端信任链。

## 四、密钥隔离和日志设计

- 生产密钥应由获许可 SDK 和安全存储策略管理，默认优先非导出式 Android Keystore；如果 OEM SDK 需要导出格式，必须先进行威胁评估与加密存储审计。
- 研究 APK 仅保存只读诊断状态，不保存任何账号 token、VIN、证书或车辆认证数据。
- 不在 GitHub Issue、CI Artifact 或聊天日志传播私钥、证书私有材料、车辆 token、原始蓝牙认证报文。
- 用户退出当前 PsAssistant 账户时，未来只能清理 PsAssistant 自己的会话/新设备注册状态；不能删除原官方 App 的设备身份或已配对钥匙。
- 生产应用无须也不应依赖 Root。Root AVD 只是开发调试手段。

平台安全参考：https://developer.android.com/privacy-and-security/keystore

## 五、本轮代码映射

- AndroidManifest：新增可选 BLE feature、Android 12+ Nearby Devices 权限、Android 11 及以下兼容权限。
- DigitalKeyEnvironment：检查 BLUETOOTH_CONNECT 后才读取蓝牙状态；不执行任何实际蓝牙连接。
- DigitalKeyResearchActivity：只读显示、仅在用户点击后请求 Nearby Devices 权限；明确未获得 OEM SDK 与车辆配对证据。
- ResearchDigitalKeyProvider：默认不可操作，保留现有云车控功能的隔离。
- 前台 PAK 服务：**尚未实现，禁止在当前版本中声称已支持无感数字钥匙**。

## 六、必须测试的系统场景

| 代号 | 场景 | 预期 |
| --- | --- | --- |
| A01 | Android 14 无 BLUETOOTH_CONNECT | 诊断页显示未知，不能崩溃 |
| A02 | 点击“附近设备权限”，用户拒绝 | 明确仍缺权限，无扫描或自动重试 |
| A03 | 用户同意 Nearby Devices | 更新状态，仅代表系统权限通过 |
| A04 | 蓝牙关闭 | 明确显示 OFF，绝不显示配对成功 |
| A05 | 硬件无 BLE | 只读错误提示 |
| A06 | 电池优化限制 | 可提示，不能自动改系统设置 |
| A07 | 重新打开 App | 无 PAK 凭据缓存，状态仍未启用 |
| A08 | 旋转/小屏/字体放大 | 文本不重叠、不遮挡操作 |
| A09 | Root 与非 Root 真机 | 诊断行为一致，不依赖 su |
| A10 | 进程被杀/重启 | 不会凭缓存状态宣称数字钥匙可用 |
