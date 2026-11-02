# OPPO Watch X2 × Polestar 2 数字钥匙 / 车控架构研究

日期：2026-11-02

## 结论

目标必须拆成三种不同能力：

1. **手表远程车控**：可以现在做。OPPO Watch 全智能模式上的轻量入口把锁车、解锁、空调、尾门、寻车等动作交给手机 PsAssistant，手机继续使用已验证的 Polestar China/C3 远程控制链路。
2. **OPPO 系统原生车辆卡 / 我的车辆 / 小布 / 手表车控入口**：继续走 CarLink 官方接口优先、LSPosed fallback；控制仍进入 PsAssistant 的真实会话与命令验证。
3. **OPPO Watch 直接成为 Polestar 2 的 BLE 启动钥匙**：不能靠伪造绑定标志实现。Polestar 2 Digital Key 属于独立 Phone-as-Key 安全域，必须创建真实、设备专属的钥匙凭据并让车辆接受该设备。

## OPPO BluetoothKey 官方合同

从用户提供的 OPPO CarLink 官方 Demo / OCS 1.4.0 AAR：

- `FLAG_CAR_BLUETOOTH_KEY = 1`
- `oppo.intent.action.opluscarlink.BIND_BT_KEY`
- `com.nearme.wallet.key.KEY_DETAIL_ACTIVITY`
- Bluetooth car-key card type = `6`
- `BluetoothKey` 保存 carId/companyId/carName/scanFilters/BT address/model/bind time/app package/unlock mode/RSSI 等描述。
- `ICarKeyCallback`:
  - onSdkInitComplete
  - onCarFound
  - onGetBluetoothKeyState
  - onUserConfirmedUnlock
- `CarLinkManager`:
  - notifyBluetoothKeyBindResult
  - notifyBluetoothKeyUnBind / notifyAllBluetoothKeyUnBind
  - notifyBluetoothKeyStateChanged
  - notifyCarVerificationResult
  - isBluetoothKeyExist
  - isSupportedBluetoothKey

最关键的官方 Demo 注释是：**Car APP 完成绑卡操作以后**才调用 `notifyBluetoothKeyBindResult` 通知 OPPO 手机。

这证明 OPPO CarLink BluetoothKey 是系统整合层，不是车企密码学钥匙生成器。

OPPO 还允许车企提供 BLE ScanFilter，可按固定 MAC、service UUID 或 manufacturer data 识别车辆广告。ScanFilter 解决“发现哪辆车”，不解决“我是否被授权打开这辆车”。

## Polestar 2 Digital Key 与 OPPO Key 的差异

Polestar 官方当前说明：
- Polestar 2 Digital Key 主要使用加密 Bluetooth，并结合云端支持；
- 不是 Polestar 3/4 的标准 Wallet/CCC 车钥匙；
- Digital Key 需要先把 Polestar App 与车辆配对，再在 App 中激活；
- 管理员首次配对需要实体钥匙在车内，非管理员也需要至少一把实体钥匙；
- Polestar 2 没有官方 smartwatch wallet key 路径。

项目此前对原始 Polestar 数字钥匙库的研究还确认过一条独立安全链：账号/设备注册 -> ECDHE/车辆确认 -> 设备与车辆证书 -> CarAccess/PassiveEntry -> PAKM/TCAM 近场数据 -> PhoneAsKeyVehicle pairing。

这里的凭据是设备专属的，因此：
- 手机号+密码登录 != 数字钥匙授权；
- 普通 BLE 连接 != 配对成功；
- OPPO UI 显示“已绑定” != 车辆接受该设备；
- 不能把手机端密钥直接复制到手表当作正常实现。

## 路线 A：Watch X2 作为手机中转遥控器

全智能模式优先落地：

```
OPPO Watch X2 UI / 小布 / 快捷卡
       ↓ authenticated command
Phone PsAssistant
       ↓ existing VehicleCoordinator
Polestar China C3 / remote command
       ↓ readback verification
Vehicle
```

手表只持有：
- 与手机配对后的短期授权/会话标识；
- 脱敏车辆快照；
- allow-list 命令。

手表不持有：
- Polestar 手机号/密码；
- access/refresh token；
- 原始 VIN；
- PAK/LAK/证书私钥；
- OPPO 车企授权。

推荐动作：
- lock/unlock（手表确认）
- climate start/stop
- air purification
- trunk（确认）
- honk/flash
- SOC/range/charging/read-only status

这条路线不依赖数字钥匙即可完成“手表控车”。

## 路线 B：OPPO 原生车辆/车钥匙系统作为 UI 与后台承载

手机端：
- CarLink / CarControl：车辆状态、远控；
- BluetoothKey：只在真实 Polestar Digital Key 已完成以后，把已有钥匙的展示/扫描/连接状态同步给 OPPO；
- Wallet / Health Wallet：只使用官方允许的接入能力；
- LSPosed：用于版本兼容、品牌入口、动作转发和诊断，不制造授权。

