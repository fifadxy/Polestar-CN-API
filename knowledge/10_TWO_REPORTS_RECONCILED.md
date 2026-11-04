# 10｜2026-10-26 两份 APK 报告交叉核验与融合决策

**对象**：第三方 `app-release.apk.1.1(1).1` (`app.c3garage.companion`, v0.1.0)。

**报告来源**（上传文本只是分析材料，不能代替二进制或运行时证据）：

- **分析 A**：`粘贴的文本 (1)(2).txt`，标题「极星助手 APK 完整技术与安全评估」，约 300 行。
- **分析 B**：`粘贴的文本 (2).txt`，DEX 方法级拆解、MASVS、依赖及代码改进建议，约 560 行。
- **Gemini 分享入口**：`https://share.gemini.google/nc6E1udW4EzB`；2026-10-26 尝试直接访问与搜索，**未能读取到分享正文，未把其未见内容作为依据**。如该网址指向上述报告之一，也不能自动视作另一个独立证据源。
- **原始二进制**：当前对话上传 APK；核验脚本 `scripts/inspect_apk.py`、`scripts/inspect_vehicle_schema.py`、`scripts/verify_apk_v2.py`。自动提取结果见 `knowledge/evidence/`。
- **项目先前运行验证**：`docs/HISINGEN_GLOBAL_CN_API_COMPARISON.md`，是官方中国 App 的既有研究，不等同于这份第三方 APK 的运行验证。

## 1. 证据级别

| 标志 | 含义 | 能否判定原 APK 在实车可用 |
| --- | --- | --- |
| `APK_CONFIRMED` | 直接核对 APK 字节、DEX 字段/类、Manifest、签名等 | 否；只证明静态存在 |
| `EXTERNAL_ANALYSIS` | 报告 A/B 声称已读方法控制流，当前未逐条独立复核 | 否 |
| `PROJECT_DOC` | 先前 Global/China 上游 API 研究结果 | 仅限相应研究已验证的链路 |
| `NEW_PROJECT_CODE` | 新 Android 分支中的源码、纯 Java 测试或 CI | 不能证明远端 API 可用 |
| `REAL_DEVICE_VERIFIED` | 真实账号、明确授权的测试车辆、时间戳、响应证据形成闭环 | 可以，仅限记录的车型/区域/操作 |

## 2. 结论逐项对照

| 主题 | 分析 A | 分析 B | 本次最终结论 / 新项目动作 |
| --- | --- | --- | --- |
| 包名/版本/SDK | 0.1.0；API 26/34/35 | 同意 | **APK_CONFIRMED**；维持原样本元数据，不代表新工程 target 必须永久为 34 |
| 签名 | v2 RSA-3072，证书 SHA-256 | 未独立给出额外签名证据 | **APK_CONFIRMED**：已独立执行 RSA 签名与内容摘要核验；签名不证明发布者身份 |
| 主业务类 | 中国登录/车辆/Widget/后台 | 7 个 Java 核心源文件 | **APK_CONFIRMED**：7 个；不是此前第三份报告误记的 6 个 |
| Vehicle 字段 | 有电池/车况等模型 | 称 **30 个字段** | **纠正**：DEX `PolestarApi$Vehicle` 字段表为 **33** 个，见 `11_VEHICLE_DATA_DICTIONARY.md` 和 JSON |
| 认证 | 手机号密码 + Refresh | `login`, `signedUrl`, `refreshIfNeeded`, session lock | 方法及字段静态存在；具体请求签名/令牌 audience 与实际登录可用性需另行核验 |
| Keystore/AES-GCM | 声称受 AES-GCM 保护 | 指出 `secretKey`、`KEY_ALIAS` | 已见密钥管理字段与方法；**完整加密调用链不在本次逐条复核范围**，保留 `EXTERNAL_ANALYSIS` |
| C3 | 车辆状态、远控 RPC | HTTP JSON + gRPC + 自行序列化 Protobuf | 相关依赖/类/方法 **APK_CONFIRMED**；请求字段编号与响应结构尚未完整解析 |
| 远控 | 已存在调用逻辑 | 可能有错误后状态回读机制 | 结构存在不等于已在特定车型成功；新项目保持 **DISABLED**；只预建状态语义枚举 |
| gRPC Live | 原 APK 未发现持续 Live | 未证实该第三方 APK 的持续流 | **不能把项目官方 China App 的 Live 证据嫁接到此 APK**；新工程 Live 为 `MAPPING_REQUIRED` |
| WorkManager | 推测约 30min 轮询 | `CampModeWorker` immediate/periodic | 后台类与 WorkManager 依赖存在；**具体任务间隔/可靠性**应补运行校验，不按定时任务保证人宠安全 |
| 小组件 | 支持状态和快捷操作 | Configure/Provider/Control/Refresh | **APK_CONFIRMED** 组件存在；导出组件是否可被利用待后期授权测试 |
| 硬编码签名材料 | 声称固定盐 + MD5 | 同意静态 `SIGN_SALT` | `SIGN_SALT`, `signedUrl` 符号已确认；材料值、算法实际使用链与服务端鉴权强度勿直接等同；不复制到 Git |
| 生物识别 | `BiometricPrompt` 回调、无 CryptoObject | 声称可 Hook 绕过 | 作为**后期安全评估项**；不能因为使用/不使用 CryptoObject 就推断已证实攻击可行或完全杜绝 Hook |
| 快照存储 | 评价为“加密存储” | 评价为“可能明文泄露” | 两份结论**存在张力**；不在未验证前断言所有快照均加密或明文；记录为待查 `storeVehicleSnapshot` |
| gRPC 每请求新建 Channel | 声称反复新建 | 讨论高连接开销 | 性能改善有价值，但应跟踪真实调用频率/耗时再决定连接池生命周期 |
| 大图优化 | 无特别比例 | 指称内存峰值降低 40% | `res/Lp.png` 尺寸和体积可查；**40% 没有基准测试支持** |
| 包体瘦身 | 比较保守 | 预计减少 70~85% DEX | **无实测依据**；首版本来未引入原 APK 的 gRPC/Guava/Room，不能把原包预测当新工程 KPI |
| SSL Pinning | 建议加入 | 将缺失视为中间人风险 | 常规 TLS 校验不等于不安全；是否 Pinning 应独立权衡证书轮换风险，本阶段不实施 |
| 发布版 R8 | 建议启用 | 给出 ProGuard 规则 | **明确延期**；不粘贴 `-dontwarn` 通配规则，不启用 `minifyEnabled` |
| Android 16 Play 适配 | API 36 | 示例设置 API 35 | 官方政策确认：2026-08-31 起普通手机新应用/更新需目标 API 36，侧载测试不等同上架；此项为未来发布里程碑 |

