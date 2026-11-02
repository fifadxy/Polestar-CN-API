# OPPO 手机 BLE 车钥匙官方资料研究（文档 10778 / 10781）

日期：2026-11-02

官方来源：
- https://open.oppomobile.com/documentation/page/info?id=10778
- https://open.oppomobile.com/documentation/page/info?id=10781

> 说明：OPPO 开放平台这两页正文采用 JavaScript/权限动态加载，自动抓取不能稳定获得完整正文。本文不猜测网页中不可读取的段落；接口合同以这两页作为官方来源锚点，并与用户提供的 OPPO CarLink 官方 Demo、OCS CarLink 1.4.0 / 1.2.17 AAR 做交叉验证。

## 1. 这两份资料在项目中的定位

这组资料属于 **OPPO 手机侧 BLE 车钥匙接入层**，不是车辆本身的密码学钥匙协议。

在项目架构中应分为三层：

1. **车企真实数字钥匙层**：Polestar PAKM / TCAM / CarAccess / Phone-as-Key，负责真正的设备身份、证书、车辆授权与解锁/启动。
2. **OPPO CarLink BluetoothKey 层**：接收车企 App 已完成的真实钥匙绑定结果，负责系统级车钥匙入口、扫描、连接状态、用户确认、Wallet/CarLink 展示与系统联动。
3. **PsAssistant / LSPosed 兼容层**：仅做能力探测、品牌入口、路由和旧系统兼容，不制造数字钥匙。

因此：

`OPPO 显示已绑定` ≠ `车辆已经接受该设备`

`BLE 扫描到车辆` ≠ `拥有开门/启动车辆的密钥`

## 2. 已确认的 OPPO BluetoothKey 合同

来自官方 Demo / AAR 的合同：

- CarLink feature flag：`FLAG_CAR_BLUETOOTH_KEY = 1`
- 官方绑定入口 action：`oppo.intent.action.opluscarlink.BIND_BT_KEY`
- Wallet key detail action：`com.nearme.wallet.key.KEY_DETAIL_ACTIVITY`
- Bluetooth car-key card type：`6`

### BluetoothKey 数据模型

已确认字段/语义包含：

- carId
- companyId
- carName
- BLE ScanFilter 集合
- Bluetooth address / model 等车辆发现信息
- bind time
- car app package
- unlock mode
- RSSI / 距离相关门槛

这些字段主要描述“**OPPO 系统如何识别和承载一把已经存在的车企蓝牙钥匙**”，不是生成钥匙私钥的材料。

### ICarKeyCallback

已确认回调：

- `onSdkInitComplete`
- `onCarFound`
- `onGetBluetoothKeyState`
- `onUserConfirmedUnlock`

方向：

`OPPO CarLink host -> 车企 App`

系统需要车企 App 提供钥匙状态、车辆发现后的处理，以及用户确认后的真实车企解锁动作。

### CarLinkManager

已确认与车钥匙相关接口：

- `notifyBluetoothKeyBindResult`
- `notifyBluetoothKeyUnBind`
- `notifyAllBluetoothKeyUnBind`
- `notifyBluetoothKeyStateChanged`
- `notifyCarVerificationResult`
- `isBluetoothKeyExist`
- `isSupportedBluetoothKey`

方向：

`车企 App -> OPPO CarLink host`

最重要的官方 Demo 语义是：**车企 App 完成真实绑卡/绑钥匙以后，再调用 notifyBluetoothKeyBindResult 通知 OPPO。**

因此不能把 `notifyBluetoothKeyBindResult` 当成“创建数字钥匙”的 API。

## 3. 手机 BLE 车钥匙推荐生命周期

### 阶段 A：能力门禁

1. 确认 `com.heytap.opluscarlink` 存在。
2. 初始化 CarLink SDK。
3. 检查 `FLAG_CAR_BLUETOOTH_KEY` / feature support。
4. 检查当前 OPPO 宿主版本。
5. PsAssistant 只在取得 OPPO 正式车企授权后启用官方 adapter。

### 阶段 B：真实车企钥匙配对

