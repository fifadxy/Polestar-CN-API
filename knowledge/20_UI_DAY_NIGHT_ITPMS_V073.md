# 20｜v0.7.3 三套日夜模式、单屏布局、iTPMS 与 Polestar VI

## 1. 三套皮肤统一日夜模式

设置中的 System / Light / Dark 现在对三套皮肤全部生效：

1. 最新模板；
2. 当前版本；
3. 此刻图片风格。

此刻图片风格不再强制浅色。夜间统一使用近黑背景、深灰卡片、白色文字和 Polestar 橙色强调。

## 2. 车辆控制页不再依赖滚动

三套控制首页都采用固定单屏布局，通过屏幕高度自动压缩车辆主视觉、标题字号、控制卡、状态条、辅助卡和首页指标数量。

控制首页不再包含 ScrollView。v0.7.2 的顶部下拉刷新保留，但页面内容本身不需要上下滚动。

## 3. Polestar 2 车辆素材

“此刻图片风格”改回项目原始 `vehicle_top.webp` Polestar 2 素材，不再使用上一版从参考 EV 截图裁出的通用车辆图。

夜间模式下，所有使用该 Polestar 车辆素材的位置通过色彩矩阵降低 RGB 并保留高光，使白色车身变成黑色/深灰车身，包括最新模板、当前版本、此刻图片风格、门窗位置图和 iTPMS 四轮状态图。

## 4. 最新模板 APK 的胎压结论

对当前用户提供模板 APK `app-release.apk.1(1).1` 的 DEX 重新提取服务名：

- `services.vehiclestates.battery.BatteryService`
- `services.vehiclestates.exterior.ExteriorService`
- `services.vehiclestates.odometer.OdometerService`
- `services.vehiclestates.parkingclimatization.ParkingClimatizationService`
- `services.vehiclestates.precleaning.PreCleaningService`

未发现 `HealthService` / `GetHealth`、`TPMS` / `iTPMS` 或可确认的四轮胎压 kPa 读取逻辑，因此模板 APK 不能作为具体胎压数值的证据。

Mac/Hisingen 上游的 Health 映射仍可作为研究线索，但中国 Polestar 2 更合理的用户界面是按 iTPMS 告警状态显示。v0.7.3 起不在 UI 显示 kPa；只有四轮都明确为 OK 才显示 ALL GOOD；任一轮告警显示橙色 + !；数值字段若底层偶尔返回，仅作为内部研究数据保留。

## 5. Polestar VI 进度条与滑块

所有程序化 SeekBar / ProgressBar 统一使用：

- Progress：Polestar Orange `#FF6B00`
- Track：Light `#D9DAD6` / Dark `#3A3C3E`
- Thumb：Polestar Orange
- 卡片/控件：黑、白、灰为主

覆盖此刻 UI 电量进度、露营模式时长/检查间隔/电量下限、Widget 透明度/磨砂程度和按键震动强度。

## 6. 版本

- branch: `android-polestar-assistant-main`
- app: `0.7.3-beta1`
- versionCode: `73`
