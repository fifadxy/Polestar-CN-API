# Polestar 2 Digital Key / PAK — Android 研究分支

**基准 APK**：极星中国 Android 4.25.0 / 262；SHA-256 99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef。  
**分支**：research/polestar2-digital-key-pak-20261027，从 android-polestar-assistant-main 分出，保持已发布应用和 Mac 工程不受影响。

## 项目状态（分层）

| 事项 | 状态 |
| --- | --- |
| 用户提供 APK 的 DEX、Manifest、PAK/VOCMO/P319 静态研究 | **已完成主要调用链梳理**，详见 06/07 |
| Root Android 14 AVD、ADB root、BLE、Keystore2 | **GitHub Actions 实测通过** |
| Android 研究适配器及只读数字钥匙诊断页 | **已提交源码**，按 CI 结果验收 |
| 独立 SDK 的 OEM 授权与重新分发权 | **未确认** |
| 新 App 的用户 identifier、OEM 设备证书注册 | **未确认** |
| 实际 Polestar 2 近场钥匙配对和启动 | **未验证** |
| 原版 APK 在远端 CI AVD 的动态安装 | **尚未完成原始字节的私密传递** |

## 当前应该从哪里开始

**1. 阅读 [移植实施方案](knowledge/08_MIGRATION_EXECUTION_PLAN.md)**：M0–M6 里程碑、代码目录、阻塞项与验收门槛。  
**2. 阅读 [身份授权差距](knowledge/10_IDENTITY_AND_OEM_AUTHORIZATION_GATES.md)**：当前 ChinaPolestarApi.Session 没有 PAK 所需的 UserModel.identifier。  
**3. 阅读 [Android 权限与后台](knowledge/09_ANDROID_PERMISSIONS_BACKGROUND_AND_SECURITY.md)**：Android 12+ BLE 授权和 Android 14+ 后台策略。  
**4. 运行代码**：只读诊断 Activity 在 PsAssistant 设置页，Lab 测试可执行 `sh digital-key-research/scripts/run_lab_tests.sh`。  
**5. 对照 [测试矩阵](knowledge/11_EVIDENCE_TEST_MATRIX.md)**，严格区分模拟成功、OEM 授权和实际车端认可。

## 研究版代码

- `android-app/app/src/main/java/io/github/polestarcn/companion/digitalkey/` — Provider、状态和环境自检。
- `digital-key-research/lab/` — 纯 Java 阶段门控和测试。
- `.github/workflows/digital-key-research-build.yml` — Android 编译和调试 APK 产物。
- `.github/workflows/digital-key-emulator.yml` — Root Android AVD 的复现环境。

## 重要约束

当前仓库的研究代码**没有启用数字钥匙功能**，不连接车辆、不签发密钥、不运行 PAK pairing。原始 APK、DEX、SO、账号、VIN、真实证书/私钥和配对报文均不得入库。不得把“未发现客户端签名校验”理解为 OEM 后端已允许第三方注册。

知识库完整目录：[knowledge/00_INDEX.md](knowledge/00_INDEX.md)。
