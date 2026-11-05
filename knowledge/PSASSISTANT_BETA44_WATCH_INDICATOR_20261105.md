# Beta44 · OPPO 手表实验接入知识库（2026-11-05）

- [完整设计、代码、验收与限制](../docs/PSASSISTANT_BETA44_OPPO_WATCH_INDICATOR_20261105.md)
- OPPO 指示器官方文档：<https://open.oppomobile.com/documentation/page/info?id=11286>
- 官方条件：本机 `Notification`、`FLAG_ONGOING_EVENT`、`category=navigation/workout`、`extras.show_heytap_indicator=true`。媒体由对应媒体服务机制处理，不能视为普通车控类别。
- 主 APK 实验功能 **默认关闭**；手机侧 90 秒通知为协议字段模拟，手表端独立测试 APK 才用于表盘 Indicator 的实机兼容性检查。
- 正常车况/空调/充电通知不属于导航/运动；真实 `CATEGORY_SERVICE` 不改动。任何手机端成功发布不能当成手表已显示。
- 模块：`android-app/oppoWatchIndicator` + `OppoWatchIndicatorLab` + `MainActivity.settings_watch` + `ResidentService`，互不访问 ChinaPolestarApi 和 LSPosed/CarLink。
- 当前不包括 Wear Engine 双端传输；后续实现需要 SDK 正式接口和型号/系统真机日志。
