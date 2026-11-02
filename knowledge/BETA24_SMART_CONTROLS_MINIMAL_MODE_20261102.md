# PsAssistant beta24 — OPPO Smart controls + 极简模式

日期：2026-11-02

## 真机现象

用户截图确认：
- My Devices 已能显示两张 Polestar 车辆卡，SOC/续航/Connected 可见；
- 负一屏车辆服务卡仍出现“绑定车辆”入口；
- OPPO Smart controls 的 Supported apps 清单包含 BYD、长安、理想、Tesla、小鹏等，但没有 Polestar；
- CarLink 直接绑定 Activity 需要 `com.oppo.permission.safe.IOT`，普通第三方应用不能直接启动。

## beta24 架构

### Smart controls 清单

PolestarProviderCatalogShim 升级为 generation 4：
- 保留 `spCode/name` provider 模型；
- 新增 `appName/packageName` 这类 Smart controls 应用模型识别；
- 识别列表中多个已知车企名称后才执行兜底注入，避免向普通应用列表误插入车辆项；
- 合成条目名称为 `Polestar 极星`；
- 如果模型提供 package 字段，设置为主 APK `io.github.polestarcn.companion`，避免复制其他车企 package 导致系统去重或错误跳转；
- 仍保留 ViewModel、LiveData、ListAdapter、AsyncListDiffer 多层注入。

不采用“永久替换某个现有车企”的破坏性方案。只有内存列表合成，不修改 OPPO 数据库。

### 已绑定状态

真实依据仍是：
1. PsAssistant 已有有效中国区 Session；
2. 已发现并缓存真实非 Demo 车辆。

满足后，本地系统车辆桥默认自动启用，AssistantScreen 读取 SceneService `accountBindList` 时只在调用方内存结果中加入 `POLESTAR`。因此无需再做一次 PsAssistant 本地“确认绑定”，但：
- 不写 SceneService 数据库；
- 不绕过 `com.oppo.permission.safe.IOT`；
- 不修改 BluetoothKey/PAK/车辆验证结果；
- Smart controls 控制动作最终仍进入主 APK 的真实 VehicleCoordinator/中国区远程控制。

### 极简模式

新增全局 `minimal_mode`：
- 默认关闭；
- 设置 > 外观与交互可切换；
- 首页车辆图片长按 1.1 秒快速切换；
- 标准布局和 VoLike 布局隐藏按钮名称；
- 控制图标适当放大；
- 状态颜色继续表达开启/警告状态；
- 精简状态反馈保留。

### 状态文字

- 门锁：`已锁 / 未锁`
- 空调：`运行中 / 未运行`
- 托管：未运行时 `未托管`，运行时直接显示剩余时间
- 空气净化：`运行中 / 未运行`
- 车窗：`已关 / 未关`

详细空调反馈模式仍可在“按钮反馈”设置中手动选择。

### 每皮肤日夜车辆图片

车辆图选择从全局 `vehicle_art_day/night` 迁移为：
`vehicle_art_day_<skin>` / `vehicle_art_night_<skin>`。

首次访问某个皮肤时自动继承旧的全局选择，因此升级不会丢原有车辆图。