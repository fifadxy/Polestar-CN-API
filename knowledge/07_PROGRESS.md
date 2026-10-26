# 07｜Android 分支增量研究与开发进度

格式：`日期 | 阶段 | 发现/实现 | 证据 | 状态 | 下一步`。每次分析追加，不覆盖过去状态。

## 2026-10-26 / Phase 0 — 参考 APK 入库

- `APK_CONFIRMED`：样本 SHA256 `8cae07a3b5c6050487443914ebb55b1d2f2f26e258f4b369837f2b88de052735`；4,856,483 字节；526 ZIP 成员；两份 DEX；包名 `app.c3garage.companion`。
- `APK_CONFIRMED`：识别主 UI、PolestarApi、Widget、WidgetRefreshWorker、CampModeWorker；7 个核心 Java 源文件名。
- `APK_CONFIRMED`：确认中国区网关 / 密码登录 / Global Token 刷新路径及 5 类 C3 状态服务。
- `PROJECT_DOC`：对照现有 `docs/HISINGEN_GLOBAL_CN_API_COMPARISON.md`，China C3 主路径与 Global 双 Token 逻辑不得混用。
- `DECISION`：R8/混淆/安全加固后期评估，本阶段 DEFERRED；真实远程命令全部禁用。

## 2026-10-26 / Phase 1 — 新 Android 工程

- `IMPLEMENTED_DEMO`：原生 Java Android 项目结构，选择 China / Global，区域独立持久化。
- `IMPLEMENTED_DEMO`：车辆看板、模拟 SOC/Range/Odometer、车身状态草图、车辆列表入口。
- `IMPLEMENTED_DEMO`：两种原生 Widget、模拟数据刷新、操作反馈与可复制日志。
- `IMPLEMENTED_DEMO`：真实登录、网络读取、C3 stream、PCCS 与所有远控均标注未验证/未开放。
- `PREPARED`：Gradle 8.7 / AGP 8.5.2 工程和 GitHub Actions Debug 编译配置。
- `BUILD_LIMITATION`：本地当前环境缺 Android SDK/Gradle/ADB 与可用 DNS 下载；本地无法编译 APK；CI 状态需独立检查。

## 下次更新需附

- Git 分支名称与 commit SHA；CI run 及 APK checksum（若构建成功）。
- 真机上各 UI/Widget 页面截图与交互问题清单；不能仅使用静态方法名推断。
- 每个新 C3 RPC 的来源、方法、请求/响应字段、测试车型与验证级别（脱敏）。

## 2026-10-26 / Phase 1.1 — 双报告交叉融合与 CI 维护

- `APK_CONFIRMED`：原始 APK v2 RSA 签名与文件内容摘要独立通过，证书 SHA256 已核对；仅证实完整性。
- `APK_CONFIRMED`：`PolestarApi$Vehicle` 共 **33 个字段**；生成 `evidence/apk-vehicle-schema.json` 与字典，纠正报告 B 的 30 个字段。
- `SOURCE_REVIEW`：分析 A/B 已逐项归纳到 `10_TWO_REPORTS_RECONCILED.md`；未见 Gemini 链接正文不计为证据；流式 API 与后台定时仍区分静态/动态。
- `NEW_PROJECT_CODE`：新增 `TelemetryStatus`, `OperationState`、`ModelStateTest`；演示数据采集时间为 0，按钮反馈无真实车辆命令。
- `BUILD_FIX`：针对两次 GitHub CI SDK 初始化失败改成内置 cmdline-tools 定位；**新 CI 状态待运行确认**。
- `DECISION`：R8、混淆、安全加固、真实远控全部延期；继续独立 Android 分支，不覆盖 Hisingen Mac。

本轮具体 commit SHA、build run、APK 产物以 GitHub Actions 页面及最终交付说明为准。

## 2026-10-26 / Phase 1.2 — Android Debug 编译完成

- `CI_VERIFIED`：Actions Run [`40261229408`](https://github.com/fifadxy/Polestar-CN-API/actions/runs/40261229408) 成功，构建 commit `5f3afc415acfe4ffc2d33fd8613511e51019b7fb`。
- `BUILD_PASS`：成功生成 `app-debug.apk`（28,951 字节，SHA256 `0e8d417cd6354ecf55ac414492a466ce10d02778e40eb59aa850bdbac0767a40`）；人工解包、zip 完整性检查、v2 签名内容摘要检查均通过。
- `NOT_YET_TESTED`：Android 模拟器/真机 UI、Widget、账户登录、中国区车辆 API、真实控制；不可从编译成功推断行为可用。
- 新增文档仅补成功记录和签名读取的准确算法位数标注，**不改变参与上述 CI 的 Android 应用源代码**。R8 与后期安全工作仍 `DEFERRED`。

## 2026-10-26 / Android Assistant Parity V3.0：主线创建与计划/知识库同步

- `USER_EVIDENCE`：用户明确报告参考 `app-release.apk.1.1(1).1` 的中国区登录、SOC、累计里程、空调控制与车门解锁实际可用，且长期使用可能弹 Token 错误但车控仍有效；标签 **`REFERENCE_USER_VERIFIED`**，非本工程设备实测。
- `PLAN_UPDATED`：将远程控制从原有后期研究调整为 V3 核心 P3，先复现参考 APK 可用的空调/解锁；Token 误报列 P1 核心诊断；P4/P5 原版 UI/Widget；P6 Global/Live/PCCS。
- `BRANCH_DECISION`：新建 `android-polestar-assistant-main` 作为 Android 子项目长期主线；保留历史 `feature/android-companion-v0.1` 和仓库原默认 `main`，Mac/Hisingen 不改。
- `KNOWLEDGE_SYNC`：新建 `13_REFERENCE_APK_FIELD_VALIDATION.md`、`14_ANDROID_PARITY_V3_MASTER_PLAN.md`、`15_TOKEN_AND_REMOTE_VALIDATION.md`、`16_ANDROID_BRANCH_GOVERNANCE.md`，并更新 INDEX/API/FEATURE/ROADMAP/VALIDATION/README。
- `CODE_STATUS_UNCHANGED`：`v0.1.0-dev2` 仍为 DEMO/OFFLINE，未加入真实认证/API/远控；既有成功构建 run `40261229408` 是旧版记录。V3 为计划版本，非新 APK 发布。
- `SCOPE`：R8、混淆、非必要安全加固仍延期；真实远控后续在授权、确认与逐项验证下实施。
