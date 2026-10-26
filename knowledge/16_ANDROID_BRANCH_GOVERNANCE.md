# 16｜Android 项目主线、分支职责与复刻开发管理

## 1. 分支命名决议（2026-10-26）

| 分支 | 职责 | 处理 |
|---|---|---|
| **`android-polestar-assistant-main`** | Android 极星助手复刻**长期主开发分支**，承载 V3.0 计划、知识库和未来经验证功能 | **本轮建立；优先进入此分支** |
| `feature/android-companion-v0.1` | v0.1.0-dev2 的原生 DEMO/Widget 可追溯基线 | 保留不删、不重写历史 |
| `android-polestar-china-api` | 既有中国区项目历史分支 | 保留，用于比较，不作为当前 Android 复刻主线 |
| `main` | GitHub 仓库原默认主分支，亦可能供其他项目使用 | **保留不修改**，避免影响 Mac/Hisingen |
| `chatgpt/hisingen-*`, `integrate/hisingen-*` | 独立 Mac/Hisingen 开发分支 | 不触碰、不合并 Android 实验性状态 |

这里的“Android 主分支”是 Android 子项目的长期开发主线，**并非把整个 GitHub 仓库的 default branch 从 `main` 改走**。

## 2. 后续开发工作流

1. Android 功能工作建议从 `android-polestar-assistant-main` 派生短期分支，如 `feature/android-assistant-auth`、`feature/android-assistant-c3-read`、`feature/android-assistant-remote-control`、`feature/android-assistant-ui-widget`。
2. 在对应阶段完成静态、单测、CI 与设备验证后，合入 Android 主线；真实车控操作必须单项授权实测后才能标 `PROJECT_DEVICE_VERIFIED`。
3. 每次功能变更同一提交同步 `03_API_RESEARCH.md`、`04_FEATURE_MATRIX.md`、`07_PROGRESS.md`、`08_ROADMAP.md`、`12_STAGED_VALIDATION.md` 及需要的参考文档。
4. GitHub Actions 监听 Android 主线和历史 Android 试验分支；文档本身的计划同步并不等于成功构建新 APK。
5. 禁止把 Android 功能分支直接当作 Mac/Hisingen China 当前应用，避免彼此 Token、存储、构建和 UI 逻辑混合。

## 3. 状态标签

- `APK_CONFIRMED`：静态 APK 二进制已验证存在。
- `REFERENCE_USER_VERIFIED`：用户实际测试参考 APK，说明参考程序可用；**不是新程序的实测结果**。
- `EXTERNAL_ANALYSIS`：第三方 AI 报告判断，独立核对前不提升等级。
- `PLANNED` / `MAPPING_REQUIRED`：复刻任务尚未实现，API/Proto/认证链未完成。
- `IMPLEMENTED_DEMO`：新工程有 DEMO 功能/模型，不能宣称车云服务可用。
- `CI_VERIFIED`：源码已按指定 commit 编译成功，不能替代设备测试。
- `PROJECT_DEVICE_VERIFIED`：新工程用授权车辆和真实账号形成脱敏的端到端运行证据。
- `DEFERRED`：R8、非必要安全增强、发布优化已明确延期；不把核心远控功能错误地归类为永久延期。

## 4. 版本和发布规则

- 初始代码基线：`v0.1.0-dev2`；编译成功的历史 Run `40261229408`，新主线初建时不会修改这个 APK 的可用功能。
- 复刻开发规划标签：`V3.0` 指**产品开发计划版本**，不是 Android 二进制 `versionName`，不能据此宣称当前已构建 v3.0 APK。
- 建议 Android 下一个代码里程碑 `v0.2.0-alpha1`，以真实提交与构建记录为准；验证通过后独立递增版本。
- 完整交付至少包含源码 commit、CI run、APK SHA256、功能矩阵、设备测试报告和已知问题。

## 5. 完成定义

“完成复刻”不是全部按钮可见，而是用户已明确验证的登录、SOC、里程、空调、解锁在新程序中可重复执行，其他操作按参考 APK 证据逐项核查；UI/Widget 正常，多车和双区域会话隔离，Token 错误不会错误覆盖远控结果；所有未实测项继续醒目标记未验证。
