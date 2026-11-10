# PsAssistant 1.2.8 / OPPO Bridge 2.7.0 — 双路径融合记录

## 最终交付（已合入 main）

- 集成源码 SHA：`b140856099ff260b38a4c39ae576ff5efebd4213`，已快进合入 `main`。
- [Android Companion APK 工作流](https://github.com/fifadxy/Polestar-CN-API/actions/runs/42793497363)：成功。
- [Android 1.2.8 Stable Regression](https://github.com/fifadxy/Polestar-CN-API/actions/runs/42793497393)：成功，JUnit 汇总 **654 测试 / 0 失败 / 0 错误**。
- 1.2.8 / VersionCode 173 的离线签名 FULL：`PsAssistant-v1.2.8-CN-Standalone-FULL-MD.apk`，大小 **9,686,620 bytes**。
- APK SHA-256：`a27e392dcba3093ca076c61c732f982a9107da383e25ae03f1b3589bca7e6b35`。
- 与历史 FULL 相同的签名证书 SHA-256：`d4cd1b06701f9a8829c5ecc5bbee1398a0f8a655703b6cc356398efa30218a7e`。
- APK 签名 v2/v3、Manifest 版本、`debuggable=false`、原始 DEX/resource 一致性和独立登录兼容资源均已通过离线验证。
- 独立 OPPO Bridge 模块仍保留，版本 2.7.0 / 40，包含 LibXposed API 102 元数据和用户选定的三模式。
- FULL 和原签名密钥只在私有交付中使用，不写入 GitHub 公共仓库。
- **验收边界：** 仅代表离线包内容和自动化测试通过；Polestar 中国区账号登录、ColorOS 17 上的 Hook 激活、OPPO 官方商业授权及实车控制未在该构建环境中实测。



日期：2026-11-10；基线：主线 1.2.7 / Bridge 2.6.0；代码工作分支：`feature/psassistant-128-oppo-dualpath-confirmed-20261110`。

## 用户确认的全部 9 项（严格原样）

| 编号 | 确认 | 落地 |
|---|---|---|
| 01 | C | 双路径并行：官方 OCS 实际回调状态和本地 PsAssistant 车控桥接状态严格分开 |
| 02 | A | 保留主 APK 集成 Bridge 的现有构建，并保留独立 Bridge 测试安装包 |
| 03 | A | 保留 Stable / New / Mock 三套模式与已有显式切换 |
| 04 | A | New 采用结构+接口特征自适应扫描；不对未知宿主强行 Hook |
| 05 | A | New 默认结构精确匹配及失败回退原数据；Stable 保留精确 SHA；用户保存的选择优先 |
| 06 | A | 车辆过期可展示历史缓存，但命令必须刷新并重新校验真实车辆状态 |
| 07 | B | 完整沿用 PsAssistant 逐动作生物识别/直接执行设置，绝不改成全部自动直控 |
| 08 | B | 四按钮仅车锁、空调、闪灯、鸣笛；闪灯/鸣笛为瞬时动作 |
| 09 | A | 主程序 1.2.8 / versionCode 173；Bridge 2.7.0 / versionCode 40 |

## 与用户上传 MD 的逐项源代码处理记录

状态：**合入** = 1.2.8 改动；**已有等效实现** = 继续沿用并保留；**仅测试** = 不能作为 OEM 授权成功证据。用户已要求直接按 MD 实施，不再设置额外的审批轮次。

| 用户提供代码/文件 | 当前落地和差异 |
|---|---|
| bridge-contract/CarLinkBridgeContract | 已有等效 v1、1001–1004 与本地 IPC 路径；保留既有协议，避免双版本冲突 |
| uploadedcarlink/CarLinkDexContract | 已有命名空间独立协议；保持已调试版本 |
| OppoCarLinkConfig | 合入 MD 的 Polestar 2 车型编码与图片 URL 默认项；保留真实 companyId/AUTH_CODE 缺项验证，测试凭证只能走 Mock |
| OppoCarLinkCommandMap | 合入 MD 四个原生快捷按钮 ID、动作别名、1001–1004 与 SDK 指令映射；缺失方向时不默认解锁 |
| OppoCarLinkRuntime | 已有真实 SDK callback 授权判定与逐操作设置；保留，不将 authResult/subscription 硬置 1 |
| OppoCarLinkProtocol | 合入过期请求触发一次真实刷新；未刷新完成拒发，刷新后用户重试仍重新核验 |
| VirtualVehicleSessionManager | 合入缓存可展示、真实在线/可控分别判定，不因 10 分钟过期直接隐藏卡片 |
| VehicleProtocolGateway | 保留实时状态安全门禁、精确 toggle 和车辆身份验证；过期不利用历史状态猜测解锁 |
| PsAssistantBridgeProvider | 保留包/UID 调用者验证、原始车辆命令与验证；过期时仅按用户点击申请刷新并返回未受理，不补发指令；没有扩大暴露接口 |
| VehicleCoordinator | 合入后台冷启动时从加密车辆选择直接恢复 selected（即使快照不存在）；保留中国区 Session、VIN 绑定与实车回读 |
| OppoBridgeModule | 已有 API 102 模块入口、Stable/New/Mock 分流；保留 |
| PsAssistantRuntimeHook | 已有独立运行状态探测、诊断及防越权校验；保留 |
| PsAssistantCarAppCatalogHook | 保留历史目录与应用包兼容 Hook，尚未增加任何官方授权成功标志；实际权限/签名授权不可用本地测试结果替代 |
| PsAssistantVehicleHostHook | 已有绑定/命令 Hook；真实执行必须继续经过本应用已有验证与回读机制 |
| AssistantScreenNativeVehicleHook | 合入用户选择的 4 控件；New 已有自适应方法探测，Stable 仍精确识别；保留宿主不匹配时回退 |
| AdaptiveShelfShortcutInterceptor | 已有对本应用私有 Scheme 的安全拦截、刷新后控车及不乐观显示成功；保留 |
| QuickAppCarCapability | 合入 New 模式结构特征识别标识，与 Stable 固定 SHA 识别分别处理；多锚点验证失败时仍不注入 |
| QuickAppRpkPatch / AdaptiveQuickAppRpkPatch | 去除阻断 RPK 注入的默认关闭实验标记；改用用户模块开关及当前车辆会话。Stable 仍精确 SHA，New 结构校验及失败回退 |
| QuickAppCarDataShim | 去除默认关闭的实验标记，按 Stable/New 模式与实际 RPK 能力门禁展示四按钮 chesuo/kongtiao/shanguang/mingdi |
| QuickAppCarDispatchShim | 已有短冷却/目标车辆检查/桥接发送；保留，不能将 accepted 视为车辆执行成功 |
| VehicleShortcutActivity & Manifest | 保留独立 Bridge 的受控 URI 入口；主 APK 已有导出的 OppoShortcutConfirmActivity，新增 BROWSABLE 供外部 psassistant-companion URI 拉起，并保留用户明确确认和授权门禁。未额外导出可直接执行的内部 Activity |

上传文本中个别“完整源码见前面”引用并未在当前附件提供完整方法体，不能覆盖现有可编译类。

## 授权与验收边界

官方 OCS 路径只在本机真正具备 OPPO 分配的 SDK、公司 ID、AUTH_CODE、车型信息、实际成功的 SDK 鉴权回调时标记为 AUTHORIZED；本地车辆卡片展示只表示 LOCAL_BRIDGE/PRESENT，不等于 OPPO 认证车辆。登录 Polestar 中国区不代表 OPPO 的合作方应用认证。

Mock 模式仍只用于 Dry-run；任何成功提示分开“触发按钮、桥接受理、后台接受、车辆回读确认”，未回读只显示待确认。

用户已明确要求“按照提供的 MD 逻辑处理，不要再问这些”。处理准则：九项已选配置全部实施，测试通过后按既有要求更新 main 并交付原签名 FULL；不再重复征询。OPPO 正式凭据不在源码中伪造，缺少商业 SDK 或授权回调时据实报告，继续支持合法 PsAssistant 登录及本地桥接。

## 2026-11-10 MD 六项阻断点落地补充

| MD 根因 | 本轮实现状态 |
|---|---|
| 5/10 分钟缓存过期门禁 | 已改为**可展示缓存但仅新鲜数据可控车**；真实按钮点击触发 `BridgeDemandRefresh` 一次完整读取（冷启动含车辆发现），20 秒去重，绝不静默补发历史命令 |
| 后台无法弹出用户验证页 | 主程序保持既有逐操作授权模式；公开 `OppoShortcutConfirmActivity` 已补 BROWSABLE URI，点击后前台显式确认；不绕过系统 BAL |
| OCS 配置缺项 | 车型默认 `POLESTAR_2` 和 MD 提供的 Polestar 2 图片 URL；官方 companyId/AUTH_CODE 仍需要正式配置，诊断不显示假成功 |
| AssistantScreen 版本锁/URI 断链 | New 复用结构+接口类型匹配；Stable 保留精确版本；四按钮按已确认的车锁/空调/闪灯/鸣笛；URI 走确认页 |
| MyDevices 隐藏实验开关/RPK SHA | 删除默认 OFF 对卡片补丁的硬阻断；New 用唯一结构锚点验证，Stable 保留固定 SHA；RPK 判定与卡片显示使用同一能力标志 |
| Provider 冷启动 selected 为空 | 从加密选择恢复车辆；若没有有效选择则主动刷新/发现，必须重新验证当前车与命令权限后由用户重试 |

源文档有两类不能直接作为真车成功依据的语句：以固定测试公司 ID、AUTH_CODE 假装 OPPO 正式签发；把 `authResult`/`subscribeResult` 强制置 1 或未知锁态反向解锁。它们没有进入真实认证分支。MD 中「完整源码见上文」的部分在附件中没有对应方法实现，则继续沿用项目里已可编译、已验证的真实实现。

以上为实际差异记录，不是省略代码或重复提出确认选项。

## 本轮构建及候选安装包校验（代码提交 ef510b39）

- [Android Companion APK](https://github.com/fifadxy/Polestar-CN-API/actions/runs/42788411155)：通过
- [Android 1.2.8 Stable Regression](https://github.com/fifadxy/Polestar-CN-API/actions/runs/42788410978)：通过
- 私有离线候选 FULL：PsAssistant-v1.2.8-CN-Standalone-FULL-candidate.apk（未上传 GitHub）
- 版本 1.2.8 / code 173，包大小 9,686,619 bytes
- SHA-256 `dc7ab40bdf70048073f39d20d555190256d8b4451017a3970f2b36ac0b288b3b`
- 历史 FULL 证书 SHA-256 `d4cd1b06701f9a8829c5ecc5bbee1398a0f8a655703b6cc356398efa30218a7e`，APK v2/v3 已验证
- 独立中国区登录兼容资源经过校验并内置，且未写入 GitHub。此处仅表示安装包配置完整，不代表已实机登录成功。
- 本分支公共 CI 不包含 OPPO 商用 SDK AAR 与正式 companyId/AUTH_CODE，因此该候选包 **不能标记为 OPPO 官方认证或已完成实车远控验证**。
- 代码、资源与已测试的 VALIDATION APK 一致；仅非调试 Manifest、原签名与兼容资源作离线封装变更。

## 本轮自动化测试

- 独立验证官方、本地状态不互相伪造
- 车况缓存过期只读展示、不能下发指令
- AssistantScreen 四按钮顺序与动作
- New/Stable/Mock 兼容切换、RPK 用户自选优先
- 原有账号登录、车控鉴权、Widget、Bridge 回归测试不得退化

所有试运行 APK 标记为 VALIDATION；只有原签名 FULL 经过构建、兼容资源及签名检查后才可按 FULL 交付。
