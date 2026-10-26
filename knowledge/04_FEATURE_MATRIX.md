# 04｜Android 功能矩阵（原始参考 APK vs 新工程）

状态说明：`ORIGINAL_STATIC` = 原 APK 存在结构/方法但未进行运行验证；`DEMO_READY` = 新工程具备演示功能，仍待编译/设备验证；`TO_MAP` = 待完成后端接口映射；`BLOCKED` = 明确禁用。

| 功能 | 原 APK 可见线索 | v0.1.0-dev1 新工程 | 下一步 |
|---|---|---|---|
| 初次选 China / Global | 原 APK 中国区为主 | `DEMO_READY` 首次选择、设置可切换 | 实现地区专用登录 |
| 独立 Token / Refresh | `Session.access/refresh` 静态存在 | `TO_MAP` 保留独立存储命名空间，无 Token | 登录验证 |
| China 车辆发现 | `discoverAndRead` | `DEMO_READY` 示例车辆选择 | 服务端车型表 |
| SOC / Range | `Vehicle.battery/range` | `DEMO_READY` 固定模拟 72%/300km 或 81%/356km | 实车值 + 时间戳 |
| Odometer | `Vehicle.odometerKm` | `DEMO_READY` 演示值 | `GetLatestOdometer` 校验 |
| Exterior | `doorStates/windowStates/tailgate` | `DEMO_READY` 简洁车身图 | 逐字段校验 |
| Charging status | `Vehicle.charging` | `DEMO_READY` | China/Global 状态映射 |
| Climate / Air | `Vehicle.climate/air` | `TO_MAP` 状态组件待升级 | C3 climate/precleaning |
| C3 Live | gRPC 依赖及 `stateRpc` | `TO_MAP` 未接网络/stream | 断线重连方案 |
| PCCS | 原 APK 未从字符串中确认 | `TO_MAP` | 根据车型/地区确定 |
| 小组件标准 / 紧凑 | `PolestarWidgetProvider` | `DEMO_READY` 两种原生 Widget | 真机添加 / 尺寸验证 |
| 小组件自动刷新 | `WidgetRefreshWorker` | `TO_MAP` 目前仅 App 主动刷新和系统首次更新 | WorkManager 优化 |
| 露营模式任务 | `CampModeWorker` | `TO_MAP` 没有后台维护 | 仅确定读写边界后研究 |
| 远程车辆控制 | `control`, 生物识别流程 | `BLOCKED` 所有按钮只提示不下发指令 | 后期逐项单独授权验证 |
| UI 操作结果日志 | 源程序存在控制状态/反馈方法 | `DEMO_READY` 刷新、菜单、日志复制、禁用反馈 | 日志模块细分 |

区别：`DEMO_READY` 不等于已通过 Android SDK 编译或实机测试；编译/运行证据请查 05。
