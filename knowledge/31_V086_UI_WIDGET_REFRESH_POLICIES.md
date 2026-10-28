# v0.8.6 UI Widget and refresh policies

2026-10-28。当前实现来源：[更新说明](../docs/Psa_v086_Update.md)、[Psa交互规范](../docs/Psa_v086_UI_Refresh_Specification.md)。基于v0.8.5，versionCode87；旧皮肤键保留兼容，选择器合并简洁/详细VoLike。

1. `VehicleConditionButton`统一参考保养百分比和居中显示；bar/percent/text可选。`UtilityButton`居中图标/标题与可配置反馈；所有车况入口仍进入同一页内展开页面。
2. `ButtonFeedback`负责内容、8–14sp和粗细；`ui_button_bold`、`ui_data_bold`、`feedback_bold`、`widget_bold`独立。默认空调保留目标/状态/剩余时间，不显示Cabin温度。TA/TM和空调时间h/m，不带~；TA/TM时长仍是同次距离/均速换算，空调倒计时仍可能用本机启动记录，不能当作执行确认。
3. `DisplayMetric`新增battery_12v/brake_fluid/coolant/washer_fluid/oil_status，复用已验证Health模型，不新增猜测的接口/单位。12V是告警状态，油液是状态，不是测量。
4. `MetricOrderView`真实拖放并有↑↓替代，保存前不写入主页顺序。固定能源区不参与扩展数据排序，紧凑屏幕保留已选项并优先显示可容纳的前几个。
5. `WidgetButtons`统一快照状态与控制路由；新增EXTRA_WIDE/5×1，原生布局按实际宽高和按钮数切换。各尺寸独立按钮/数据；48dp触控目标优先，缩窄隐藏后序按钮，不改状态为成功。全provider图标/预览均同App主图标。
6. `RefreshPolicy`分前台、App后台、Widget后台、车辆停车点缓存和手机位置刷新。前台5分钟默认；App后台默认独立关闭，常驻或widget来源仍30分钟；后台15分钟下限并由Android节电调度。仅读共享队列，无后台位置获取。
7. c3Lbs缓存默认15分钟，仅预览打开/手动刷新；过期不复用，切车/退出清除。手机定位另需授权，手动或二级预览打开时一次，WebView短窗口放行可信域，关闭即释放，不调用后台定位。
8. 充电卡底层流光4.8秒循环，约20fps，离开/暂停关闭；无新增动画或地图依赖。原5/10/15/20秒远控回读和生物验证不改。

完整独立APK继续由私有构建配置生成并本地签名交付；公开CI的无配置Debug只验证源码，不代表可全新登录的发行包。实车/账号与OEM调度验收需设备证据。
