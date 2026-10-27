# 03｜Root ARM64 Android 模拟器验证 Runbook

## 环境核对结论

### 2026-10-27：Root Android Emulator 已真实跑通

**EMULATOR_VERIFIED / ADB_ROOT_VERIFIED / KVM_VERIFIED**

通过 GitHub Actions `Digital Key Emulator Lab` 在真实 hosted runner 上完成：

- Host：Ubuntu 26.04 / x86_64 / AMD EPYC。
- `/dev/kvm`：存在并可用；AMD-V 嵌套虚拟化可用。
- Android：API 34 / Android 14 / Google APIs / `userdebug` / `dev-keys`。
- Emulator：Android Emulator 37.2.12；一次已记录启动约 **38.5 s**。
- ADB：启动前 `uid=2000(shell)`；执行 `adb root` 后为 **`uid=0(root)` / `whoami=root`**。
- SELinux：**Enforcing**，没有为了测试关闭强制访问控制。
- Guest ABI：`x86_64,arm64-v8a`。
- Native bridge：**`libndk_translation.so`**，因此 ARM64-only native APK 具有运行兼容路径。
- Bluetooth：`android.hardware.bluetooth` + `android.hardware.bluetooth_le`；Bluetooth Manager 为 **ON**；GATT Service 已运行。
- Keystore：`android.hardware.hardware_keystore=300`；Keystore2、KeyMint、Remote Provisioning 相关 Binder service 均存在。
- 纯 Java 数字钥匙安全门控：16/16 组合测试通过。

运行证据由 Draft PR #2 的 Actions 自报告评论与 workflow logs 保存。研究分支包含可重复 workflow：`.github/workflows/digital-key-emulator.yml`。

### 官方 APK 进入虚拟机的当前状态

用户上传的中国区 4.25.0 APK 已在本地确认：
- MD5 `d66b01f6b6cf8cdbda0d226ae0926c60`
- SHA-256 `99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef`

CI 已尝试从公开应用商店/镜像站获取相同文件，但当前可自动解析到的 URL 没有返回完全匹配字节。因此**没有安装未校验的 APK**。Root AVD 环境本身已验证成功；官方 APK 动态运行仍单独标为 `APK_RUNTIME_PENDING`。

## 建议环境

- Android Studio、Platform Tools、Emulator 与相匹配的 **ARM64 userdebug / 可 root 的授权实验镜像**；使用合法获取的镜像，避免重签官方 APK。
- 如果是 Mac Apple Silicon，优先 ARM64 虚拟设备；普通 PC 则在支持 ARM64 客体的虚拟化环境中验证，注意性能与系统差异。
- 使用单独测试账号/模拟设备、空白实验数据；不要导入已经在真实车辆上使用的数字钥匙、备份、Keystore 或认证文件。
- PAK 的扫描/配对还依赖真实 BLE 广播、车端状态及授权系统；普通虚拟蓝牙只能验证权限、生命周期和模拟信号，**不能据此证明完成真实车钥匙配对**。

## 无副作用的先验检查

执行 `sh digital-key-research/scripts/emulator_preflight.sh`：
1. 列出本机 adb/emulator 可执行文件、已连接模拟器。
2. 打印设备 ABI、build.type、shell uid 和官方包安装状态。
3. 如果客体不是 arm64-v8a，报告 SO 兼容性阻塞。

在正常安装官方 APK 的前提下，仅验证以下项目：
- App 能否启动、是否出现系统权限弹窗。
- 数字钥匙入口能否展示；是否有权限缺失 / 不支持设备 / 授权异常提示。
- `PhoneAsKeyBootCompleteReceiver` / `MSDKService` 在系统中是否被注册（仅检查清单与组件状态）。
- 使用模拟 BLE 外设时，观察广播发现与生命周期 **是否** 在设备提供的系统能力下正常；不连接实车或重放车辆信息。
- 退出登录、蓝牙关闭、权限撤回、Doze、屏幕关闭、重启后，界面状态是否正确；不记录敏感数据。

## 实验记录模板

| Test ID | 测试 | 结果 | 证据 | 判定 |
| --- | --- | --- | --- | --- |
| R01 | AVD ABI + Native libs 匹配 | PENDING | getprop cpu abi | 环境 |
| R02 | APK 原签名安装成功 | PENDING | 包安装状态 | 安装 |
| R03 | 官方 App 启动/UI | PENDING | 截图（脱敏） | UI |
| R04 | PAK BLE 权限申请 | PENDING | 系统权限状态 | 系统 |
| R05 | 设备不支持 BLE | PENDING | UI 拒绝/错误码（不含数据） | 错误路径 |
| R06 | PAK Service 生命周期 | PENDING | 组件生命周期统计（不含认证载荷） | 系统 |
| R07 | 模拟 BLE 广播可用性 | PENDING | 模拟设备的非敏感测试标识 | 实验 |
| R08 | 无 OEM 许可时正常阻断 | PENDING | 类型化错误与 UI 状态 | 安全 |
| R09 | 退出/重启/撤权限后的隔离 | PENDING | 本地状态机 | 安全 |
| V01 | 新 APP 独立注册与配对授权 | NOT RUN | OEM 同意 + 本人车辆书面测试计划 | 实车 |
| V02 | 授权设备近场认证/启动 | NOT RUN | 本人授权与车辆验证 | 实车 |

**禁止项：** 不导出真实凭证、加密材料、授权口令，不拦截/重放真实车端挑战，不绕过 License、TLS、APP 签名或 MFA。只要许可/授权不明，停止独立配对验证。

## 下一步所需验证

1. 用户可在个人 Mac 的 Android Studio / ARM64 模拟环境运行无敏感数据实验。
2. 在本项目授权范围下确认 PAK SDK 是否可获得正式 SDK 许可与新设备配对授权。
3. 比对模拟器检测结果与真机，但**不能以模拟器结果替代实车验收**。
