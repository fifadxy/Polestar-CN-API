# KB34 · v0.8.9 China 登录兼容 / 完整分发门禁
2026-10-28。优先级高于历史的 v0.8.5 Standalone / v0.8.6 Hardening / v0.8.8 通用发布记录。

确认的直接故障：v0.8.8 公共 Debug 缺 `assets/client-signing.bin`，Login UI 在任何网络请求前就 `ClientSignatureProvider.isReady()==false`。这不是服务器登录失败。

修复：
- 旧 PsAssistant Standalone 构建机器输入 `assets/standalone/account_compat.bin`，旧 32 位 `client-signing.bin`、新版48位 PSACFG2、同应用 Keystore 加密迁移均有解析；不安装/扫描其他 APK。
- 只在本地/受控 CI 构建阶段读取已授权的旧完整 PsAssistant 安装包或已有私人配置，并以可信配置指纹校验后生成版本化资源；不打印账户兼容参数。
- 签名 Release/Preview 缺配置或原持久身份就必须失败；公开 Debug 不包含资源是设计限制，禁止宣传可登录。
- 不修改网络地址、密码请求、刷新、车辆发现、远控回读/写次数、Token 保存/Widget/UI。

验收：合成资源格式测试、CI 编译；真实手机需要同一签名覆盖、首次手机号登录、授权车辆发现、Token 刷新、Widget和后台刷新。无实车日志时仍为 IMPLEMENTED_NEEDS_DEVICE_VERIFICATION。
