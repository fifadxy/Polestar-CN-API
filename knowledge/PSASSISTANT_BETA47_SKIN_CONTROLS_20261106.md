# PsAssistant Beta47 — UI 与设置实现要点

变更基准：v1.0.0-beta47 /161（2026-11-06）

- 净化按钮关闭状态：以 `AirButtonMetrics.value(...)` 为唯一可显示的传感器文本（AQI + PM2.5），无读数显示 `--`，不渲染「已关闭」。
- 运行、命令待确认及极简无文字仍按既有交互逻辑处理。对于中高密度按钮，Compact NIO 与非 NIO 使用单行，信息丰富的卡片使用两行或独立读数；CenteredStatusButton 至多三行。
- 每皮肤每按钮一份 `skin_button_<skin>_<control>_<field>` 设置；7皮肤 × 8 控件 ×10属性，范围及规则见 [实施与验收](../docs/PSASSISTANT_BETA47_SKIN_CONTROLS_20261106.md)。
- 网格尺寸固定，实际按钮视觉外框在槽内按百分比缩放；控车点击热区不允许与邻位重叠。图标/文字位置偏移不影响控车执行判断。
- 后续若出现 NIO 特殊布局或字体截断，应先使用 Robolectric + 实机布局验证再修改；不能为通过测试删除已批准需求。
- SettingsBackup 仅追加严格键名白名单，不导出个人账户或服务签名材料。
- 版本更新详见 [Beta47实施文档](../docs/PSASSISTANT_BETA47_SKIN_CONTROLS_20261106.md)。
