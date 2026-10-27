# Polestar 2 数字钥匙研究知识库 · 索引

更新时间：2026-10-27｜官方 APK：4.25.0 / 262｜主线：`research/polestar2-digital-key-pak-20261027`

## 证据等级

- **APK_CONFIRMED**：直接解析上传 APK 的清单、DEX 类型/方法或调用关系。
- **PUBLIC_DOCUMENTED**：官方车主手册、官方隐私说明、Android 开发文档。
- **CODE_LAB_VERIFIED**：不涉及真实汽车的本地纯 Java / 静态脚本测试通过。
- **EMULATOR_VERIFIED**：Android 14 / KVM / ADB root / BLE / Keystore 实验环境已真实运行；官方 APK 动态运行仍单独标记。
- **VEHICLE_PENDING**：未在本人授权车辆上验证配对、无感进入和启动。
- **UNVERIFIED_HYPOTHESIS**：需后续测试，不作为可移植的证据。

## 文件

1. [01_STATIC_EVIDENCE.md](01_STATIC_EVIDENCE.md)：APK 基本信息、PAK/CCC 分流、底层调用关系。
2. [02_PORTABILITY_DECISION.md](02_PORTABILITY_DECISION.md)：迁移路线、SDK 授权限制、当前结论。
3. [03_ROOT_EMULATOR_RUNBOOK.md](03_ROOT_EMULATOR_RUNBOOK.md)：ARM64 Root 模拟器安全实验与验收。
4. [04_SECURITY_AND_ACCEPTANCE.md](04_SECURITY_AND_ACCEPTANCE.md)：禁止项、权限、证据门槛与实车验收。
5. [05_PUBLIC_SOURCES.md](05_PUBLIC_SOURCES.md)：公开文档与发布依据。\n6. [06_ORIGINAL_APK_DEEP_ANALYSIS.md](06_ORIGINAL_APK_DEEP_ANALYSIS.md)：直接基于用户上传 4.25.0 APK 的 provisioning / pairing 主链与移植判断。
6. [../evidence/apk_4250_summary.json](../evidence/apk_4250_summary.json)：无凭据元数据、证据及完成状态。
7. [../lab/README.md](../lab/README.md)：可编译的非联网门控模型与测试。

## 决策原则

**使用官方 App 已配好的合法钥匙** 与 **在自研 APK 独立申领新钥匙** 是完全不同的目标；前者可以通过双 App 并存实现，后者需要 OEM 提供合法的分发/签发/授权与车端认可。Android BLE 功能存在 ≠ 可充当已配对车钥匙。模拟器 BLE ≠ 实车 PAK 认证。

从当前 APK 可确定：极星 2 有自己的 PAK 代码链，CCC/Wallet 是另一个需要单独归类的能力。