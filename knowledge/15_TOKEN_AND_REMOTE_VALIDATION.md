# 15｜Token 误报排查、远程控制状态机与实车验收

**适用版本**：Android Parity V3.0 及以后；**当前只是一份诊断/开发计划，不是已修复的宣称，也不授予自动控制权限**。

## 1. 观测事实与原因假设分离

`REFERENCE_USER_VERIFIED`：第三方参考 APK 的 China 登录、SOC、里程、空调、车门解锁可以实际使用；运行一段时间偶发 Token 相关报错，但车辆操作仍可能完成。

`HYPOTHESIS`：刷新并发、提前过期窗口、`expiresAt` 时区/单位错误、服务端时钟偏差、State/Command 认证上下文混淆、旧请求覆盖新 Session、读取失败误报登录失败、命令已受理后状态回读失败，以及 gRPC 业务错误被统一翻译为 Token 错误。**尚无可靠运行时证据指认哪一项是真正原因**。

## 2. 精细化网络与错误分类

| 环节 | 日志记录（脱敏） | UI 的正确状态 |
|---|---|---|
| Login | 请求 ID、时间、HTTP/业务状态、区域、登录状态；不记密码/验证码 | 成功 / 认证拒绝 / 网络失败，不混为 Token 过期 |
| Refresh | 会话版本、触发原因、重试次数、相对有效期、结果 | 刷新异常不自动断言已有车辆命令失败 |
| Discovery | 鉴权类型、车型、主机发现成功与否；脱敏车标识 | 车辆发现错误与登录状态分开 |
| C3 Read | RPC、gRPC Status、采集时间、源新鲜度 | 读取失败可保留带时间的缓存，不标 LIVE |
| Remote Write | 指令种类、用户确认、请求 ID、超时/受理/失败 | `SENDING`、`SERVER_ACCEPTED`、`FAILED` 或 `UNCONFIRMED` |
| State Re-read | 匹配的车/目标状态、服务端采集时间、结果 | 仅匹配真实状态才标 `VEHICLE_CONFIRMED` |
| Widget/Worker | 是否后台、Doze/网络、请求环节和采样时效 | 后台请求失败不直接强制整个 App 退出登录 |

网络原始错误码要保留到诊断层（并脱敏），不要由一个 catch 块统一变成“Token 失效”。

## 3. Session 状态模型建议

```text
REGION + ACCOUNT_SESSION
  access / refresh / expiresAt / tokenAudience / sessionRevision
        |
        +-- valid -> API request
        +-- expiring -> single-flight refresh -> atomic update
        +-- unauthorized for specific call -> classify endpoint/audience
        +-- refresh unavailable -> preserve state, show recoverable error
```

- `TokenRefreshCoordinator`：同区域/账号只合并兼容的并发刷新请求，避免不同区域和不同 audience 混用。
- 每次更新为新的 `sessionRevision`；迟到的旧响应不可覆盖新会话。
- Token 失效判定需要具体服务端错误，不能只依赖 UI 报错文本。网络超时、DNS、gRPC `UNAVAILABLE` 应是独立类型。
- 不能通过用户关掉界面提示来“修复”认证问题：必须证实实际会话、状态读取和车控链路工作正常。
- 测试日志不上传原文 Token、Authorization Header、Cookie、密码、短信验证码或 VIN。

## 4. 命令状态机及禁止的重试行为

```text
NOT_SENT --用户确认--> SENDING
SENDING --明确拒绝--> FAILED
SENDING --服务端受理--> SERVER_ACCEPTED
SENDING --网络超时/断线--> UNCONFIRMED
SERVER_ACCEPTED --车端回读/回执匹配--> VEHICLE_CONFIRMED
SERVER_ACCEPTED --回读无法确定--> UNCONFIRMED
UNCONFIRMED --只读状态确认--> VEHICLE_CONFIRMED 或保留 UNCONFIRMED
```

- `SERVER_ACCEPTED` **不等于** `VEHICLE_CONFIRMED`。
- 只有用户主动发起的合法授权车辆操作才会生成写入请求；解锁等操作需明确确认。
- 请求状态不明确时**不能无条件自动重复** `unlock`、`lock`、尾门、窗、鸣笛、空调等写入指令。
- `refreshControlState` 与类似 `controlReached` 的状态核对逻辑应作为 `CommandStateVerifier` 独立模块。
- 每条命令应按 China/Global、车型、车端版本分别建立可用性判断；未经验证不可展示“已成功”。
- v0.3.0-alpha2 的 `CapabilityMatrix.ENABLE_REMOTE_COMMANDS=true` 仅表示 China 空调启停和解锁已进入手动确认的设备验证通道；未验证的上锁、车窗、尾门、鸣笛/闪灯和净化仍不展示为可用。

## 5. 可复现测试矩阵

| 用例 | 测试操作 | 验收条件 |
|---|---|---|
| T01 首次登录 | 本人授权中国区账号登录 | Session 创建成功、车表与账户一致 |
| T02 读取 SOC | 参考客户端/官方客户端对比 | 值、单位、采样时间一致或可解释 |
| T03 读取里程 | 多次刷新并跨网络 | 数据新鲜度正确，无无依据归零 |
| T04 空调远控 | 确认车辆后手动启动/停止 | 请求结果与实际运行状态分别记录 |
| T05 车门解锁 | 手动确认并执行 | 真车状态确认；不存在未经授权重复发送 |
| T06 Token 闲置 | 登录后长时间未使用再刷新 | 不把普通超时归为身份无效 |
| T07 并发刷新 | 多请求同时跨越 token 临期 | 相同会话刷新合并，旧结果不覆盖新 Token |
| T08 请求受理+状态读取失败 | 模拟回读异常 | UI 表示“已受理/待确认”，不误报命令明确失败 |
| T09 网络超时 | 指令下发阶段断网 | 标 UNCONFIRMED；只读确认，不自动重试写操作 |
| T10 后台恢复 | 锁屏/Doze/切换 Wi-Fi 与移动网络 | 有效数据源与缓存标识，准确反馈 |
| T11 多车和区域 | China/Global 切换及不同 VIN | Token、车辆、缓存、指令对象严格隔离 |
| T12 UI 与 Widget | 主页面和两个 Widget 上刷新、切换、控制 | 一致状态机、控件反馈可用 |

**测试执行状态**：本文件的 T01–T12 对新工程**尚未执行/尚未通过**。用户对参考 APK 的已验证功能记录在 `13_REFERENCE_APK_FIELD_VALIDATION.md`。

## 6. 调试记录格式（不包含秘密）

```json
{
  "timestamp": "ISO8601",
  "app": "reference-or-new-android",
  "region": "China",
  "operation": "climate-start | unlock | battery-read | refresh",
  "stage": "auth | command | state-read | widget",
  "transportStatus": "REDACTED_STATUS_CODE",
  "sessionRevision": "non-secret-sequence",
  "commandState": "UNCONFIRMED",
  "vehicleStateVerified": false,
  "notes": "sanitized human observation"
}
```

仅结构示例，不作为已经取得的网络日志。测试样本里不能包含用户标识、Token、完整 VIN 或私人行程。

## v0.4.0-beta1 增量

新增 Lock/TailgateControl/PreCleaning 的模板方法级映射，系统 BIOMETRIC_STRONG 成功后才发送；取消/不可用不发送。单次发送后最多四次只读回读；状态值匹配只说明车辆状态已达目标，不声明获得独立的命令执行回执。通知 Token 时间取最近登录或刷新签发时间，不因车辆 Host 保存而更新。导入兼容参数采用 Keystore AES-GCM，密码不持久化。
