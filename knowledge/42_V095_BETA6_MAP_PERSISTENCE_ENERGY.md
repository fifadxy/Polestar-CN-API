# 42｜beta6 地图复用与能耗闪退

2026-10-29，版本101。地图背景在同一Activity内保持同一WebView，普通切页/菜单返回/前后台恢复不再创建网页或额外定位。自动读取按RefreshPolicy.map和上次成功时间，0为仅手动。新图钉VI橙，删除原生点，清理路况/卫星控件；首次冷启动3s/5s探测保留。

修复已由自动化复现的EnergyActivity两个NPE：可空SOC被条件运算自动拆箱；无历史车辆ID时vehicle.equals。保留原SQLite记录和缺失值，不补造数据。默认PS皮肤SOC加粗、图库文字黑白对比增强。

以[beta6说明](../docs/Psa_v095_beta6_Update.md)覆盖此前切页销毁WebView的约定。地图真实显示及控件DOM兼容性没有新增真机证据。
