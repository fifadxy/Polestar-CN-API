# 03｜China / Global API 研究映射（Android Parity V3.0）

> 证据分层：APK 静态 / 用户参考 APK 实车反馈 / 官方 China App 与 Hisingen 历史研究 / 新 Android 已实现能力，各自独立。详见 `13_REFERENCE_APK_FIELD_VALIDATION.md`。

| 领域 | 参考 APK 的静态依据 | 用户反馈与上游研究 | 本项目当前代码 | V3 开发 |
|---|---|---|---|---|
| China 登录 | `gw.app.polestar-auto.com`、`/user/api/v2/oauth/login/password`、`login` | 参考 APK `REFERENCE_USER_VERIFIED` | 无真实登录 | P0/P1 优先复刻与验证 |
| China Token refresh | `/user/api/v2/oauth/refresh-global-token`、`refreshIfNeeded`、`SESSION_REFRESH_LOCK` | 用户反馈偶发 Token 报错，功能仍可能可用 | 无 Token | P1：并发、过期、错误分类 |
| China 车辆发现 | `discoverAndRead`、`collectVehicles`、`ensureC3Host` | 参考客户端实际能操作本人车辆；完整 Discovery 参数未独立核对 | 演示车单 | P1/P2 映射 |
| China C3 discovery | `cnepmob.volvocars.com.cn` | 真实 Host 需发现，不能硬编码假设 | 未实现 | P1/P2 验证 |
| C3 电池 | `BatteryService`、`GetLatestBattery`、`Vehicle.battery` | 参考 APK SOC 已由用户实测 | DEMO | P2 真实读数 |
| 续航/充电 | `Vehicle.range`、`charging` 等 | 尚无明确用户逐字段实测 | DEMO / 未覆盖 | P2 逐字段确认 |
| C3 里程 | `OdometerService`、`GetLatestOdometer` | 参考 APK 累计里程已由用户实测 | DEMO | P2 真实读数 |
| C3 Exterior | `ExteriorService`、`GetLatestExterior` | 中国官方 App 另有 C3 运行证据；不等同第三方所有字段实测 | DEMO | P2 状态回读，P3 解锁验证 |
| 远控空调 | `control`、`ClimatizationStart` / `ClimatizationStop` 线索 | 参考 APK 空调远控已由用户实测 | `ENABLE_REMOTE_COMMANDS=false` | **P3 核心复刻** |
| 远控解锁 | `control`、`Unlock` 线索 | 参考 APK 车门解锁已由用户实测 | `ENABLE_REMOTE_COMMANDS=false` | **P3 核心复刻** |
| 其他远控 | `Lock`、`WindowControl`、`TailgateControl`、`HonkFlash`、`PreCleaning` 线索 | 未经用户逐项确认 | 未实现 | P3 逐项确认车型支持 |
| PCCS China | 当前参考 APK 尚未独立确认实际 PCCS 主机 | 既有官方 China App/Hisingen 文档：`api.pccs-prod.origin.polestar.cn` | 未实现 | P6 后续按车型确认 |
| C3 Live Stream | 第三方 APK 的持续 Stream 尚未实证 | 官方中国 App 研究记录 C3 Live；不同软件，不能混为同一结论 | 未实现 | P6；不阻塞 P3 |
| Global | 参考 APK 以 China 为主 | 上游 Hisingen Global 有 OIDC、Vehicle、C3/PCCS 对照 | 区域选择 DEMO | P6 独立 Session 与接口 |

## 已确认的 API 研究边界

- China 与 Global 不只差一个域名。China 官网网页会话不能直接视为官方移动 App 的车云 Token；Global Data Portal M2M Token 不适用于 China C3。
- 参考 APK 的 `Session.access/refresh/expiresAt/c3Host`、`signedUrl`、`refreshIfNeeded` 只证明存在相应代码结构；动态 Token Audience、Proto 字段编号和服务端返回仍须授权测试。
- `SIGN_SALT`、`signedUrl` 等标识可研究方法职责，但**不在仓库复制第三方固定签名秘密、验证码或登录凭证**。
- 远控状态 `SERVER_ACCEPTED` 不等于车辆已执行。控制超时后只读回读，不自动重复高影响写入。
- 第三方报告 A/B 的分析已在 `10_TWO_REPORTS_RECONCILED.md` 逐条校对；不同技术线索不得合并成“已实车验证”。

## 开发顺序

`方法级拆解(P0) → 授权 China 登录+Token(P1) → Vehicle Discovery/C3 Read(P2) → 用户已证实空调与解锁(P3) → 其他远控逐项验证(P3) → UI/Widget(P4/P5) → Global/Stream/PCCS(P6)`。

每取得新的 RPC/响应字段证据，同时更新 `docs/HISINGEN_GLOBAL_CN_API_COMPARISON.md` 和 `11_VEHICLE_DATA_DICTIONARY.md`；每个实测结论必须说明测试目标是**参考 APK**还是**本项目 APK**。
