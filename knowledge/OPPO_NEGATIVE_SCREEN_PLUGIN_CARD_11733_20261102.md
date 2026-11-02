# OPPO 负一屏插件卡 / Seedling 卡片研究（文档 11733）

日期：2026-11-02

官方来源锚点：
- https://open.oppomobile.com/documentation/page/info?id=11733
- OPPO 开放平台「智慧服务 → 负一屏 / 流体云 / 服务接入指南」相关文档
- SeedlingCardOptions 相关官方文档（公开索引可读取到字段和示例）

> 文档 11733 正文为动态页面，自动抓取无法稳定读取全部正文。因此本文只记录可从官方页面索引、相关 Seedling 文档与当前 ColorOS 真机结构交叉验证的内容，不猜测不可读取字段。

## 1. 与 PsAssistant 当前三条 OPPO 通道的关系

ColorOS 原生车辆体验应拆成三条互补通道：

1. **CarLink / CarControl**：负责「我的车辆」、车企授权、车辆状态、官方远控回调。
2. **CarLink BluetoothKey / Wallet**：承载已经由车企真实完成的 BLE 数字钥匙状态和系统入口。
3. **Shelf / Seedling 插件卡**：负责负一屏、泛在卡片、可能的流体云等浅层车辆状态展示和快捷入口。

旧的 MyDevices / AssistantScreen / Pantanal 私有 Hook 只作为兼容和诊断，不再把 Android AppWidget 当成正式负一屏插件卡。

## 2. 官方 Seedling 卡片生命周期合同

公开可验证的核心类/概念：

- `SeedlingCard`
- `SeedlingCardOptions`
- `SeedlingTool`
- `IntelligentData`
- `onUpdateData(context, card, data)`
- `SeedlingTool.updateIntelligentData(context, intelligentData)`
- `SeedlingTool.updateData(card, businessData, cardOptions)`
- `SeedlingTool.updateAllCardData(card, businessData, cardOptions)`
- UPK / `config.json`

业务数据和卡片布局应分离：

- `businessData`：展示所需数据，例如 SOC、续航、充电、锁车、空调。
- `data/params`：与 OPPO 约定的事件参数。
- UPK 页面：定义卡片布局/页面。
- `SeedlingCardOptions`：控制卡片页面、里程碑、入口展示等行为。

## 3. SeedlingCardOptions 已确认字段

### pageId

切换卡片页面，值来自 UPK `config.json` 的 `pages`。

PsAssistant 计划：
- `pages/vehicle`：正常车辆状态；
- 后续若 OPPO 正式模板允许，可增加 charging / climate 页面。

公开主线只使用字符串预览，不提交未获授权的 UPK。

### dataSourcePkgName

用于声明卡片数据来源应用。

PsAssistant 官方卡片接入后应填写：

`io.github.polestarcn.companion`

不冒充 Polestar 官方 App 或其他车企包名。

### isMilestone

标记本次数据是否属于里程碑事件。

PsAssistant 原则：
- 普通定时 SOC/续航刷新：`false`；
- 不能因为每次数据更新都希望卡片可见而滥用里程碑；
- 充电开始/结束、用户明确控车完成等事件是否允许作为 milestone，要以 OPPO 对接规范为准。

### grade

OPPO 文档明确该级别字段需要与 OPPO 沟通，不能自行使用。

因此公开代码不写死 GRADE_1~5。

### notificationIdList

用于卡片和通知同时存在时做去重。

PsAssistant 后续正式接入时可以把常驻车辆状态通知的 ID 同步给卡片，减少 SystemUI 重复展示，但必须先核对当前通知生命周期。

### showHostMap / lockScreenShowHostMap

用于动态控制部分入口显示。官方相关文档当前明确说明这些设置主要对 SystemUI 入口生效。

因此不推断不存在的 `Shelf` 枚举；负一屏卡片入口仍按官方模板/服务配置处理。

### requestShowPanel / requestHideStatusBar / panelActionConfigMap

这些字段主要与流体云/SystemUI 面板行为有关，不应直接等同于负一屏插件卡。

### remindType

需要 UPK `runtime.interactive.seedling-type = live`，并且提醒类型需要 OPPO 配置。

PsAssistant 不在公开版本自行启用强提醒。

### extensibleActionMap

官方相关文档已列出 `shouldShake`、`overrideTransition`、`stateDuration` 等扩展字段，其中部分需要白名单。

车辆状态卡默认不使用抖动/强提醒；避免把普通车辆刷新做成骚扰式系统提醒。

## 4. PsAssistant 车辆卡数据模型

beta23 新增 `OppoShelfCardSnapshotAdapter`，公开映射以下非敏感字段：