允许的兼容 Hook：
- 经过版本/结构校验后显示 Polestar 入口；
- 把 Polestar 点击事件导向 PsAssistant/官方绑定页；
- 将已有真实绑定状态映射到 OPPO 卡片；
- 将系统车控动作转发到 PsAssistant；
- 观察 CarLink/Wallet/Health 的回调和能力门禁。

不实现：
- 强制 `isBluetoothKeyExist=true`
- 强制“绑定成功”
- 跳过车辆确认/实体钥匙要求
- 导出/复制数字钥匙私钥
- 绕过 OPPO/Polestar 钥匙认证

## 路线 C：Watch X2 作为独立 Polestar PAK Endpoint

这条路线只有在“手表自己完成一次合法配对”时才成立。

研究目标：
1. 验证手表全智能 Android 侧是否具备 Polestar Digital Key 库所需 BLE、Keystore/native ABI、后台执行能力；
2. 验证 Polestar 车辆/云端是否允许手表作为一个新的 guest/admin endpoint 完成标准配对；
3. 配对必须经过原车正常的人机确认/实体钥匙条件；
4. 若成功，手表生成自己的设备密钥并保存在手表安全存储，不复制手机密钥。

当前没有 Polestar 官方证据表明 Polestar 2 支持 smartwatch endpoint，因此只能作为授权真车研究方向。

## Watch X2 双系统判断

OPPO Watch X2 官方硬件/系统：
- ColorOS Watch 7
- Snapdragon W5 + BES2800BP
- BLE 5.2
- NFC
- 全智能模式
- 长续航模式

OPPO 中国区官方已经支持部分品牌无感车钥匙、NFC 车钥匙和车控 App，说明硬件与系统存在汽车入口，但品牌支持来自系统级合作/白名单，不代表任意第三方 App 自动获得相同权限。

**全智能模式**：第三方 Android App 可运行，是 PsAssistant Watch companion 的首发目标。

**长续航模式**：不能假定普通 Android App 仍运行。只有 OPPO 自带车钥匙服务/RTOS 桥接真正保留对应车辆能力时，才可能继续无感钥匙或车控。需要真机采集验证。

## beta22 更新

- 新增 `OppoCarKeyCapability`：
  - 检测 OPPO BluetoothKey 官方绑定入口；
  - 检测 Wallet key detail 入口；
  - 检测 BLE 硬件能力；
  - 明确 `requiresOemKeyProvisioning=true`。
- 新增 `oppo-car-key-status` bridge 诊断；
- Manifest 增加 `BIND_BT_KEY` / Wallet key detail package visibility；
- Bridge 诊断明确显示真实 key provisioning 门槛；
- Watch X2 轻量采集器增加 CarKey/DigitalKey/Wallet/Health Wallet 包扫描；
- 不添加 fake binding / key bypass。

## 下一阶段真机研究

需要两份小型只读资料：
1. OPPO 手机：PsAssistant OPPO 轻量研究包，重点看 `com.heytap.opluscarlink`, `com.finshell.wallet`, `com.heytap.health.wallet`, `com.oplus.cardigitalkey`。
2. OPPO Watch X2：更新后的 `tools/oppo-watch/capture_watch.py` 在全智能、长续航模式各采一次。

下一步根据手表端真实存在的系统 car-key service/package 决定：
- 官方 Watch/Wallet 车钥匙桥；
- 手机中转 Watch App；
- 或独立 Polestar PAK endpoint 实验。

不再以伪造“已绑定”为里程碑。


## beta22.1：CarLink 宿主只读 LSPosed 探针

为确认 ColorOS 真机上官方 CarLink 是否可被模块观察，增加：
- Xposed scope: `com.heytap.opluscarlink`
- `CarLinkHostReporter`：只记录宿主版本、进程、模块版本与加载状态；
- `OppoCarLinkHostDiagnostics`：PsAssistant 本地保存最后一次只读报告；
- Bridge 诊断页显示 CarLink 宿主是否实际加载模块。

该探针**不 Hook/篡改** `isBluetoothKeyExist`、绑定结果、验证结果或密钥状态。只有确认具体手机固件的 CarLink 宿主版本后，才允许进一步做版本门禁的 UI/路由兼容。

当前 AssistantScreen 17.14.60 已存在一条安全的品牌入口兼容链：
- `PolestarProviderCatalogShim` 仅在内存 Provider 列表增加一个品牌入口；
- `PolestarBindIntentShim` 只把该入口导向 PsAssistant 本地绑定/诊断页面；
- 不写 SceneService 数据库；
- 不宣称车辆或蓝牙钥匙已经绑定。

因此“让系统出现极星选项”与“让车辆接受数字钥匙”已经明确分离。


## 官方公开资料交叉验证

### Polestar 2

