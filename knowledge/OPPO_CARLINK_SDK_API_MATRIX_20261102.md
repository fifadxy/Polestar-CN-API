> beta26更新：本轮已读取完整官方目录并新增SDK客户端及Root定向导出；LOCK-CAR语义、回调成功值及旧SDK差异以 [最新研究](OPPO_CARLINK_PRIORITY_BETA26_20261102.md) 为准。

# OPPO CarLink / OCS 车辆能力 API 矩阵（PsAssistant）

日期：2026-11-02  
来源：用户提供 OPPO CarAppDemo（platform_sdk 历史）与其中 OCS CarLink 1.4.0 / 1.2.17 AAR 的公开 API 合同；不记录 Demo 的 AUTH_CODE、companyId 或其他授权值。

## 1. 项目采用的接口层级

| 层级 | 接口 | PsAssistant 用途 | 状态 |
|---|---|---|---|
| 真实车辆后端 | PsAssistant China / C3 | 登录、车辆发现、SOC、续航、里程、车况、真实远控 | 主后端 |
| OPPO 官方主通道 | CarLink / OCS CarControl | “我的车辆”、负一屏车辆状态、官方远控回调 | 首选，等待本应用 OPPO 授权 |
| OPPO 官方辅助 | CarLinkUnitClient | 宿主版本/Feature 支持/SDK 初始化能力判断 | 可用于接入门禁 |
| 兼容层 | MyDevices / AssistantScreen / Pantanal / QuickApp Hook | 旧固件兼容、诊断、实验卡片 | fallback |
| 车钥匙 | FLAG_CAR_BLUETOOTH_KEY / BIND_BT_KEY | 后续数字钥匙联动 | 独立阶段，不与远控混为一体 |

## 2. SDK 包与版本

### OCS CarLink 1.4.0
- Maven/包命名：`com.oplus.ocs.carlink`
- AAR 版本：`1.4.0-SNAPSHOT`
- minSdk 21 / targetSdk 30
- 官方宿主包：`com.heytap.opluscarlink`
- 初始化广播：`oppo.intent.action.opluscarlink.INIT_OPLUS_SDK`
- SDK Service：`oppo.intent.action.opluscarlink.OPLUS_SDK_SERVICE`
- CarAppProvider：`com.heytap.opluscarlink.CarAppProvider`
- 车控最低宿主版本常量：`130000`

### OCS CarLink 1.2.17
- 旧公开包名：`com.coloros.ocs.carlink`
- 回调/切车/状态查询接口与 1.4.0 有差异
- beta20 以后以 1.4.0 为主合同；仅在兼容诊断中识别旧包名，不按旧版重新设计主架构。

## 3. 初始化与生命周期

### CarLink
核心初始化模式：

`CarLink.init(context, FLAG_CAR_CONTROL | FLAG_CAR_BLUETOOTH_KEY, callback, ...)`

能力位：
- `FLAG_CAR_BLUETOOTH_KEY`
- `FLAG_CAR_CONTROL`

初始化后应检查：
- `isInitComplete()`
- `isCarControlInitComplete()`
- `isCarKeyInitComplete()`

PsAssistant 原则：
- 仅远程车控场景先申请/初始化 CAR_CONTROL。
- 蓝牙钥匙属于独立安全域，不因为远程车控可用就自动启用。
- 用户同意隐私条款、PsAssistant 已登录、OPPO SDK 鉴权完成后才向系统上报车辆数据。

### CarLinkUnitClient
已确认接口：
- `getVersion()`
- `hasFeature(String)`
- `initSdk()`
- `isFeatureSupport(int, String)`
- `isSupportedFeature(int)`
- `resetPermitBits()`

建议：
- 首次启动不主动初始化官方 SDK。
- 设置页打开“OPPO CarLink”或系统发起绑定时初始化。
- 先做 version/feature gate，再注册车辆回调。

## 4. CarControl 主接口

