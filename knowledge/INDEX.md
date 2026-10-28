[当前v0.9.2 · 本地能耗、刷新与状态](35_V092_ENERGY_REFRESH_STATUS.md) · versionCode93。

[最新 Android v0.9.1 · 高清 PNG 车图及远控回读](../docs/Psa_v091_PNG_And_Command_Readback.md)：恢复默认黑白车图、加入极星 2/5 分车型图库，5/10/15/18/26 秒五次读回与连续三次确认；公开 CI 缺私有配置不能实车登录。

当前增量：[v0.8.9 China 登录构建兼容](34_V089_CHINA_LOGIN_BUILD.md)。本项修复不等于公开 CI Debug 可以登录；完整构建需私有配置和固定签名。

当前增量：[v0.8.8 图标家族、Widget与露营可读性](33_V088_ICON_FAMILIES_WIDGET_CAMP.md)。当前版本 **v0.8.8-beta1 / 89**，本增量优先于历史图标与Widget显示约定。

当前增量：[v0.8.7 空调试验、原生车况、图标与光效](32_V087_CLIMATE_NATIVE_STATUS_GLOW.md)。当前版本 **v0.8.7-beta1 / 88**，本增量优先于历史记录。

当前增量：[v0.8.6 UI、Widget与刷新策略](31_V086_UI_WIDGET_REFRESH_POLICIES.md)。以本增量和版本说明为准；历史文档保留原验收上下文。

当前版本增量：**v0.8.5-beta1 / 86**，见[KB30](30_V085_INLINE_CONDITION_STANDALONE.md)。历史模板导入要求已被完整签名版独立构建替代。

当前增量：**v0.8.4-beta1**，见[图标、保养与四次回读](29_V084_ICONS_SERVICE_TARGETSOC_READBACK.md)。与历史记录冲突时以此页为准。

# Android Polestar Assistant 主线知识库 · 索引

