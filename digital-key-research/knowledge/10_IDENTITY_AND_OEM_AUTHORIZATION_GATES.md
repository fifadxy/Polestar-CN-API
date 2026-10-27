# 10｜China 登录与 OEM PAK 新设备注册之间的实际缺口

本页基于当前研究分支的 ChinaPolestarApi.java 和 VehicleCoordinator.java、用户提供的 4.25.0 官方 APK 的静态分析，以及极星数字钥匙官方流程整理。

## 1. 当前 China App 数据结构真实情况

当前项目 android-app/app/src/main/java/io/github/polestarcn/companion/api/ChinaPolestarApi.java：

```text
ChinaPolestarApi.Session
   access
   refresh
   expiresAt
   c3Host
   c3LbsHost
   revision
```

并不含原版 PAK 初始化所需的 UserModel.identifier。VehicleCoordinator 的登录逻辑调用 ChinaPolestarApi.login(...), 在主线程只标记 signedIn，并管理车辆清单和车辆快照。

因此，**不能因为“中国区账号已登录/远程解锁已能用”就假定 PhoneAsKeyRepository.loginUser 所需的用户标识和授权已经齐全**。

另需注意：当前 ChinaPolestarApi 的兼容实现中存在 clientId（polestar-app-android）与 ReferenceSignatureProvider。这些属于既有账号/远程车控项目的兼容实现，**不是**可独立授权使用 OEM 数字钥匙服务的法律或技术证明。数字钥匙分支不得把这种兼容行为当作厂商授权，也不得通过复制官方包签名来获得信任。

## 2. 必须分开的五个身份

| 身份 | 来源/作用 | 是否已由现有 China API 提供 | 数字钥匙用途 |
| --- | --- | --- | --- |
| Account session | 现有 China 登录返回 | 是 | 用户已登录的必要条件之一 |
| PAK user identifier | 原版 UserModel.identifier | **未建立映射** | PhoneAsKeyRepository 的用户范围 |
| App instance identity | 原版 SDK App-Instance-ID（静态分析关联随机 UUID） | 不是同一个概念 | 单个安装实例标识 |
| Device/Actor certificate identity | OEM 注册/车辆授权流程 | 否 | 新手机自身的可信身份 |
| Vehicle trust record | 本人车辆正常确认并登记 | 否 | 允许数字钥匙工作 |

不要通过猜测 JSON 字段名或解码未授权令牌来冒充任一身份。账户 token 的 audience/scope 与设备证书注册权限必须由 OEM 支持渠道或正常授权行为验证。

## 3. 必须增加的接口（授权层抽象）

```text
AuthorizedIdentityPort
  canUseDigitalKeyServices(): EntitlementStatus
  accountIdentity(): AuthorizedAccountIdentity
  obtainOemScopedSession(): Result<AuthorizedSession>

AuthorizedProvisioningPort
  isSdkDistributedWithPermission(): boolean
  deviceEnrollmentStatus(): ProvisioningState
  beginOwnerApprovedEnrollment(): Result<ProvisioningState>

AuthorizedPairingPort
  pairingState(): PairingState
  revokeOwnDevice(): Result<RevocationStatus>
```

注意：上述是工程内部接口设计建议，**不是声称 OEM 对外公开了这些方法**。在得到合法 SDK 和账户授权之前，各个接口都必须返回 UNAVAILABLE/OEM_NOT_AUTHORIZED，不能执行设备注册请求。

## 4. 实际开工前需要确认的请求/响应问题

| 待核查点 | 为什么重要 | 验证方式（不获取任何私钥） | 当前 |
| --- | --- | --- | --- |
| UserModel.identifier 映射 | PAK 用户关联可能不同于手机号 | 正式账号/SDK API 文档或获授权测试 | UNKNOWN |
| VOCMO access token scope/audience | C3 token 不一定能访问 PKI | OEM 授权文档/测试环境 | UNKNOWN |
| 应用身份 / SDK 许可 | 随机 App-Instance-ID 不排除服务器白名单 | 合法 SDK 分发和测试环境反馈 | UNKNOWN |
| UCC/USEC device enrolment | 新设备必须被签发/认可 | 获授权测试账号的非敏感阶段结果 | UNKNOWN |
| Catalogue enrolment | 车辆服务加载依赖已注册身份 | SDK 的 typed success/error | UNKNOWN |
| PassiveEntry 授权 | P319 材料必须属于本机本车 | OEM 正常用户确认流程 | UNKNOWN |
| P319 pairing | 仅本地 BLE 权限不足以注册钥匙 | 本人车辆正常配对与确认 | UNKNOWN |
| 撤销/重装/换机 | 不可重复使用旧设备证书 | OEM 设备管理/车端用户管理 | UNKNOWN |

## 5. 重要安全和失败策略

- 如果服务器返回访问拒绝、证书注册失败或 OEM 未授权，立即停止注册尝试；不可把错误当成“要绕过的校验”。
- 不在日志中写 token、用户 identifier、VIN、Actor/Near-Field 私钥、实际 PAKM 配对数据。
- Root 只用于 Android 系统诊断，正式数字钥匙应能在普通未 Root 手机按官方流程授权工作。
- 对真实车辆进行任何操作前，应由车主本人在车辆上参与确认，保留所有实体钥匙并准备可回退方案。
- 多次重试应遵守 OEM 速率限制，避免把配对失败进一步变成账户或车辆暂时封锁。

## 6. 本阶段工作优先级

**第一优先：获取正式的用户身份/SDK 许可和测试资格**。第二优先：补齐设备注册的安全存储/错误模型。第三优先：才是 P319 具体 BLE 生命周期。这样可以避免在授权门槛未解决时，对底层 BLE 重复投入。
