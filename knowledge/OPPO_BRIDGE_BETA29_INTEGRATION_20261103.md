# PsAssistant beta29：合并 OPPO Bridge 1.15

日期：2026-11-03。主APK升至 `0.9.7-beta29 /143`，整合Bridge为 `1.15.0 /27`。实现以此前独立模块1.15.0为基础；现可从主APK的“OPPO互联/高级诊断”打开统一页面。

## 主程序改动

- `BridgeDiagnosticsActivity`保留总开关、MyDevices、负一屏、Watch/钱包探测、4×2直控作用面开关。
- 提供一键全路径只读检测、模块内部模拟链路、一键修复并Root重载、统一诊断与导出。
- 新增集成版设置Provider `io.github.polestarcn.companion.bridge-settings`，让LSPosed宿主读取同一套开关。
- 所有Hook回报统一使用Bridge 1.15.0，检测器会把旧版本回报标为不匹配。
- 保留精确MyDevices 17.4.15 RPK SHA门禁、4×2快捷控制和本地 `POLESTAR / Polestar 极星`目录注入。
- 主APK版本升至143，Release仍要求既有客户端配置与固定签名身份。

## 设置和测试

推荐只安装主APK并在LSPosed启用内置模块。旧独立Bridge应停用，避免同一宿主重复加载。总开关、四个作用面和一键修复统一使用主APK私有设置，Hook宿主通过设置Provider只读获取。

模拟测试使用模块内临时车辆状态，复用卡片序列化与四个车辆命令映射函数，不调用命令提交接口，不写OPPO真实鉴权/数据库。全路径检测只读检查Root、包版本、Bridge状态、车辆快照、宿主Hook、RPK/Converter/快捷项、Provider、POLESTAR目录与Watch宿主。

## 数据导出

“诊断与导出”是唯一入口，可复制统一文字摘要或导出同源轻量ZIP。摘要上限约60KB，ZIP硬限制小于10MB。敏感键过滤Token、密码、VIN、AUTH_CODE、Secret、Cookie、Session ID；CarLink数据库只读导出文件大小与SQLite schema，不读表行、不修改数据库。

## 验收边界

本地编译、自动测试和APK结构检查不证明ColorOS真机已经显示入口或执行车辆命令。真机验收必须确认绑定目录出现Polestar、负一屏快捷项可见、按钮进入主APK命令队列，并以车辆新状态回读或现场现象证明执行。`lastCommandSent`仅表示进入应用队列。


## beta29 构建验收（2026-11-03）

- 自动验证：`:app:testDebugUnitTest`, `:oppoBridge:testDebugUnitTest`, 两模块 `lintDebug`、主APK与独立模块 `assembleDebug` 均通过；随后 `:app:assembleRelease :app:assemblePreview` 通过。
- FULL APK身份：`io.github.polestarcn.companion`，`0.9.7-beta29-standalone` / versionCode 143；固定签名证书与beta25 FULL相同。
- FULL APK大小：6,679,549 bytes（6.37 MiB）；SHA-256：`caef548dbb85d4c0e41f223f8309411e49809569df85527d2362de61240e62e4`。
- FULL APK通过 standalone-login资源检查、`aapt`版本清单检查、`apksigner verify`签名验证。
- SDK/R8输出有OPPO AAR stack-map兼容警告，但构建与lint完成；需真机验证`BridgeSettingsProvider`在MyDevices、ColorOS助手屏、CarLink宿主进程的跨进程读取及模块启用后功能。
