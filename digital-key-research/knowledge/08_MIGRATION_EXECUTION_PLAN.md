# 08｜极星 2 数字钥匙移植实施方案（工程开工版）

**基线**：用户提供的极星中国官方 APK 4.25.0，SHA-256 为 99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef。  
**目标分支**：research/polestar2-digital-key-pak-20261027；**Android 现有应用基线**：PsAssistant 0.8.2-beta1。  
**当前性质**：Android 研究原型，不是可用的车钥匙。**不合入正式分支、不替代实体钥匙**，直至满足本文验收门槛。

## 1. 项目目标与边界

目标是让 PsAssistant 在获得厂商许可、车主授权并经过车辆正常配对后，独立管理其**新生成的新设备身份**，通过正式 PAK 能力完成近场使用。非目标包括：借用官方 App 现有钥匙、伪造手机或车辆身份、重放认证消息、绕过设备信任、使用 Root 绕过配对。

极星 2 官方数字钥匙架构以手机应用、加密蓝牙和账号/云端辅助为主，不等于极星 3/4 的钱包数字钥匙。参考：
- https://www.polestar.cn/zh-cn/support/faq/polestar-2-app-and-digital-key/
- https://www.polestar.cn/us/manual/polestar-2/2025/article/212ec99cb18f734fc0a801517dee3fba/

## 2. 已有证据与不能越过的结论

| 事项 | 证据等级 | 工程意义 | 不代表 |
| --- | --- | --- | --- |
| Ps2PairingFragment、PakPairViewModel、PhoneAsKey、P319 | 原 APK 静态确认 | 有可映射的功能层次 | 获得第三方 SDK 使用权 |
| VOCMO/CarAccess → PassiveEntry → P319 | 静态调用图，部分逻辑待动态交叉验证 | 主链可以建模 | 新包名已能成功登记 |
| 设备端出现 UCC/USEC、Actor/Near-Field 证书注册 | 原 APK 静态分析 | 应实现新设备身份流程 | 服务端一定接受新设备 |
| App-Instance-ID 与 UUID 生成有关 | 原 APK 静态分析 | 有独立安装实例概念 | OEM 不检验其他客户端属性 |
| 找不到客户端签名校验 | 有限静态扫描 | 不能把签名锁当作既定事实 | 服务端不存在客户端限制 |
| Android 14 AVD、KVM、ADB root、BLE/GATT、Keystore2 可启动 | GitHub Actions 实测 | 环境和生命周期测试可开工 | BLE 实车钥匙配对成功 |
| Android adapter、诊断页与实验状态机 | 研究分支代码 | 能进行工程编译和只读自检 | 能开锁或启动 |

## 3. 推荐的模块分层

```text
PsAssistant / China 账号和车辆 UI
      │
      ├─ 现有 China API / VehicleCoordinator（保持原样）
      │
      └─ digitalkey（研究隔离、初始默认关闭）
           ├─ DigitalKeyResearchActivity  —— 只读状态/系统授权
           ├─ DigitalKeyEnvironment       —— 系统 BLE / 权限 / 省电
           ├─ DigitalKeyProvider          —— 宿主与实现层的唯一入口
           ├─ DigitalKeyStatus            —— 可信阶段与失败类型
           ├─ ResearchDigitalKeyProvider  —— 默认不可操作
           │
           └─ 未来获许可后新增（当前未实现）
                ├─ AuthorizedIdentityPort —— 安全交付的登录身份和范围
                ├─ AuthorizedProvisioningPort —— OEM 新设备注册/证书
                ├─ AuthorizedPairingPort  —— 车辆确认与 PAK 状态
                ├─ SensitiveStorePort     —— 私钥不出域/分离保管
                └─ ConnectionLifecycle   —— BLE/后台服务状态
```

**隔离规则**：原有远程门锁、空调、Widget 不得调用数字钥匙研究接口；不能用 C3 状态显示“已配对”，不能因 BLE 已连接显示“可以启动车辆”。

## 4. 分阶段实施任务与明确交付

| 阶段 | 本阶段必须完成 | 验收标准 | 当前状态 |
| --- | --- | --- | --- |
| M0 证据基线 | 原 APK 文件指纹、DEX/Manifest 索引、PAK 与 CCC 分流、证据级别 | 可复查、无密钥入库 | 已完成 |
| M1 原型与权限 | 只读环境页、接口与状态机、Android 12+ 权限安全读取、编译 | CI Debug 成功；未授权时不崩溃、不调用车辆 | 已提交，待本轮 CI 终验 |
| M2 SDK 可用性 | 找到**正式授权**的 SDK 取得/许可方式、版本/ABI/依赖、支持地域和车型 | OEM 或合法分发方书面使用条件及技术兼容确认 | 未完成；关键外部依赖 |
| M3 账号与设备注册 | 将现有 China 会话通过最小授权接口映射到 OEM；新设备身份与证书按官方流程建立 | 非生产测试账号；OEM 返回正式成功；不导出密钥 | 未完成 |
| M4 车辆身份与配对 | 经车主本人和车端正常界面确认；完成配对注册 | 车端用户管理能看到新设备；失败时不影响已有钥匙 | 未完成 |
| M5 PAK 服务 | 合法的 PAK SDK 生命周期、恢复、无感靠近 | 正常模式、断网、锁屏、重启、蓝牙断连；真机验证 | 未完成 |
| M6 集成与交付 | UI 完整、回归、签名、权限、撤销与丢手机流程 | 实车验收矩阵全通过；可回滚 | 未完成 |

