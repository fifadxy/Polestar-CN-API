# beta25 / 139：外观菜单、极简车控与 OPPO 路由纠错

日期：2026-11-02。承接 beta24 / 138，内置/独立 Bridge 同源版本 1.11.0 / 23。
本页更新优先于 beta24 中的 1.1 秒直切极简、运行中/未运行文字及历史路由判断。

## 用户可见行为

- 首页长按车图 **1.5 秒**打开外观二级菜单：切换皮肤、当前主题车图、白天车图、夜间车图、极简模式。短按原交互保留；拖动、取消触摸或离开页面不会触发长按。
- 极简模式在 PS、VoLike、NIOlike、简洁2中统一放大控制图标并居中；名称隐藏，关闭/已锁/已关闭的反馈隐藏，开启和等待确认时显示反馈。正常模式的反馈偏好保留。
- 控制名称统一为 **空调 / 净化**。空调反馈为关闭、开启；有剩余时间时直接显示 `15m`。保留状态颜色。
- 净化只有在本应用确认启动成功后才建立五分钟本地倒计时，反馈如 `5:00`、`4:59`；已有有效车辆剩余时间也可显示。外部启动但没有计时依据时显示开启，避免凭空补足五分钟。
- 计时按车辆隔离，退出账号时清除；到期显示待确认，不伪造关闭，不自动发停止指令。前台秒级显示只读缓存；不再通过 `onModelChanged()` 驱动显示计时，避免联动调度定位/露营检查。后台不运行此显示计时。

## 设置合并

主设置只保留 OPPO 互联入口；统一页面包含总开关、本地车辆入口开关、车辆面板、高级诊断、组件添加及轻量导出。移除重复的总开关入口及会被 IOT 权限阻止的直接官方绑定按钮。
本地绑定 facade 现在同时受桥接总开关约束；回报明确包含 `bindingScope=local-bridge`、`officialSdkAuthorized=false`。

## 为什么原来卡片可见但没有直接控制

| 层 | 本轮证据和问题 | beta25处理 / 边界 |
| --- | --- | --- |
| MyDevices 原生首页 | 原生 View 可替换，控制回到主程序 | 原有方案保留；不能等同于负一屏 QuickApp |
| MyDevices 17.4.15 的 4x2 RPK | 存在 CAR 组件和图标；实际 `eventSummary` 不含四个 CAR 事件，`smartHomeDefault` 只有 LAMP/AIR_CONDITIONER/CAMERA | 修复原 12KB 相邻文本搜索造成的假阳性，改为解析对应初始化数组/对象；不再因旁边 CAR 图标名称误判为可用控车路由 |
| AssistantScreen 17.14.60 原生 MyDevices 模型 | 原代码只改已有 PsAssistant 行；原生 Provider 查询未返回本项目行时，Hook 即使加载也没有行可改 | 按实机 APK 的20参数 DeviceInfo 构造器，在缺失时构建一条自有缓存车辆行，保留其他设备；对本项目行配置四个 SwitchMenu |
| Smart controls / 官方车辆目录 | 品牌目录、账号绑定 facade、实际 SDK 权限和事件分发是独立层 | beta24 品牌入口保留；本地入口不冒充官方 CarLink 授权 |
| Watch X2 长续航 | 手机侧包和静态资料没有提供手表 RTOS 端事件入口、分发回包和授权证据 | 仍待配对设备事件抓取及官方支持；不能用手机端“已绑定”字段宣称手表全控车已实现 |

重新检查用户提供的 MyDevices/AssistantScreen 完整 APK 与轻量资料；不把旧 Bridge 1.3.x 的历史回报当成 1.11.0 已运行。
AssistantScreen 原生模型分支与现有 `DeviceQuickAppCardProvider` 4x2 是两条路径。新增原生行仅是有静态构造器证据的实现，**不代表已将用户正在用的 QuickApp 卡片切换到该原生分支**。

## 精确原生契约

`DeviceInfoManager.c()` 返回 `ArrayList<DeviceInfo>`。DeviceInfo 构造参数依次为 MAC、ID、名称、DeviceType、ConnectState、TemplateType、icon、iconUrl、authority、UseState、priority、connectTime、audio、multiConnect、templateList、supportSwitch、switchMenuList、actionMenuList、batteryList、batteryInfoList。
PsAssistant 使用稳定的自有 ID/authority；SOC未知时电量列表为空，不填0。`NOISE_CANCELING + SwitchMenuList` 选择现存通用开关 holder，展示的是车锁/空调/净化/尾门，指令继续使用缓存复核与主程序原有确认链路。
版本不符、没有有效真实车辆或关闭桥接时不构建行。仅调整宿主内存中的自有行，不修改 OEM 数据库或 RPK 文件。

## 官方资料与下一步

- 用户给定 [10978](https://open.oppomobile.com/documentation/page/info?id=10978) 本轮未取得可读正文，不能用它证明车辆注入 API 存在。
- 已有官方 demo/SDK分析：[CarLink API矩阵](OPPO_CARLINK_SDK_API_MATRIX_20261102.md)、[CardWidget 1.2.3](OPPO_CARDWIDGET_SDK_1_2_3_11733_20261102.md)、[手表/数字钥匙边界](OPPO_WATCH_POLESTAR_DIGITAL_KEY_20261102.md)。CarLink 的包名/签名授权和 IOT 保护 Activity 不能靠本地品牌行补齐。
- 下一次真机优先验证 `assistant_native_vehicle applied=1` 是否出现，确认卡片实际由原生分支承载；同时读取最终4x2数据的 routeSafe / shortcutCount 摘要，避免反复试改不经过的路径。
- 若仍为 QuickApp 自选设备卡，下一步为官方 CardWidget 交互 Provider POC：先验证宿主发现及按钮事件回调，再接缓存和已授权命令。厂商应用准入需要独立验证，不能把 SDK 编译成功当作卡片已注册。
- 手表单独抓取一次 Smart controls 点击前后手机宿主的脱敏事件摘要（目标包/动作/返回码即可），确认实际到达的服务；不采集账号或令牌。手机轻量导出继续限制≤2MB，不包含 OEM完整APK。

## 验证状态

新增回归覆盖1.5秒菜单/取消手势、所有皮肤极简图标与反馈、净化确认计时/车辆隔离/到期语义、空调分钟显示、RPK邻近文本假阳性、20参数原生自有行构建。CI及打包结果以GitHub Actions和交付验证记录为准。
OPPO实机负一屏直接控制、Watch X2 长续航、官方 SDK 授权和数字钥匙仍未端到端验收。不得标记 `PROJECT_DEVICE_VERIFIED`。
