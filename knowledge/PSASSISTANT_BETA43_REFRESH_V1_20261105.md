# PsAssistant v1.0.0-beta43 — 统一刷新调度与数据有效期

- 日期：2026-11-05
- 源码：`feature/psassistant-beta43-unified-refresh-20261105`，发布目标 `android-polestar-assistant-main`
- 主程序：`versionName=1.0.0-beta43`, `versionCode=157`
- 目标：共享一次云端读取、减少不必要的手机网络工作和车辆端状态查询。此优化不等于已测得车辆12V省电，仍需实车确认。
- 所有参数均是**可申请新读取的最短间隔/TTL**，不是云端上报频率、不是系统保证准时执行的定时任务。系统后台/Doze可能延迟。

## 1. 七组已确认默认参数

| 场景/领域 | 前台 | 后台 | 细则 |
|---|---|---|---|
| CORE SOC/续航/车锁/门窗/尾箱/空调初始识别 | 10分钟 | 有消费者60分钟 | CORE触发时检查电池、外观、空调、净化；不重复检查尚未到期的健康/保养/里程 |
| 从后台返回前台 | 上次成功读取超过10分钟，延迟2秒判定 | — | 后台刚成功读取可以复用；不同数据组独立TTL，不能因电池新鲜压制空调到期检查 |
| 停车位置 | 智能15/30/60分钟；恢复前台独立阈值10分钟 | 关闭 | 仅车辆停车位置需要显示时申请；c3Lbs坐标，高德地图/地名；用户手动强制可绕过缓存 |
| 空调运行中 CLIMATE | 2分钟 | 15分钟 | 定向读取，不触发所有其它状态RPC |
| 净化 AIR | 事件驱动 | 不额外周期读取 | 启停后仍保留远控确认；倒计时本地计算；运行到期且前台需要时最多追加一次定向读取 |
| 充电 CHARGING | 5分钟 | 30分钟 | 电池及充电信息；停止充电恢复CORE周期 |
| 健康 HEALTH（胎压、12V、告警） | 30分钟 | 120分钟 | Health服务字段、异常警告；安全告警并非实时保障 |
| 保养 MAINTENANCE | 120分钟（2小时） | 720分钟（12小时） | 与Health来自同一次RPC，任一到期时一起读取，分别显示上报时间 |
| 里程/行程/能耗 MILEAGE | 10分钟 | 60分钟 | Mileage域独立TTL，避免每次CLIMATE读取顺带请求 |
| 无有效消费者的后台空闲 | — | 120分钟 | 默认保留最低频读取；手动长停省电可暂停所有普通自动读取 |
| 其它普通读取同时发生 | 2秒去重窗口 | 2秒 | 受控VIN相同且scope重叠合并；已在飞行的请求不再重复发起 |
| Widget / 通知栏 / OPPO | 缓存变动时重新展示 | 缓存变动时重新展示 | 本地倒计时可按分钟显示；不因RemoteViews重绘再注册网络定时任务 |
| 手机低电量/Doze | 遵循Android系统机制 | 遵循Android系统机制 | 不绕过系统强制电源策略 |

注：`refresh_air_policy` 在设置中可以改成周期模式，用户确认默认仍然是 event。手动刷新和 P0 远控结果确认不受普通TTL限制。

## 2. 源码设计与入口

