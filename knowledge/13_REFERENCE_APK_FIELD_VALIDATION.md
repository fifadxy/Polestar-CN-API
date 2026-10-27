# 13｜参考极星助手 APK：用户实车验证与证据分级

> 计划基准：2026-10-26；对象 `app-release.apk.1.1(1).1` / `app.c3garage.companion` / v0.1.0。
> 本文件记录**用户对参考 APP 的亲自测试反馈**，不代表项目组已在当前独立 Android APK 上复测，也不包含测试账号、VIN、验证码或 Token。

## A. 实车反馈：REFERENCE_USER_VERIFIED

用户明确说明：**参考 APK 的中国区登录、电量读取、累计里程读取、空调远程控制和车门解锁均已在实际车辆上可用**；程序运行一段时间偶尔报告 Token 相关错误，但这些提示出现时并不能简单断定车辆 API 或远控已经失效，实际功能仍可能可用。

| 能力 | 参考 APK 状态 | 当前 Android 主线状态 | 独立验证要求 |
|---|---|---|---|
| 中国区账号登录 | `REFERENCE_USER_VERIFIED` | `NOT_IMPLEMENTED` | 合法授权账户登录、退出、恢复、过期处理 |
| 电量 SOC | `REFERENCE_USER_VERIFIED` | `DEMO_ONLY` | 对照参考客户端与官方车况、时间戳和单位 |
| 车辆里程 | `REFERENCE_USER_VERIFIED` | `DEMO_ONLY` | 原值、公里单位、采样时间与缓存区分 |
| 空调远程控制 | `REFERENCE_USER_VERIFIED` | `DISABLED_IN_CURRENT_BUILD` | 分别核对原 APK 实际支持的开启/停止行为；新版本主动触发及状态确认 |
| 车门解锁 | `REFERENCE_USER_VERIFIED` | `DISABLED_IN_CURRENT_BUILD` | 车辆选择、明确确认、执行和锁具状态回读 |
| 续航、充电、开闭状态 | `APK_CONFIRMED`（字段/服务存在） | `DEMO_ONLY` | 分项真实接口验证，不能从 SOC 成功外推 |
| 锁车、车窗、尾门、鸣笛、闪灯、净化 | `APK_CONFIRMED`（操作线索） | `NOT_IMPLEMENTED` | 逐项对照参考 APK UI/行为与车型权限 |
| Widget 刷新/快捷控制 | `APK_CONFIRMED`（组件） | `DEMO_WIDGET` | 设备布局、真实数据刷新、主动授权控制 |
| C3 Live Stream | 官方 China App 另有项目研究证据；本参考 APK 未证实 | `NOT_IMPLEMENTED` | 单独验证真实服务端流，不把普通状态查询称 Stream |

**重要**：这里的 `REFERENCE_USER_VERIFIED` 是用户提供的实车验证事实，不是本项目新的 `PROJECT_DEVICE_VERIFIED`；尚未获知明确车型/年款、实测时间戳、网络环境和逐指令结果统计，也不会臆造。后续拿到脱敏记录可增加更细粒度证据。

## B. 已由原 APK 静态确认

- `classes.dex` 和 `classes2.dex`，参考 APK SHA-256：`8cae07a3b5c6050487443914ebb55b1d2f2f26e258f4b369837f2b88de052735`。原版 v2 内容摘要/签名已独立核查，不能据此认定为官方 Polestar 软件。
- 七大核心类：`MainActivity`、`PolestarApi`、`PolestarWidgetProvider`、`WidgetConfigureActivity`、`WidgetControlActivity`、`WidgetRefreshWorker`、`CampModeWorker`。
- `PolestarApi$Vehicle` 字段数 **33**，见 `11_VEHICLE_DATA_DICTIONARY.md`；字段存在≠真实响应路径已完全恢复。
- 账号线索：`gw.app.polestar-auto.com`、`/user/api/v2/oauth/login/password`、`/user/api/v2/oauth/refresh-global-token`、`login`、`signedUrl`、`refreshIfNeeded`、`SESSION_REFRESH_LOCK`；不在新工程导入第三方私有签名盐。
- 车况 / 控制线索：`discoverAndRead`、`collectVehicles`、`ensureC3Host`、`stateRpc`、`readStates`、`control`、`refreshControlState`、`authenticateControl`、`sendControl`、`controlReached`；服务路径和 Proto tags 需要方法级解析与可复现验证。
- 远控相关操作符可能包括 `climate-start/stop`、`lock/unlock`、`windows-open/close`、`trunk-open/close`、`unlock-trunk`、`honk/flash/honk-flash`、`air-purification-start/stop`。**这不是所有操作都实测可用的证据**；需确认实际分发方法、参数、车型能力和认证上下文。

## C. Token 误报：已观察到的现象与尚未证实的根因

- `USER_REPORTED`：参考 APK 持续使用一段时间后偶发 Token 相关错误提示，真实操作仍可能成功。
- `HYPOTHESES_ONLY`：Token 时钟偏差、并发刷新、某一状态查询失败被泛化为全部认证失败、会话更新竞态、控制响应与后续状态回读混淆、不同 Audience 不匹配、业务异常误归类为 Token 错误。
- 必须记录 **失败发生在哪个请求**（登录、刷新、发现、状态读取、远控下发、控制结果回读），不能仅凭界面弹窗断言根因。
- 请求超时后的控制结果可能处于 `UNCONFIRMED`；先作只读状态确认，禁止自动重发开锁、开窗、尾门等写指令。

## D. 下一次实测的最小证据记录（不保存秘密）

| 字段 | 填写要求 |
|---|---|
| 参考客户端版本/样本 SHA | v0.1.0 + SHA，若发生更新重新提取 |
| 区域与车型 | China；车型及年款，可选脱敏车辆标识；VIN 不入仓 |
| 测试功能 | 登录、SOC、里程、空调启动/停止、解锁等单项 |
| 操作时间与网络 | 本地时间、Wi-Fi/蜂窝、前后台状态 |
| 客户端提示 | 原始错误类别、HTTP/gRPC 状态、是否仅为 UI 报错 |
| 车辆实际结果 | 本人观察或官方状态读数，分别记录请求接受与车辆执行 |
| 认证上下文 | token 类型/audience、expiresAt 相对时长，**不得保存 Token 值** |
| 验证等级 | `REFERENCE_USER_VERIFIED` 或独立新增 `PROJECT_DEVICE_VERIFIED`，不混写 |

## E. 证据来源与边界

- APK 静态基础：`01_APK_EVIDENCE.md`、`knowledge/evidence/`、`10_TWO_REPORTS_RECONCILED.md`。
- 当前本工程：`04_FEATURE_MATRIX.md`、`05_BUILD_AND_TEST.md`、`12_STAGED_VALIDATION.md`。
- Global / 官方 China 区 API 研究：`docs/HISINGEN_GLOBAL_CN_API_COMPARISON.md`，**与第三方参考 APK 的实测能力不自动等同**。


## 2026-10-27 v0.7.8 当前增量

用户确认新上传参考 0.1.20261027.081805 中国区正常（REFERENCE_USER_VERIFIED），包含扩展健康、931h/75d/14959km 剩余保养及停车位置。新项目只完成独立映射与合成验证，此参考结论不转移为 PROJECT_DEVICE_VERIFIED。 具体来源、字段和限制见 [v0.7.8 车况、位置与行程](25_V078_CONDITION_LOCATION_TRIP.md)。
