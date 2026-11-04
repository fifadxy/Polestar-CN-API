# CarLink 云端签名参考（服务端）

这部分不进入Android APK。依据本次获取的OPPO官方11112、11113、11306文档和上传的SignUtils/DigestUtils，分别实现：

- 应用身份token申请：SHA256(appKey + serverSecret + 毫秒timestamp)。
- 用户级接口：appId + 按字段排序拼接的参数 + appSecret，然后SHA1。body按实际发送的原始JSON字符串参与签名，不可在验签时重新格式化。
- 应用级业务接口：common字段、参数JSON值、serverSecret拼接，按Java URLEncoder规则编码后SHA256。

签名函数仅计算摘要，未发起真实请求。appSecret、serverSecret及Polestar用户会话都不应放入Android APK或公开仓库。

OPPO文档叙述写name:value，但上传Java工具formatUrlMap实际拼接key+value；实现跟随对应Java工具，联调时必须对照相同版本的测试向量，不能混用两个签名方案。application_sign只接收标量；msgContent/body等复杂JSON按接口规定先序列化成字符串，避免跨语言对象排序产生歧义。

运行：`cd tools/oppo-carlink-cloud && python3 -m unittest -v`

完整云接入还需要注册service_url/OAuth域名、用户授权码与refreshToken流程、绑定/解绑、车辆信息、缓存车况、异步taskId结果回调和OPPO后台车型技能配置。此工具不声称这些服务已经上线。
