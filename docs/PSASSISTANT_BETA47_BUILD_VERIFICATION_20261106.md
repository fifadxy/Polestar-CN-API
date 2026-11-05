# PsAssistant Beta47 — 构建、签名与测试验收（2026-11-06）

**版本**：`v1.0.0-beta47` / `versionCode=161`，Android 目标主分支 `android-polestar-assistant-main`。

## GitHub Android CI 结果

- 通过测试的 Android 代码提交：`c5ea4cb6bb693542a638359098a7170eeb45586f`。
- CI：[Android Beta47 Per-Skin Controls，run 42124120790](https://github.com/fifadxy/Polestar-CN-API/actions/runs/42124120790)。
- `assembleDebug`、`oppoBridge:assembleDebug`、`lintDebug`：**PASS**。
- Robolectric / JVM：**主程序 554 项通过、OPPO Bridge 12 项通过，合计 566 项通过；失败 0、错误 0、跳过 0**。
- 新增 `Beta47AirIdleReadoutTest`、`Beta47SkinButtonLayoutTest`、`Beta47CenteredMetricsTest`：通过。
- CI 工件：`PsAssistant-v1.0.0-beta47-VALIDATION-debug.apk`、`PsAssistant-OPPO-Bridge-beta47-debug.apk`，及测试结果 XML。

## 已制作的独立登录兼容 FULL APK

| 字段 | 结果 |
|---|---|
| 文件 | `PsAssistant-v1.0.0-beta47-Standalone-FULL.apk` |
| 字节 | **9,668,971 B**（低于 10,000,000 B） |
| SHA-256 | `a4b60cecc794f2539cca4639eeed5591759e874426d48e55027b27cf10275a04` |
| Android 应用 ID | `io.github.polestarcn.companion` |
| 版本 | `1.0.0-beta47` / `161` |
| 来源 CI Debug APK SHA-256 | `4dbf93d9f44ea5daa4c9d85cd63f682871ffdc0f9ce20113725f8b0ec426fa70` |
| 签名 | v2 与 v3 验证均 PASS |
| 原历史 FULL 证书 SHA-256 | `d4cd1b06701f9a8829c5ecc5bbee1398a0f8a655703b6cc356398efa30218a7e` |
| 静态二进制完整性 | 原 ZIP 全部条目字节未改变，仅应用 Manifest 的 debuggable 位由 true 改为 false；新增已核验的私有独立登录兼容资源与 APK 签名 |
| 兼容输入 | 从用户已批准的 Beta46 Standalone FULL 中提取同版格式客户端兼容资源；不写入公开 GitHub |
| 真实中国区账号登录和远程车控 | **未实机验证** |

**发布边界**：该 FULL 包使用成功 CI 的 Debug 编译字节码，离线封装并以相同历史签名重新签名、关闭 Manifest 的可调试标志，**不等于独立构建的正式 Production Release**。旧版完整安装包若有相同包名和签名，可按 Android 版本码规则覆盖升级，建议先备份设置。中国区真实账号登录、手机实机 UI、ColorOS CarLink 及车辆命令回读仍须实际验证。

## 文件与功能

- [Beta47 UI 专项说明](PSASSISTANT_BETA47_SKIN_CONTROLS_20261106.md)
- [Beta47 技术知识库](../knowledge/PSASSISTANT_BETA47_SKIN_CONTROLS_20261106.md)

本次变更只涉及显示、设置保存和离线编辑预览；不改变手机与极星云端认证、远控确认、露营托管安全检查、OPPO LSPosed Stable/NEW/Mock 逻辑。
