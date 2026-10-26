# 12｜Android 分阶段验证总表（参考 APK 与新工程区分）

**重要**：参考 APK 用户已反馈 China 登录、SOC、里程、空调、解锁真实可用，标 `REFERENCE_USER_VERIFIED`；本工程尚无相应 API 实现与端到端实测，故不能标 `PROJECT_DEVICE_VERIFIED`。

## A. 现有工程历史验收

| 项目 | 核验依据 | 状态 |
|---|---|---|
| 原 APK ZIP/DEX/SHA、v2 签名、33 字段 | `01_APK_EVIDENCE` / `evidence` | `APK_CONFIRMED` |
| Java 纯模型 + 语义测试 | `scripts/test_models.sh`、`tests/ModelStateTest.java` | 既有源码单测通过 |
| Android 0.1.0-dev2 Debug 编译 | Actions run `40261229408`，APK SHA256 `0e8d417cd6354ecf55ac414492a466ce10d02778e40eb59aa850bdbac0767a40` | `CI_VERIFIED`，**仅旧版本** |
| 首次区域选择、页面和两个 Widget | 已有代码，但无本次新设备日志 | `IMPLEMENTED_DEMO / DEVICE_UNTESTED` |
| China 真实登录及车辆接口 | v0.3.0-alpha2 已实现协议与 UI，尚无本项目设备日志 | `IMPLEMENTED_NEEDS_DEVICE_VERIFICATION` |
| China 远控 | 空调启停和解锁已接手动确认通道，无自动写重试 | `IMPLEMENTED_NEEDS_DEVICE_VERIFICATION` |
| Global 完整网络 API | 现有代码没有 | `NOT_IMPLEMENTED` |

## B. P0–P7 分项新验收要求

| 阶段 | 主要验收 |
|---|---|
| P0 | 参考 APK 七业务类、33 字段、认证方法、RPC/Proto 字段、UI 组件、Widget Worker 形成可复现依据；静态存在不等于运行成功 |
| P1 | China 本人账号合法登录、Session/Token 刷新、闲置和并发场景、车辆发现及多车绑定 |
| P2 | China 真实 SOC/里程优先，其余 Range/Exterior/Charging/Climate 分项核对，采样时间、单位、缓存新鲜度明确 |
| P3 | 用户主动解锁、空调操作逐项验证，指令服务受理与车辆真实状态核对分开；未确认的指令不自动重发；其余控制按车型逐项开放 |
| P4 | 原 APK 与新 APK 的同设备截图和操作流程对照，页面/手势/字体/素材状态正确 |
| P5 | Widget 配置/刷新/主动快捷控制与主界面一致，Doze、重启、断网不虚报实时 |
| P6 | Global 独立认证/车表与 China 分离；C3 Live/PCCS 每个方法独立验收 |
| P7 | GitHub CI、调试 APK、SHA256、源代码、测试报告和未通过项；不能用旧 v0.1 CI 代表新版本 |

## C. 命令与认证验收底线

- 状态：`DISABLED`、`NOT_SENT`、`SENDING`、`SERVER_ACCEPTED`、`VEHICLE_CONFIRMED`、`FAILED`、`UNCONFIRMED`。
- `SERVER_ACCEPTED` ≠ `VEHICLE_CONFIRMED`；没有可靠回读不展示“车辆已执行”。
- 参考 APK 一段时间后 Token 误报属于用户观察事实，根因尚未确认。分类登录、刷新、Discovery、C3 Read、Remote Write 和结果回读异常，禁止单个错误覆盖其他已成功操作。
- 真实指令只能针对用户授权的本人车辆，在明确主动操作后发起。命令超时先只读回读，不自动重复写入。
- 不提交密码、Access/Refresh Token、短信验证码、Cookie、第三方签名密钥或完整 VIN。
- WorkManager 不能作为生命安全相关的精确定时器；露营/宠物功能不得承诺通过手机轮询保障车内安全。

## D. 长时间与设备回归

详尽的 T01–T12 测试用例见 `15_TOKEN_AND_REMOTE_VALIDATION.md`，原 APK/新 APK 实测对照格式见 `13_REFERENCE_APK_FIELD_VALIDATION.md`。新代码在确认并接通 API 之前必须继续标 DEMO/OFFLINE，不能隐藏真实数据源状态。

R8、混淆及非必要安全加固按 `06_DEFERRED_SECURITY_R8.md` 延期，不意味着远控功能应永久搁置。