已确认：
- `registerCallback(ICarControlCallback)`
- `unregisterCallback(...)`
- `getCallbacks()`
- `requestConnected(...)`
- `auth(...)`
- `subscribe(...)`
- `accountExpired(...)`
- `accountUnbind(...)`
- `sendCarInfo(...)`
- `sendCarInfoList(...)`
- `sendCarStatus(...)`
- `sendCarStatusList(...)`
- `sendSwitchCarResult(...)`
- `sendCarInstruction(...)`
- `sendControlMessage(...)`
- `supportCarControl(...)`
- `hasCarBinding(...)`
- `checkCarBind(...)`

PsAssistant 对应关系：
- auth/subscribe：OPPO 系统绑定生命周期，不替代 Polestar 中国账户登录。
- sendCarInfo/List：把当前 PsAssistant VehicleInfo 转为 OPPO 车辆目录。
- sendCarStatus/List：把 VehicleStateSnapshot 转为 OPPO CarStatus。
- sendControlMessage：把 PsAssistant 真实控车执行/回读状态反馈给 OPPO。
- accountExpired/accountUnbind：Polestar Session 失效/用户退出时同步给 OPPO。
- switchCar：与 PsAssistant 当前选择车辆同步。

## 5. ICarControlCallback

1.4.0 合同确认：
- `onControl(ControlInstruction) -> ControlTask`
- `onSubscribeResult(...)`
- `onAuthResult(...)`
- `onUnbindResult(...)`
- `inquireCarInfo()`
- `inquireCarStatus()`
- `onVerify(...)`
- `onComplete(...)`
- `onShowCard(...)`
- `onHideCard(...)`
- `onSwitchCar(...)`
- `activateCar(...)`

实现顺序建议：
1. inquireCarInfo
2. inquireCarStatus
3. onControl + ControlTask
4. onVerify
5. onSwitchCar
6. auth/subscribe/unbind 生命周期
7. show/hide card 优化刷新频率

## 6. CarInfo 映射

CarInfo 可包含：
- openId
- carId
- name
- currentCar
- companyId
- controlType
- typeName / typeCode
- image
- license
- VIN
- engineNumber
- tankCapacity
- bluetoothName
- powerType

PsAssistant：
- carId：使用稳定、不可逆的本地车辆标识；公开诊断不输出 VIN。
- name：优先 Polestar 2 / API modelName。
- currentCar：映射当前选中车辆。
- powerType：Polestar 2 为纯电。
- image：后续使用 PsAssistant 自有车辆图资源。
- 不向公开日志写 license/VIN/engineNumber。

## 7. CarStatus 完整能力矩阵

| OPPO CarStatus | PsAssistant 当前来源 | beta20 状态 |
|---|---|---|
| batteryLeftPercent | SOC | 可映射；OPPO Demo 使用 0.0–1.0 浮点比例，不能直接传 59 |
| batteryEnduranceMileage | Range | 可映射 |
| totalEnduranceMileage | Range/组合 | 纯电车型可按剩余续航策略映射 |
| oilEnduranceMileage | 无 | 不填 |
| isCharging | Charging | 可映射 |
| isActive | 当前车辆/在线策略 | 可映射但需明确定义 |
| totalMileage | Odometer | 可映射 |
| isAllDoorStatus / 单门 | Exterior/Openings | 可映射，需统一枚举 |
| 单窗 / 全窗 | Exterior/Openings | 可映射状态 |
| trunk | Tailgate | 可映射 |
| airConditionerStatus | Climate | 可映射 |
| outsideTemperature | 暂无稳定源 | 不填 |
| insideTemperature | 暂无稳定源 | 不填 |
| airTemperature / wind | 空调目标温度仅部分可用 | 谨慎 |
| engine | EV 不适用 | 不填 |
| sunroof | 当前无独立控制 | 状态可有则上报，控制不宣称 |
| light | 当前未独立读取 | 不填 |
| fuel | EV 不适用 | 不填 |
| tire pressure/temperature | 当前主线未稳定 | 后续 |
| batteryVoltage | 当前无 | 不填 |
| longitude / latitude | LastParkedLocation | 可映射，需用户隐私与位置语义校验 |
| coordinateType | 车辆位置坐标系 | 必须明确后才上报 |
| address | 可选反向地理信息 | 不强制 |
| positionUpdateTime | 车辆位置时间 | 可映射 |
| availableControlInstructions | PsAssistant command capability | 动态生成 |
| timestamp | VehicleSnapshot timestamp | 可映射 |

