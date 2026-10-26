# Polestar Assistant for Android · China / Global

**Android 长期主开发分支**：`android-polestar-assistant-main`  
**总开发计划**：`Android Parity V3.0`（**计划版本号，并非已编译 APK 版本号**）  
**当前构建**：`v0.2.0-alpha1`，独立原生 Android Java 工程；主界面/Widget 仍保留 DEMO，但已加入 **China 实验实车链路**。

## 本次复刻计划的实测基础

用户已在第三方参考 APK `app-release.apk.1.1(1).1` (`app.c3garage.companion`, v0.1.0) 上实际使用 **中国区账号登录、SOC 电量读取、累计里程读取、空调远程控制和车门解锁**。用户另报告：长期使用可能弹出 Token 错误，但实际功能仍可能可用。这些是 `REFERENCE_USER_VERIFIED` 的**参考 APP**行为，**不能说明本独立 Android 工程已经实现**。

本工程目标是完整分析参考 APK 的 Manifest/DEX、UI、资源、C3/Proto、认证/会话、远控、Widget/后台机制，以独立、可维护源码复刻功能；同时解决 Token 误报归因并增强 China/Global 分区。**不直接将反编译源码拼接生产、不复制第三方私有密钥或用户凭证。**

## 推荐阅读

1. [V3 总开发计划](knowledge/14_ANDROID_PARITY_V3_MASTER_PLAN.md)：P0–P7 任务、架构、验收与交付。
2. [参考 APK 实测与证据](knowledge/13_REFERENCE_APK_FIELD_VALIDATION.md)：已由用户测试的能力和未证实项。
3. [Token / 远控验证](knowledge/15_TOKEN_AND_REMOTE_VALIDATION.md)：请求与状态独立分类、误报、超时和逐项远控验收。
4. [功能矩阵](knowledge/04_FEATURE_MATRIX.md)：参考 APK vs 新工程的真实实现状态。
5. [Android 分支管理](knowledge/16_ANDROID_BRANCH_GOVERNANCE.md)：长期主线、历史分支与 Mac 分支隔离。
6. [知识库完整索引](knowledge/INDEX.md)：全部文件、APK 字典和实测证据。

## 当前实际代码与构建状态

- Native Java / Android SDK：AGP 8.5.2、Gradle 8.7、JDK 17、compileSdk35、minSdk26、targetSdk34。
- 已有基础 UI：首次 China/Global 选择、车辆总览、演示 SOC/里程/续航、车辆状态示意、标准/紧凑 Widget、日志复制、DEMO/CACHED/LIVE 状态语义与命令状态模型。
- `v0.2.0-alpha1` 新增 China 实验链路：手机号+密码登录、Keystore 加密 Session、Token refresh、C3 Host/车辆发现、Battery/Odometer/Exterior 原始 Protobuf 读取、空调启动/停止与车门解锁手动确认调用。
- 兼容签名材料**不进入 Git**：仅在设备端检测已安装的参考 APK，且 SHA-256 必须匹配已验证样本，再从该 APK 运行时读取；不写日志和持久存储。
- 主界面/Widget 仍是 DEMO；Global、C3 Live、PCCS、完整 Exterior 字段、其他远控、命令车辆侧确认和原版 UI 复刻仍待后续阶段。
- `v0.2.0-alpha1`：GitHub Actions [run 40265773414](https://github.com/fifadxy/Polestar-CN-API/actions/runs/40265773414) 已成功编译；Actions artifact `polestar-android-companion-v0.2.0-alpha1-debug`。**构建成功不等于实车链路已经由本项目验证。**

## 本地 / CI 构建

Android Studio 打开 `android-app`，或在具备 Android SDK 35 和 Gradle 8.7 环境的终端：

```bash
cd android-app
gradle --no-daemon :app:assembleDebug
```

产物：`android-app/app/build/outputs/apk/debug/app-debug.apk`。

静态与 JVM 测试：`python3 scripts/verify_source.py`、`python3 scripts/check_knowledge.py`、`sh scripts/test_models.sh`；新 Android 主线已加入 `.github/workflows/android-apk.yml` 的 push 监听。

## 分支和项目边界

- 此分支为 **Android 子项目主线**，并非修改整个仓库的默认 `main`；Mac/Hisingen 的 Native Swift 和其它历史分支保持原状。
- China/Global 需独立 Token、车辆列表和能力矩阵，不假设登录/认证/主机可互换。
- 真实远控须针对本人授权车辆、主动发起、逐功能验证；网络不确定时只读回读，禁止自动重复写入。
- R8、混淆和非必要安全加固继续 `DEFERRED`；基本凭证隔离、权限/用户确认、敏感日志脱敏属于功能正确性要求。
- 本项目是非官方技术研究应用，不代表 Polestar 官方客户端。
