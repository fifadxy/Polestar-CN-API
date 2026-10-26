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
