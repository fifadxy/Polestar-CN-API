# Psa Android v0.7.9 免 Key 地图预览

2026-10-27；Android 0.7.9-beta1 / 79；基线 031ef0b。此增量替代 v0.7.8 的可选 staticmap Key 方案，保留其车况与行程功能。

## 证据与用户选择

本轮上传文本 `已粘贴的文本.txt` 是外部 AI 建议，不是协议权威或自动执行授权。发现两处冲突：它一方面仍要求高德 Static Map Key，另一方面把位置 payload 的经纬度写反。实际用户要求免 Key，并随后明确选择“在 App 二级弹窗内嵌高德地图网页（需要 WebView）”，优先于附件“不使用 WebView”的建议。

参考 APK SHA f468c76eae50cfe753d62d6efd4a4706efb084a9bceb7ec26e0fa0fca6a161c6 的 PolestarApi 方法已复核：Dtlinternet 响应 payload2 内 **1=longitude、2=latitude、3=timestamp**，外层3=stale。原字段定义不反转。0,0、NaN/无穷和越界无效。

## 当前方案

- c3Lbs.grpcHost → dtlinternet.DtlInternetService/GetLastParkedLocation，只在用户请求位置时读取；不并入后台车况轮询。
- 已保存和新发现 LBS host 都验证可信官方子域；LBS RPC 4秒截止，与主车况读取错误隔离。失败不清除账号。切车/退出 generation 失效，丢弃旧结果。
- ParkingMapLinks 构造官方免 Key URI：`https://uri.amap.com/marker?position=lon,lat&name=...&src=PsAssistant&coordinate=gaode&callnative=0`。网页仅接收停车点和通用名称，无 VIN/token。高德 App 使用 androidamap://viewMap，dev=0。
- ParkingWebPreview 在二级弹窗中加载官方位置网页，不抓取网站内部 Key、不绕过 staticmap 认证、不请求未公开地图瓦片。外部打开顺序为高德 App → 高德 HTTPS 网页 → geo 系统地图。
- 删除 StaticMapPreview、SecureMapPreferences；升级清理 map_provider_private/key 和 amap_preview 旧项。没有 Key 配置入口，设置显示使用说明。
- 无地图 SDK、native 库、新依赖和 Android Location 权限。系统 WebView 网页允许必要 JS/DOM，但不接受 Cookie，不使用磁盘页面缓存，不允许文件/content 访问或混合内容；拒绝网页定位及权限，不提供 JS bridge。顶层 HTTPS 域名限定 amap.com/autonavi.com 的真正子域，拒绝伪造后缀和不安全 scheme。
- 弹窗关闭、切车/退出、Activity停止/销毁均释放 WebView，清理本应用网页缓存/历史/表单/DOM存储；不将 WebView 状态写入 Activity Bundle。精确停车点不进入普通 Snapshot、SharedPreferences 或日志；不建立停车历史。
- 地图有20秒加载提示超时及错误兜底；无浏览器/高德也不崩溃。不注册手机定位或后台地图作业。相比静态图片，网页资源和渲染开销更高，仅在可见时运行。

## 公开来源及验证边界

[高德官方 URI 单点标注](https://lbs.amap.com/api/uri-api/guide/mobile-web/point)记录 position 经度在前、coordinate 默认高德坐标、callnative=0 不自动调起 App，没有 Key 参数。[静态地图](https://lbs.amap.com/api/webservice/guide/api/staticmaps)则明确 Key 必填，不能混同。

使用官方公开示例点 121.287689,31.234527 测试 HTTPS URI：返回200，跳转 m.amap.com，页面包含指定点和地图 canvas，随后使用临时浏览器的环境代理 CA 公钥固定和软件图形渲染显示实际底图。没有修改持久证书库，也没有在 APK 中加入证书校验绕过；网页骨架/canvas 存在本身不算底图成功。Robolectric 验证的是 Android 容器、设置和行为，不渲染在线网页。手机实际网页底图、服务可用性与 China 坐标偏移仍需实机验收。

`China C3 LBS coordinate system requires real-car verification`。沿用参考 dev=0，不自行二次偏移。报告时间与 stale 标志保留；定位始终称“最后停车位置”，不称实时位置。地址/POI 没有独立证据，不制造地址或做额外逆地理编码。

Debug/Preview 各140项测试通过，Lint无错误，Release/Preview已编译并验证v2签名。新回归覆盖协议轴/请求/超时、失败不影响主状态、切车/退出不串车、URI fallback、旧 Key 清理、拒绝手机定位、无 JS bridge、WebView 生命周期与四皮肤中英日夜/小屏横屏。发布校验值和最终测试数量见 [v0.7.9 更新说明](../docs/Psa_v079_Update.md)。


## 2026-10-27 v0.8.3 当前增量

基于最新v0.8.2/9744759，恢复PS默认并保留VoLike新布局；自动保养只用Health剩余值，历史手动记录不再补充显示。地图共用精简网页，并新增默认关闭的手机前台位置授权；无后台定位。所有Widget改用原生文字/按钮与等比车图，新增PSeu、自选数据、统一粗体/字号及精确尺寸适配。按钮资源和后续迭代参考Psa图标规范v1。协议/实车证据标签不因此升级。详见 [v0.8.3统一知识](28_V083_UI_WIDGET_MAP_ICONS.md)。


## v0.8.6 当前增量

参见[KB31](31_V086_UI_WIDGET_REFRESH_POLICIES.md)和[更新说明](../docs/Psa_v086_Update.md)：VoLike合并切换、车况/反馈/字体配置、拖动排序、5×1与自选Widget按钮、Health状态选择、独立刷新策略。车辆c3Lbs与手机定位分开；手机定位只在二级预览按需获取。运行时间h/m无~，计算来源未改变。此前与上述行为不同的说明作为历史记录。完整发行包本地编译签名，无需其他APK。
