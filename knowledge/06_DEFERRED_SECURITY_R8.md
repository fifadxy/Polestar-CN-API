# 06｜后期工作：安全、混淆及 R8（当前不执行）

**阶段决策：DEFERRED / DO NOT IMPLEMENT NOW。** 用户明确要求：以功能研究、API 映射、知识库、独立编译为先，暂不实施安全加固、R8 代码混淆和相关压缩优化。

| 后期项目 | 目标与当前备注 | 当前动作 |
|---|---|---|
| R8 shrinking / obfuscation | Release minify/shrink；需保留 gRPC/反射/JSON mapping 与动态 Widget | **不执行** |
| signing / anti-tamper | 发布签名、证书管理、完整性校验 | **不执行** |
| Keystore 细化 | 未来 China/Global Token 生命周期及访问边界 | **不执行**（本版无真实 Token） |
| 组件导出检查 | WidgetConfigure 与 BroadcastReceiver 访问边界 | **不执行**（首版没有外部远控入口） |
| Network security audit | TLS/超时/重试/证书/敏感日志 | **不执行**（首版无实车联网） |
| 硬编码常量治理 | 不能把原 APK 的私有签名盐复制入工程 | **不执行相关逆向复制** |
| 依赖裁剪 | Room、Guava、Gson、gRPC 未用项梳理 | **不做 R8 压缩**；新项目首版本身不依赖这些重库 |
| targetSdk 35 升级适配 | 前台服务与 Android 15 行为差异 | **暂缓** |
| 应用测评与渗透测试 | 安全风险清单、组件/通信审计 | **暂缓** |

**不属于“安全加固项目”的底线**：首版没有真实 Token、没有签名常量、没有向实车写入的远控路径；UI 中必须清晰标识 `DEMO/OFFLINE`；不得误报真实控制成功。这些属于防误操作的功能正确性设计，不能因为安全工作延期而取消。

后期启动条件：用户明确要求开始安全工作，并且 API 主流程已稳定；到时单独建立 `security-hardening` 里程碑，不干扰功能分支。
