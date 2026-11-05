# PsAssistant Beta44 — OPPO Watch Indicator 构建与自动化验收

2026-11-05 · Android 主程序 `v1.0.0-beta44` / `versionCode=158` · 手表实验模块 `0.1.0-indicator-lab`

## 实际构建验证

| 指标 | 结果 |
|---|---|
| 编译来源提交 | `5c69f075c2074e85f515890784206973ca64c2ca` |
| GitHub Actions | [41982051695](https://github.com/fifadxy/Polestar-CN-API/actions/runs/41982051695) |
| Android、手表测试、OPPO Bridge Debug 编译 | PASS |
| Android Lint Debug | PASS |
| APK ZIP 完整性、Manifest、classes.dex | PASS |
| Android 主程序 Robolectric + Watch + Bridge 回归 | **549 测试，0 failure，0 error，0 skipped** |
| 手机新增 Indicator 单元测试 | 2 PASS |
| 手表 Indicator 单元测试 | 1 PASS |
| 正式 OPPO Watch 表盘显示 | **未实机验证** |
| 手机→手表 Wear Engine IPC | 本次尚未接入，不能宣称支持 |
| 真实账号登录、车辆控制、OPPO 正式授权 | 本次未实机验证；旧车控链路保持不变 |

## APK 交付

| 文件 | 字节 | SHA256 | 说明 |
|---|---:|---|---|
| `PsAssistant-v1.0.0-beta44-Standalone-FULL.apk` | 9,628,013 | `cb1fe05993743622355e49d02985d70f00b4dc73750830fcc1432685490a3828` | 从本次成功 CI 的 Debug 二进制封装，内置**此前批准**的中国区独立登录资源；关闭 debuggable，v2/v3 签名 PASS |
| `PsAssistant-OPPO-Watch-Indicator-Lab-v0.1.0-debug.apk` | 16,168 | `d66d2e3a44eecbd01ac38ddfdfec054cb7c3a1f26fc6f044835dc999199cb2e4` | 单独安装至兼容手表；v2 Debug 签名 PASS |
| `PsAssistant-v1.0.0-beta44-VALIDATION-debug.apk` | 9,949,494 | `e9b1953a188d6cf5c1c645593c57199ddd7c9b8665619e9b8a8fc9db6b1f0b88` | 公开 CI Debug 验证包，**没有独立登录资源** |

原 FULL APK 的证书 SHA256 与 Beta43/Beta40 完全一致：`d4cd1b06701f9a8829c5ecc5bbee1398a0f8a655703b6cc356398efa30218a7e`。源码 APK 中所有文件内容保持一致，只有 `AndroidManifest.xml` 的 `debuggable` 从 true 修改为 false，同时新增已验证独立登录兼容资源并使用原证书签名。新 APK `versionCode=158` 高于 Beta43 的 157，满足原 FULL 同证书覆盖更新的基本条件。

## 隐私、功能与适用性界限

- 上述 FULL APK、签名 JKS、私有 client asset **仅在用户私有交付空间**；不可上传至公开 GitHub。
- OPPO 文档中的 Indicator 是**手表本地应用**通过 `Notification` 实现的功能：`setOngoing(true)`、`category=navigation/workout` 和 extra `show_heytap_indicator=true`。本次手机端只做条件诊断、手动模拟；本机手表 APK 用于真实表盘显示实验。
- 实际车况、空调、充电或露营不是导航/运动，保留 `CATEGORY_SERVICE`；即使附加 Indicator extra，手表仍可能忽略。
- 模拟类别仅在用户明确点按按钮后发送，默认开关关闭，90 秒自动结束，可提前取消，不触发任何车端联网/远控命令。
- **该 FULL 为非 debuggable 的 Debug 编译代码封装版本，并非经 R8/ProGuard 重编译的生产 Release**，与前一 FULL 的私有签名一致；中国区真机账号登录和 OPPO 系统授权不能凭静态校验保证。
- 详见 [接入与测试技术说明](PSASSISTANT_BETA44_OPPO_WATCH_INDICATOR_20261105.md)。

