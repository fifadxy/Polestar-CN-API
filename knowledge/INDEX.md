# Android Polestar Assistant 主线知识库 · 索引

> 版本 `KB-0.3 / Android Parity V3.0` · 2026-10-26 · 主线 `android-polestar-assistant-main`

本目录是 **Polestar2 Widget&API** 独立 Android 子项目的长期知识库。`main` 为 GitHub 仓库默认主分支，本项目专属的 Android 主线是 `android-polestar-assistant-main`，不修改 Mac/Hisingen 的独立工程。

| 文件 | 作用 | 状态 |
|---|---|---|
| [01_APK_EVIDENCE.md](01_APK_EVIDENCE.md) | 参考 APK 指纹、DEX、Manifest、业务方法 | `APK_CONFIRMED` |
| [02_ARCHITECTURE.md](02_ARCHITECTURE.md) | 当前工程结构和计划中的模块分层 | DEMO + V3 规划 |
| [03_API_RESEARCH.md](03_API_RESEARCH.md) | China/Global 登录、C3/PCCS、远控研究 | 映射中 |
| [04_FEATURE_MATRIX.md](04_FEATURE_MATRIX.md) | 参考 APK 用户实测 vs 本项目实际功能矩阵 | V3 更新 |
| [05_BUILD_AND_TEST.md](05_BUILD_AND_TEST.md) | 编译环境、历史 CI 与验证记录 | 历史 Debug CI 通过 |
| [06_DEFERRED_SECURITY_R8.md](06_DEFERRED_SECURITY_R8.md) | R8、混淆和非必要安全优化后期池 | `DEFERRED` |
| [07_PROGRESS.md](07_PROGRESS.md) | 项目研究、开发和分支同步流水 | 持续追加 |
| [08_ROADMAP.md](08_ROADMAP.md) | P0–P7 复刻开发优先级和里程碑 | V3 规划 |
| [09_EXTERNAL_ANALYSIS_REVIEW.md](09_EXTERNAL_ANALYSIS_REVIEW.md) | 外部 AI 建议独立核对 | 已审核 |
| [10_TWO_REPORTS_RECONCILED.md](10_TWO_REPORTS_RECONCILED.md) | 两份 APK 报告交叉验证与证据边界 | 已审核 |
| [11_VEHICLE_DATA_DICTIONARY.md](11_VEHICLE_DATA_DICTIONARY.md) | 参考 APK 的 33 字段字典 | 静态确认 |
| [12_STAGED_VALIDATION.md](12_STAGED_VALIDATION.md) | 参考程序/新程序不同层级测试门槛 | V3 更新 |
| [13_REFERENCE_APK_FIELD_VALIDATION.md](13_REFERENCE_APK_FIELD_VALIDATION.md) | **用户已实测可用**的 China 登录、SOC、里程、空调、解锁和 Token 现象 | `REFERENCE_USER_VERIFIED` |
| [14_ANDROID_PARITY_V3_MASTER_PLAN.md](14_ANDROID_PARITY_V3_MASTER_PLAN.md) | **完整复刻主计划**：P0–P7、架构、优先级、交付 | V3 计划主文件 |
| [15_TOKEN_AND_REMOTE_VALIDATION.md](15_TOKEN_AND_REMOTE_VALIDATION.md) | Token 误报故障树、真实远控状态机、T01–T12 验收 | 计划/待测试 |
| [16_ANDROID_BRANCH_GOVERNANCE.md](16_ANDROID_BRANCH_GOVERNANCE.md) | 新 Android 主线命名、版本、合并和证据管理 | V3 决议 |
| [evidence/apk-v2-signature.json](evidence/apk-v2-signature.json) | 参考 APK 签名及内容摘要证据 | 已验证 |
| [evidence/apk-vehicle-schema.json](evidence/apk-vehicle-schema.json) | 33 字段独立提取 | 已验证 |
| [evidence/apk-inventory.json](evidence/apk-inventory.json) | APK 静态清单 | 可复现 |

### 统一事实标签

`APK_CONFIRMED` 仅代表 APK 字节/类/方法/字段存在；`REFERENCE_USER_VERIFIED` 代表**用户亲测参考 APK**功能可用；`EXTERNAL_ANALYSIS` 代表外部报告判断；`IMPLEMENTED_DEMO` 代表新代码只有演示模型；`CI_VERIFIED` 代表可编译；`PROJECT_DEVICE_VERIFIED` 才代表**本项目新 APK 在授权车辆上端到端成功**；`PLANNED` 或 `MAPPING_REQUIRED` 代表待开发。

**当前状态**：历史 Android `v0.1.0-dev2` 已通过 CI，但实际车辆登录/数据/远控均未接通。用户对参考 APK 的实车经验是复刻开发依据，而不是当前代码完成证明。

**文档更新规则**：源码/协议/接口新证据出现时，同提交更新功能矩阵 `04`、进度 `07`、相关研究 `03`、验证 `12`；跨平台证据同步 `docs/HISINGEN_GLOBAL_CN_API_COMPARISON.md`，不得把 Global 的 RPC 或第三方 APK 的实测结论冒充本项目 China API 完成。
