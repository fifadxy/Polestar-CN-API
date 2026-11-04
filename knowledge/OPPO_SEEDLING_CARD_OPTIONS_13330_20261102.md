# OPPO 负一屏 / SeedlingCardOptions 官方补充研究（13330）

日期：2026-11-02

官方来源：
- https://open.oppomobile.com/new/developmentDoc/info?id=13330
- 关联入口：https://open.oppomobile.com/documentation/page/info?id=11733

## 与 11733 / CardWidget 1.2.3 的关系

11733 页面正文在公开抓取环境依赖 JavaScript/权限动态加载；用户提供的 CardWidget 1.2.3 external SDK 已给出插件卡运行时合同。OPPO 当前可公开读取的 13330 文档则补充了 Seedling/泛在卡的数据更新与显示选项。

因此项目把两条官方卡片技术作为并行能力：

- CardWidget：AssistantScreen / SmartEngine / CardService 的插件卡运行时；
- Seedling：事件驱动、里程碑、入口可见性、页面切换和提醒策略。

正式发布仍取决于 OPPO 提供或审核的 UPK/template、event/eventCode、白名单及业务配置。

## SeedlingCardOptions 已确认字段

### pageId

用于选择卡片页面，对应 UPK `config.json` 的 `pages`。不填写时使用默认页面。

PsAssistant 预览映射目前使用 `pages/vehicle`；正式 UPK 确认后应改成真实 pageId，不应自行假定生产模板已存在。

### dataSourcePkgName

标识实际数据来源应用。若 PsAssistant 自己提供车辆数据，应使用 PsAssistant 包名；作为其他应用的数据中转方时才填写对方包名。

这正好支持项目目前的架构：负一屏卡数据源是 PsAssistant，车辆真实数据仍来自 PsAssistant China/C3 后端。

### isMilestone

表示本次数据是否属于里程碑事件。普通状态刷新不应全部标成 milestone。

建议：
- SOC/续航轮询：false；
- 充电开始/结束、车辆解锁确认、空调启动完成等是否属于 milestone，要按 OPPO 审核后的业务协议配置；
- 公共预览层默认 false，避免自行扩大系统展示优先级。

### grade

OPPO 文档提供 GRADE_1..GRADE_5，但明确要求与 OPPO 沟通后使用。

因此 PsAssistant 不在公开代码里自行设置生产 grade；只保留“未配置”状态。

### requestShowPanel / remindType

`requestShowPanel` 涉及流体云面板/胶囊，并需要白名单。OPPO 当前建议优先考虑 `remindType`。

`remindType` 也依赖 UPK 中对应的 live seedling 配置和 OPPO 侧业务配置。

PsAssistant beta23 不宣称这些能力已经可用。

### requestHideStatusBar

控制临时隐藏/恢复状态栏流体云入口。车辆状态卡当前不需要主动使用。

### extensibleActionMap

可携带入口扩展参数，例如数据更新反馈、转场、状态持续时间等。部分参数明确需要白名单。

beta23 不注入未获授权的扩展值。

### showHostMap / lockScreenShowHostMap

官方示例显示 Seedling 卡可以控制不同系统入口是否展示。

这为后续“负一屏显示车辆卡，但状态栏不重复显示”的配置提供了正式路径；需要结合 OPPO 批准的模板和业务规则实现。

## 官方更新流程

公开文档确认两类典型动作：

1. 通过 IntelligentData + SeedlingCardOptions 触发/推送卡片；
2. 在卡片数据更新回调中，通过 SeedlingTool 更新单卡或全部入口数据。

PsAssistant 对应设计：

```
VehicleSnapshot
    ↓
OppoShelfCardSnapshotAdapter
    ↓
authorized Seedling/CardWidget adapter
    ↓
OPPO negative screen / other approved hosts
```

beta23 已完成前两层和运行时能力探测；正式 SeedlingTool 调用保留到取得可合法打包的官方 SDK/UPK/eventCode 后。

## beta23 的安全门禁

公开代码不伪造：
- OPPO 白名单；
- grade；
- eventCode；
- UPK/template；
- CardWidget 注册成功；
- Seedling 正式发布状态。

诊断只报告：
- AssistantScreen / SmartEngine / CardService 是否存在；
- CardWidget 运行时是否满足；
- UIEngine 版本；
- Provider catalog 是否出现 POLESTAR；
- PsAssistant 的真实车辆快照是否可以映射成卡片数据。

## 结论

11733 + CardWidget 1.2.3 + 13330 共同说明：

- “车辆品牌绑定目录”和“负一屏车辆卡”是两条可以独立推进的链路；
- Polestar 品牌入口由 beta23 generation 3 LSPosed 兼容层解决；
- 负一屏数据由 PsAssistant 真实 VehicleSnapshot 映射；
- 正式 OPPO 卡片发布仍需官方模板、事件/白名单配置，不能由 Hook 假装完成。
