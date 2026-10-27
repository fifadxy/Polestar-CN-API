# Psa v0.8.4 图标、保养、Target SoC、协议和回读

2026-10-27。版本0.8.4-beta1/85，基于最新c14de67。具体行为、证据边界和验证见[更新说明](../docs/Psa_v084_Update.md)与[Psa图标规范v2](../docs/Psa_Icon_Design_Specification_v2.md)。

## 当前权威规则

- 确认的车辆线框母版替代黑武士/星形App图标，昼夜仅换色；通知是透明alpha。车况与露营不带icon、同尺寸；门锁与尾箱复用锁/解锁图形，动作路由不变。
- 保养payload3字段2/3/4是剩余工时/天/km，最新模板`PolestarApi.readHealthState`一致。30,000km替代旧20,000km参考；365天不变。统一保养周期余量为min(天数/365,km/30000)，不是后台原生百分比，缺失不倒推本地历史。原始读数不改。
- Target SoC新增C3官方已定义只读路径`chronos.services.v1.TargetSocService/GetLatestTargetSoc`，请求1=ChronosRequest(id1,vin2)；响应3=TargetSoc(chargeTarget1,settingType2,updatedAt3,source4,id5)，响应4=pending不得当已生效值。校验响应VIN2。字段1是uint32百分比，时间3是int64，秒/毫秒按量级规范化并拒绝未来值。范围仅1–100。缓存独立时间。每VIN失败后冷却5分钟，4秒调用截止，16项上限。全局PCCS方法仍仅历史参考，不向C3错误发送PCCS请求。未新增写接口。中国实车方法权限仍待验收。
- 字段证据来自用户中国官方APK的classes7.dex：GetTargetSocRequest/Response、TargetSoc、ChronosRequest生成类；最新复刻模板本身未读取充电上限，不能假称模板返回已验证。静态来源不包含账号请求。
- 协议中文来源为用户最新模板`MainActivity.fullDisclaimerText`，完整保留风险范围并给英文说明；版本化legal. accepted_disclaimer_v1与accepted_at。未打勾不接受，未接受不执行登录/车况/命令/定时入口。升级也需完成新增确认。
- CommandReadback四个排入时点5/10/15/20秒，不因早成功少读、不重发写。共用单工作队列避免并发RPC；网络慢会顺延。每次有效回读更新，最终才反馈，瞬时寻车只确认服务接受。commandPending与busy分离，首页小字进度0–95等待、完成后内部100；每秒只通知可见首页更新，不刷系统通知。设置页不反复重建。远控/切车仍防冲突，其他UI与读取可用；露营原独立保护不变。
- 位置旁单行文字只显示停车点摘要；免Key官方高德URI不能提供稳定原生逆地理编码，街道地址不伪造。未加Key/SDK/后台定位/额外位置提供商。

Debug/Preview173项测试，Lint0错误；源码/纯模型/地图JS/知识库检查与签名构建见交付记录。非实车验收。
