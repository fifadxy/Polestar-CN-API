# OPPO CardWidget SDK 1.2.3 / 负一屏 11733 接口研究

日期：2026-11-02

来源：
- OPPO 开放平台文档 11733：`https://open.oppomobile.com/documentation/page/info?id=11733`
- 用户提供官方 SDK：`CardWidget_1.2.3-external.jar`
- OPPO 开放平台「智慧服务 / 负一屏」公开产品资料

## 1. SDK 身份

官方 JAR `BuildConfig`：
- library package：`com.oplus.cardwidget`
- build type：`external`
- version code：`10203`
- build time：`2022-06-14 14:17:24`

项目不把该官方二进制提交到公开 GitHub；公开主线只保留基于公开类签名和宿主元数据的兼容探测与数据映射。

## 2. 核心类

### AppCardWidgetProvider

`com.oplus.cardwidget.serviceLayer.AppCardWidgetProvider`

生命周期：
- `onCreate()`
- `initCardWidget()`
- `onCardCreate(context, widgetCode)`
- `onCardsObserve(context, List<widgetCode>)`
- `onPause(context, widgetCode)`
- `onDestroy(context, widgetCode)`
- `subscribed(context, widgetCode)`
- `unSubscribed(context, widgetCode)`
- `onRenderFail(context, widgetCode)`

这证明正式负一屏卡不是普通 Android AppWidget，而是由 CardWidget Provider/Channel 生命周期驱动。

### BaseAppCardWidgetProvider / BaseInterfaceLayerProvider

SDK 内部通过 ChannelClient 与：

`com.oplus.cardservice.repository.provider.CardServiceServerProvider`

通信，并支持：
- request / requestOnce
- observe / unObserve
- replaceObserve
- card thread
- onCallback

因此 `com.oplus.cardservice` 是正式卡片服务端关键宿主之一。

### CardWidgetAction

公开动作：
- `postUpdateCommand(...)`
- `switchLayoutCommand(widgetCode, layoutName)`
- `switchLayoutCommand(widgetCode, layoutName, layoutData)`

对应命令：
- `CardUpdateCommand`
- `UpdateLayoutCommand`

说明正式卡片更新有“数据更新”和“布局切换”两条路径。

### ICardState

状态接口：
- onCardCreate
- onResume
- onPause
- onDestroy
- subscribed
- unSubscribed
- onCardsObserve
- onRenderFail

PsAssistant 正式卡片 adapter 应在 `subscribed/onResume` 时开始有限频率状态同步，在 `onPause/unSubscribed` 时停止高频刷新。

## 3. AssistantScreenSelector 官方能力判断

SDK 的 `AssistantScreenSelector.isSupportCardWidget()` 逻辑已经从公开字节码确认：

1. 读取 `com.coloros.assistantscreen` application metadata；
2. 读取 `oplus.feature.support.flags`；
3. bit 0 (`flags & 1`) 表示 CardWidget 支持；
4. 如果 flag 未开启，再检查 `com.oplus.smartengine` versionCode；
5. SmartEngine 存在且 versionCode > 0 时同样认为支持 CardWidget。

UI Engine 版本：
- SmartEngine 存在时使用其 versionCode；
- 否则读取 AssistantScreen metadata：`com.oplus.uiengine.version`。

beta23 新增 `OppoCardWidgetRuntimeCapability`，按同一公开合同只读判断真机能力。

## 4. SDK 允许访问宿主

`BaseCardStrategyProvider` 的公开字节码包含以下允许访问包：
- `com.coloros.assistantscreen`
- `com.oplus.assistantscreen`
- `com.oplus.cardservice`
- `com.oplus.smartengine`

这与 11733 的负一屏插件卡路线吻合。因此 beta23：
- Manifest queries 增加 SmartEngine / CardService / alternate AssistantScreen；
- LSPosed scope 增加上述宿主做只读版本/结构探测；
- `ShelfCardHostProbe` 保存这些宿主版本和相关组件。

## 5. 与 PsAssistant 数据模型对接

公开主线继续使用 `OppoShelfCardSnapshotAdapter` 输出：
- provider = POLESTAR
- vehicleName
- SOC
- range
- charging
- lock
- climate
- tailgate
- updatedAt
- allow-list actions

不输出：
- VIN
- token
- 精确位置
- BLE address
- PAK 私钥/证书
- OPPO 授权值

正式 CardWidget adapter 获得 OPPO 对接资格后，再把这些业务数据写入官方 CardWidget/UPK 数据包。

## 6. Polestar 品牌入口 generation 3

“我的车辆”品牌列表长期不出现 Polestar 的根因之一，是旧 Hook 只改 ViewModel 内部某个 List；ColorOS UI 可能已经把它复制到 LiveData / ListAdapter / AsyncListDiffer。

beta23 generation 3 增加：
- coroutine 结果注入；
- ViewModel provider/account/vehicle 方法结果注入；
- provider-shaped 字段注入；
- `MutableLiveData.setValue/postValue` 出口注入；
- `ListAdapter.submitList` 出口注入；
- `AsyncListDiffer.submitList` 出口注入；
- immutable list 自动复制后替换参数；
- 350ms / 1200ms 延迟二次注入；
- provider code 去重；
- 不写 SceneService 数据库。

判定 provider list 仍然采用严格结构门禁：元素必须暴露 `getSpCode()` 和 `getName()`，避免修改普通列表。

## 7. CarLink / Wallet / CardWidget 宿主探测

beta23 扩展 CarLink 类指纹：
- CarLink
- CarLinkManager
- CarLinkUnit
- CarLinkUnitClient
- BluetoothKey
- BluetoothKey.Builder
- ICarKeyCallback
- CarControl
- ICarControlCallback
- CarInfo
- CarStatus

Wallet / DigitalKey 仍只做组件名/宿主版本只读探测。

CardWidget 宿主新增：
- com.oplus.smartengine
- com.oplus.cardservice
- com.oplus.assistantscreen

## 8. 正式发布仍需 OPPO 对接资格

CardWidget JAR 本身不足以让任意应用自动出现在官方负一屏插件卡目录。正式上线仍可能需要：
- OPPO 服务/卡片资格；
- UPK / DSL layout；
- event / eventCode；
- card/widget code；
- 白名单/grade/remind type；
- 发布审核。

因此 beta23 的目标是：
1. 车辆绑定页最大化可靠显示 `Polestar 极星`；
2. 点击后安全转发 PsAssistant；
3. 负一屏卡片能力和宿主识别完整；
4. 真实车辆数据映射完成；
5. 不伪造官方卡片授权或数字钥匙。