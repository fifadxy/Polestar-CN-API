# 12｜Android 分支可复现验证清单与远程操作边界

**目的：把静态“可能有功能”转化为可测量的“这次版本确实实现”。** 本文件并未授权发送任何真实车控指令。

## A. 当前可执行（无需账户/车辆）

| 测试 | 环境 | 通过标准 | 状态 |
| --- | --- | --- | --- |
| 静态 APK 指纹 | Python ZIP/DEX | SHA256/Manifest/DEX 统计可复现 | 已有提取脚本 |
| APK 签名验证 | Python `cryptography` | 签名 + 摘要通过；不推断开发者身份 | 已完成 |
| 33 字段提取 | Python DEX | 与 `evidence/apk-vehicle-schema.json` 一致 | 已完成 |
| 纯 Java 状态测试 | JDK | 7+ 类分类不变量通过 | `scripts/test_models.sh` |
| Android 编译 | GitHub Actions / SDK | `assembleDebug` 成功且有 `app-debug.apk` | 见 CI 最新运行 |
| 首次区域选择 | 模拟器/真机 | 首次弹 China/Global，之后能切换 | **待设备测试** |
| 车辆首页 | 模拟器/真机 | SOC、续航、里程完整；明示 DEMO/OFFLINE | **待设备测试** |
| 两种 Widget | 模拟器/真机 | 不截断、区域切换更新，点击打开 App | **待设备测试** |
| 日志与点击反馈 | 模拟器/真机 | 有反馈、可复制、不包含真实凭证 | **待设备测试** |
| 远控禁用 | 静态 + 真机 | 所有指令按钮仅显示禁用，零网络写入 | 静态通过，真机待验证 |

## B. 后期功能接入阶段（当前不执行）

| 里程碑 | 关键验证 |
| --- | --- |
| China / Global 登录 | 各自官方支持的方式、Token Audience、有效期和刷新互不污染；凭证不入仓 |
| Vehicle Discovery | 账户下多车选择与 VIN 身份匹配，避免数据串车 |
| C3 Read | 电池/续航/里程/开闭分项检验，分别记录来源和采集时间 |
| C3 Stream | 真正 stream 的订阅、断线重连、后台休眠、服务器速率限制；仅有 `stateRpc` 不可写成 Stream 已实现 |
| PCCS | 每项按车型/年款独立验证，禁止“改域名套 Global RPC” |
| Widget & 省电 | 长时间锁屏、Doze、省电、重启、网络断连，禁止把缓存当 Live |
| 操作状态机 | 请求中 → 服务端接受 → 车辆确认 / 失败 / 未确认；未确认不等同失败，也不自动重复写入 |
| 露营与宠物托管 | 本阶段不开放。以后即便能周期运行，手机云端轮询也不是车端安全保障；不能用于保障儿童/动物生命安全 |

## C. 指令状态语义（仅模型，不是功能）

`OperationState`：`DISABLED`、`NOT_SENT`、`SENDING`、`SERVER_ACCEPTED`、`VEHICLE_CONFIRMED`、`FAILED`、`UNCONFIRMED`。

- 只有 `VEHICLE_CONFIRMED` 可以作为「车辆已执行」展示。
- 网络返回不确定时标 `UNCONFIRMED`，**不得**自动发送重复车控命令。
- 当前 CapabilityMatrix 的 `ENABLE_NETWORK_VEHICLE_API=false`、`ENABLE_REMOTE_COMMANDS=false`；任何带后端认证的代码评审必须单独检查这两项。

## D. 安全工作延期政策

R8、混淆、Release 保护、密码学绑定鉴权、SSL Pinning、依赖漏洞扫描和完整组件安全测评按 `06_DEFERRED_SECURITY_R8.md` **暂不执行**。但不会为赶进度牺牲底线：本版本不收集真实密码、Token，不内置第三方静态密钥，不发出远程写操作，也不宣称演示值为实时值。

## E. 本次平台材料

- WorkManager `PeriodicWorkRequest` 文档：`https://developer.android.com/reference/androidx/work/PeriodicWorkRequest`，最短周期 15 分钟，不保证严格按时执行。
- Google Play target API 规则：`https://support.google.com/googleplay/android-developer/answer/11926878`，截至 2026-10-26 普通手机新应用与更新的 Play 提交要求 API 36；当前侧载 Dev APK 不因此自动变成正式发布版本。
