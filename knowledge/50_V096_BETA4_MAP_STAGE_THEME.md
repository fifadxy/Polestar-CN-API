# v0.9.6-beta4 地图裁切与模式

版本109，更新详见[说明](../docs/Psa_v096_beta4_Update.md)。

## 故障与处理

beta3通过选择器隐藏已知控件，但地图仍依附分享页父布局，未知后加载内容也可能漏出。其背景WebView宽视口和概览缩放还会干扰实际CSS尺寸。beta4将同一.amap-container移入固定的#psa-map-stage，用overflow:hidden与contain:paint约束绘制；隐藏其外部页面壳与内部非地图层。监听body的childList/subtree，不监听逐帧属性变化，合并清理任务100ms一次；幂等处理不反复移动节点或重写相同marker。来源与版权小号保留。

原位地图样式不重新定位。焦点调整通过实际SVG包裹span的中心底部对齐；SDK可用则panBy，否则整图平移。几何变化键仅含zoom/视口尺寸，日夜/颜色/模糊变化不触发resize。就绪检查要求地图已进入独立裁切容器。

## 配置

map_theme为system/light/dark；MapAppearance校验未知值回退system。system读取Configuration.UI_MODE_NIGHT_MASK，与App手动theme独立。默认跟随系统，模式进入设置备份白名单及默认恢复。日夜使用渲染层明暗处理，不声称是新增授权的高德原生地图样式接口。地图位置来自既有c3Lbs，不增加手机GPS调用。

## 验证

Chromium离线布局测试位于scripts/test_map_background_browser.py，使用合成地图层而非实际地图服务，覆盖未知及迟到浮层、来源保留、焦点四角、日夜、节点复用、横竖屏。原生覆盖系统/手动主题、备份恢复、同一WebView以及禁用概览缩放；真实高德网络页面仍需手机验收。

Release验证：349通过，0失败/0错误；JS 9组与Chromium离线布局通过；Lint仅19项既有UnusedResources。APK 9,883,207字节，原签名v2完整验证，SHA256 `a0015a65258250065ca24843aaf3005df724d892650aaeb15a758bb21cf661af`。13张WebP与独立配置保持，源码按用户已授权同步Android GitHub主线，完整APK通过原私有下载页交付。
