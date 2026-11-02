# OPPO CarLink 官方车辆接入研究 / PsAssistant beta20

日期：2026-11-02

## 架构结论

PsAssistant 的 ColorOS 车辆集成改为三层：

1. PsAssistant China vehicle core：账号、车辆发现、C3 状态、远程控制仍是唯一真实车辆后端。
2. OPPO CarLink 官方车企 SDK：新的首选 ColorOS 系统接入通道。
3. MyDevices / AssistantScreen / Pantanal / QuickApp Hook：保留为旧固件兼容、诊断和研究兜底，不再作为最终产品主接口。

## 官方资料与用户提供 Demo 的共同证据

用户提供的 OPPO CarLink Demo 已恢复到 platform_sdk 分支的较新历史，包含 OCS 1.4.0 SDK 与远程车控示例。公开产品资料同时确认 OPPO “离车管家/我的车辆”在负一屏完成车企授权后提供车辆状态和远程控制。

Demo 中确认的正式 OCS 模型：

- CarLink.init(context, FLAG_CAR_CONTROL | FLAG_CAR_BLUETOOTH_KEY, callback, ...)
- CarControl.registerCallback(ICarControlCallback)
- CarControl.auth() / subscribe() / accountExpired() / accountUnbind()
- inquireCarInfo() -> sendCarInfoList(...)
- inquireCarStatus() -> sendCarStatusList(...)
- onControl(ControlInstruction) -> ControlTask
- onVerify(...) -> sendControlMessage(...)
- onSwitchCar(...) / activateCar(...)
- 官方绑定入口：card://carlink.heytap.com/control?operation=bind

OCS 1.4.0 还提供 sendCarInfo/sendCarInfoList、sendCarStatus/sendCarStatusList、sendSwitchCarResult、sendCarInstruction、supportCarControl、hasCarBinding、checkCarBind。

CarStatus 可上报续航、电量、充电、门窗、尾门、空调、里程、胎压、位置等。PsAssistant 当前已有的数据可以覆盖 SOC、续航、里程、充电、门锁/门窗、尾门、空调和部分位置数据。

## 指令映射

可直接对齐：

- CAR-DOOR / LOCK-CAR -> lock / unlock
- ALL-WINDOW -> windows-open / windows-close
- CAR-BOOT -> trunk-open / trunk-close
- CAR-HORN -> honk
- DOUBLE-FLASH -> flash
- CAR-HORN&&DOUBLE-FLASH -> honk-flash
- AC-TEMP -> climate-start / climate-stop

暂不伪造单个车窗、SUNROOF、CAR-ENGINE、ACTIVE-CAR 的 OEM 语义。

## 车控状态机

OCS ControlMessage code：

- 0 CONTROL_EXECUTING
- 1 CONTROL_SUCCESS
- 2 CONTROL_FAILED
- 3 NEED_VERIFY
- 4 VERIFY_SUCCESS
- 5 VERIFY_FAILED
- 6 ERROR_MESSAGE
- 7 VERIFY_FAILED_TO_LIMIT

VerifyData 支持安全码与指纹两类输入。后续正式接入时应把 PsAssistant 现有生物识别/确认策略映射到这一状态机，不绕过用户验证。

## SDK 版本差异

1.2.17 使用 com.coloros.ocs.carlink；1.4.0 已迁移到 com.oplus.ocs.carlink。
1.4.0 新增/调整 sendCarInfo、sendSwitchCarResult、onSwitchCar、ACTIVE-CAR、CAR-HORN&&DOUBLE-FLASH、LOCK-CAR 等接口，后续实现应以 1.4.0 合同为主，同时保留旧包名兼容检测。

## 官方宿主合同

从 OCS 1.4.0 确认：

- 宿主包：com.heytap.opluscarlink
- 服务 Action：oppo.intent.action.opluscarlink.OPLUS_SDK_SERVICE
- 初始化 Action：oppo.intent.action.opluscarlink.INIT_OPLUS_SDK
- Provider：com.heytap.opluscarlink.CarAppProvider
- 车控宿主最低版本常量：130000

## 授权边界

官方 Demo 含示例 AUTH_CODE/companyId。PsAssistant 不复制、不提交、不复用这些值。
真正初始化 OCS SDK 需要 OPPO 开放平台给 PsAssistant 应用/签名分配自己的开发者授权。公开仓库也不分发用户提供的专有 AAR。

beta20 因此先完成官方宿主探测、绑定入口、车控指令映射、诊断与研究导出；取得 PsAssistant 自己的 OPPO 授权后，再接入 OCS AAR 与 ICarControlCallback 实际回调。

## beta20 代码

- OppoOfficialCarLink：官方 CarLink 系统能力探测 + 我的车辆绑定入口。
- OppoCarLinkCommandMap：官方远程车控指令到 PsAssistant 命令映射。
- PsAssistantBridgeProvider：新增只读 oppo-carlink-status。
- OppoWidgetSetupActivity：官方 CarLink 绑定入口和版本诊断。
- BridgeDiagnosticsActivity：官方路线状态优先展示。
- OppoLightResearchExport：加入 com.heytap.opluscarlink 元数据。
- Manifest：增加 CarLink package/service/provider/deeplink visibility。
- CI：修复 beta19 VALIDATION 命名断言，版本推进 beta20。