**关键路径：M2 → M3 → M4 → M5。** 在 OEM SDK 与新设备合法注册没有证据前，不开始实车密钥签发或认证层的非授权复刻。

## 5. M1：已经可以做的代码工作

代码入口：
- android-app/app/src/main/java/io/github/polestarcn/companion/digitalkey/
- android-app/app/src/main/AndroidManifest.xml
- android-app/app/src/main/java/io/github/polestarcn/companion/MainActivity.java
- digital-key-research/lab/
- .github/workflows/digital-key-research-build.yml

具体开发顺序：

1. 只在研究分支加入独立的“数字钥匙 · 环境检测”设置项，默认不运行任何蓝牙扫描。
2. 实现硬件、权限和系统服务探测；在 Android 12+ **先检查 BLUETOOTH_CONNECT 再读取 adapter 状态**。
3. 对用户点击的权限请求只申请“附近设备”，不自动要求后台位置或忽略电池优化；待实际 SDK 需求核准后调整。
4. 定义有阶段、有来源的数字钥匙状态；Lab 虚构的 PAK_PAIRED 永远不能变成可用钥匙证明。
5. 单测覆盖缺少蓝牙、权限拒绝、禁用蓝牙、系统杀后台、账号切换、恢复失败；编译为 Debug APK。
6. Root AVD 安装研究版并检测设置页是否可展示和正常申请权限。模拟器结果不作实车结论。

## 6. M2：SDK 取得与版本适配的现实问题

原 APK 内包含原厂使用的第三方/厂商私有 SDK 和原生库，但**这些二进制在用户取得 App 时不当然授予重分发许可**。正式工程要优先寻找 Polestar/Volvo VOCMO/PAK 授权渠道，取得接口文档、AAR/SO、最小 App 权限、ABI/Android 版本支持矩阵和隐私条款。

若拿到正式 SDK：
1. 在私人制品仓库/批准的构建环境提供 SDK，而非把 SO、AAR 和正式证书提交到公开 GitHub。
2. 先用最小独立测试 APK 验证 SDK 初始化、厂商授权与无凭证错误分支。
3. 对接 AuthorizedIdentityPort / AuthorizedProvisioningPort 的接口，不暴露刷新令牌至 UI 或诊断日志。
4. 编译和 UI 通过后，在本人车辆上按官方流程授权配对。
5. 保留 ResearchDigitalKeyProvider 作为无 SDK/不可用时的安全回退。

如果 OEM 无法提供合法接口，可继续开展静态互操作研究，但不得假设“有代码就可以独立签发车钥匙”。

## 7. M3：同现有 China 登录链的正确衔接

需要核实：
- 现有 China 登录是否确实提供**同一账号主体的合法用户标识**。
- VOCMO 所需 token scope / audience 是否与 C3/PCCS 远程车控一致（**目前未知**）。
- OEM 是否支持为新包名、新 App-Instance-ID 和新证书注册申请正式授权。
- 账户切换、设备撤销时是否能安全注销本项目自身身份，而不触及官方 App 已注册钥匙。

接口契约必须将“用户已登录”和“可合法申请数字钥匙”分开，例如：
```text
CHINA_SESSION_ACTIVE  !=  OEM_DEVICE_ENROLMENT_GRANTED
BLE_READY             !=  VEHICLE_PAIRED
PAK_PAIRED_OBSERVED   !=  VEHICLE_VERIFIED
```

## 8. M4/M5：配对及长期连接测试边界

车主在车辆上按官方流程确认新的设备，未授权前保持所有钥匙写入/配对接口关闭。真实密钥材料由 OEM SDK 在内存/安全存储内部处理，调试只采集阶段、错误类型和耗时；不得导出 KeyPair、CSR 私钥、车辆证书私钥、认证 challenge-response。

获得实车成功后再建设 Android connectedDevice 前台服务与通知；不得利用 Root 隐藏前台服务或突破 Android 后台限制。

## 9. 发布 / 回滚标准

- Debug 研究版与已安装的正常 App 在标识、签名或升级策略上隔离，不能直接替换已配好的正式钥匙 App。
- 正式发布必须经过 OEM 授权验证、用户确认流程、独立设备密钥、断线恢复、注销/撤销、丢失手机、离线/低电/异常电量场景和回归测试。
- 任何失败必须能**回到仅远程车控 + 官方数字钥匙并存**，不改写原车钥匙登记。
- 实车证据全部合格后才可标记 RELEASE_READY；GitHub CI 绿灯只说明代码构建成功。

## 10. 推荐验收次序（最少返工）

**先证实注册权，再投入 PAK 复刻。** 当前最大风险不是 UI 或蓝牙扫描，而是 OEM 后端是否认可新应用实例的设备身份、证书和车型授权。未经服务端认可时，深挖底层 BLE 也无法形成可靠、可维护的数字钥匙产品。
