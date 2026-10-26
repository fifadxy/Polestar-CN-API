# PsAssistant for Android

当前版本：**v0.6.1-beta1**（versionCode 61）。Android 主开发分支：`android-polestar-assistant-main`；包名继续使用 `io.github.polestarcn.companion`，保留更新兼容所需的应用身份。

Android 应用统一使用用户指定的黑武士车头图标。车辆略微缩小，保留纵横比，并为自适应桌面遮罩留出安全区；圆形图标和应用页眉复用同一资源。后续 Android 版本沿用 [图标与使用约定](branding/android/README.md)。

支持中国区手机号/密码登录。首次使用需要导入已验证的原模板 APK，或在本机安装匹配样本；兼容参数与会话使用 Android Keystore 加密保存，密码不保存。Global 账号登录尚未实现。

## 当前功能

- 默认一页车辆控制首页；电量、续航及最多 9 项自选数据显示，四项控制无需滑动。
- 修正总里程响应层级；显示 Last Trip / TA、Since Reset / TM 的距离、AVG Speed、Average Consumption（实际响应未提供的字段为 --）。
- 四门、四车窗独立状态，空调当前/目标温度、AQI、PM2.5，以及充电连接、类型、kW、A、充满时间、充电电源状态。
- 控制使用强生物识别；车辆确认后的开启/未锁状态为橙色，关闭/已锁为白色，发送及未确认另行提示。
- 露营模式保留 1–24 小时托管和可选空气净化。默认 31 分钟检查、10% 电量下限；可调 20–60 分钟及 5%–90%。启用前读取电量；运行中发现低于下限便结束托管并尝试停止受托管功能。
- Widget 2×1、4×1、2×2、4×2 及 Data 4×4，分别最多 1/2/2/4/8 项自选数据；保留门锁与空调入口、独立中英文、不透明/透明与模拟磨砂设置。
- 可选静默常驻通知显示基础车况与数据更新时间；独立的露营检查不依赖开启常驻通知。
- 主程序中英文、日夜模式；设置内提供圆屏/方屏手表布局预览。当前 APK 是手机应用，未宣称已完成独立手表应用或 OPPO Watch X2 实机适配。

露营检查受 Android 后台限制、网络和车辆服务影响，并非车辆本地恒温功能。状态未知时不盲目启动，超时写请求不自动重发，停止未确认时明确显示结果。

## 文档

- [本轮字段核对、使用方式、验证边界与待确认优化方案](docs/Psa_v060_Parity_and_Review.md)
- [Psa VI/UI Design Specification v5](docs/Psa_VI_UI_Design_Specification_EN_v5_Optimized.md)
- [初次导入、指纹与通知说明](docs/ANDROID_V04.md)
- [知识库历史索引](knowledge/INDEX.md)（历史阶段状态以各文件日期为准）
- [China / Global 接口来源比较](docs/HISINGEN_GLOBAL_CN_API_COMPARISON.md)

## 构建与验证

JDK 17、Gradle 8.7、Android SDK 35；minSdk 26、targetSdk 34。

```bash
cd android-app
gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

输出为 `android-app/app/build/outputs/apk/debug/app-debug.apk`。CI 上传同版本名称的 APK artifact；debug 签名在不同构建环境可能不同，正式更新需固定签名。

仓库根目录可运行：`python3 scripts/verify_source.py`、`python3 scripts/check_knowledge.py`、`sh scripts/test_models.sh`。测试包括协议字段、空值与单位、缓存迁移、露营阈值/过期/取消/不重复写入、手机一屏布局和 Widget 控制触区。

构建和合成样本测试不代替用户账号/车辆/手表实测。Mac 分支及默认 main 不受本 Android 分支修改影响；本项目是独立的非官方客户端。
