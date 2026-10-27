# 02｜数字钥匙移植的方案决策

## 必须满足的五层条件

1. **设备**：Android 支持 BLE，运行后台扫描/连接，有兼容的蓝牙硬件和系统权限。
2. **账号**：车主本人 Polestar ID 和车辆绑定已被官方系统认可；远程车辆 API token **不是** 数字钥匙凭证。
3. **OEM 许可**：获得明确的可分发 PAK SDK、授权与使用范围；安装包中自带私有 SDK **不代表有权移植或重新签名发布**。
4. **注册**：通过受支持的流程，由车辆和/或 OEM 授权系统承认新设备与新的钥匙身份。
5. **本地认证**：车辆接受该设备合法钥匙的 BLE 加密认证；重新签名、换包名、Root、导入缓存都不能取代车端认可。

## 方案比较

| 方案 | 预期可用功能 | 当前可行性 | 决定性限制 |
| --- | --- | --- | --- |
| **A：官方 App 持钥匙 + 自研 PsAssistant 做车控** | 由官方 App 实现无感进入/启动；本项目实现车况、Widget、云端命令 | **立即可用的架构方向**（需手机/车上实际正常配对） | 需要保留官方 App；两套应用不可声称钥匙能力互通 |
| **B：获得许可的 PAK/VOCMO SDK 独立集成** | 新 APK 在合法车主确认后获取属于新设备的 PassiveEntry 材料并完成 PAK 配对 | **可行性上升，已有明确主链** | VOCMO Actor Certificate / Account Linking、新设备 CarAccess 授权、SDK 分发权、车端确认 |
| **C：仅研究官方协议、Clean-room 兼容实现** | 如果全部开放且合法注册，理论上可独立实现 | **不确定性极高** | 认证标准、服务端许可、车端验签、知识产权、稳定性 |
| **D：直接移植 CCC / Google Wallet 模块给极星 2** | 不应承诺 | **不推荐** | Polestar 2 硬件/体系不等于后续车型 Wallet 链路 |
| **E：复制原 APK 的钥匙、Keystore、签名或伪造车辆授权** | 不可接受 | **排除** | 无法证明所有权、破坏设备信任边界，易导致非法访问 |

## 本次样本对迁移路线的实际影响

- 找到 `com.volvocars.phoneaskey` 与 `pak.p319.sdk` 意味着已有明确的官方软件边界，可以按 API/适配器方式设计自研模块。
- **修正：** 直接解析原 APK 后确认 P319 `Vehicle$LicenseState` 为 `ACTIVE / PENDING / EXPIRED / INVALID`，并由 `ConnectionInfo.startTime/endTime` 计算；`PairingError.InvalidLicense` 当前更强地指向**车辆这份 PAK 凭据的时间有效性**，而不是宿主 APK 签名许可证。
- `PhoneAsKeyRepository.loginUser` 已追到 Polestar `UserModel.identifier`；`addPassiveEntry` 则由 VOCMO/CarAccess 收到车辆下发的 PassiveEntry 材料后创建 P319 `ConnectionInfo`。技术主链已经明确，但合法设备证书签发与车端授权仍必须验证。
- CCC 的 `DigitalKeyFramework` 位于另一链路；不能把“APK 中有 CCC”错误归因为“极星 2 可以 Wallet 激活”。
- 目前缺少设备绑定的真实业务权限、可公开发布的 SDK、独立新设备配对证明；**不能判定方案 B 已可交付**。

## 决策门槛（Go / No-Go）

- **Go-A**：继续主 APK 的 China 云车控 + 官方钥匙双 App 共存。
- **Go-B-Research**：继续验证 VOCMO Actor Certificate / Account Linking / PassiveEntryConfiguration / 新设备正常配对链；Root Android 14 AVD 已可用于环境和生命周期实验。
- **No-Go-B-Production**：直到 OEM 许可/签名/服务器/车端注册均有可检查证据，不将“独立数字钥匙”放入可控制车辆的 Release APK。
- **No-Go-Credential-Cloning**：不转存、不复制、不重放原已配对钥匙或真实认证数据。

## 与 Android 主项目衔接

建议在后续合规方案成立后，定义 `DigitalKeyProvider`（接口）和 PAK 实现（授权可替换）、`DigitalKeyState`、BLE 能力检查、系统权限 UX、后端可用性与错误隔离。当前分支中的纯 Java Lab 仅验证“没有授权就不允许宣称钥匙可用”的门控逻辑；主分支和当前用户安装包不作改动。