### 特别勘误

1. **“未申请手机位置权限”不意味着无法读到车辆位置**。车辆位置可能由车云端返回，需要单独识别权限与业务用途。
2. **“命令 API 接受”不等于“车辆执行”**。异步远控需要独立的实际状态回读或回执判定，网络超时应标 `UNCONFIRMED`，不能自动重复危险写入。
3. **“Keystore 有方法”不等于每条车控操作使用密码学绑定**；“没有 SSL Pinning”也不能单独证明已遭中间人攻击。
4. **原 APK 名字可读不等于能从 APK 恢复原始 `build.gradle`**；具体构建选项只能作为推断。
5. **不将第三方客户端的静态接口当成官方开放 API**，更不保证其可长期调用。

## 3. 本次执行的融合

**工程本阶段落实（功能与数据正确性，不是安全加固）：**

- `VehicleSnapshot` 演示样本使用 `timestampMillis=0`：不伪造最新实车采集时间。
- 新增纯 Java `TelemetryStatus`，区分 DEMO / LIVE / CACHED / STALE / UNKNOWN；先实现显示语义，真实数据源尚未接入。
- 新增纯 Java `OperationState`，区分 NOT_SENT / SENDING / SERVER_ACCEPTED / VEHICLE_CONFIRMED / UNCONFIRMED / FAILED / DISABLED；所有状态目前仅设计与测试，**不产生远程操作**。
- `ModelStateTest` 自动检验「RPC 接受不等于车辆成功」「不自动重试命令」「过期数据不得标 LIVE」。
- 补充 33 字段数据字典、验证矩阵、真实 API 跨区域隔离原则。
- 修正 GitHub Actions Android SDK 安装路径，构建状态以实际 GitHub run 为准。

**仅保留规划，不在本阶段运行：** R8 混淆、ProGuard keep、密码学认证加固、签名盐处理、Pinning、资产保护、防调试、后台自动远控、真实车辆指令。

## 4. 在线权威参考（只用于核对平台事实）

- Android 官方 `PeriodicWorkRequest`：`https://developer.android.com/reference/androidx/work/PeriodicWorkRequest`（最小 15 分钟，Doze / 电源优化导致时序不保证）。
- Android 官方 Widget configure：`https://developer.android.com/reference/android/appwidget/AppWidgetManager`（配置 Activity 与 `EXTRA_APPWIDGET_ID`）。
- Google Play Target API：`https://support.google.com/googleplay/android-developer/answer/11926878`（普通手机 2026-08-31 起 API 36；见最新延期条款）。

下一轮可获取完整方法级 smali/反编译流程与脱敏运行日志后，把 `EXTERNAL_ANALYSIS` 逐条提升或驳回；不要一次性把报告全部标为“已验证”。
