# 12｜研究成果移交至 Android v0.8.3-beta1 的工程交接

**资料研究线**：research/polestar2-digital-key-pak-20261027  
**实际 Android 集成线**：feature/android-digital-key-lab-v083-20261027  
**临时验收 PR**：https://github.com/fifadxy/Polestar-CN-API/pull/3 （Draft，不自动合并）

## 为什么需要第二个分支

研究分支创建于 Android 0.8.2-beta1；截至 2026-10-27，正式 Android 主分支已是 0.8.3-beta1，MainActivity、AndroidManifest、Gradle 以及若干知识文件发生更新。两条线已分叉；把整个旧分支合并过去可能破坏新版 UI 或定位权限。

为避免覆盖新主线代码，新建直接来自 `android-polestar-assistant-main` 的独立集成分支，只移植已经可测试的数字钥匙研究模块。

## 迁移的文件

```text
android-app/app/src/main/java/io/github/polestarcn/companion/
  digitalkey/
    DigitalKeyProvider.java
    DigitalKeyStatus.java
    DigitalKeyEnvironment.java
    ResearchDigitalKeyProvider.java
    DigitalKeyResearchActivity.java

android-app/app/src/main/AndroidManifest.xml
android-app/app/src/main/java/io/github/polestarcn/companion/MainActivity.java
android-app/app/src/test/java/io/github/polestarcn/companion/digitalkey/
  DigitalKeyContractTest.java

digital-key-research/scripts/run_android_ui_smoke.sh
.github/workflows/digital-key-v083-integration.yml
```

只对新版 `MainActivity.settings()` 添加“数字钥匙·环境检测（研究）”按钮。没有复制旧版 MainActivity，不修改 ChinaPolestarApi 的用户 token 逻辑，也不更改原有车辆远控接口。

## 安装隔离

在此研究分支的 **Debug** 配置中添加 `applicationIdSuffix '.digitalkeylab'`，构建后的 Android applicationId 为：

`io.github.polestarcn.companion.digitalkeylab`

正式主线原有包名仍是 `io.github.polestarcn.companion`。这样研究 APK 与已有安装应用的数据、密钥、Session 和签名空间相互独立。**Debug 版本不能用于升级正式主线 App**。

注意：如果 CI 仅显示 build 成功，不能声称在所有目标手机上安装通过；实际 Android14 Root AVD smoke 结果单独记录。

## 工程入口及验收

- `gradle -p android-app :app:assembleDebug`
- `gradle -p android-app :app:testDebugUnitTest --tests io.github.polestarcn.companion.digitalkey.DigitalKeyContractTest`
- `sh digital-key-research/scripts/run_android_ui_smoke.sh` 在 Android 14/ADB-root 环境里安装、启动并保存 UI 截图
- GitHub Actions workflow `Digital Key v0.8.3 Read-only Integration`，输出 APK 和 UI Smoke artifacts

状态：
- Android v0.8.3 构建和安全测试：已在 CI Run 40474095279 通过。
- Root Android 14 AVD 隔离包名安装、启动、UI 检查、截图：**已验证通过**（Run 40474095279，ADB root、安装 Success、当前前台 Activity、截图均得到证据）。
- OEM 新设备授权、证书签发、P319 真正配对：全部尚未启用、尚未验证。

## 下一阶段合入准入

**当前 Draft PR 不允许自动合并**。合入前至少必须：
1. 确认 Android 主分支没有新增冲突，进行 merge-commit/rebase 构建；保留 MainActivity 最新 UI 及定位权限。
2. 确认 Debug 研究包名隔离且不会覆盖手机上的正常 PsAssistant。
3. 完成 Android 12+ 权限拒绝/同意、后台、字体大小、安全提示、AVD UI 的回归。
4. 后续若要加入真实 OEM SDK，必须经厂商许可、新设备正常注册和实车配对的单独 Go/No-Go，不能因为本轮集成成功就声称钥匙移植成功。

**研究知识库与应用代码应持续分离：** 原静态分析、公开资料、厂商疑点始终更新研究分支的知识库；与最新主线适配的新代码只更新 v0.8.3 集成分支，等待验证后择机合并。
## 真实交付记录（2026-10-27）

- Workflow Run：`40474095279`（Android v0.8.3 分支，两个 Job 均成功）
- GitHub artifact：`psassistant-v083-digital-key-lab-debug` + `psassistant-v083-digital-key-lab-screenshot`
- APK 经过本轮 Debug 编译并通过 Android 14 Root AVD 安装测试，文件大小 `4801173` bytes。
- APK 原文件 SHA-256：`a2b111804ab1e4f733b02ae1ad340e3ce05b0b9e87b4edb27fab41b6cc811c27`
- 诊断截图 SHA-256：`7a8277c22b669453fb7c429b3f6a09c94f2cfef2fe8e5b42a369e9ab0586e062`
- Activity：`io.github.polestarcn.companion.digitalkeylab/io.github.polestarcn.companion.digitalkey.DigitalKeyResearchActivity`
- 屏幕观察：No Bluetooth CONNECT permission 时显示 UNKNOWN，不因未授权崩溃；OEM not connected、Vehicle not verified、Working key NO。按钮前后景对比度问题已经在集成 branch 处理。
- 设备端未进行任何车端连接、PAK 设备注册或真实钥匙认证。即使 Root AVD 运行通过，也**不能声称具有可用数字钥匙**。
