# 03｜China / Global API 研究映射

本文件**继承**仓库已有 `docs/HISINGEN_GLOBAL_CN_API_COMPARISON.md` 的判断口径。不要在这里复制登录账号、Token、Cookie、手机验证码、签名盐。

| 领域 | 本次参考 APK 静态证据 | 现有项目文档证据 | 新 Android 状态 |
|---|---|---|---|
| China 登录入口 | `gw.app.polestar-auto.com`，`/user/api/v2/oauth/login/password` | China 移动账户不同于官网 Web Session | `MAPPING_REQUIRED` |
| China refresh | `/user/api/v2/oauth/refresh-global-token` | 中国 token 家族与 audience 不能直接假设 | `MAPPING_REQUIRED` |
| C3 discovery | `cnepmob.volvocars.com.cn` | 中国 C3 dynamic host 需要 discovery | `MAPPING_REQUIRED` |
| 车辆列表 | `discoverAndRead`, `collectVehicles` | China 官方 App 独立账户车辆发现链路 | `MAPPING_REQUIRED` |
| C3 电池 | `BatteryService`; `GetLatestBattery` | 中国 C3 电池状态曾有官方 App 运行证据 | `DEMO_ONLY` |
| C3 外部状态 | `ExteriorService`; `GetLatestExterior` | 中国官方 App C3 exterior live 已观察 | `DEMO_ONLY` |
| 里程 | `OdometerService`; `GetLatestOdometer` | China 端到端映射仍需验证 | `DEMO_ONLY` |
| 空调 | `ParkingClimatizationService`; `GetLatestParkingClimatization` | China climate 运行时间并不证明可任意设置 duration | `MAPPING_REQUIRED` |
| 净化 | `PreCleaningService`; `GetLatestPreCleaning` | 中国区具体能力待核 | `MAPPING_REQUIRED` |
| PCCS | 本 APK 字符串扫描未发现明确 `pccs` 主机 | `api.pccs-prod.origin.polestar.cn` 来自项目已有分析 | `MAPPING_REQUIRED` |
| Live stream | 本 APK 包含 gRPC，但未证实 stream 端到端 | 中国官方 App C3 Live 已通过另一研究路径验证 | `MAPPING_REQUIRED` |
| 远程控制 | `PolestarApi.control`、`ClimatizationStart` 等标识 | Global 可作参考，但 China 权限不同 | `DISABLED` |

### 研发验证顺序

```text
解析 APK 方法 -> 输出字段来源/候选 RPC -> 明确 Token audience 与过期机制
-> 采用授权账户的非写入接口做单次车辆发现 -> 再验证 C3 latest
-> 再验证 Exterior/Odometer -> 最后验证 stream 断连重试
-> 独立评审 Remote Commands（当前禁止）
```

不要把 Global 的 RPC 直接换域名在 China 下调用；不要为了“编译通过”将随机数模拟成实时车辆数据。
