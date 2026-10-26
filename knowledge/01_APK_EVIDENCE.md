# 01｜基础 APK 静态证据

## 样本指纹（APK_CONFIRMED）

| 项目 | 结果 |
|---|---|
| 样本 | `app-release.apk.1.1(1).1` |
| SHA256 | `8cae07a3b5c6050487443914ebb55b1d2f2f26e258f4b369837f2b88de052735` |
| 字节数 | `4,856,483`（4.63 MiB） |
| ZIP 条目 | 526 |
| Package | `app.c3garage.companion` |
| versionName / code | `0.1.0` / `1` |
| minSdk / targetSdk / compileSdk | 26 / 34 / 35 |
| AGP（APK 元数据） | 8.5.2 |
| Dex | `classes.dex` + `classes2.dex` |
| 编译后业务类 | `app.c3garage.companion` 命名空间下 96 类（包含合成类与 R） |
| 两份 DEX 类型数 | 7899 + 1218 = 9117 个类 |

## 可定位核心业务类（APK_CONFIRMED）

- `MainActivity` — Java 动态 UI、登录/车况/控制卡片、示意图与状态刷新。
- `PolestarApi` — 登录、发现、状态、控制及 session 辅助功能。
- `PolestarWidgetProvider` — 小组件渲染和更新。
- `WidgetConfigureActivity` — 小组件车辆选择界面。
- `WidgetControlActivity` — 小组件控制流程入口。
- `WidgetRefreshWorker` — WorkManager 小组件同步。
- `CampModeWorker` — WorkManager 露营模式任务。

**勘误**：共 **7 个**核心命名源文件，并非另一 AI 文本所称“仅 6 个”。

## 权限与组件（APK_CONFIRMED）

Manifest 可见：INTERNET、RECEIVE_BOOT_COMPLETED、USE_BIOMETRIC、USE_FINGERPRINT、WAKE_LOCK、ACCESS_NETWORK_STATE、FOREGROUND_SERVICE，以及应用自定义动态广播权限。主 Activity、小组件 Provider 和配置 Activity 导出；`WidgetControlActivity` 非导出。已有分析文本中的完整 AXML 可作为辅助材料；对可疑导出逻辑的动态风险推断不在本阶段核验。

## 通信证据（APK_CONFIRMED）

从 `classes.dex` 字符串池独立确认：

- `https://gw.app.polestar-auto.com`
- `https://cnepmob.volvocars.com.cn`
- `/user/api/v2/oauth/login/password`
- `/user/api/v2/oauth/refresh-global-token`
- `services.vehiclestates.battery.BatteryService`
- `services.vehiclestates.exterior.ExteriorService`
- `services.vehiclestates.odometer.OdometerService`
- `services.vehiclestates.parkingclimatization.ParkingClimatizationService`
- `services.vehiclestates.precleaning.PreCleaningService`

以下类方法/字段存在：`PolestarApi.login/refreshIfNeeded/discoverAndRead/stateRpc/ensureC3Host/control`, `Session.access/refresh/c3Host/expiresAt`, `Vehicle.battery/range/odometerKm/doorStates/windowStates`, `MainActivity.authenticateControl`, `PolestarWidgetProvider.render`, `WidgetRefreshWorker.doWork`, `CampModeWorker.doWork`。

存在服务/方法名 **≠** 登录成功或车辆支持同一调用。未来需读方法实现、请求 JSON/Proto、错误码、Token audience 并做有授权的真机验证。

## 未确认内容

- 具体密钥盐值及密钥生成细节：**不入库**。
- 原 APK 原始源码及真实 Gradle 配置：没有提供，不得根据 APK 伪造。
- APK 签名者身份、设备兼容/可靠性、接口是否在线：未在当前阶段验证。
- `PolestarApi.control` 实际可用性、不同车型/地区对应权限：未验证。
