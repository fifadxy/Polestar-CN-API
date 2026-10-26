# 08｜Android 开发里程碑与优先级

| 阶段 | 工作 | 验收条件 | 当前 |
|---|---|---|---|
| P0 / 基础 | APK 指纹、Manifest/DEX、来源标记、知识库 | 可复现 inventory + 结构表 | 已完成静态整理 |
| P1 / 工程 | 原生 Android 项目、首次区域选择、状态 UI 与两个 Widget | CI 编译 APK + 实机首启和桌面添加验证 | 源码完成，CI 待验 |
| P2 / 登录 | China / Global 各自完整授权登录 + Token 刷新 | 各区域独立认证，不串 Token | 待映射 |
| P3 / 车辆 | China Vehicle Discovery、C3 SOC/Range/Odometer/Exterior | 同一 VIN + 时间戳核对官方客户端 | 待映射 |
| P4 / 实时 | C3 gRPC Live Stream、断线重连、后台刷新 | 连续稳定性及休眠恢复测试 | 待映射 |
| P5 / 扩展 | PCCS/车型差异、空调/净化/充电相关状态 | 区分车型支持及接口权限 | 待映射 |
| P6 / 命令 | 单项远控验证、确认与结果回读 | 完成独立安全/权限评审后再开放 | BLOCKED |
| P7 / 后期 | R8、混淆、安全审计、依赖瘦身与发行 | 用户同意启动该阶段后定义 | **DEFERRED** |

研究始终优先复用 Hisingen 已验证的 Global 模型和 China 官方客户端观察结果，但两者既不共用 Token，也不假定服务端可替换。新增能力先形成 `README` / `knowledge` 记录再提代码评审，防止不同 AI 将推论当成已交付事实。
