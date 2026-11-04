# v0.9.6-beta5 裁切焦点与来源文字

版本110，详见[更新说明](../docs/Psa_v096_beta5_Update.md)。

新增map_crop_focus_x/y（0–100，默认50），MapMarkerSettings校验，加入style key及设置备份。JS组合焦点为clamp(pinFocus+(50-cropFocus)/200,0,1)，每轴最多偏移视口的25%，与既有pinFocus分别保存。最终焦点参与模糊中心与整图平移；不将marker单独挪离道路，不更改位置数据或刷新规则。恢复取景与标记移除裁切和标记六个偏好键。

背景模式隐藏.amap-logo，独立#psa-map-credit显示小号来源文字，并移动保留.amap-copyright原内容，取消原logo及署名背景块。只影响背景专用脚本，二级交互地图原来源设计保留。来源节点幂等创建，动态插入控件仍通过既有观察器处理。

测试：JS覆盖默认参数兼容与独立裁切；Chromium离线布局覆盖logo不可见、来源可见、取景极值不露底及恢复中心；原生覆盖默认50、范围限值、备份恢复和地图复用。真实高德网页及手机端验收仍待进行。

交付验证：350项Release回归通过，0失败/0错误；10组JS检查与Chromium离线裁切测试通过；Lint仅保留既有19项UnusedResources。原签名v2验证通过，13张WebP和独立配置与beta4逐字节一致。APK 9883611字节，SHA256 `dfee63aa588f7005e0616f21ece7b85bfdf0a4c46290972d7b413a53cf6c0a79`。
