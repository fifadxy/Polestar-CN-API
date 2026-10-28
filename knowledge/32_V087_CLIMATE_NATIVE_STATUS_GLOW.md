# KB32 · v0.8.7 原生车况、光效与空调目标温度试验

2026-10-28。当前增量优先于KB31。用户新增要求：所有光效可关闭；点击车辆恢复二级车况弹窗，不直接导航完整车况页。详细交付说明见[更新说明](../docs/Psa_v087_Update.md)。

## 实现

CenteredStatusButton以原生TextView/compound drawable/spans维护标题、反馈、百分比；按TextView实际字体回退及图标占用测量，避免PS默认车况消失和中英文大字溢出。默认皮肤四个辅助按钮共享图标槽；三项导航包含设置矢量。PS默认暗色关闭按钮使用暗色surface/浅色ink。

所有车辆图片进入carStatusDialog，显示原门窗位置图与可展开状态。车况按钮仍进入完整详情页；监听在关闭时清除，切车/退出关闭。光效light_effects总开关、all_good_shimmer独立开关与charging_animation共同约束；AllGoodTextView 6400ms循环、80ms节流，只在前台可见、非省电且系统允许动画时运行。弹窗暂停背景动画；Widget无循环绘制。

## 温度证据与语义

[独立静态证据](evidence/reference-climate-start-20261028.json)。两版模板ClimatizationStart字段3=fixed32 float，调用点均22°C；ParkingClimatization回报字段8=目标温度。跨平台历史能力supportsParkingClimatizationDirectStartSettings=false，Mac远控unsupported。APK_CONFIRMED不等于PROJECT_DEVICE_VERIFIED。

ClimateTarget默认off/22°C。实验范围18–28°C、1°C仅为App保护。API拒绝非有限/越界参数，不触发网络。主程序及Widget用同一coordinator；新露营会话存储targetC/customTarget，旧记录22/false，启动后不随设置漂移。用户保存设置不会发送指令。

实验请求按既有5/10/15/20秒只读回查，匹配目标温度且报告>=发出时间、非缓存/演示且空调运行才确认。缺失、旧回报、不同温度均UNCONFIRMED；消息区分请求值与实际回报。无额外写重试、无静默温度降级。本项目未实测车端接受能力。

## 验收与后续

新增原生像素/文字、全部兼容皮肤键、昼夜/中英/小屏/横屏/大字体、三种车况模式、图标、弹窗生命周期、光效节电、FakeChannel温度wire及拒绝不重发测试。最终测试数量在版本说明记录。完整签名包无其他APK依赖；公开CI仍为缺私有配置的验证包。下一步由用户授权车辆验证目标温度回报及OEM真机显示；不把静态协议证据提升为实车通过。

本地最终验收：Debug/Preview各221项测试全通过；Lint各0错误、140警告，源码/知识库/纯Java/地图JS门禁通过。NIO窄车况按钮仅显示百分比摘要，完整周期语义在详情和无障碍描述保留。
