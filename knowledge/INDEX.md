# Android 项目知识库 · 索引

> 版本 `KB-0.2` · 建立于 2026-10-26 · 持续维护

本知识库面向 Polestar2 Widget&API Android 分支，与 Mac/Hisingen 分支共用上游 China/Global API 事实，但 Android 运行状态单独记录。

| 文件 | 内容 | 当前状态 |
|---|---|---|
| [01_APK_EVIDENCE.md](01_APK_EVIDENCE.md) | 原始 APK 指纹、Manifest、DEX 与代码模块证据 | 静态证据已入库 |
| [02_ARCHITECTURE.md](02_ARCHITECTURE.md) | Android 项目结构、模块职责、模拟/真实接口边界 | v0.1 骨架已落地 |
| [03_API_RESEARCH.md](03_API_RESEARCH.md) | 登录、Discovery、C3/PCCS、Proto 服务对照 | 映射中 |
| [04_FEATURE_MATRIX.md](04_FEATURE_MATRIX.md) | 原 APK vs 新工程的功能差异及验证状态 | 持续更新 |
| [05_BUILD_AND_TEST.md](05_BUILD_AND_TEST.md) | 编译环境、指令、验证步骤、限制 | dev2 CI 编译通过 |
| [06_DEFERRED_SECURITY_R8.md](06_DEFERRED_SECURITY_R8.md) | 混淆、安全、权限等优化待办 | **DEFERRED，不执行** |
| [07_PROGRESS.md](07_PROGRESS.md) | 累计增量更新记录与阶段结果 | 进行中 |
| [08_ROADMAP.md](08_ROADMAP.md) | 研究/实现优先级、验证条件 | 规划完成 |
| [09_EXTERNAL_ANALYSIS_REVIEW.md](09_EXTERNAL_ANALYSIS_REVIEW.md) | 另一位 AI 建议中可采纳与需核查部分 | 交叉审核 |
| [10_TWO_REPORTS_RECONCILED.md](10_TWO_REPORTS_RECONCILED.md) | 两份用户上传报告与 APK、原项目事实逐条交叉审阅，含 Gemini 访问边界 | 完成首轮 |
| [11_VEHICLE_DATA_DICTIONARY.md](11_VEHICLE_DATA_DICTIONARY.md) | 33 个 DEX 车辆字段清单，数据源与新鲜度语义 | 静态确认 |
| [12_STAGED_VALIDATION.md](12_STAGED_VALIDATION.md) | 纯 Java / CI / 设备 / 真车不同阶段验收条件 | 持续维护 |
| [evidence/apk-v2-signature.json](evidence/apk-v2-signature.json) | 只读 APK v2 RSA 签名与内容完整性复核结果 | 证据已确认 |
| [evidence/apk-vehicle-schema.json](evidence/apk-vehicle-schema.json) | 从 DEX 提取参考 APK `Vehicle` 全部 33 字段 | 静态确认 |
| [evidence/apk-inventory.json](evidence/apk-inventory.json) | 原 APK 自动静态提取结构清单 | 可复现 |

**证据级别**：`APK_CONFIRMED` = 直接从当前 APK 可读二进制验证；`PROJECT_DOC` = 已有项目知识文件记录；`EXTERNAL_ANALYSIS` = 附件中另一 AI 判断但尚未独立复核；`IMPLEMENTED_DEMO` = 新工程有可运行实现但非车机实测；`REAL_DEVICE_VERIFIED` = 真机真实账户/车辆端到端验证。

维护规程：每当分析得到新事实，同时更新本知识库相应表格 + [07_PROGRESS.md](07_PROGRESS.md)；API 研究验证后同步上游 `docs/HISINGEN_GLOBAL_CN_API_COMPARISON.md`。**不把“源码中有接口字符串”自动升级为真实车辆可用。**
