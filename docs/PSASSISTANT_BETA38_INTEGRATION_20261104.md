# PsAssistant beta38：按上传原逻辑融合

主程序 `0.9.7-beta38-protocol /152`，内置/独立模块 `2.1.0 /34`，保留 beta37 全部 UI。用户明确要求仅做工程适配，绑定 URI、会话与车况、四类事件的业务逻辑不得擅改。本次以此要求为准，取代本分支最初采用的严格返回值版本。

## 原文与一致性

上传原文存于 `knowledge/incoming/CARLINK_DUAL_ROUTE_USER_SOURCE_20261104.txt`。`scripts/verify_uploaded_carlink.py` 比较24个核心方法的完整方法体，涵盖绑定判断、两种事件解析、Session构造、乐观更新、默认值、回调、失败成功兜底、Provider call/query、Service Binder及八个模块Hook方法；要求与上传原文一致。

保留原有行为：

- `hasCarBinding(null)` 返回true，`force_mock_bind_enabled` 默认true，未能读取选车时保留原文 VIN 回退值。
- 缺少方向时默认true；保留原文子串映射，包括 air→空调、honk/flash→寻车组合事件。
- 缺少车况时保留82% /415km及原有锁车、空调等默认值；保留乐观状态更新和持久化顺序。
- `dispatchCarCommand` 立即回执的 `exec_success=true` / `exec_code=0`、实际调用全部失败后的成功兜底均保留；未知指令的异步失败返回也按原文处理。
- Provider的原列结构和投影处理保持原样；Service保留原文 Universal Binder事务逻辑；模块保留原权限/签名、目录查询、会话和四事件Hook业务逻辑。

这些兼容返回按用户要求保留，不自动等同于实际车辆执行。主程序原有 `VehicleCoordinator` 继续独立记录真实发送与回读；诊断额外标出 `uploadedLogic` / `realVehicleConfirmed`，不改上传的已有返回键值。

## 仅做的工程适配

| 输入文件 | 融合位置与适配 |
|---|---|
| CarLinkDexContract | 放入共享源目录，包名为 `companion.uploadedcarlink`；主程序和模块编入同一份；原同名DEX扫描器不覆盖 |
| OppoCarLinkCommandMap | 上传原文编译于 `companion.uploadedcarlink`，保留业务方法；主程序旧官方指令映射不覆盖 |
| OppoOfficialVehicleBindState | 保留原判断；读取当前VIN时补接现有选车接口，读取不到仍走原回退 |
| VirtualVehicleSessionManager | 保留原默认/持久化/乐观更新；通过类型适配器读取当前工程的缓存字段 |
| VehicleProtocolGateway | 保留原分发顺序、回调和成功兜底；把反射猜测Coordinator入口替换为当前实际命令队列调用 |
| PsAssistantBridgeProvider | 上传Provider作为现有Provider的内嵌委托，使用同一个`.bridge` authority，不重复注册；启用系统接入后路由上传方法/查询；现有调用方边界和其他接口不覆盖 |
| OppoOfficialCarLink | 原Service编入独立命名空间，保留上传Action与Binder；现有OCS发现类不覆盖 |
| OppoCarLinkInitReceiver | 上传原文保留；现有受保护OCS初始化接收器继续工作，新增显式命令接收器转交同一上传Provider |
| OppoCarLinkBridgeModule | Hook业务方法原样保留，通过LegacyApi把before/after接口转至现有libxposed API102，不并装第二套旧Xposed运行时 |

主开关关闭时仍按当前项目的关闭行为处理。服务/广播沿用项目受保护的入口边界。模块默认作用域不扩大到系统框架；原文 system_server 分支保留在源码，实际是否加载需相应框架/作用域支持并由真机确认。不是声称已经在所有宿主触发了全部候选Hook。

## 使用与验收

1. 相同签名FULL APK覆盖安装。主程序已内置模块，通常无需安装独立Bridge；不要同时启用两份同类Hook。
2. LSPosed选择主APK模块，保留目标宿主作用域，重启宿主或手机。
3. 主程序→设置→系统车辆接入→检查并配置接入，进行只读URI/会话/映射自检。主程序正常控车仍使用已有账号和真实极星接口。
4. 上传通道使用同一个`content://io.github.polestarcn.companion.bridge`；绑定查询路径`auth/checkCarBind/hasCarBinding`；call方法为`getVirtualSession`、`getVehicleSnapshot`、`dispatchCarCommand`。
5. 请求键/事件码/缺省值按上传原文；兼容成功与真实车辆回读分开核对，不能只用exec_success断定车辆已动作。
6. 本版还保留早期开发的明确Messenger协议端点供独立对照；上传默认路径使用原Service/Provider，不替换成该协议。
7. 当前没有ADB连接真机。工程编译、模拟设备测试、签名检查与实际手机安装/宿主Hook验证分别报告。OPPO云端车型/鉴权和手表支持不由本机兼容回执推定。

## 构建交付

构建、测试、APK大小、签名及GitHub提交在完成后追加。
