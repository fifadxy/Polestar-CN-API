# Psa Android 图标资源

当前统一母版：[Psa_Car_Master_v2.svg](icons/Psa_Car_Master_v2.svg)。用户确认的车辆正面、双横灯、轻弧肩线和底部凹口；不添加无线弧线、渐变或发光框。`psassistant-icon-master.png` 是该母版的日间预览导出，不能作为独立几何来源。

Android `ic_psa_car.xml` 和 `ic_notification.xml` 使用同一组路径；前者按 `values` / `values-night` 配色，后者为白色透明底alpha图形，由系统通知栏着色。自适应启动器有安全留白；Android13+提供monochrome层。主界面与Widget统一复用。OEM启动器可能缓存旧图标，实际切换取决于系统资源/桌面刷新；不为换图标增加后台轮询。

旧黑武士启动器资源已被本次确认稿替代。按钮规范见 [Psa v2图标与交互](../../docs/Psa_Icon_Design_Specification_v2.md)。旧v1图册保留为历史参考，冲突以v2为准。


## v0.8.8 当前增量 · 2026-10-28

当前图标以[Psa规范v3](../../docs/Psa_Icon_Design_Specification_v3.md)与icons/v3为准：三款可切换、各有昼夜与状态栏。scripts/generate_app_icons.py统一输出SVG及Android路径；v2资产保留历史。默认outline/auto，其他仅为外观，不表示车辆告警。
