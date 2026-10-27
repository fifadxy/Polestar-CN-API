# PsAssistant v0.8.0-beta1 UI 更新

版本：`0.8.0-beta1` / `versionCode 81`

## 1. VoLike 手机皮肤

原有“简洁 / 详细”两套皮肤更新为 **VoLike 简洁 / VoLike 详细**。布局参考用户提供的 Volvo 风格界面，但保留 PsAssistant / Polestar 2 的车辆身份与控制逻辑：

- 续航与电量成为首屏第一信息层级。
- 使用现有 `vehicle_hero` Polestar 2 三分之四车辆主视觉，不拉伸原比例。
- 门锁、空调、净化、尾箱使用四个等尺寸圆形核心控制；按钮下只保留名称。
- 车况横条位于车辆主视觉下方，图标颜色表达正常 / 告警。
- 车窗、车况、露营、鸣笛进入紧凑功能区；车况按钮继续直接显示保养时间 / 里程剩余比例。
- 详细版增加最多 12 项的紧凑数据矩阵；简洁版优先显示前 4 项。

## 2. 统一充电动态反馈

所有手机皮肤共用新的 `ChargingPulseView`：

- 仅 `ChargingVisibility.shouldShow()` 判定正在充电时显示。
- 明确显示“正在充电 / Charging”、功率与预计完成时间。
- 动画为单条低高度能量脉冲，不使用全屏光效；View detach 或充电结束后立即停止动画。
- VoLike 使用低饱和绿色，NIOlike 保留青绿色，其余沿用 PsAssistant VI 强调色。

## 3. 手表皮肤

现有 Watch UI Preview 增加第二套 **Volvo 风格**：

- Psa / Volvo 两套预览可切换并持久化选择。
- 圆屏 / 方屏继续独立切换。
- Volvo 风格包含外圈电量弧、Polestar 2 主视觉、电量 / 续航、充电状态、门锁与空调快捷入口。
- 当前仍是手机 APK 内的手表布局预览，不宣称已经生成独立 Wear OS APK。
