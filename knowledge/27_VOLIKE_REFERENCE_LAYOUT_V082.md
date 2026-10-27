# 27｜v0.8.2 VoLike 参考布局收敛

## 目标

把 v0.8.1 已完成的 VoLike 功能、极星配色、Widget 与皮肤选择器进一步对齐用户提供的 Volvo App 参考图，同时不改变真实车控、安全确认和 China API。

## 竖屏手机布局

三套 VoLike 共用以下骨架：

1. 顶部车型标题、最后停车位置、刷新；
2. 独立能源条：SOC、预估续航、车辆/充电状态和电量进度；
3. 中部车辆舞台：
   - 左侧 4 个纵向圆形核心控制：门锁、空调、净化、尾箱；
   - 中间 Polestar 2 顶视车辆；
   - 详细/极星配色在右侧增加车况信息轨；
4. 真实充电时才出现 ChargingPulseView；
5. 常规自选数据、车况/车窗/露营/鸣笛等扩展入口；
6. 下方三导航保持圆角屏安全边距。

## 简洁版

`volike_simple_labels` 默认 ON。关闭时左侧 4 个圆形控制只保留图标，但点击热区、状态配色、contentDescription 和 BIOMETRIC_STRONG 操作链全部保留。

## 详细版 / 极星配色

右侧 `volike_detail_rail` 显示：

- 保养时间剩余百分比和进度条；
- 保养里程剩余百分比和进度条；
- 当前车内温度；
- 最后停车位置入口。

这些百分比继续使用 ServiceReading 和当前保养周期逻辑，不声称为车辆直接上报百分比。

极星配色只改变视觉强调色：黑/白/灰 + Polestar Orange，不改变功能或接口。

## 横屏

640×320、小高度和 1.3×/1.56× 系统字号继续使用 v0.8.1 已验证的横屏结构，不强制套用竖屏纵向控制，以防重新出现裁切。

## 选择器 / Widget / 手表

- SkinPreviewView 同步新版 VoLike 结构；
- Widget 继续保持 classic / VoLike Simple / VoLike Detailed / Polestar VI 四种独立样式；
- WatchPreviewView 继续提供 Psa / Volvo、圆屏 / 方屏手机内预览；
- 独立 Wear OS APK 尚未构建，不能把手机预览标为手表端实装。

## 验证

必须继续通过：

- source / knowledge safety gates；
- testDebugUnitTest；
- lintDebug；
- assembleDebug；
- 320×568 / 360×780 / 640×320；
- 中英文、Light/Dark、1.0×/1.3×/1.56× 系统字体；
- 五套手机皮肤；
- 充电显示门控；
- 主控按钮 BIOMETRIC_STRONG 链。


## 2026-10-27 v0.8.3 当前增量

基于最新v0.8.2/9744759，恢复PS默认并保留VoLike新布局；自动保养只用Health剩余值，历史手动记录不再补充显示。地图共用精简网页，并新增默认关闭的手机前台位置授权；无后台定位。所有Widget改用原生文字/按钮与等比车图，新增PSeu、自选数据、统一粗体/字号及精确尺寸适配。按钮资源和后续迭代参考Psa图标规范v1。协议/实车证据标签不因此升级。详见 [v0.8.3统一知识](28_V083_UI_WIDGET_MAP_ICONS.md)。
