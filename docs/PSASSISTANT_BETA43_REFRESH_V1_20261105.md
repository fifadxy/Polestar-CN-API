# PsAssistant v1.0.0-beta43 / 157 — 版本更新报告

2026-11-05 | Android 正线候选 | 统一刷新系统 V1.0

## 主要变化
1. **统一数据调度**：App、Widget、通知栏、OPPO Bridge共享普通远端刷新入口，按VIN、TTL与2秒请求合并管理，避免重复查询。
2. **核心车况**：前台10分钟；后台有消费者时60分钟；无消费者仍保留120分钟最低频率；返回前台距离上次成功读取10分钟内只用缓存，超时延迟2秒检查。
3. **运行场景**：空调开启前台2/后台15分钟；充电中前台5/后台30分钟；净化事件驱动、倒计时本地计算。
4. **慢数据**：健康/胎压/12V前台30后台120分钟；保养前台2小时后台12小时；里程/能耗前台10后台60分钟。
5. **停车位置**：c3Lbs独立于车况，智能15/30/60分钟；返回前台10分钟阈值，仅显示需要时读取；后台自动默认关闭。
6. **UI与设置**：「数据刷新管理」全参数独立修改、恢复默认、刷新统计诊断；Widget重绘不再重复注册后台Job。
7. **按需API**：新增中国区scoped gRPC读取，CORE仍能发现其他客户端的空调/净化状态变更；局部高频刷新不再携带全部低频接口。

## 交付和边界
- 核心车况远程读取及设置基于真实接口，不使用Mock伪造状态；OPPO SDK的授权/宿主进程状态必须在真机证明。
- 远控结果确认、手动强制刷新、露营托管安全停止与恢复保持原有独立安全实现。
- 普通刷新周期表示最小请求资格，并非系统保证的实时上报；Doze可以延后任务。云端查询是否唤醒车辆及12V耗电节省没有真车证明。
- GitHub Actions产物若标识`VALIDATION-debug`，不等于FULL中国区独立登录签名包。
- 技术证据和验收方法见 [Beta43统一刷新技术知识库](../knowledge/PSASSISTANT_BETA43_REFRESH_V1_20261105.md)。

## 代码变更路径
`android-app/app/src/main/java/io/github/polestarcn/companion/`：`RefreshPolicy`, `RefreshDomain`, `RefreshPlanner`, `VehicleRefreshManager`, `RefreshDisplay`, `VehicleCoordinator`, `ResidentService`, `VehicleRefreshJob`, `ParkingRefreshJob`, `MainActivity`, `WidgetRenderer`, `PsAssistantBridgeProvider`, `OppoCarLinkRuntime`; `api/ChinaPolestarApi`; Widget providers及相关测试。

版本: `versionName=1.0.0-beta43`, `versionCode=157`.
