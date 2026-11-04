# PsAssistant beta38 双路径协议融合

版本：主程序 `0.9.7-beta38-protocol /152`，内置与独立模块 `2.1.0 /34`。基于 beta37 UI 分支。输入：用户上传的九个 Java 类及 Manifest 片段。

## 文件适配

| 输入文件 | 工程中的整合方式 |
|---|---|
| CarLinkDexContract | 原同名类是 DEX 声明扫描器，保留；新共享 `bridge-contract/CarLinkBridgeContract` 同时编入主 APK、独立模块 |
| OppoCarLinkCommandMap | 保留官方 InstructionType 映射，新增精确字符串及本机 1001–1004 / 1–4 事件映射；方向必须明确 |
| OppoOfficialVehicleBindState | 复用真实登录、选车和已缓存车辆判断；本机绑定与 OCS 官方授权分别报告 |
| VirtualVehicleSessionManager | 复用真实缓存和稳定合成标识，未知数值不补造 82%/415km，不乐观覆盖已确认车况 |
| VehicleProtocolGateway | 保留类型明确的 VehicleCoordinator，统一现有鉴权与有界回读；不反射猜方法，不以失败兜底成功 |
| PsAssistantBridgeProvider | 新增三个 call 别名、绑定 Cursor 查询、列投影和变更通知，保留已有所有入口及调用方限制 |
| OppoOfficialCarLink | 保留现有 OCS 发现、初始化入口；新 `OppoCarLinkBridgeService` 实现明确定义的 Messenger 协议，未冒充未知 AIDL |
| OppoCarLinkInitReceiver | 原有受保护 OCS 初始化接收器不覆盖；另增受 IOT 权限保护的显式命令接收器 |
| OppoCarLinkBridgeModule | 不并装旧 IXposedHookLoadPackage；适配当前 libxposed API102，由现有模块/BridgeClient 调用共享协议，四类指令实际接入 |

## 两条可测试路径

- Provider：`content://io.github.polestarcn.companion.bridge/auth/checkCarBind/hasCarBinding` 查询；call `getVirtualSession`、`getVehicleSnapshot`、`dispatchCarCommand`。
- Service：绑定 `io.github.polestarcn.companion.OppoCarLinkBridgeService`，使用 Android Messenger。Message Bundle 的 `method` 为上述方法；`sendingUid` 必须是主程序、独立模块或已识别 CarLink 宿主；Service 本身还要求 IOT 权限。上传文档里的 CarLink Action 字符串作为兼容发现别名，并不证明 OEM 会自动调用。
- 广播仅作手工显式兼容通道，组件 `OppoCarLinkCommandReceiver`、Action `io.github.polestarcn.companion.action.CARLINK_COMMAND`，同样受平台 IOT 权限保护。模块不自动广播重试，避免重复写车。

命令 Bundle：`sdk_cmd_name`（lock/unlock/climate_on/climate_off/window_close/window_vent/find_car 等精确别名），或 `car_event_id` + Boolean `action_target_state`。必须使用稳定 `request_id`；可提供 `result_receiver` 接受异步状态。1001/1002/1003 的 true 分别是锁车/开启空调/关闭车窗；false 为反向；1004 寻车。它们是本机协议编号，不是已证实的 OEM 事件编号。请求温度字段目前明确拒绝，不能无声忽略。

受理先返回 queued，`exec_success=false`。需要验证时通过回调返回一次性授权 PendingIntent；模块打开该入口。取消、超时、换车均返回未发送。只有既有车端回读确认才返回 `exec_success=true / vehicleConfirmed=true`；鸣笛/闪灯仍需现场确认。相同请求 ID 不重复执行，改动作或换车辆复用 ID 则拒绝；队列上限128、有效期120秒。所有真实命令复用中国区已有接口。

## 为什么没有直接复制原代码

原文默认强制绑定、默认虚构 VIN/车况、网络调用全失败仍返回 true，且用任意 Binder transaction 编号代表车控。模块另有将任意 UID 的 IOT 检查置成功、按任意 SQL 中 car/scene 字样拼行的逻辑；这些既没有匹配当前固件签名/表结构，也无法保证正常运行。工程融合使用当前已识别宿主与现有路由，不把这些猜测包装成成功的官方绑定。

主 APK 与模块都不含新申请的 OPPO AUTH_CODE/companyId，也未证明云端车型白名单放行。Root 本机面板沿用 beta36 实现；此版本不能据此宣称官方鉴权、手表、数字钥匙全链路已打通。

## 真机安装与使用

1. 用相同签名 FULL APK 覆盖安装，保持账号与车辆选择。主 APK 已内置模块，通常无需再装独立 Bridge。
2. 在 LSPosed 中选择主 APK 模块，启用原有目标作用域；不要同时启用主 APK 和独立 Bridge 的同类 Hook。更改后重启相应系统宿主或手机。
3. 主程序登录、选车、手工刷新一次。设置→系统车辆接入→检查并配置接入，会执行只读绑定 URI / Provider 协议和四类映射自检，不发送车控。
4. 测试负一屏、设备空间、CarLink 本机面板：选择一个动作，按原设置完成验证，核对操作后的车况回读。重发需要新 request_id；同一个请求跨通道投递仍只执行一次。
5. 失败时复制该页文字诊断或导出≤2MB ZIP。IOT 权限、官方授权、宿主挂载、车辆回读分别核对，不能互相推定。

没有连接的 ADB 真机时，只能验证编译、模拟设备回归与 APK 签名，不能声称已在用户手机安装或运行 Hook。本次实际构建结果在交付验证段追加。