> 版本 `KB-0.8.3 / Android Parity V3.0` · 2026-10-27 · 主线 `android-polestar-assistant-main`

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
| [24_VOLIKE_WIDGET_SKIN_V081.md](24_VOLIKE_WIDGET_SKIN_V081.md) | VoLike 极星配色、五皮肤视觉预览、按钮文字开关、桌面 Widget 可切换渲染 | v0.8.1 |
| [27_VOLIKE_REFERENCE_LAYOUT_V082.md](27_VOLIKE_REFERENCE_LAYOUT_V082.md) | VoLike 参考图版式：顶部能源条、纵向控制、详细信息轨与横屏保护 | v0.8.2 |
| [08_ROADMAP.md](08_ROADMAP.md) | P0–P7 复刻开发优先级和里程碑 | V3 规划 |
| [09_EXTERNAL_ANALYSIS_REVIEW.md](09_EXTERNAL_ANALYSIS_REVIEW.md) | 外部 AI 建议独立核对 | 已审核 |
| [10_TWO_REPORTS_RECONCILED.md](10_TWO_REPORTS_RECONCILED.md) | 两份 APK 报告交叉验证与证据边界 | 已审核 |
| [11_VEHICLE_DATA_DICTIONARY.md](11_VEHICLE_DATA_DICTIONARY.md) | 参考 APK 的 33 字段字典 | 静态确认 |
| [12_STAGED_VALIDATION.md](12_STAGED_VALIDATION.md) | 参考程序/新程序不同层级测试门槛 | V3 更新 |
| [13_REFERENCE_APK_FIELD_VALIDATION.md](13_REFERENCE_APK_FIELD_VALIDATION.md) | **用户已实测可用**的 China 登录、SOC、里程、空调、解锁和 Token 现象 | `REFERENCE_USER_VERIFIED` |
| [14_ANDROID_PARITY_V3_MASTER_PLAN.md](14_ANDROID_PARITY_V3_MASTER_PLAN.md) | **完整复刻主计划**：P0–P7、架构、优先级、交付 | V3 计划主文件 |
| [15_TOKEN_AND_REMOTE_VALIDATION.md](15_TOKEN_AND_REMOTE_VALIDATION.md) | Token 误报故障树、真实远控状态机、T01–T12 验收 | 计划/待测试 |
| [16_ANDROID_BRANCH_GOVERNANCE.md](16_ANDROID_BRANCH_GOVERNANCE.md) | 新 Android 主线命名、版本、合并和证据管理 | V3 决议 |
| [17_TYRE_MAINTENANCE_WIDGET_V07.md](17_TYRE_MAINTENANCE_WIDGET_V07.md) | 胎压、保养剩余量与 Widget 自适应设计 | v0.7 实现 |
| [18_UI_SKINS_REFERENCE_IMPORT_V071.md](18_UI_SKINS_REFERENCE_IMPORT_V071.md) | 三套 UI 皮肤与模板 APK 签名兼容修复 | v0.7.1 实现 |
| [19_CONTROL_PULL_REFRESH_V072.md](19_CONTROL_PULL_REFRESH_V072.md) | 控制首页模板式下拉刷新与横向手势保护 | v0.7.2 实现 |
| [20_UI_DAY_NIGHT_ITPMS_V073.md](20_UI_DAY_NIGHT_ITPMS_V073.md) | 三套日夜模式、单屏布局、iTPMS 与 Polestar VI | v0.7.3 实现 |
| [21_TWO_SKINS_CLIMATE_V074.md](21_TWO_SKINS_CLIMATE_V074.md) | 两套皮肤、详细风格重排、动态 VI 与空调剩余时间 | v0.7.4 实现 |
| [22_VEHICLE_HEALTH_UI_V075.md](22_VEHICLE_HEALTH_UI_V075.md) | VIN 二级菜单、车辆状态聚合、12V/玻璃水/iTPMS 与动态密度 UI | v0.7.5 实现 |
| [23_V076_STABILITY_WIDGET_RUNTIME.md](23_V076_STABILITY_WIDGET_RUNTIME.md) | v0.7.5 核查、首页/Widget/倒计时/流式读取与固定签名 | v0.7.6 实现 |
| [24_V077_NIOLIKE_VI_CHARGING.md](24_V077_NIOLIKE_VI_CHARGING.md) | NIOlike 日夜皮肤、静态 VI、胎压显示与充电门控 | v0.7.7 实现 |
| [25_V078_CONDITION_LOCATION_TRIP.md](25_V078_CONDITION_LOCATION_TRIP.md) | 四套皮肤、健康/保养、c3Lbs 高德预览与行程来源冲突 | v0.7.8 实现 |
| [26_V079_KEYLESS_PARKING_WEB.md](26_V079_KEYLESS_PARKING_WEB.md) | 免 Key 官方地图网页、WebView限制、错误隔离和实测边界 | v0.7.9 实现 |
| [28_V083_UI_WIDGET_MAP_ICONS.md](28_V083_UI_WIDGET_MAP_ICONS.md) | PS默认、自动保养、精简地图/可选定位、PSeu/粗体/原生Widget与图标 | v0.8.3 实现 |
| [evidence/reference-20261027-mapping.json](evidence/reference-20261027-mapping.json) | 最新参考 APK 元数据与健康/位置方法映射 | 静态确认 + 用户参考实测 |
| [evidence/apk-v2-signature.json](evidence/apk-v2-signature.json) | 参考 APK 签名及内容摘要证据 | 已验证 |
| [evidence/apk-vehicle-schema.json](evidence/apk-vehicle-schema.json) | 33 字段独立提取 | 已验证 |
| [evidence/apk-inventory.json](evidence/apk-inventory.json) | APK 静态清单 | 可复现 |

### 统一事实标签

`APK_CONFIRMED` 仅代表 APK 字节/类/方法/字段存在；`REFERENCE_USER_VERIFIED` 代表**用户亲测参考 APK**功能可用；`EXTERNAL_ANALYSIS` 代表外部报告判断；`IMPLEMENTED_DEMO` 代表新代码只有演示模型；`CI_VERIFIED` 代表可编译；`PROJECT_DEVICE_VERIFIED` 才代表**本项目新 APK 在授权车辆上端到端成功**；`PLANNED` 或 `MAPPING_REQUIRED` 代表待开发。

**当前状态**：Android v0.8.7-beta1 延续真实 China 登录、车况/远控、停车位置和 Widget 能力，并在 CI 中持续做手机多皮肤/大字号布局回归；尚未完成本轮账号、车辆和独立 Wear OS 端到端验收。历史 DEMO 记录按各文件的版本阅读，不代表当前工程功能。

**文档更新规则**：源码/协议/接口新证据出现时，同提交更新功能矩阵 `04`、进度 `07`、相关研究 `03`、验证 `12`；跨平台证据同步 `docs/HISINGEN_GLOBAL_CN_API_COMPARISON.md`，不得把 Global 的 RPC 或第三方 APK 的实测结论冒充本项目 China API 完成。
