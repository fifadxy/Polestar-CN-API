# Psa v0.8.3 主程序 Widget 地图与图标统一

2026-10-27。版本0.8.3-beta1 / 84，基线用户最新0.8.2-beta1 / 83 / 97447593e2f59cd683d908e2049cfba71025ce06，未继续旧0.7.9分支工作。

## 本轮用户完整范围

恢复PS默认皮肤；自动读取真实剩余保养；地图读取/显示同VoLike简洁；缩小高德遮挡浮层与地点/距离卡；可选手机定位；固定All Good！与矢量勾；统一空调/净化及车况/露营图标；所有Widget自适应；新增PSeu多尺寸/类型/数据选择；全Widget粗体开关；图标设计规范供后续迭代；所有完成后统一签名APK与GitHub源码交付。

## 实现证据

- UiSkin新增ps_default，第六套。原buildDashboard(false)详细模式回归当前自适应骨架，不覆盖VoLike新布局。PS/VoLike/NIO正常状态均固定All Good！并显示ic_check，未知和告警仍分别处理。
- VehicleConditionButton用同一个图标/标题绘制区和独立进度条区域，避免TextView默认绘制与自绘重叠；camp使用ic_camp。控制图标槽对齐，标题/数据/状态行统一。无动态资源拉伸。
- ServiceReading只读取Health.serviceKmLeft/DaysLeft；没有数据不再回退MaintenanceSchedule。SERVICE时间戳未知时0，不用现在时间。设置的保养页只读；历史手动记录不清除但不再参与显示。百分比固定参考365天/20000km，非后台返回的真实寿命百分比。小时字段不转换为天数。
- 定位API、经纬度轴（1经度/2纬度）、时间/stale、4秒截止及选车隔离沿用已确认实现。所有皮肤和Widget使用同一个locationDialog/parkingWebPreview；不新增车辆写接口。
- parking_map_compact.js只调整已核对的高德分享页面覆盖层，底部卡48px、距离行16px、浮层display:none；保留地图canvas和署名。网页DOM会变，必须保留原生打开地图兜底。
- MapLocationPermission默认off，必须pref+系统coarse或fine授权+可信HTTPS origin均满足。只申请前台权限，无ACCESS_BACKGROUND_LOCATION；拒绝网页相机等权限，无addJavascriptInterface。用户开启后可点网页定位键，距离精度/路由含义由网页与授权精度决定；关闭或退出前台释放。没有手机GPS历史落盘。
- 分享页即使授权仍可能保留距你占位，因此parking_map_distance.js只在用户启用定位时一次性请求浏览器getCurrentPosition，在网页内显示约距车距离及accuracy。手机点按WGS84在中国范围转换一次，车辆报告GCJ路径保持不动；不通过JS bridge把手机坐标传入原生层，不写普通缓存/日志，无watchPosition或后台轮询。坐标系假设仍需实车核对。独立Node测试覆盖关闭不请求、单次请求、单位/精度、坐标转换与直线距离。
- VoLike旧Canvas固定420×220文字/车图非等比缩放已移除。全部Widget通过createNativeViews共用TextView/布局/48dp按钮，车辆FIT_CENTER。布局依据实际高度与宽度切换，不只看名义组件类型。SizeF精确尺寸最多四个（Android12+），旧主机双方向兜底，onAppWidgetOptionsChanged直接使用提供的Bundle；实际尺寸背景有界LRU缓存。
- PSeu采用现有brand_light/dark、独立黑白橙配色。battery/range/car/controls类型，全局选择；字段使用pseu_前缀按尺寸保存，其他Widget设置不覆盖。短组件保留门锁/空调，宽组件扩展明确lock/unlock/climate-start/stop请求，全部路由到现有生物验证。
- widget_bold默认off，所有文本/预览共享StyleSpan.BOLD，测宽也用粗体Typeface。独立widget_font_percent叠加系统字号，按字段/行高调整。单行与叠放取决于实际字体空间，余下字段+N不丢选择。
- 新Psa_Button_Icon_Design_Specification_v1.md及SVG图册是用户新增参考的独立实现，后续按钮复用公共drawable；不是官方源资产许可或master宣称。启动器黑武士图标保持。

## 验证与边界

Debug/Preview各166项测试通过，0失败/0跳过，Lint无错误，签名构建与原生渲染见docs/Psa_v083_Update.md。Widget新矩阵覆盖五种风格/五种尺寸/系统字号1与1.3/用户80%和120%/粗体开关，并增269×51、420×90、300×170、200×220长宽比。Android15验证精确SizeF和旧范围fallback。手机矩阵覆盖六皮肤、语言/日夜/小屏横屏及1.56倍率。

公开示例网页验证浮层隐藏、卡48px、距离行16px和署名保留；这不代表本项目实车位置、GPS/高德距离或所有OEM桌面都已验证。手机位置是新增可选授权，替代历史“永不读取手机位置”的绝对描述；默认行为仍拒绝。保养自动显示替代历史本地记录主导逻辑。新图标规范优先于旧示意，但不更改协议和授权事实。

来源：[Android Widget官方布局](https://developer.android.com/develop/ui/views/appwidgets/layouts)、[高德URI](https://lbs.amap.com/api/uri-api/guide/mobile-web/point)。PROJECT_DEVICE_VERIFIED仍待用户设备/车辆验收。
