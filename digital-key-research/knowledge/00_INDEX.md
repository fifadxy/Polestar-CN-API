# Polestar 2 数字钥匙研究知识库 · 索引

更新：2026-10-27｜原始 APK：极星中国 4.25.0 / 262｜研究分支：research/polestar2-digital-key-pak-20261027

## 证据等级

- **APK_STATIC**：直接来自用户上传 APK 的 DEX、Manifest、方法签名/调用引用等静态结果。
- **PUBLIC_DOCUMENTED**：极星官方车主手册与 Android 官方开发文档。
- **CODE_LAB**：纯 Java 实验代码和测试通过；**不能证明钥匙可用**。
- **EMULATOR_ENV_VERIFIED**：Android 14 + KVM + adb root + BLE/GATT + Keystore 环境确已跑通。
- **APK_RUNTIME_PENDING**：原版 APK 尚未在远程 AVD 实际安装/运行。
- **OEM_PENDING**：第三方 SDK 的合法集成、授权与证书注册尚未证明。
- **VEHICLE_PENDING**：本人授权车辆的新设备配对/无感进入/启动尚未验证。
- **HYPOTHESIS**：静态推测或尚待厂商/实车证据的结论。

## 按阅读顺序

1. [01_STATIC_EVIDENCE.md](01_STATIC_EVIDENCE.md) — APK 基本信息、PAK/CCC 分流、SDK 调用关系。
2. [02_PORTABILITY_DECISION.md](02_PORTABILITY_DECISION.md) — 可行性方案 A/B/C 与 Go/No-Go。
3. [03_ROOT_EMULATOR_RUNBOOK.md](03_ROOT_EMULATOR_RUNBOOK.md) — Root Android 14 AVD 实测及复现。
4. [04_SECURITY_AND_ACCEPTANCE.md](04_SECURITY_AND_ACCEPTANCE.md) — 安全边界与准入。
5. [05_PUBLIC_SOURCES.md](05_PUBLIC_SOURCES.md) — 极星/Android 公共参考。
6. [06_ORIGINAL_APK_DEEP_ANALYSIS.md](06_ORIGINAL_APK_DEEP_ANALYSIS.md) — Original APK 用户/PAK/PassiveEntry 主链。
7. [07_CERTIFICATE_ENROLMENT_CHAIN.md](07_CERTIFICATE_ENROLMENT_CHAIN.md) — UCC/USEC、ECDHE、Actor/Near-Field、Catalogue、PAK 先后关系。
8. [08_MIGRATION_EXECUTION_PLAN.md](08_MIGRATION_EXECUTION_PLAN.md) — **实际开发路径和任务分解**。
9. [09_ANDROID_PERMISSIONS_BACKGROUND_AND_SECURITY.md](09_ANDROID_PERMISSIONS_BACKGROUND_AND_SECURITY.md) — Android 权限、后台连接与安全。
10. [10_IDENTITY_AND_OEM_AUTHORIZATION_GATES.md](10_IDENTITY_AND_OEM_AUTHORIZATION_GATES.md) — **当前 China Session 的身份缺口及 OEM 准入**。
11. [11_EVIDENCE_TEST_MATRIX.md](11_EVIDENCE_TEST_MATRIX.md) — 静态、模拟器、OEM、车辆的分层证据和验收。
12. [12_V083_INTEGRATION_HANDOFF.md](12_V083_INTEGRATION_HANDOFF.md) — **已基于 Android v0.8.3 主线创建独立集成分支，避免旧研究代码覆盖新 UI**。

其他文件：
- [../evidence/apk_4250_summary.json](../evidence/apk_4250_summary.json) — 脱敏摘要与构建记录。
- [../lab/README.md](../lab/README.md) — 纯 Java lab 使用说明。

## 关键纠偏

1. **可以模拟完成 PAK 配对状态，不意味着已经拥有车辆认可的数字钥匙。**
2. **未在 DEX 中找到 App 签名校验，不意味着服务端没有白名单或客户端约束。**
3. **APK 内含厂商私有 SDK，不等于允许将该 SDK 二进制移植和发布。**
4. **当前自研 ChinaPolestarApi.Session 只有会话 token / 服务地址等，未直接提供原版 PAK UserModel.identifier。**
5. 正式 App 不应依赖 Root。Root 只是用于模拟器诊断。
