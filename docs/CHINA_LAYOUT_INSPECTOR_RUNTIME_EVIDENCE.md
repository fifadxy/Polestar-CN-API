# 极星中国 App Layout Inspector 运行时证据

> 采集日期：2026-10-23
>
> 证据类型：Android Layout Inspector 的 View 创建栈与 `mSourceLayoutResId` 运行时导出
>
> APK 静态补充：2026-10-24，已用签名有效且 16 个 DEX 与真机导出逐个一致的 `4.25.0-262` 完整 APK 补齐点击调用链
>
> 适用边界：页面结构、类/布局对应关系、UI 到 Interactor 的静态映射和可复用的动态分析锚点

## 1. Layout Inspect 1.1.6 官方能力边界

**来源：** LSPosed 模块镜像 [Xposed-Modules-Repo/com.flass.layoutinspect](https://github.com/Xposed-Modules-Repo/com.flass.layoutinspect/tree/1.1.6-20) 的 `1.1.6-20` 标签（commit `69a115f`）项目说明。

| 菜单 | 项目说明中的实际能力 | 本项目用途 |
| --- | --- | --- |
| 应用进程 | 枚举当前/更多进程；预览对象；杀死进程 | 只用于确认目标进程；不需要杀进程 |
| 捕获布局 | 捕获普通窗口、PopupWindow、Dialog、Menu、悬浮窗等；读取 View 属性、`layoutRes`、`listenInfo`、列表 adapter、WebView 信息和 View 构造调用栈；可临时修改或移除 View | **最高优先级**：定位点击监听器、Binding、Provider 和构造栈 |
| 活动记录 | 枚举当前运行中的 Activity，并显示 `class`、`application`、`intent` | 确认当前页面所属 Activity；不是操作时间线录制器 |
| 活动管理 | 枚举 Manifest 声明的 Activity；预览类结构；直接启动 Activity | 可定位入口；直接启动内部 Activity 可能改变 App 状态，默认不使用 |
| 类加载器 | 枚举内存中的 ClassLoader；预览对象；按加载器 Dump Dex | 用于确认动态模块和加载来源 |
| 搜索类名 | 枚举 ClassLoader 搜索静态或动态加载类；预览字段、构造、方法、父类和内部类 | **高优先级**：核对 RCS、认证与页面类是否已加载 |
| Inject So | 向目标进程注入 SO | 当前不需要，也不用于常规取证 |
| Dump So | 枚举已加载 SO 的 pathname/base/size 并导出 | 完整 APK 已提供原始 SO；后续只需确认目标进程实际加载的 pathname/base/size |
| Dump Dex | 可按 ClassLoader、cookie/DexCache、DexFileSize、主动加载类等方式脱壳 | 本轮 16 个 DEX 已完成，不需要重复全量导出 |
| Dump Xml | 从内存中的 `xmlBlock` 还原 Epic、DexProtect 等加密 AXML | 不是当前页面 View 树导出；本项目暂无加密 AXML 需求 |
| Dump Maps | 解析 `/proc/self/maps`；查看映射详情；导出单段或连续内存 | 用于确认或导出 native 映射；不是 Activity/网络记录 |
| Dump Assets | 从内存还原部分加密 Assets | 当前没有明确需求 |
| 移除安全限制 | 主动关闭截屏限制并列出设置限制的来源 | 极星页面当前可截图，无需使用 |
| 调试浏览器 | 配合 Chrome 动态调试 WebView 的 HTML、网络请求和脚本 | **高优先级**：只读检查官方登录/滑块页面的请求结构 |

项目说明还明确指出“捕获布局”可以读取 WebView 的 URL、Cookie、User-Agent 和 JavaScript interface，也可能显示 VideoView 请求头。这些界面可能暴露会话凭据；本项目只记录 URL host/path、header 名和字段名，不保存 Cookie、Authorization 或字段值。

“临时上帝模式”、移除 View、修改文本/尺寸、JavaScript 注入、杀进程、启动内部 Activity 和 Inject So 都会改变目标进程或页面状态，不属于默认只读取证范围。

## 2. 输入与安全检查

本次分析使用 `Polestar-China-StackTrace.zip`，SHA-256：

```text
8d7658abe485026eabd620db6f57adbe6597f2e01104f389f03d7a269ec49e78
```

归档包含：

- 1,903 个 `LayoutRes@21174/View@…` 记录；
- 849 个 `StackTrace@21174/View@…` Java 序列化记录；
- 其中 753 个 View 同时具有布局资源 ID 和创建栈；
- 71 个创建栈包含 `com.polestar.carcontrol` 类。

分析过程没有反序列化 Java 对象，只读取归档条目、可打印字符串和整数资源 ID。路径检查未发现目录穿越或符号链接；文本扫描未发现 JWT、Bearer、VIN、手机号或邮箱候选值。原始导出仍不进入仓库。

## 3. 已确认的车辆控制页结构

**VERIFIED — Layout Inspector 运行时 View 创建栈**

车辆控制页的根 Fragment 为：

```text
com.polestar.carcontrol.ui.main.CarMainFragment
    ↓ onCreateView
com.polestar.carcontrol.databinding.CarcontrolMainFragmentBinding.inflate
```

页面内容通过 `com.chad.library.adapter.base.BaseProviderMultiAdapter` 的多 Provider 列表构造。捕获到的车辆控制模块如下：

| 运行时资源 ID | View Binding | 已观察到的自定义 View / Provider | 可确认的 UI 职责 |
| --- | --- | --- | --- |
| `0x7f0d00e9` | `CarcontrolMainBluetoothKeyItemBinding` | obfuscated provider `clinew` | Bluetooth Key 卡片 |
| `0x7f0d00ea` | `CarcontrolMainClimateCardItemBinding` | `CarSwitchButton`；obfuscated provider `clitry` | Climate 卡片与开关控件 |
| `0x7f0d00eb` | `CarcontrolMainControlCommandItemBinding` | obfuscated provider `clichar` | Control Command 区块 |
| `0x7f0d00ef` | `CarcontrolMainFragmentBinding` | `CarMainFragment` | 车辆控制页根布局 |
| `0x7f0d00f0` | `CarcontrolMainHeaderItemBinding` | `CarChargingProgressBar`；obfuscated provider `clivoid` | Header / charging progress |
| `0x7f0d00f1` | `CarcontrolMainItemCommandItemBinding` | `CarCommandButton`、`InnerShadowCardView`；obfuscated provider `clibyte` | 单项车辆命令卡片 |
| `0x7f0d00f3` | `CarcontrolMainTipsItemBinding` | `CarMainTipsProvider`、`ViewSwitcher` | 提示轮播区块 |

这些十六进制 ID 只用于定位本次构建。跨版本分析应优先使用完整类名和 View Binding 名称，不能把资源整数或 `View@…` 对象标识当成稳定接口。

## 4. 对动态分析的直接价值

即使 Provider 类经过 R8 混淆，下列类名仍可作为相对稳定的观察点：

```text
com.polestar.carcontrol.ui.main.CarMainFragment
com.polestar.carcontrol.databinding.CarcontrolMain*Binding
com.polestar.carcontrol.views.CarCommandButton
com.polestar.carcontrol.views.CarSwitchButton
com.polestar.carcontrol.views.CarChargingProgressBar
```

建议按以下顺序关联 UI 行为与后端调用：

1. 在 `CarMainFragment` 完成页面级生命周期和 ViewModel 关联；
2. 以 View Binding / 自定义 View 类区分 header、climate、command 和 Bluetooth Key 模块；
3. 对具体点击监听器只记录调用入口，继续追踪到 ViewModel / Interactor / Controller；
4. 最终以 C3/PCCS/VOCMO 请求和新鲜车辆状态确认命令语义。

不要把布局中存在按钮或开关解释为功能已经授权，也不要把点击后的 UI 变化解释为车辆已经完成动作。

## 5. 完整 APK 补充的动态入口

完整 APK 的 `assets/therouter/routeMap.json` 给出了车辆控制页面的静态路由。下列入口可与“活动记录”和“捕获布局”交叉核对，但不应通过“活动管理”直接启动，以免绕过正常状态检查：

| 路由 | Activity / Fragment |
| --- | --- |
| `/psCarControl/popupcar` | `com.polestar.carcontrol.phone.PopupCarControlActivity` |
| `/psCarControl/popupcar/charging` | `com.polestar.carcontrol.phone.page.charging.ChargingActivity` |
| `/psCarControl/popupcar/ps2/climate` | `com.polestar.carcontrol.phone.page.climate.Ps2ClimateActivity` |
| `/psCarControl/popupcar/ps4/climate` | `com.polestar.carcontrol.phone.page.climate.ps4.Ps4ClimateActivity` |
| `/psCarControl/popupcar/location` | `com.polestar.carcontrol.phone.page.location.LocationActivity` |
| `/psCarControl/popupcar/ps2/pak/settings` | `com.polestar.carcontrol.phone.page.pak.ps2.PakSettingsActivity` |
| `/psCarControl/ps4/pak/settings` | `com.polestar.carcontrol.ui.pak.ps4.PAKSettingsActivity` |
| `/psCarControl/polestarIdWeb` | `com.polestar.carcontrol.auth.PolestarIdWebActivity` |

APK 仅包含 `arm64-v8a` native 库。`libvocmo-lib.so` 的 JNI 符号确认存在车辆访问、充电、空调和基础车辆控制消息的编码/解码实现。这证明部分命令载荷会进入 native 层，但符号存在本身不能确定当前车型选中的 provider、中国 production RPC、Token 权限或车辆执行结果。

### 5.1 车辆主页命令映射

**STATIC-VERIFIED — 官方 APK 的 UI 数据模型 → `CarMainFragment` → `CarMainViewModel` → Interactor**

`com.polestar.carcontrol.ext.clitry` 把中文标签与命令类型整数写入 `CarMainCommandItemUiState`，`CarMainViewModel.V0` 再按该类型分发。完整静态映射为：

| UI 标签 | 类型 | 当前 UI 状态 | ViewModel 分支后的具名语义 |
| --- | ---: | --- | --- |
| 闪灯 | `0` | 任意 | `CarLocationInteractorImpl.flash()` |
| 鸣笛 | `1` | 任意 | `CarLocationInteractorImpl.honk()` |
| 充电口 | `2` | 扩展命令模型；Polestar 2 不展示 | `closeChargingCover()` / `openChargingCover()`，三种 controller 均无可用实现 |
| 车锁 | `3` | 任意 | `toggleLockStatus()`，由最近锁状态决定 lock/unlock |
| 后备箱 | `4` | 已打开 / 未打开 | 先刷新 exterior 状态后 `lockTrunk(...)` / `unlockTrunk()` |
| 车窗 | `5` | 已打开 / 未打开 | `closeWindow()` / `openWindow(openLittle)`；开窗弹窗将两种选项映射为 `openLittle=true/false` |

常规命令集只创建“车锁”和“后备箱”；扩展命令集在通用数据模型中定义六项。已确认 Polestar 2 的能力配置不包含“充电口”按钮；不应因 APK 中存在 type `2` 而将其列为 Polestar 2 功能。其他扩展项也要同时满足车型能力与账号授权。

### 5.2 Climate 卡片映射

`CarMainFragment` 传入 climate Provider 的两个回调分别进入 `CarMainViewModel.X0()` 和 `U0()`：

| UI 操作 | ViewModel 判断 | Interactor 语义 |
| --- | --- | --- |
| 车内空调开关 | climate 正在运行时停止；否则读取保存的目标温度 | `stopClimate()` / `startClimate(targetTemperature)` |
| 空气净化开关 | 直接分发 | `toggleAirPurification()` |

`startClimate` 向当前 climate controller 传入目标温度；未提供时代码使用 `26.0°C`。上述表已确认用户操作到 RCS controller 接口的语义，尚未确认特定车型在运行时选择 C3、PCCS 或 VOCMO 中的哪一个实现。

### 5.3 APK 内的 controller 实装边界

RCS 接口的三类 controller 并非实装了所有命令。以 `4.25.0-262` 的具名 coroutine 类和接口默认返回值为准：

| 控制能力 | C3 | PCCS | VOCMO |
| --- | --- | --- | --- |
| 闪灯 / 鸣笛 | `honkOrFlash` 实装 | `honkOrFlash` 实装 | 无 location controller 实现 |
| lock / unlock | 实装 | 实装 | `VocmoVehicle.lock/unlock` 实装 |
| open / close windows | 实装 | 实装 | 接口默认返回 unsupported |
| open / close trunk | 实装 | 显式返回 unsupported | 接口默认返回 unsupported |
| unlock trunk | 实装 | 实装 | 接口默认返回 unsupported |
| open / close charging cover | 接口默认返回 unsupported | 接口默认返回 unsupported | 接口默认返回 unsupported |
| start / stop climate | 实装 | 实装 | `VocmoVehicle` 适配实装 |
| start / stop air purification | 实装 | 实装 | `VocmoVehicle` 适配实装 |

因此“充电口”只是通用扩展 UI 数据模型中的预留项：Polestar 2 不支持且不展示该按钮，本版 APK 内的三种 exterior controller 也都没有可用实现。上表的其他“实装”命令仍然不代表当前账号/车型已获授权或车辆会成功执行。

### 5.4 C3 Invocation gRPC 方法与 Proto

C3 controller 调用 `com.volvocars.conncar.mdapi.InvocationServiceGrpcKt.InvocationServiceCoroutineStub`。下表为从具名 coroutine 实现恢复的方法与请求模型：

| UI / Interactor 语义 | C3 gRPC 方法 | 请求 Proto 与关键枚举 |
| --- | --- | --- |
| 车锁 | `lock` | `LockRequest`；`LOCK` 或 `LOCK_REDUCED_GUARD` |
| 全车解锁 | `unlock` | `UnlockRequest` |
| 只解锁后备箱 | `unlock` | `UnlockRequest`；`UNLOCK_TYPE_TRUNK_ONLY` |
| 开/关车窗 | `windowControl` | `WindowControlRequest`；`OPEN_ALL` / `CLOSE_ALL` |
| 开/关尾门 | `tailgateControl` | `TailgateControlRequest`；`OPEN_TAILGATE` / `CLOSE_TAILGATE` |
| 闪灯/鸣笛 | `honkFlash` | `HonkFlashRequest`；由 `HonkFlashType` 区分 |
| 启动空调 | `climatizationStart` | `ClimatizationStartRequest`；由 mapper 写入 VIN 和可用 climate settings |
| 停止空调 | `climatizationStop` | `ClimatizationStopRequest` |
| 启动/停止空气净化 | `preCleaning` | `PreCleaningRequest`；两个 Interactor 分支最终都进入该方法 |

这些 C3 请求都在 `InvocationRequest` 中写入 VIN，响应通过 server-streaming Flow 收集并解析统一 `InvocationResponse`。PCCS 存在对应的 `pccs.invocation.v1` Proto 和 coroutine stub，并额外写入调用关联 ID。本节恢复的是客户端 RPC 名和载荷结构；动态 discovery 选出的中国 host/port、metadata 的完整组合、服务端权限和车辆完成状态仍需运行时确认。

因此后续 Layout Inspect 工作应优先收集：

1. 正常导航进入上述页面时的 Activity class 与 intent；
2. `CarCommandButton` / `CarSwitchButton` 的 `listenInfo` 和监听器实际类名，用于交叉验证上述静态映射；
3. 点击前后当前 RCS controller 的实际类名，以确认 C3、PCCS 或 VOCMO 选择；
4. `/proc/self/maps` 中 `libvocmo-lib.so` 是否已加载及其 pathname/base/size；
5. 同一次操作对应的脱敏请求方法、ACK 和随后刷新出的车辆状态。

无需再次 Dump 全量 DEX、SO、XML 或 Assets；完整 APK 已覆盖这些静态输入。

## 6. 当前证据不能证明的内容

Layout Inspector 的创建栈只记录 View 如何被构造。完整 APK 已把按钮/开关语义静态追踪到 Interactor 和 RCS controller 接口，但当前证据仍不能证明：

- Bluetooth Key 的配对、授权或密钥流程；
- 当前车型和会话实际选中的 C3、PCCS 或 VOCMO controller；
- 中国 production 运行时实际调用的 host/port、metadata、字段值和 controller fallback 顺序；
- token audience 或命令权限；
- provider ACK 与车辆实际状态之间的关系。

这些内容需要结合 DEX 静态调用链、点击时运行时追踪和网络/RPC 观测后再提升为 `VERIFIED`。

## 7. 后续取证目标

后续运行时分析应围绕已确认的稳定入口展开：

- 从正常导航和未修改的 View 开始，记录 Activity、listener 和 controller 实际类名；
- 对照本文的 type/中文标签/Interactor 表，确认运行时分支；
- 只记录脱敏的 command 方法、host/path、字段名与 response/ACK；
- 等待 App 正常刷新，将 ACK 与随后的新鲜车辆状态对齐。

只有形成“UI 入口 → 业务层 → provider 请求 → 车辆状态确认”的闭环，才应更新远程控制方法映射。
