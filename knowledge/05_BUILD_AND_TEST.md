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

## 2026-10-26 构建验收：dev2 已生成可下载 APK

- GitHub CI：`https://github.com/fifadxy/Polestar-CN-API/actions/runs/40261229408`，针对 commit `5f3afc415acfe4ffc2d33fd8613511e51019b7fb`。
- 结果：**SUCCESS**；纯 Java 测试、知识库一致性检查、Android API 35 初始化、Gradle `:app:assembleDebug`、`Check output`、`upload-artifact` 全部通过。
- 构建产物：`polestar-android-companion-v0.1.0-dev2-debug`（GitHub Actions Artifact），解包后 `app-debug.apk` 大小 **28,951 字节**；SHA-256 `0e8d417cd6354ecf55ac414492a466ce10d02778e40eb59aa850bdbac0767a40`。
- APK ZIP 可读，含 `classes.dex`、`classes2.dex`，二次核查证实后者含 `MainActivity`, `TelemetryStatus`, `OperationState`。APK v2 签名与内容摘要可独立复核通过；证书主体 `CN=Android Debug`，并非参考原 APK 的发布证书。
- 这是 **debug APK / 离线 DEMO**；目前没有模拟器启动测试、真实登录、C3 状态读取或实车远控成功证据，不能将“编译通过”写成“功能回归全部通过”。
- 新工程包名不同于原 APK，可并行安装。不同 CI 运行环境产生的 Debug 证书可能变化；如果覆盖安装提示签名冲突，需要采用稳定的本地调试签名或卸载旧版后安装（卸载会删除本地测试数据）。


## 2026-10-27 v0.7.8 当前增量

v0.7.8 Debug/Preview 各 123 项回归，四套皮肤中英日夜、小屏/横屏、合成倍率 1.56、12 项字段、保养条和 48dp Widget 触区。构建输出与签名见更新说明；合成通过不能代表车辆或高德在线成功。 具体来源、字段和限制见 [v0.7.8 车况、位置与行程](25_V078_CONDITION_LOCATION_TRIP.md)。


## 2026-10-27 v0.7.9 免 Key 地图增量

当前位置方案改为用户明确选择的内嵌高德官方 URI 网页，移除 staticmap Key 依赖并清理旧配置。参考 APK 仍证明 field1=经度、field2=纬度，未采用外部 AI 文本中的反序定义。c3Lbs 4秒截止、失败隔离及切车/退出结果丢弃已补强；网页需网络和系统 WebView，手机底图/坐标精度仍待实车验收。见 [v0.7.9 知识库](26_V079_KEYLESS_PARKING_WEB.md)。

本地 v0.7.9 最终验证：Debug/Preview 各140项测试，0失败/0跳过；两种Lint无错误，Release/Preview签名有效、debuggable=false。APK校验值见 docs/Psa_v079_Update.md。


## 2026-10-27 v0.8.3 当前增量

基于最新v0.8.2/9744759，恢复PS默认并保留VoLike新布局；自动保养只用Health剩余值，历史手动记录不再补充显示。地图共用精简网页，并新增默认关闭的手机前台位置授权；无后台定位。所有Widget改用原生文字/按钮与等比车图，新增PSeu、自选数据、统一粗体/字号及精确尺寸适配。按钮资源和后续迭代参考Psa图标规范v1。协议/实车证据标签不因此升级。详见 [v0.8.3统一知识](28_V083_UI_WIDGET_MAP_ICONS.md)。


## v0.8.4 当前增量 · 2026-10-27

[KB29](29_V084_ICONS_SERVICE_TARGETSOC_READBACK.md)与[更新说明](../docs/Psa_v084_Update.md)记录新母版/昼夜通知、30000km与合并保养周期、C3 TargetSoC只读字段、首次协议、5/10/15/20秒四次回读和非阻塞等待。历史20000km、单次15秒回读与黑武士启动器约定被本轮授权替代。实车TargetSoC/平台桌面昼夜刷新仍待验收。


## v0.8.5 当前更新 · 2026-10-27

[KB30](30_V085_INLINE_CONDITION_STANDALONE.md)记录统一车况入口、页内展开、已报告API优先、设置7组与移除运行时模板依赖。[独立构建](../docs/Psa_Standalone_Build.md)区分完整私有签名包和无配置的公开CI验证包；不把客户端配置值写入公开源码。旧导入流程不再是用户安装步骤。真实账号/实车仍待设备验收。


## v0.8.6 当前增量

参见[KB31](31_V086_UI_WIDGET_REFRESH_POLICIES.md)和[更新说明](../docs/Psa_v086_Update.md)：VoLike合并切换、车况/反馈/字体配置、拖动排序、5×1与自选Widget按钮、Health状态选择、独立刷新策略。车辆c3Lbs与手机定位分开；手机定位只在二级预览按需获取。运行时间h/m无~，计算来源未改变。此前与上述行为不同的说明作为历史记录。完整发行包本地编译签名，无需其他APK。


## v0.8.7 当前增量 · 2026-10-28

v087新增原生像素/文字边界、全皮肤模式图标及弹窗、动画暂停/省电、目标温度fixed32与非法参数无网络/拒绝不重试测试。完整签名包本地构建私有交付，CI无私有配置仅开发验证。

本地Debug/Preview各221项全部通过，Lint各0错误/140警告；源码/KB/Java模型/地图JS检查通过，Release/Preview保留证书签名构建成功。


## v0.8.8 当前增量 · 2026-10-28

v088增加Android8/15稳定alias、alpha安全区、通知与选择持久化、Widget全风格/尺寸/大字无省略号与边界、报告计时/fallback及露营字体检查。完整包本地签名私有交付。

## 本地构建验收

- Debug/Preview各237项测试，0失败/错误/跳过；Lint各0错误、147警告。源码/知识库/Java模型/地图JS检查全部通过。
- Android8/15切换、九个组合唯一入口、保留MainActivity、日夜/透明alpha/中央安全区、通知与选择一致性通过。Widget覆盖六类尺寸/五风格、中英、字体1/1.3/1.56、Widget120%和粗体；新增检查行宽/行高及无省略号，另测窄条/充电值。
- 完整Release/Preview沿用固定证书SHA-256：`cc87b578976ba45bc06219d66a9cce8a771de457785b2af34be76aeaa3768c94`；不新增SDK/native库、后台轮询或写重试。真实车辆与OEM桌面仍需设备验收。
