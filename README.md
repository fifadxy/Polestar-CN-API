# Polestar Android Companion · Experimental Branch

独立 Android 项目分支，隶属于 **Polestar2 Widget&API / Polestar-CN-API**。首版版本号：`0.1.0-dev2`（研究与 UI / Widget 骨架）。

**本工程不是上传 APK 的反编译源码；是根据 APK 的静态结构、用户提供的研究材料以及现有项目 China/Global API 对照资料重新实现的可维护 Android 工程。**

## 当前成果

- 原生 Java Android 工程，AGP `8.5.2`，`compileSdk 35`，`minSdk 26`，`targetSdk 34`。
- 首次启动 `Select Polestar Region`，China / Global 独立地区选择与切换。
- 车辆概览、SOC、续航、里程、外观开闭状态、车辆示意图。
- 两种桌面小组件（标准 / 紧凑），点击进入 App，区域切换时同步演示值。
- 车辆选择占位、日志复制、模拟刷新反馈、命令禁用反馈。
- **全部车辆数据均为 DEMO / OFFLINE；没有执行真实账户登录或车辆远程命令。**
- R8、安全加固、混淆均标为 `DEFERRED`，本阶段不开展。

## 构建

在 `android-app/` 目录，用 Android Studio 打开工程后使用菜单 **Build > Build APK(s)**，或者在具备 Android SDK 35、JDK 17 和 Gradle 8.7 的终端执行：

```bash
cd android-app
gradle --no-daemon :app:assembleDebug
```

输出：`android-app/app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions 工作流 `.github/workflows/android-apk.yml` 也会编译并上传 Debug APK 构建产物；构建是否成功以工作流记录为准。

## 双报告审阅 / 阶段成果（2026-10-26）

- 复核用户提供的两份外部 APK 报告，纠正参考 APK `Vehicle` 为 **33 个字段**（非 30 个）；原 APK v2 签名和摘要独立验证。
- 新增 `TelemetryStatus`（DEMO/LIVE/CACHED/STALE/UNKNOWN）和 `OperationState`（发送/接受/确认/未确认等语义），纯 Java 测试可运行；**没有接入真实车辆命令**。
- `scripts/test_models.sh`：无需 Android SDK，仅 JDK 17+ 即可执行语义测试。
- Gemini 分享链接当前无法直接读取正文；只吸收实际提供的两份文本。详细交叉审核见 `knowledge/10_TWO_REPORTS_RECONCILED.md`。
- 当前工程仍为 China/Global **离线 DEMO**，所有账号和远控功能尚未接通。R8、代码安全加固仍延期。

## 项目知识库

从 [knowledge/INDEX.md](knowledge/INDEX.md) 开始阅读，按证据、架构、接口、功能、构建、待办、后期安全优化分层维护。

## 版权与使用边界

应用名与源码独立，未分发原始 APK 的图片、凭证或私有签名常量。对于 China 登录 / C3 / PCCS，研究基于已提供证据推进；不绕过官方登录或权限校验。软件为非官方技术研究项目。
