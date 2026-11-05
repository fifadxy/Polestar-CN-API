# PsAssistant v1.0.0-beta42 · 稳定性与Widget/停车位置专项
日期：2026-11-05 · Android 正线候选 · versionCode 156

## 核心交互
- 原生二级菜单关闭系统窗口缩放与位移动画，保留固定居中窗口、预先设置尺寸和滚动视图，不再使用跨窗口动态模糊导致的窗口重新测量。仍须在 ColorOS 真机以弹窗触发、键盘与旋转验证。
- 极简模式所有按钮反馈、百分比/进度条、车况文字隐藏，主电量/续航数据不隐藏。NIO皮肤布局保持结构。
- 极简图标按皮肤独立设定大小（65%~175%）与粗细（70%~150%），PS 默认放大至125%；图标严格居中、应用安全边界，标准模式不受影响。

## 桌面组件 / 净化
- 激活的Widget图标使用VI橙色 #FF6B00，不再根据背景填充转为黑色。工作态按钮底色采用低强度透明橙色，保留自定义外框与透明度。
- 净化按钮下方默认 PM2.5 + AQI。设置可选 PM2.5、AQI、两者、隐藏；缺数值显示 --，不伪造。NIO原布局不额外插入行。
- 默认皮肤双数值展示时第二列右侧对齐预计续航基线。

## 停车点读取与刷新
- 坐标源：Polestar c3Lbs 停车位置；高德负责地图/逆地理展示，不应把高德显示当作车辆位置上报。
- 独立读取，不触发 readVehicle()、车辆写指令或手机GPS；不保证第三方云端读取绝对不会唤醒车辆。
- 智能：首次15分钟，连续不变为30分钟、60分钟；失败退避到最多120分钟；固定：5/15/30/60/120分钟；手动：不自动刷新。
- 显示背景启用时才自动定时请求；预览优先缓存，点击手动强制刷新可绕过缓存。长停省电会暂停所有自动车辆读取。

## 测试与验证门槛
- 单元测试 ParkingRefreshStrategy 累进、固定/手动和空值处理。
- 需要 GitHub Actions：testDebugUnitTest、lintDebug、assembleDebug、oppoBridge assemble。
- DEBUG 验证包与 FULL 正式独立登录包分开；FULL 必须使用已批准的客户端签名配置与原证书，绝不从 CI Debug 推断正常登录。
- 真机必测：二级菜单无抖动/无跳入、四种皮肤两种方向/字号、Widget颜色及空气质量、位置缓存/强制更新、前后台省电、OPPO Bridge 真实回报。
- 无真机数据不宣称车控或OPPO绑定已真实打通。

## 代码路径
- MainActivity.java / HapticDialogBuilder.java / CenteredStatusButton.java / ButtonAppearance.java
- MinimalIconAppearance.java / AirButtonMetrics.java / WidgetButtons.java / WidgetAppearance.java / WidgetRenderer.java
- RefreshPolicy.java / ParkingRefreshStrategy.java / VehicleCoordinator.java
