# OPPO 快应用卡片的额外过滤：进一步研究

证据：用户导出的 MyDevices 17.4.15 内置 assets/com.heytap.mydevices.quickapp.kdRpk.rpk。
此记录补充 beta8 的原生契约修复，区分快应用模板与原生 LocalPackerParser 路径。

## 新确认

SingleDeviceCard2x2 与 SingleDeviceCard4x2 在收到 deviceData 后，使用 eventSummary 集合过滤 shortcuts，按 item.id 匹配。该内置配置没有任何 ps_* ID。因此，style=8 修复原生首页条件，并不能自动解决这些快应用模板的过滤。

4x2 的注册表包含 CAR 插槽；CAR 组件的点击代码发送 deviceInfo.id、item.id、Number(!item.state)。通用 clickEvent 帮助函数经设备空间 ContentProvider 发送 deviceId、shortcutId 和 valueBoolean/valueInt。不能把出现 CAR 图标或 CAR 插槽等同于已经支持第三方任意车辆指令。

## 本版范围

beta8 已补全原生首页展示条件、车辆 data JSON、控制器三参数回调。回调只认自有明确 ps_* ID；不会将未知 TSL 属性名或整数猜成解锁指令。它可处理保留自有 ID 的路径，但没有修改 OEM RPK 的事件白名单。

## 下一步

1. 真机确认当前绑定卡片来自 legacy/native 还是 quick-app provider，记录卡片规格及来源，不采集账号信息。
2. 若为原生路径，检查 style=8 与 data 是否到达 LocalPackerParser；测试一个主动上锁动作及实车读回。
3. 若为上述快应用模板，优先研究仅针对 PsAssistant 的原生模板选择或独立卡片模板接入；继续保留 CAR 类型。
4. 若走原生车辆 TSL，逐个确认车辆属性 ID、状态枚举、目标值及命令映射。不能借用台灯/空调事件来掩盖车型，也不能只改白名单后就宣称全链路完成。
5. Watch X2 长续航模式仍需手表双模式采集，手机模板研究不构成手表支持证据。

## 校验摘要

```json
[
  {
    "entry": "cards/SingleDeviceCard2x2/index.js",
    "sha256": "f63dfc8554ef4465800dae5264ceab0b7c873d924cd37436867b9db854ec5483",
    "allowsPsAssistantIds": false,
    "filterPresent": true
  },
  {
    "entry": "cards/SingleDeviceCard4x2/index.js",
    "sha256": "69bdfacad2cd7ad7a65da678b9435eaa54fa7011debb7381164e1d699cfb86e2",
    "allowsPsAssistantIds": false,
    "filterPresent": true
  }
]
```


## 继续反编译：双重过滤链

MyDevices 17.4.15 不是只在 RPK 里过滤 shortcut。

`QuickCardDeviceDataConverter.q(...)` 在生成 `QuickCardDeviceData` 前已经把 TSL + DeviceModel shortcuts 合并、去重，并使用 `QuickCardDeviceDataConverter.r()` 返回的集合过滤。该集合来自 `assets/support/card_short_cut.json`，精确版本中不包含任何 `ps_*`。

因此实际链路是：

```
DeviceModel shortcuts / TSL
        ↓
QuickCardDeviceDataConverter
        ↓ card_short_cut.json
第一层过滤
        ↓
QuickCardDeviceData
        ↓
SingleDeviceCard2x2 / 4x2
        ↓ eventSummary
第二层过滤
        ↓
具体设备组件
```

当前 2x2/4x2 `eventSummary` 也不包含 `ps_*`。所以只修 style=8、只注入 controller 或只放宽 converter 都不足以显示自定义快捷键。

4x2 的 CAR 组件确实包含 `kongtiao / chesuo / shanguang / mingdi` 四类视觉语义，点击统一发送 `deviceId + item.id + Number(!item.state)`。但这四个 ID 同样不在当前 `eventSummary` 和 converter 白名单里。现阶段只把它视为 OEM CAR 模板资源证据，不猜测其云端车企协议。

beta10 / Bridge 0.9.0 新增只读 `QuickAppContractProbe`：在精确 17.4.15 上报告 converter 是否放行 PsAssistant ID 以及我们的 CAR 行转换后剩余 ID；不修改白名单、不改 RPK。

## Controller 进一步确认

`QuickAppCardAction` 的控制器注册表以 `packageName + deviceType` 选择控制器；三参数 `void(device, shortcutId, value)` 是点击，单参数 `void(device)` 是详情。

精确版本中 `com.heytap.opluscarlink` 对应控制器的点击方法为空实现；一个 `com.heytap.mydevices` wildcard 控制器的点击方法同样为空。因此 CarLink/Car+ 不能作为已验证的远程车控后端。

MyDevices 首页直接控车继续优先使用 exact-build `NativeHomeCardHook`，它只替换 PsAssistant CAR 行的内容并绕开 QuickApp 两层过滤，其他设备保持 OEM。
