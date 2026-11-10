# PsAssistant 1.2.8 / OPPO Bridge 2.7.0 — 双路径融合记录

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

状态：**合入** = 1.2.8 改动；**已有等效实现** = 继续沿用并保留；**待独立授权确认** = 不擅自改变安全边界；**不作为真实授权证据** = 不能用于宣称 OEM 认证成功。

| 用户提供代码/文件 | 当前落地和差异 |
|---|---|
| bridge-contract/CarLinkBridgeContract | 已有等效 v1、1001–1004 与本地 IPC 路径；保留既有协议，避免双版本冲突 |
| uploadedcarlink/CarLinkDexContract | 已有命名空间独立协议；保持已调试版本 |
| OppoCarLinkConfig | 保留真实 companyId/AUTH_CODE/typeCode 缺项验证；文档里的测试值仅适用于 Mock，不伪装真实凭证 |
| OppoCarLinkCommandMap | 已有官方指令映射与 OEM 1001–1004；保留 **不猜测缺失方向** 的严格分支 |
| OppoCarLinkRuntime | 已有真实 SDK callback 授权判定与逐操作设置；保留，不将 authResult/subscription 硬置 1 |
| OppoCarLinkProtocol | 合入过期请求触发一次真实刷新；未刷新完成拒发，刷新后用户重试仍重新核验 |
| VirtualVehicleSessionManager | 合入缓存可展示、真实在线/可控分别判定，不因 10 分钟过期直接隐藏卡片 |
| VehicleProtocolGateway | 保留实时状态安全门禁、精确 toggle 和车辆身份验证；过期不利用历史状态猜测解锁 |
| PsAssistantBridgeProvider | 保留包/UID 调用者验证、原始车辆命令与验证；没有扩大暴露接口 |
| VehicleCoordinator | 保留原始中国区账号 Session、VIN 绑定、命令发出及实车回读机制 |
| OppoBridgeModule | 已有 API 102 模块入口、Stable/New/Mock 分流；保留 |
| PsAssistantRuntimeHook | 已有独立运行状态探测、诊断及防越权校验；保留 |
| PsAssistantCarAppCatalogHook | 已有目录与应用包检测 Hook；包含签名/权限返回改写逻辑，**需独立确认是否从正式路径隔离** |
| PsAssistantVehicleHostHook | 已有绑定/命令 Hook；真实执行必须继续经过本应用已有验证与回读机制 |
| AssistantScreenNativeVehicleHook | 合入用户选择的 4 控件；New 已有自适应方法探测，Stable 仍精确识别；保留宿主不匹配时回退 |
| AdaptiveShelfShortcutInterceptor | 已有对本应用私有 Scheme 的安全拦截、刷新后控车及不乐观显示成功；保留 |
| QuickAppCarCapability | 已有精确版本 SHA 与多锚点功能识别；保留“未知结构不注入” |
| QuickAppRpkPatch / AdaptiveQuickAppRpkPatch | 已有精确和自适应两条入口；合入 New 默认结构匹配，保留用户自选模式和源数据回退 |
| QuickAppCarDataShim | OEM 4x2 已是 chesuo/kongtiao/shanguang/mingdi，与 08B 一致 |
| QuickAppCarDispatchShim | 已有短冷却/目标车辆检查/桥接发送；保留，不能将 accepted 视为车辆执行成功 |
| VehicleShortcutActivity & Manifest | 独立模块已公开受控 URI 入口；主 APK 集成版本为非导出。**修改主 APK 导出与外部访问策略需要独立确认** |

上传文本中个别“完整源码见前面”引用并未在当前附件提供完整方法体，不能覆盖现有可编译类。

## 授权与验收边界

官方 OCS 路径只在本机真正具备 OPPO 分配的 SDK、公司 ID、AUTH_CODE、车型信息、实际成功的 SDK 鉴权回调时标记为 AUTHORIZED；本地车辆卡片展示只表示 LOCAL_BRIDGE/PRESENT，不等于 OPPO 认证车辆。登录 Polestar 中国区不代表 OPPO 的合作方应用认证。

Mock 模式仍只用于 Dry-run；任何成功提示分开“触发按钮、桥接受理、后台接受、车辆回读确认”，未回读只显示待确认。

当前 **只提交上述确定选项到功能分支，不合并主分支**。下面未答复事项必须由用户确认：
1. 是否允许将现有权限/签名强制返回类 Hook 从 **正式双路径** 中隔离，仅作为隔离 Mock/调试逻辑分析保留？
2. 是否同意测试后将此分支合并 main，并继续使用既有私有登录资源与原签名打包 FULL（不提交密钥/个人信息到公开仓库）？
3. PsAssistant 是否拥有 OPPO 官方签发并匹配包名/签名的 CarLink SDK 接入资格？没有也可测试本地桥接路径，但不能宣布官方授权成功。

## 本轮自动化测试

- 独立验证官方、本地状态不互相伪造
- 车况缓存过期只读展示、不能下发指令
- AssistantScreen 四按钮顺序与动作
- New/Stable/Mock 兼容切换、RPK 用户自选优先
- 原有账号登录、车控鉴权、Widget、Bridge 回归测试不得退化

所有试运行 APK 标记为 VALIDATION；只有原签名 FULL 经过构建、兼容资源及签名检查后才可按 FULL 交付。
