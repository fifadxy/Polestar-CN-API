# PsAssistant v1.0.0-beta44 /158 — OPPO Watch Indicator 实验接入

日期：2026-11-05 | 适用版本：Beta44 Android、独立 OPPO Watch Indicator 测试 APK v0.1.0

## 一、接入条件与事实边界

用户提供的 [OPPO 官方 Indicator 文档](https://open.oppomobile.com/documentation/page/info?id=11286)规定，手表端 `Notification` 需同时具备：

1. `setOngoing(true)`；
2. Category 为 `navigation`/`Notification.CATEGORY_NAVIGATION`，或 `workout`/`Notification.CATEGORY_WORKOUT`；
3. `Bundle.putBoolean("show_heytap_indicator", true)`，通过 `addExtras(bundle)` 加入通知；
4. 任务结束时移除通知；点按通知 `PendingIntent` 回到相应应用。

**重要：这是手表端系统解释其本机应用发出的通知的能力。手机端投递相同字段不能证明手表的表盘会出现 Indicator。** 本次按这一边界将“手机接口实验”与“真正手表端通知验证”分开构建。中国版 OPPO Watch X2 使用 ColorOS Watch 7.0，手表 App 的安装方式和不同固件的 Indicator 接收情况必须实机验证；不把所有 Wear OS 机型或所有 OPPO 产品假定为兼容。

车辆状态、充电、空调、露营并不属于导航或运动。正常车况服务即使开启可选的 `show_heytap_indicator` 额外标记，仍保持真实 `CATEGORY_SERVICE`；OPPO 手表可能忽略这一通知，不冒充官方支持的导航/运动服务。

## 二、已实现功能

| 功能 | 默认值 | 说明 |
|---|---|---|
| `设置 → OPPO 手表 · 功能与测试 → Indicator 指示器` | **关闭** | 不主动启用，重装或首次运行均默认关闭 |
| 手机端实验通知 | 手动执行，90 秒 | 导航/运动类别模拟可选择；通知有 ongoing、category、extra 和点击返回，自动取消 |
| 手动清除 | 支持 | 独立 Notification ID，不影响车况常驻通知 |
| 权限/通知通道诊断 | 支持 | 检查手机 POST_NOTIFICATIONS、通道、当前发布的 category / ongoing / extra |
| 常驻车况提示 | 关闭 | 可实验性附加 `show_heytap_indicator`，**服务类别仍为 SERVICE** |
| 独立手表 APK | `:oppoWatchIndicator` | 在手表上安装，在表端点击“导航/运动模拟”，发出本地 ongoing Notification，90 秒退出 |
| 远程车辆操作 | **无** | Watch Lab 不含账号、不含车辆网络服务、不读取 VIN 或 Token；不向手机发送指令 |

### 界面说明

手机设置提供：实验开关、是否给真实常驻车况 Notification 附加 extra、两种测试 category、90 秒模拟通知、取消按钮、条件诊断以及通知权限入口。

独立手表 App 采用黑色圆表友好简单界面。手表端有“导航 Indicator · 90 秒”“运动 Indicator · 90 秒”“取消测试”三种操作。点击测试后返回表盘，观察是否出现小图标、是否可点击返回 App，以及是否在 90 秒后消失。**此阶段不包含手机到手表的 Wear Engine 传输，也不包含真实手表控车**。

## 三、源码入口

- `android-app/app/src/main/java/io/github/polestarcn/companion/OppoWatchIndicatorLab.java`：手机端显示、权限、Notification、诊断，默认关闭。
- `android-app/app/src/main/java/io/github/polestarcn/companion/MainActivity.java`：设置页面和交互入口。
- `android-app/app/src/main/java/io/github/polestarcn/companion/ResidentService.java`：常驻车况的可选提示 extra，不修改真实 category。
- `android-app/oppoWatchIndicator/build.gradle`：独立手表应用的 Gradle 模块。
- `android-app/oppoWatchIndicator/src/main/java/io/github/polestarcn/watchindicator/IndicatorTestActivity.java`：手表端本地 Notification 测试。
- `android-app/oppoWatchIndicator/src/main/AndroidManifest.xml`：Watch 应用声明与通知权限。
- 两份 Robolectric 单元测试：`OppoWatchIndicatorLabTest.java`、`IndicatorTestActivityTest.java`。

## 四、编译、安装与验收

用 JDK 17 / Gradle 8.7 / Android SDK API 35，在 `android-app/` 目录：

```bash
gradle --no-daemon :app:assembleDebug :oppoWatchIndicator:assembleDebug :oppoBridge:assembleDebug
gradle --no-daemon :app:testDebugUnitTest :oppoWatchIndicator:testDebugUnitTest :oppoBridge:testDebugUnitTest
```

CI 工作流 `.github/workflows/android-beta44-watch-lab.yml` 产生独立的手机 Debug APK、手表 Debug APK 和 Bridge Debug APK。手机 Debug 包**不带中国区独立登录私有配置**；使用用户此前已批准的 FULL 资源和相同证书才能制作与原安装版兼容的独立登录 FULL 包。

在手表实际测试时：安装独立手表 APK → 打开测试 App → 授予通知权限 → 点击导航类别测试 → 返回表盘查看图标 → 轻点返回 App → 等 90 秒验证移除；再对运动类别重复。若不显示，用通知设置/系统版本/手表机型记录结果。手机端通知不会自动转发为表盘 Indicator。

**验收不包含**：官方 OPPO Wear Engine SDK 的手机手表配对、后台同步、手表直控车辆、CarLink 授权模拟成功。下一阶段如需手表车辆卡片或安全控车，先获取正式 Wear Engine SDK 与配对设备日志，使用带用户确认的授权会话和 P0 远控回读，不直接复用模拟类别通知。

## 五、耗电与安全设计

- 没有新的前台服务、常驻定时器、车端数据拉取或轮询。
- 测试通知在 90 秒后由系统 `setTimeoutAfter` 取消，可人工提前取消。
- 已存在的 ResidentService 通知按原有刷新策略工作。Indicator Extra 不导致车辆网络请求。
- 公开 GitHub 只记录源码和非私密报告；无账户配置、SDK 私有 AAR、签名私钥、Token。
