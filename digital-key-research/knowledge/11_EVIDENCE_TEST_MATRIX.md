# 11｜移植证据、测试矩阵与 Go/No-Go 规则

## 一、证据等级，不允许越级

| 等级 | 证据来源 | 可以声称 | 不可以声称 |
| --- | --- | --- | --- |
| STATIC | 用户上传的 4.25.0 APK/DEX/Manifest | 组件、方法、部分静态调用关系存在 | 该调用一定在实车成功 |
| LAB | 纯 Java 单测、模拟状态机 | 代码门控、拒绝错误状态 | OEM SDK 工作 |
| AVD | Android Emulator 实际安装运行 | Android 权限、BLE API、Activity/Service 生命周期 | 车辆认同钥匙 |
| OEM | 取得合法 SDK/测试资格，厂商认可新设备身份 | 服务端许可和注册可行 | 真实车辆完成配对 |
| VEHICLE | 本人授权的实车正常操作、车端确认 | 具体车型/车机版本/手机的可用性 | 所有车型通用 |
| RELEASE | 安全、可靠性和回归矩阵通过 | 本项目对应版本可发布 | 未来永久有效 |

任何 UI 文案“钥匙已配对”、“可启动车辆”、“已解锁”都必须有真实来源，不可由开发模拟阶段、云端 HTTP 200、BLE GATT connected 自动推出。

## 二、APK 静态证据索引（不包含私钥或网络载荷）

| ID | 客户端证据 | 观察 | 置信度 |
| --- | --- | --- | --- |
| S01 | Ps2PairingFragment / PakPairViewModel | 极星 2 专属配对界面/控制模型 | 高：类定义存在 |
| S02 | ICupDigitalKeyController → PhoneAsKey | App 层与 SDK 管理层存在调用 | 高：引用存在 |
| S03 | PhoneAsKeyManager → P319 PAKService | SDK 桥接底层 P319 服务 | 高：调用引用 |
| S04 | Vocmo.initialize(...PhoneAsKeyRepository) | PAK repository 是初始化注入项 | 中高：签名 |
| S05 | Authentication.getToken / AuthInterceptor | VOCMO 请求通过认证接口取 token | 中高：调用引用 |
| S06 | EntryPointLocatorImpl / UUID.randomUUID | SDK App-Instance-ID 初始生成 | 中高：方法体调用 |
| S07 | Configurator + UCC/USEC enrolment | 存在设备证书注册流程 | 中高：静态流程 |
| S08 | PairingSession ECDHE + Actor/Near-Field CSR | 存在新身份与证书交换路径 | 中高：静态流程 |
| S09 | CatalogRepository / CA-CAT-030/031 | 存在目录注册的客户端路径 | 中高：静态流程 |
| S10 | PassiveEntry → ConnectionInfo → Vehicle.pair | 后续本地 PAK 初始化与配对 | 中高：静态流程 |
| S11 | 未发现宿主 APK 签名检查 | 有限扫描未见特定 API | **低：否定性证据** |
| S12 | ChinaPolestarApi.Session 不含 user identifier | 当前自研应用没有已验证身份映射 | 高：源代码明示 |

注意：S11 **不证明**无服务器白名单、SDK 许可、账号权限或移动平台安全策略。

## 三、最少测试案例

| 测试组 | 具体操作 | 期望结果 | 当前状态 |
| --- | --- | --- | --- |
| UNIT-01 | 模拟跳过证书阶段 | 状态机拒绝越级 | 已有 Lab 测试 |
| UNIT-02 | 模拟 PAK_PAIRED，无实车 proof | 不得宣称数字钥匙可用 | 本轮修复 |
| UNIT-03 | ResearchDigitalKeyProvider | 永远 isOperational=false | 设计成立，待单测 |
| ANDROID-01 | 未授 Nearby Devices 启动检查页 | 不抛 SecurityException | 待 AVD 新版 |
| ANDROID-02 | 用户手动批准/拒绝权限 | 正确更新且不扫描车辆 | 待 AVD 新版 |
| ANDROID-03 | 蓝牙 OFF / BLE 无硬件 | 显示状态，钥匙不可用 | 待 AVD/真机 |
| ANDROID-04 | 设备旋转/系统放大字体 | 无文字重叠与卡死 | 待 AVD |
| ANDROID-05 | 程序被系统杀掉、重启、撤销权限 | 不会恢复虚假的钥匙状态 | 待 AVD |
| ACCOUNT-01 | 普通 China 远程车控会话 | 不声称具备 OEM PAK 资格 | 已有架构约束 |
| OEM-01 | 使用有权集成的 OEM SDK | 合法初始化及注册获授权 | 未具备前置 |
| OEM-02 | 使用独立的新 App 身份 | OEM 正式接受/拒绝，结果可审计 | 未具备前置 |
| VEHICLE-01 | 车主本人确认新增设备 | 车端正常认可，原钥匙不受影响 | 未测试 |
| VEHICLE-02 | 断网、锁屏、蓝牙关开、失效及撤销 | 工作与失败状态准确 | 未测试 |
| RELEASE-01 | 安卓 12–16/目标设备版本回归 | 兼容性、后台、电池行为合格 | 未测试 |

## 四、必须记录的日志（只记录无敏感阶段）

建议收集：
- Android API、设备 ABI、BLE feature、权限布尔值、App build SHA；
- 是否成功初始化**获授权**的 SDK、阶段名、失败错误类型、持续时间；
- 是否进入授权用户确认页面（YES/NO）；
- 是否取得已验证的车辆确认（YES/NO）。

禁止记录：姓名、手机号、VIN、IMEI、App-Instance-ID 的真实值、token、CSR/私钥/对称密钥、证书原始字节、BLE 认证报文、位置。

## 五、Go/No-Go 判定

**Go-M1（当下）**：只读诊断、状态机、防误报、Android 兼容性和公开 SDK 接口研究。

**Go-M2（有前提）**：取得合法 SDK、版本兼容说明、明确的官方注册权限；仅运行正常授权设备注册。

**No-Go**：
- 试图通过原 App 已配对数据克隆钥匙；
- 不明来源的生产证书或 secret 被写入仓库；
- 试图绕过服务端拒绝、车辆授权或 App 签名校验；
- 未能在本人授权实车验证，却把“蓝牙连接成功”写作“钥匙可用”。

## 六、现有 CI

- Android 研究 Build workflow：.github/workflows/digital-key-research-build.yml
- Android 14 Root AVD workflow：.github/workflows/digital-key-emulator.yml
- Java Lab：digital-key-research/scripts/run_lab_tests.sh

GitHub PR #2 用于隔离测试，不是合并审批；研究完成前不要把它直接合入 Android 主线。
