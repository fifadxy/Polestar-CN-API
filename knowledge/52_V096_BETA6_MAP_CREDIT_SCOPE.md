# v0.9.6-beta6 地图来源范围

版本111，详见[更新说明](../docs/Psa_v096_beta6_Update.md)。

按用户最新要求，地图来源仅在定位二级菜单显示。parking_backdrop.js仅用于backgroundMode，隐藏.amap-logo/.amap-copyright/#psa-map-credit，移除额外credit节点的创建。parking_map_compact.js及二级地图来源不变。既有地图裁切焦点、模式、标记、复用和刷新规则保留。

浏览器离线测试先运行二级地图compact脚本确认logo/版权可见，再运行背景脚本确认二者不可见；另覆盖后插入来源节点、日夜和裁切焦点。真实高德网页与手机端仍待验收。

交付：11项地图相关Release回归通过，0失败/0错误；10组JS检查与Chromium离线布局通过。Lint仅19项既有UnusedResources提醒。APK原签名v2与摘要通过，13张WebP及独立配置保持；APK 9883379字节，SHA256 `9c18e2453a9311ae0d968049e2e5bdea0e4d9dc92ae87b270521deb2ee168698`。