- `RefreshPolicy.java`：默认间隔与后备合法范围；新增`RefreshDomain.java`（CORE、CLIMATE、AIR、CHARGING、HEALTH、MAINTENANCE、MILEAGE）。
- `VehicleRefreshManager.java`：管理普通读取合并、失败30秒退避、每VIN分组时间、正常窗口、简单诊断计数；可从Activity/JobService/OPPO调用。
- `RefreshPlanner.java`：纯逻辑TTL校验，使用成功时间与时间回拨防护；`RefreshDisplay.java`：视觉字段指纹决定是否需要刷新Widget。
- `ChinaPolestarApi.readVehicleScoped()`：电池、电池目标SOC、外观、里程、Health、空调及净化按需分组调用；CORE仍包含空调和净化以发现来自其他客户端的状态改变。复用旧领域数据时保留原始上报时间与来源含义。
- `VehicleCoordinator.refreshScoped()`：串行普通网络更新，完成后保存原VehicleSnapshotStore、向Widget/通知栏/OPPO变更通知。与`refreshUser()`手动完整刷新、`CommandReadback`确认、`CampEngine`托管严格隔离。
- `MainActivity.onResume/onPause()`：普通车况10分钟阈值 + 2秒检查；页面切换不触发网络；周期与完成时间有关，用户可配置。
- `VehicleCoordinator.readLastParkedLocationOnResume()`：停车坐标单独阈值与失败缓存，按VIN核对，复用c3Lbs而非普通车况；`ParkingRefreshJob`默认禁用。
- `VehicleRefreshJob`/`ResidentService`：后台共享一份定时计划，非消费者但不在长停模式时保留120分钟间隔；Doze由系统调度。
- `WidgetRenderer`：移除每次RemoteViews渲染时重新调度JobScheduler的副作用；各WidgetProvider只在enabled/disabled时更新后台任务。
- `PsAssistantBridgeProvider`、`OppoCarLinkRuntime`：一般状态刷新采用统一管理器；OPPO CAR控制指令仍由原安全链路验证；官方授权状态不能由缓存伪造。
- `MainActivity.refreshRulesDialog`：设置内分组编辑全部TTL、停车显示策略、单独设置恢复默认、请求统计。
- `UpdateHistoryActivity`、`android-app/app/build.gradle`：新增beta43记录及versionCode 157。

## 3. 安全例外和禁止行为

1. P0远控：门锁、车窗、尾箱、空调等必须在已发送命令后取得新的真实结果；旧缓存/普通队列不能宣告控制成功。保留既有认证、确认与超时行为。
2. 露营托管：运行期间低SOC保护、超时停止、未确认远程STOP持久状态、网络重试均保持独立；不可仅凭普通缓存宣告已停止。
3. 用户明确手动刷新：仍调用原`VehicleCoordinator.refreshUser()`完整路径，并使用服务器原始时间戳，不把缓存读入时间当做车辆上报时间。
4. VIN隔离：时间统计持久化使用SHA-256短摘要键，不将原VIN、token、登录凭据写入刷新统计文件。账号变更/旧请求完成必须验证当前车辆。
5. 限制：gRPC调用仍是独立网络请求，并非所谓一次服务器调用返回全部车辆状态；真实唤醒、夜间12V耗电、省电幅度尚未实测，不能做承诺。

## 4. 验收清单

- 5分钟反复前后台切换，没有到期请求不重复查询；超过CORE 10分钟后延迟2秒检查。
- 托管运行中所有安全停止/待核实路径不受普通刷新阈值阻断。
- Widget/OPPO/通知栏同VIN同分组在两秒窗口内合并；分页、皮肤选择只触发本地重绘。
- 空调运行前台2分钟时只请求所需CLIMATE服务，不顺带请求未到期保养/胎压/里程。
- AIR启停回读来自真实车端，倒计时本地；预计结束时有有效期闸门的前台检查。
- CHARGING前台5/后台30、HEALTH前台30/后台120、MAINTENANCE前台120/后台720、MILEAGE前台10/后台60。
- 停车位置仅在需显示且超过10分钟resume TTL时请求，连续显示15/30/60分钟回退，后台默认无独立请求。
- 测试各皮肤中英文、高缩放小屏；完整编译、lint与Android/Bridge单元测试；OPPO真机CAR状态与车控独立验收。
- 记录源级别失败、退避、缓存命中、并发合并和调用次数；与Beta42在相同使用条件下对比，不凭模拟推断车端电耗。

## 5. 构建/安装边界

GitHub Actions `.github/workflows/android-beta43-refresh.yml`会分别编译/上传`PsAssistant-v1.0.0-beta43-VALIDATION-debug.apk`和`PsAssistant-OPPO-Bridge-beta43-debug.apk`，之后单独执行Android和Bridge回归测试。调试包缺私有中国区登录签名配置或认证输入时仅作VALIDATION，不当作可完整登录的FULL独立签名版。生产更新需要与已有安装包同一签名、已获准的配置输入以及实机回归。

**从Beta42开始的已知回归基线：** 若旧测试要求极简模式显示开启中的计时文字、或要求默认5/30分钟/旧地图缓存周期，应按用户已确认的新功能要求更新测试；尺寸裁切必须修复真实布局而不是掩盖失败。