Polestar 官方 FAQ / Manual 当前明确：
- Digital Key 是加密数字钥匙，安全存储在用户手机，通过加密 Bluetooth 与车辆通信；
- Polestar 2 主要使用 Bluetooth + cloud connection；
- Polestar 2 因技术/硬件架构不同，不能使用 Polestar 3/4 的 Wallet 标准化数字钥匙；
- Digital Key 需先将 Polestar App 与车辆连接，再在 App 中激活；
- 首个管理员配对要求所有实体钥匙在车内；guest 配对仍要求至少一把实体钥匙；
- Digital Key 的能力包括 lock / unlock / vehicle start；
- 车端 Phone-as-Key 射频位于 2.4 GHz，Polestar 手册将其标为 PAKM/Phone as Key system。

官方参考：
- https://www.polestar.com/us/support/faq/polestar-2-app-and-digital-key/
- https://www.polestar.com/us/manual/polestar-2/
- https://www.polestar.com/en-il/manual/polestar-2/2024/article/dcc960b6c83a3190c0a801517b420846/

### OPPO Watch / 穿戴

OPPO 官方 Watch X2 中国区产品页明确支持：
- 部分合作品牌“无感车钥匙”；
- 部分合作品牌 NFC 车钥匙；
- 多品牌车控 App；
- BLE / NFC 硬件能力；
- 全智能/长续航双模式。

OPPO 开放平台穿戴服务也明确：Watch 应用可通过网络或蓝牙与手机、汽车等设备联动，实现数据同步与操作控制。

官方参考：
- https://store.oppo.com/cn/app/product/index?skuId=32731
- https://open.oppomobile.com/new/introduction?page_name=wearabledevice

这些资料证明 Watch X2 的硬件与 OPPO 系统可以承载“车钥匙/车控”能力，但能否用于 Polestar 2 取决于真实 OEM 配对协议与系统合作/授权，不能由 LSPosed 的 UI 假绑定替代。


## Watch Companion 通信协议建议（远程控车，与数字钥匙分离）

OPPO 开放平台公开说明中国版 Watch 基于深度定制 Android，按普通 Android 应用思路开发，不是 Android Wear；同时明确手表应用可通过网络或蓝牙与手机、汽车联动。因此 PsAssistant Watch Companion 不依赖 Google Wear Data Layer。

### 首选：Phone-mediated BLE GATT

手机 PsAssistant：
- 仅用户主动开启“手表控制”后启动 BLE advertiser/GATT server；
- 自定义 service UUID，只暴露脱敏状态与 allow-list 命令；
- 控车最终仍调用 `VehicleCoordinator.command()`，继续使用现有确认/回读；
- 不把 Polestar token、密码、VIN 或 PAK 材料发给手表。

Watch X2：
- 只扫描 PsAssistant service UUID；
- 首次配对需要手机端用户确认；
- 保存一个独立 Watch pairing key；
- 显示 SOC/range/charging/lock/climate；
- 发送 lock/unlock/climate/air/trunk/honk/flash 等有限动作。

安全建议：
- BLE characteristic 要求 encrypted + MITM/bonding（如果固件支持）；
- 另加应用层 challenge + HMAC，避免只依赖 BLE MAC；
- 每条命令包含 nonce、counter、timestamp、action，禁止重放；
- unlock/trunk 等高风险动作需要 Watch 本地确认/生物识别（若可用）；
- 手机 Session 失效时 Watch 只显示“需在手机重新登录”，不接收账号凭据；
- 配对密钥可随时在手机端撤销。

### eSIM / 网络模式

Watch X2 支持独立网络后，可以扩展“Watch -> PsAssistant relay service”的网络模式，但不应直接把手机 refresh token复制到手表。若未来需要真正离手机远程控车，应使用独立设备授权/短期 token 或自建受控 relay，且需要单独安全审计。

### 长续航模式

普通 Android Watch App 不应假定在长续航 RTOS 路径继续运行。只有真机采集证明 OPPO 的系统 CarLink/Wallet/DigitalKey 服务在该模式仍由系统保留并提供受支持接口后，才进入长续航原生车控/钥匙适配。

## 真钥匙 PAK Endpoint 判定门槛

只有同时满足以下条件，才继续“Watch X2 直接作为 Polestar 2 BLE key”实验：

1. Watch 全智能系统 ABI 能运行 Polestar 数字钥匙 native library 或可重新实现其公开协议客户端；
2. Android Keystore/硬件密钥能力满足设备私钥不可导出要求；
3. BLE 扫描/连接/后台执行满足 PAKM/TCAM 会话需要；
4. Polestar 云端和车辆接受 Watch 作为**新的设备 endpoint**；
5. 配对走正常车辆确认与实体钥匙要求；
6. Watch 自己生成并保管设备密钥，不从手机复制。

未满足第 4/5 条之前，即使 BLE 广播和 UI 都能模拟，也不能称为数字钥匙完成。

## 现阶段优先级

P0：Watch Companion 手机中转遥控（可实现完整远程控车）
P0：AssistantScreen / CarLink 显示 Polestar 入口与真实状态
P1：官方 BluetoothKey/Wallet 对真实已配对钥匙的展示与系统承载
P1：长续航模式系统组件验证
P2：Watch 作为独立 PAK endpoint 的合法配对实验
P3：只有 Polestar/OPPO 官方支持后再评估真正无手机启动体验