1. 用户从 OPPO/车企入口发起绑定。
2. PsAssistant/Polestar 数字钥匙层进入 **Polestar 自己的合法 PAK 配对**。
3. 车辆完成实体钥匙/车内确认/设备证书等真实授权。
4. PsAssistant 得到“该设备已真正成为车辆钥匙”的结果。
5. **此时**才构造 BluetoothKey 元数据并调用 `notifyBluetoothKeyBindResult`。

这一步是项目最重要的安全边界。

### 阶段 C：车辆发现

OPPO BluetoothKey 可使用 BLE ScanFilter 让系统识别目标车辆广告。

目前从 Demo/AAR 已确认至少可按：
- 固定 Bluetooth address
- service UUID
- manufacturer data

做过滤。

ScanFilter 只负责 **发现**，不负责 **认证**。

对 Polestar：
- 不能从 VIN 人为生成一个“看起来像车辆”的 ScanFilter；
- 只有从真实 Polestar PAK/车辆 BLE 行为确认稳定广告特征后才配置；
- 不把可用于跟踪用户车辆的 BLE 标识写入公开日志或研究导出。

### 阶段 D：连接与钥匙状态

OPPO 系统发现候选车辆后：
- 通过 `onCarFound` 通知车企 App；
- 通过 `onGetBluetoothKeyState` 请求当前真实钥匙状态；
- 车企 App 用 `notifyBluetoothKeyStateChanged` 把真实连接/钥匙状态同步回系统。

系统展示状态不能反向成为授权来源。

### 阶段 E：用户确认与解锁

`onUserConfirmedUnlock` 表示用户在 OPPO 系统入口确认了动作。

正确实现：
1. 校验 BluetoothKey 对应的真实 Polestar key identity；
2. 进入 Polestar PAK/CarAccess 解锁流程；
3. 等待真实车辆/钥匙层结果；
4. 将验证/执行结果回报 OPPO。

错误实现：
- 收到 `onUserConfirmedUnlock` 后直接把 UI 状态改为 unlocked；
- 通过 LSPosed 强制返回成功；
- 把云端远程 unlock 冒充成“BLE 数字钥匙验证成功”。

云端远程控车可以作为独立功能，但必须和 Digital Key 状态分开。

### 阶段 F：解绑

真实 Polestar key 已撤销后：
- 再调用 `notifyBluetoothKeyUnBind`；
- 多钥匙/账号清理时按真实状态决定是否使用 `notifyAllBluetoothKeyUnBind`；
- 清理 PsAssistant 本地 OPPO 映射；
- 不因为 OPPO UI 删除卡片就自动删除 Polestar 真实钥匙，除非用户明确执行车企解绑流程。

## 4. OPPO 层与 Polestar PAK 层映射

| OPPO BLE key 能力 | Polestar/PsAssistant 对应层 | 是否可直接实现 |
|---|---|---|
| BluetoothKey 元数据 | 已真实配对的 PAK endpoint 描述 | 可以，前提是真实配对完成 |
| ScanFilter | Polestar 车辆 BLE 广播特征 | 需真车采集确认 |
| onCarFound | PAK 候选车辆发现 | 可以做 adapter |
| onGetBluetoothKeyState | 当前 PAK/连接状态 | 需 Polestar 数字钥匙层暴露真实状态 |
| onUserConfirmedUnlock | 用户发起近场解锁 | 只能转入真实 PAK 流程 |
| notifyBluetoothKeyBindResult | “车企钥匙已绑定”的系统通知 | 只能在真实 PAK 成功后调用 |
| notifyBluetoothKeyStateChanged | 系统钥匙状态展示 | 可同步真实状态 |
| notifyCarVerificationResult | 车辆/车企验证结果 | 可映射真实结果 |
| isBluetoothKeyExist | OPPO 系统是否已有 key entry | 不能作为 Polestar 权限判据 |
| isSupportedBluetoothKey | OPPO 平台/机型能力 | 只能作为平台能力门禁 |

## 5. 与 PsAssistant 远程控车必须隔离

PsAssistant 当前已有 China/C3 远程控制：
- lock / unlock
- climate
- air purification
- trunk
- honk / flash

这些是 **账号/云端远程控制**。

OPPO BluetoothKey 是 **近场数字钥匙**。

UI 可以共用一个“车辆”入口，但内部状态必须至少拆成：
- `remoteControlAvailable`
- `oppoBluetoothKeySupported`
- `polestarPakPaired`
- `oppoKeyRegistered`
- `nearbyVehicleDetected`
- `pakAuthenticated`

