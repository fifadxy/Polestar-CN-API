# KB33 · v0.8.8 图标家族、Widget与露营可读性

2026-10-28。当前增量优先于v2图标及历史Widget停止风扇约定。用户在本轮追加Widget状态/剩余时间/无省略号，以及默认皮肤露营文字可读性要求，均纳入同一v0.8.8 / 89交付。

## 图标

AppIcons管理三款外观、三个昼夜模式，默认outline/auto；九个稳定alias统一指向始终启用的MainActivity。API33批量原子切换，API26–32先启用目标再关闭其他，不杀进程、无写指令、无新权限。启动/开机/升级恢复持久选择。日夜由限定资源或显式资源控制，通知透明单色由Android着色。内部App/Widget品牌随选择，系统Widget选择器静态receiver图标仍为默认App轮廓。详情见[Psa规范v3](../docs/Psa_Icon_Design_Specification_v3.md)。

## Widget

WidgetButtons统一climate/climate-start/climate-stop为ic_climate。颜色反映已报告运行状态；固定Start/Stop名称和不同intent保持。运行时显示ClimateRuntime的车辆报告剩余时间，必要时使用已确认本地会话fallback，缺失--m、到时0m，不据此更改运行状态。仅随Widget绘制更新，没有新增高频网络或服务循环；数据更新时间仍可见。

原生布局去除XML及动态TextUtils省略号；metricText完整值按实际像素缩字，标签先缩写再整项隐藏，chargeContent保留完整值、可隐藏辅助label，标题/按钮/更新时间原生autosize。48dp触区、透明/模拟磨砂、字体/加粗、数据和按钮选择全部保留。超出容纳项为+N，不删除已保存选择。

## 露营

CampModeManager.compactStatus仅供按钮显示；完整status原因与全部托管参数不变。默认标题沿用公用CenteredStatusButton，与邻居同字号/基线，简短状态避免反过来压缩标题。

## 验证

覆盖所有皮肤与六尺寸、CN/EN、字体1/1.3/1.56及Widget120%/粗体、250–380dp条形/51–70dp高度、完整数值、布局宽高、真实报告和本地计时来源。图标Android8/15路径、mask安全区、alpha及通知资源一致性有独立检查。最终数量在版本说明记录；不把合成/静态验证冒充实车或OEM桌面通过。无China/Global RPC新增，温度实验边界沿用KB32。

## 本地构建验收

- Debug/Preview各237项测试，0失败/错误/跳过；Lint各0错误、147警告。源码/知识库/Java模型/地图JS检查全部通过。
- Android8/15切换、九个组合唯一入口、保留MainActivity、日夜/透明alpha/中央安全区、通知与选择一致性通过。Widget覆盖六类尺寸/五风格、中英、字体1/1.3/1.56、Widget120%和粗体；新增检查行宽/行高及无省略号，另测窄条/充电值。
- 完整Release/Preview沿用固定证书SHA-256：`cc87b578976ba45bc06219d66a9cce8a771de457785b2af34be76aeaa3768c94`；不新增SDK/native库、后台轮询或写重试。真实车辆与OEM桌面仍需设备验收。
