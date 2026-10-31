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
