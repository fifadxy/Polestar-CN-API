# OPPO 轻量资料分析与更新 · 2026-10-31

## 结论
设备空间卡片应继续沿 MyDevices 主进程 + `:cards` 适配；负一屏、Pantanal、手表分别验证。当前证据能定位组件与权限，不能证明车辆已绑定、按钮指令可达或长续航模式可用。

## 本机事实
- OnePlus PJZ110，Android 16 / SDK 36，构建 PJZ110_16.0.10.501(CN01)。系统应用 17.x 版本号不等于已证明 ColorOS 17。
- PsAssistant 0.9.7-beta7 / 118；MyDevices 17.4.15；负一屏 17.14.60。
- 模块 0.5.0 的 main/cards `hooks-installed` 明确标记 historicalOnly；两条历史 ready 不能用于当前会话验收。
- MyDevices 58/58 组件；负一屏 128/282、健康 128/953、语音 128/467、Car+ 128/198、Pantanal 128/194、快应用 128/887。缺席不能推断不存在。
- 原始 MyDevices Manifest 为二进制 AXML，不是文本 XML。两个复制静态文件 SHA-256 均经脚本校验。

## 接口线索与意义
| 线索 | 证据及下一步 |
| --- | --- |
| device_app_config.json | 存在 CarLink、car+、隐藏 Device App 样例；只是支持应用配置，不能证明新增配置即可实现车辆绑定 |
| DeviceCardHomeActivity | 运行在 :cards，受 com.oppo.permission.safe.IOT 约束；导出组件不等于普通应用可调用 |
| DeviceQuickAppCardWidgetProvider | authority 为 com.oplus.mydevices.authority.quickappcards；读写 IOT 权限 |
| DeviceLiteCardWidgetProvider | authority 为 com.oplus.mydevices.authority.lite_cards；读写 IOT 权限 |
| DeviceCardWidgetProvider | authority 为 com.oplus.mydevices.authority.launcher_cards；读写 IOT 权限 |
| SearchCarProvider | 负一屏车辆线索，读写 OPPO_COMPONENT_SAFE；不能据名称认定为远程控车，可能仅寻车功能 |
| FNThirdPartyBridgeActivity | 受 CONNECTIVITY 权限约束；尚无参数及方法行为证据 |

## 本次代码更新
1. 轻量导出 schema 4，组件分三档：车辆/设备卡片/快应用优先，普通 card 等其次，其余最后。保持 128 项上限与 2 MB 硬上限。
2. 记录 componentSelectionPolicy，便于区分新旧导出。
3. inspect_oppo_capture.py 支持轻量包；此前没有完整 APK 时输出空 packages，造成资料无效的错觉。现在保留各包版本、采集数量、截断状态、车辆线索，校验复制静态文件哈希，并标记历史模块证据与手表支持未证实。
4. 增加 Java 筛选优先级回归用例。

## 下一步验证顺序
1. 重新导出 schema 4，确认车辆相关组件优先保留；截断信息继续保留。
2. 主进程和 :cards 分别提供当前会话握手；记录版本、进程、阶段、时间。历史记录只展示为历史，不作为连通证明。
3. 最小补充目标：QuickAppCardAction、卡片注册/设备观察者以及点击分发相关类的方法签名和必要方法体。轻量包没有 DEX，不能凭 Manifest 编造 Hook 参数。
4. 原生卡片 → 本地桥接 → PsAssistant → 原有车辆命令 → 实车状态回读逐层验证。保留控制二级菜单。
5. 手机链路通过后，再单独测试手表标准/长续航两种模式；手机包不能证明 RTOS 端能力。

## 验证与交付边界
Python 分析脚本在真实附件运行通过；静态文件哈希校验通过；篡改哈希拒绝测试通过；git diff --check 通过。环境没有 Java/Gradle/Android SDK，未运行 Java 单测，未编译 APK，也未做真机控车验收。此次交付为源码补丁和分析结果，未推送远端分支。