## 8. ControlInstruction 与 PsAssistant

OPPO 状态枚举：
- UNKNOWN = -1
- CLOSE = 0
- OPEN = 1
- OPENING = 2
- CLOSING = 3

指令字符串：
- CAR-ENGINE
- CAR-DOOR
- ALL-WINDOW
- LEFT-FRONT-WINDOW
- RIGHT-FRONT-WINDOW
- LEFT-BACK-WINDOW
- RIGHT-BACK-WINDOW
- SUNROOF
- CAR-BOOT
- CAR-HORN
- DOUBLE-FLASH
- AC-TEMP
- ACTIVE-CAR
- CAR-HORN&&DOUBLE-FLASH
- LOCK-CAR

当前映射：
- CAR-DOOR / LOCK-CAR -> lock / unlock
- ALL-WINDOW -> windows-open / windows-close
- CAR-BOOT -> trunk-open / trunk-close
- CAR-HORN -> honk
- DOUBLE-FLASH -> flash
- CAR-HORN&&DOUBLE-FLASH -> honk-flash
- AC-TEMP -> climate-start / climate-stop

保持 unsupported：
- 独立四窗
- SUNROOF
- CAR-ENGINE
- ACTIVE-CAR OEM 激活语义

## 9. ControlMessage / 验证状态机

返回码：
- 0 CONTROL_EXECUTING
- 1 CONTROL_SUCCESS
- 2 CONTROL_FAILED
- 3 NEED_VERIFY
- 4 VERIFY_SUCCESS
- 5 VERIFY_FAILED
- 6 ERROR_MESSAGE
- 7 VERIFY_FAILED_TO_LIMIT
- 512 UNKNOWN_ERROR

PsAssistant 建议状态：
- 系统发起 onControl -> 先返回 ControlTask。
- 如果 PsAssistant 当前策略为“生物识别/确认”，先向 OPPO 报 NEED_VERIFY / 执行本地授权。
- 真正调用 Polestar C3 后先报 EXECUTING。
- 只有 commandReached / 状态回读确认后报 SUCCESS。
- RPC 接收但回读未确认不能直接报 SUCCESS。
- 超时/服务器错误映射 FAILED + 安全、短错误描述。

## 10. VerifyData

已确认：
- VERIFY_CANCEL = -1
- VERIFY_OK = 0
- taskId
- type
- data
- securityCodeLength
- freeze time / enable 等

Demo 包含安全码和指纹两类输入。PsAssistant 不保存 OPPO 验证数据，不将验证材料写诊断日志；只保留任务 ID、状态码和时间。

## 11. 错误码（重点）

已确认常见错误：
- 0：成功
- 400：参数错误
- 500：服务端异常
- 999：上一指令仍执行中
- 1002：时间戳非法
- 1003：OPLUS token 非法
- 1004：company token 非法
- 1005：签名校验失败
- 1014：不支持的指令
- 1021：有其他指令执行中
- 1023：指令执行超时
- 1024：车辆响应超时
- 5200：未绑定车辆
- 5201：多用户场景不支持
- 5300 / 5400：无法完成车辆绑定
- 6001：车企 App 未安装
- 6002：APID 非法

诊断 UI 只显示语义化错误，不展示 token/signature/私有授权材料。

## 12. OPPO 系统入口

官方 Demo 确认：
- `card://carlink.heytap.com/control?operation=bind`

beta20 已提供该入口，并检测：
- `com.heytap.opluscarlink`
- OPLus SDK Service
- CarAppProvider
- bind deeplink handler

这些检测仅证明系统能力存在，不等同于 PsAssistant 已获车企资格。

## 13. 开发者授权与发布门禁

正式 OCS 接入必须满足：
- PsAssistant 自己的 OPPO 开发者应用身份
- PsAssistant 自己的 AUTH_CODE / companyId 或平台当前要求的等价凭据
- 与正式 APK 签名身份匹配
- OPPO 测试/认证流程通过
- 用户隐私与 SDK 合规声明完成