不能只用一个 `bound=true`。

## 6. LSPosed 正确用途

允许：
- 识别 `com.heytap.opluscarlink` 宿主版本；
- 只读确认 BluetoothKey/Wallet 类与入口是否存在；
- 在 AssistantScreen 经过版本门禁后显示 Polestar 品牌入口；
- 把入口路由到 PsAssistant；
- 观察系统请求的 callback 类型和生命周期；
- 将**真实** Polestar/OPPO 绑定状态映射到系统卡片。

不作为产品实现：
- 强制 `isBluetoothKeyExist=true`
- 强制 bind success
- 跳过用户确认
- 跳过 Polestar 实体钥匙/车辆配对
- 导出/复制 PAK 私钥
- Hook 成功返回来冒充车辆验证成功

## 7. 对 Watch X2 的影响

这两份资料是 **手机侧 BLE 车钥匙资料**。

它们能证明：
- OPPO 手机系统有 BluetoothKey 承载框架；
- 真实车企钥匙可被 OPPO 系统整合；
- 系统支持车辆发现/钥匙状态/用户确认/Wallet 入口。

它们**不能单独证明**：
- Watch X2 可以直接调用同一套 BluetoothKey API；
- 长续航 RTOS 保留这些 API；
- Polestar 2 接受 Watch 作为 PAK endpoint；
- 手机现有 PAK key 可以安全迁移到 Watch。

所以 Watch 继续保持两条路线：
1. P0：Watch -> 手机 PsAssistant -> 云端远程车控；
2. P2：Watch 自己作为新的合法 Polestar PAK endpoint 完成一次完整配对。

## 8. PsAssistant 下一步代码建议

公开主线继续保持无凭据、可测试：

- `OppoCarKeyCapability`：手机侧官方入口/宿主/BLE 能力探测；
- `OppoCarLinkHostDiagnostics`：确认 LSPosed 是否实际加载到 CarLink 宿主；
- 新增只读 `OppoBluetoothKeyStateModel`：明确 remote / PAK / OPPO registration 三种状态；
- 新增 `OppoBluetoothKeyMapper`：只接受“真实 PAK paired”输入，生成非敏感 OPPO metadata preview；
- 新增单测：禁止在 `polestarPakPaired=false` 时输出 bind-success；
- 研究导出继续禁止 BLE address、VIN、token、证书、私钥。

正式受控构建以后再加入：
- OCS BluetoothKey adapter
- `ICarKeyCallback` 实现
- `CarLinkManager` notification adapter
- OPPO 正式 AUTH_CODE/companyId
- Polestar PAK 实际状态桥

## 9. 真机验证清单

手机：
1. 检测 `com.heytap.opluscarlink` 版本。
2. 检测 `BIND_BT_KEY` handler。
3. 检测 Wallet key detail handler。
4. LSPosed 只读确认 CarLink host 已加载。
5. 导出公开组件/版本信息。
6. 不测试假 bind success。

Polestar：
1. 先验证现有手机 Polestar Digital Key 是否真实可用。
2. 记录真实配对前后 PsAssistant 可观察到的**非敏感状态变化**。
3. 再设计 OPPO BluetoothKey adapter。
4. 所有近场解锁成功必须以车辆真实响应为准。

Watch：
1. 全智能模式采 ABI/BLE/NFC/Keystore。
2. 长续航模式采系统 car-key/wallet 组件。
3. 只有确认 Watch 有独立 PAK endpoint 条件后，再进入真钥匙实验。

## 10. 结论

文档 10778 / 10781 进一步强化了当前项目应采用的分层：

```
Polestar PAK / TCAM / CarAccess
        ↓ 真实钥匙绑定结果
PsAssistant OEM key adapter
        ↓ BluetoothKey metadata/state
OPPO CarLink / Wallet / system vehicle surfaces
        ↓
Phone / Watch UI
```

最终目标可以是“OPPO 手机/手表完整控制 Polestar”，但真正的 BLE 启动钥匙必须保持 **Polestar 真实密钥链为根信任**；OPPO 系统负责承载和体验，LSPosed 负责兼容与研究，而不是生成授权。
