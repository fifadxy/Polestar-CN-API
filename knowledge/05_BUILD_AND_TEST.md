# 05｜构建、静态校验与设备验证

## 工程版本

- Java Android；AGP 8.5.2；Gradle 8.7；JDK 17。
- `compileSdk=35`；`targetSdk=34`；`minSdk=26`（按参考 APK 对齐）。
- `applicationId=io.github.polestarcn.companion`（与原 APK 不冲突）。
- Debug 安装包：`android-app/app/build/outputs/apk/debug/app-debug.apk`。
- `minifyEnabled=false`，`shrinkResources=false`；R8/安全加固 deferred。

## Mac / Android Studio

1. Android Studio 打开 `android-app`。
2. 使用 Android SDK Manager 安装 Android API 35、Build Tools 35.0.0、JDK 17。
3. `gradle --no-daemon :app:assembleDebug` 或 Android Studio 的 Build APK。
4. 安装前检查模拟器/手机 Android API >=26。

```bash
cd android-app
gradle --no-daemon :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n io.github.polestarcn.companion/.MainActivity
```

## 自动构建

GitHub Actions `.github/workflows/android-apk.yml` 为 Debug 构建工作流；触发后从 Artifacts 下载 APK。是否成功以实际 CI run 为准，不能因源文件存在就写 `BUILD_PASS`。

## 初版应验证

- 首次运行弹出 `Select Polestar Region`，分别选择 China/Global。
- App 设置中切换区域，UI 和桌面 Widget 演示值同步切换。
- 登录按钮明确提示未接入；没有真实网络或远控发出。
- SOC、续航、里程、外观示意图显示无遮挡；旋转/重开后地区保持。
- 添加标准、紧凑 Widget 后查看字体边距、尺寸变化、点击跳转。
- 点击刷新得到反馈；点击远控按钮仅显示禁用；复制诊断日志不包含 VIN、Token、密码等。

## 当前环境局限（2026-10-26）

当前交付环境内有 Java 21、Python，但未安装 Android SDK、Gradle、ADB，且容器无法 DNS 访问 Google/Maven 下载站，因此无法在此处本地执行 `assembleDebug`、`adb install`、模拟器验收。可以使用 GitHub Actions 的 SDK 环境或在用户 Mac 编译；以实际 CI 构建日志作为下一状态。

## 2026-10-26 CI 故障与修复记录

- 既往 CI `40260517435` 失败：`android-actions/setup-android@v3` 尝试安装过时 SDK 包 `tools`，未进入 Java 编译。
- 既往 CI `40260573372` 失败：去掉 action 后调用 `sdkmanager` 时未设置路径，命令未找到；**也未进入 Java 编译**。
- 本轮修改工作流：先执行 `python3 scripts/verify_source.py` 与 `sh scripts/test_models.sh`；从 `$ANDROID_HOME/cmdline-tools/` 定位 `sdkmanager`，在缺 API 35 或 Build Tools 35.0.0 时再安装。是否解决以更新后实际 CI 为准。
- 新增 `python3 scripts/inspect_vehicle_schema.py <apk>` 和 `python3 scripts/verify_apk_v2.py <apk>`，二者是参考 APK 的只读证据检查，不改变用户 APK。
- 原始 APK 应用 ID `app.c3garage.companion` 与新工程 `io.github.polestarcn.companion` 不同，新 APK **不能**作为原包同包升级安装；需独立安装。