禁止：
- 复制 Demo 的授权值
- 在 GitHub 公共仓库提交授权值
- 把用户提供的专有 AAR 公开再分发
- 用 Hook 绕过 OPPO 官方鉴权来冒充已认证车企

## 14. 下一阶段代码拆分

建议新增私有/受控构建的 `oppoCarLinkOfficial` adapter：

- `OppoCarLinkAdapter`
- `OppoCarInfoMapper`
- `OppoCarStatusMapper`
- `OppoCarControlCallback`
- `OppoCarLinkAuthState`
- `OppoCarLinkCommandSession`

公开主线只保留：
- 数据模型
- Mapper 单测
- 宿主探测
- 官方绑定入口
- 不含凭据的接口适配层

取得 OPPO 授权与可合法打包的正式 SDK 后，再将官方 adapter 作为 Release 可选依赖接入。


## 15. beta21：CarStatus 适配层落地

beta21 新增不依赖 OPPO 专有 AAR 的公开适配层：

- `OppoCarLinkSnapshotAdapter`：将 PsAssistant `VehicleSnapshot` 转成可检查的 CarLink 状态语义。
- `oppo-carlink-preview`：通过本地桥接只读查看映射结果。
- Bridge 诊断页可显示 SOC 比例、续航、里程、充电状态和可上报控制指令。
- 单测明确验证 59% SOC 必须映射为 `0.59f`，并对异常 SOC 做 0..1 clamp。
- 缺失续航/里程不会伪造数值。
- 当前可上报控制指令严格限制为已有 PsAssistant 真实控制能力。

该层的目的，是让“真实车辆数据 -> OPPO CarStatus”在没有 AUTH_CODE/companyId 时也能独立测试。取得 OPPO 正式授权后，只需增加受控的 OCS Adapter，把这些字段写入 `CarStatus` 并调用 `sendCarStatus/List`，不重写车辆后端。

### 官方开放文档目录研究范围

OPPO CarLinkUnit 文档左侧目录已确认包含：

- CarLinkUnit 介绍
- CarLinkUnit 接入指南
- CarLinkUnit 接口指导
- CarLink 云端接入指导
- CarLinkUnit 版本更新说明
- CarLinkUnit 常见问题（FAQ）
- CarLinkUnit 附录
- 联系我们

网页正文采用登录/动态加载机制，公开抓取不能稳定导出所有正文；接口合同因此以用户提供的官方 Demo、OCS AAR 与公开可验证的车企 SDK 使用说明交叉校验，不猜测不可读取章节内容。


## 16. OPPO 手机 BLE 车钥匙官方文档 10778 / 10781

官方来源锚点：
- https://open.oppomobile.com/documentation/page/info?id=10778
- https://open.oppomobile.com/documentation/page/info?id=10781

网页正文为 JavaScript/权限动态内容，自动抓取不稳定，因此本节仅记录可由官方 Demo/AAR 交叉验证的合同，不推测不可读取段落。

### BluetoothKey 主流程

```
OPPO CarLink capability check
        ↓
车企 App 完成真实 OEM / PAK key provisioning
        ↓
notifyBluetoothKeyBindResult
        ↓
OPPO 保存/展示 BluetoothKey metadata
        ↓
BLE ScanFilter 发现候选车辆
        ↓
ICarKeyCallback.onCarFound
        ↓
onGetBluetoothKeyState / notifyBluetoothKeyStateChanged
        ↓
onUserConfirmedUnlock
        ↓
车企 App 执行真实钥匙认证/解锁
        ↓
notifyCarVerificationResult
```

### PsAssistant 门禁

必须拆分：
- `remoteControlAvailable`
- `oppoBluetoothKeySupported`
- `polestarPakPaired`
- `oppoKeyRegistered`
- `nearbyVehicleDetected`
- `pakAuthenticated`

只有 `polestarPakPaired=true` 后，才允许正式 adapter 上报 OPPO bind-success。

详细手机侧研究：
`knowledge/OPPO_MOBILE_BLE_CAR_KEY_10778_10781_20261102.md`