- `provider = POLESTAR`
- `vehicleName`
- `socPercent`
- `rangeKm`
- `charging`
- `chargingLabel`
- `lockLabel`
- `climateLabel`
- `tailgateLabel`
- `updatedAt`
- `sourceLabel`
- allow-list actions

默认：

- `isMilestone = false`
- `pageId = pages/vehicle`

不输出：

- VIN
- access/refresh token
- Polestar PAK 私钥/证书
- BLE address
- OPPO AUTH_CODE/companyId
- 精确车辆位置

## 5. 卡片快捷动作设计

建议负一屏卡片 P0 动作：

- lock / unlock
- climate start / stop
- air purification start / stop
- trunk open

P1：

- honk
- flash

执行链：

`Seedling card click -> PsAssistant explicit/deep-link action -> VehicleCoordinator.command() -> Polestar real command -> readback`

原则：

- 卡片本身不能把“已发送”当成成功；
- 只有车辆状态回读确认后才显示成功；
- unlock / trunk 保留用户确认策略；
- 数字钥匙 BLE 解锁与云端 `unlock` 必须显示为不同能力。

## 6. 负一屏宿主 / Pantanal 探测

beta23 增加 `ShelfCardHostProbe`：

- `com.coloros.assistantscreen`
- `com.oplus.pantanal.ums`

只读收集：

- 宿主版本
- 进程
- card / shelf / seedling / pantanal / plugin / vehicle 相关 Activity / Service / Provider 名称

原有 `ShelfPantanalProbe` 也升级：

- 不再硬编码只允许 AssistantScreen 17.14.60；
- 以 Provider/query 结构是否存在作为只读能力门禁；
- 同时识别 vehicle candidate 和 shelf-card candidate；
- 仍不读取卡片正文、账号、Token。

## 7. 与 Polestar 品牌入口注入的关系

车辆绑定页面没有 Polestar，和插件卡是两个问题：

### 「我的车辆」品牌列表

使用：
- `PolestarProviderCatalogShim`
- `PolestarBindIntentShim`
- `BIND_BT_KEY` 可发现 Activity

目标：让 AssistantScreen / CarLink 的合法入口能找到 `Polestar 极星`，点击后进入 PsAssistant。

### 负一屏插件卡

使用：
- `OppoShelfCardSnapshotAdapter`
- `OppoShelfCardCapability`
- `ShelfCardHostProbe`
- 后续正式 `Seedling` adapter

目标：在完成 OPPO 正式卡片接入后显示车辆状态和快捷动作。

两者可以共存，但不能用“品牌行注入”冒充正式插件卡注册。

## 8. beta23 代码状态

主程序：

- 版本 `0.9.7-beta23-standalone` / versionCode 135；
- 新增 `OppoShelfCardCapability`；
- 新增 `OppoShelfCardSnapshotAdapter`；
- 新增 `OppoShelfHostDiagnostics`；
- Bridge IPC 新增 `oppo-shelf-card-status`、`oppo-shelf-card-preview`、`shelf-host-report/status`；
- OPPO 设置页显示负一屏插件卡 host / Pantanal Hook / 车辆数据预览；
- 轻量研究导出优先收集 seedling/shelf/pantanal/plugin 组件。

LSPosed：

- Bridge 1.8.0 / versionCode 19；
- 新增 `ShelfCardHostProbe`；
- AssistantScreen/Pantanal 同时做只读插件卡宿主指纹；
- `ShelfPantanalProbe` 升级到结构门禁；
- 保留 Polestar Provider 多入口注入与 CarLink/Wallet 探测。

## 9. 正式插件卡还缺什么

当前公开代码已经把数据、宿主和点击路由准备好，但要真正让 PsAssistant 出现在 OPPO 官方负一屏插件卡列表，还需要 OPPO 对接侧提供或确认：

1. 对 PsAssistant 开通对应卡片/服务资格；
2. 正式 Seedling SDK / 接入依赖；
3. UPK 模板配置；
4. event / eventCode；
5. 允许的 card host / 服务 ID；
6. 若需要，白名单和 grade/remindType 配置；
7. 发布/测试流程。

在这些条件未满足前，LSPosed 只用于真机研究和兼容，不通过伪造系统数据库记录宣称“官方插件卡已注册”。


## 10. CardWidget 1.2.3 官方 SDK 补充

用户提供的 `CardWidget_1.2.3-external.jar` 已完成公开 API/字节码合同分析，详见：
`knowledge/OPPO_CARDWIDGET_SDK_1_2_3_11733_20261102.md`

新增确认了 AssistantScreen support flags、SmartEngine/UIEngine 版本门禁、CardService server provider、AppCardWidgetProvider 生命周期和 CardWidgetAction 更新命令。beta23 已把这些合同用于真机能力判断与宿主诊断。