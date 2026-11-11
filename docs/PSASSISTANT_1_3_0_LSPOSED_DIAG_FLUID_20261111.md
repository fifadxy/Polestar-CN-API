# PsAssistant 1.3.0 / OPPO Bridge 2.7.1 — LSPosed 诊断和流体云

日期 2026-11-11。用户目标：针对新 LSPosed 在 Root 手机上没有实际起作用的问题，提供一键自动诊断、脱敏报告、限定范围的进程重载；工作中的空调（制冷/制热）、净化、露营、充电默认请求流体云并自适应提高车况刷新频率。

## 源码与构建

- 主分支应用代码：`504e454c3d03f70ccfaeb36afd638d1a87f52f93`
- App `1.3.0` / versionCode `175`，Bridge `2.7.1` / versionCode `41`
- [Android Companion APK CI](https://github.com/fifadxy/Polestar-CN-API/actions/runs/42986631589)：**成功**
- [Stable Regression CI](https://github.com/fifadxy/Polestar-CN-API/actions/runs/42986631609)：**成功，664 tests / 0 failures / 0 errors**
- 最终离线完整包 `PsAssistant-v1.3.0-CN-Standalone-FULL.apk`，**7,171,352 bytes**。
- SHA-256 `f1e6e8e3bfea066194a59796b08717ae6dd0b55c0ed1c7bc9429cdc46d09b7b1`
- 原签名证书 SHA-256 `d4cd1b06701f9a8829c5ecc5bbee1398a0f8a655703b6cc356398efa30218a7e`
- APK v2/v3 正常，`android:debuggable=false`，包含经旧 FULL 审批的 `PSACFG2` 独立登录资源与 LibXposed API 102 入口。
- FULL 和原签名只做私有离线交付；不能将密钥、独立登录资源或真实账号资料放在 GitHub。

## 诊断入口与代码

**PsAssistant → 设置 → OPPO 系统兼容 → LSPosed**。

- `android-app/app/src/main/java/io/github/polestarcn/companion/OppoHookDoctor.java`：读取 Root、系统版本、宿主包版本、Hook 模式回报、Bridge self-test、RPK 和 IPC 能力、实际会话状态，输出故障解释，并对 Token、VIN、电话号码、虚拟车 ID 和车名脱敏。
- `android-app/app/src/main/java/io/github/polestarcn/companion/OppoHookProcessManager.java`：使用 `su` 执行**固定白名单**进程的 `pidof` 和 `kill -TERM`，包含 MyDevices/AssistantScreen 已知子进程。单击前显式确认；不会重启整机、SystemUI 或杀死 PsAssistant 自身。操作后请重新打开相关宿主以触发 Hook 加载。
- `android-app/app/src/main/java/io/github/polestarcn/companion/OppoSystemCompatActivity.java`：新增“自动诊断并导出脱敏报告”和“一键重启相关 OPPO 进程 (Root)”按钮，通过 `FileProvider` 分享 `PsAssistant-LSPosed-Diagnosis.txt`，Root 操作在工作线程执行。
- `android-app/oppoBridge/src/main/java/io/github/polestarcn/oppobridge/BridgeDiagnosticsActivity.java`：集成模块设置入口跳转到 OPPO 系统兼容页面，独立 Bridge 仍通过主程序接入。
- 自动诊断仅只读，不下发车辆指令。Root 重载也不意味着新的 Hook 一定匹配宿主版本；重新打开后观察回报才能判断。

### 可手工核对的核心命令

```sh
su -c 'id -u'
su -c 'pidof com.heytap.mydevices'
su -c 'pidof com.coloros.assistantscreen'
```

App 的实际重载命令使用固定白名单和数字 PID 验证，由用户在 UI 中确认后执行。不要对 SystemUI、system_server 或任意 UID 执行范围外的重载。

## 流体云与刷新

- `OppoFluidPolicy.MASTER` 默认开启，仍支持用户在设置中关闭；`climate`、`air`、`camp`、`charging` 四个场景默认允许通知与流体云申请。
- `ActiveVehicleRuntime` 识别真实回读状态：`开启`、`制冷`、`制热`、`cooling`、`heating` 等；不根据未确认的操作指令编造运行状态。
- `OppoFluidCloud` 对已回读的四类运行状态申请 1.0/2.0 兼容通知，带倒计时或充电 SOC。是否有 ColorOS 原生胶囊由系统通知授权和真实 OEM serviceId 支持情况决定。
- `RefreshPolicy` 与 `OppoAdaptiveRefresh` 的工作中核心数据目标：空调/净化/露营 **前台 2 分钟、后台 15 分钟**；单独充电 **前台 5 分钟、后台 30 分钟**。多场景取较快适用档，仍经过请求合并、系统省电和 API 限频。
- 无运行场景时恢复原设置的常规周期。设置关闭自动读取时不绕过用户的省电策略。

## 交付后的实机步骤

1. 在 Root 管理器允许 PsAssistant 执行授权的 `su` 操作。
2. 在 LSPosed 勾选当前要测试的 OPPO 宿主作用域；同时**只启用一种集成/独立模块**，避免重复 Hook。
3. 打开 PsAssistant → OPPO 系统兼容 → **自动诊断并导出脱敏报告**；把 TXT 发给 ChatGPT 即可精确定位当前宿主版本/Hook 阻断点。
4. 当报告提示宿主模式不一致时，可用 **一键重启相关 OPPO 进程**，重新打开设备空间/负一屏/CarLink 后再次诊断。
5. 验证制热、制冷、净化、露营、充电的流体云通知与运行中刷新是否符合设备实际行为。

**验证边界：** CI 与离线 APK 校验已经完成，**没有**在此环境实际进入 Root/LSPosed 手机、验证 OPPO OEM SDK 授权、确认 ColorOS 胶囊展示或执行真实车控；不能把本地 UI/Hook 回报冒充 OEM 官方授权或实车成功。
