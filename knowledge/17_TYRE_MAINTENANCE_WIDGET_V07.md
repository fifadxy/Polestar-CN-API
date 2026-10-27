# 17｜胎压、保养与 Widget V0.7 增量

> 版本：Android `v0.7.0-beta1`；日期：2026-10-26；主线：`android-polestar-assistant-main`。

## 1. 证据交叉结论

- 参考 APK `app-release.apk.1(1).1` 继续作为 China 登录、车况与远控行为参考；本轮重新检索 DEX，能确认 `HonkFlash` 等远控符号，但未把“未看到胎压业务字符串”错误解释为车辆无胎压能力。
- Mac/Hisingen China 知识库已经将 `tyreAndWarnings` 归入 Health 能力，China 0.19.5 会在需要胎压/健康状态时调用 `HealthService/GetHealth`。
- 上游 Hisingen 当前解析实现明确：胎压报警字段 **9–12**，四轮压力字段 **39–42 kPa**；顺序为左前、右前、左后、右后。报警枚举：1 正常、2 严重偏低、3 偏低、4 偏高。
- 同一 Health payload 中，field 3/4 分别用于官方“剩余保养天数/距离”研究；Android 本轮仍以用户指定的本地规则 **365 天 / 20,000 km** 为主，避免未完成 China 实车语义验证前把官方 Health 字段当成稳定业务规则。

## 2. Android v0.7 实现

### 胎压
- 新增 `TireSnapshot`，读取 China C3 `services.vehiclestates.health.HealthService/GetHealth`。
- 主控制页在“门窗 / 露营”旁增加胎压快捷状态。
- 全部正常：显示 **ALL GOOD**，使用橙色 VI 文字。
- 任一轮胎报警：橙色强调 + `!`。
- 点击后使用与门窗一致的俯视车辆图，按四轮实际位置显示 kPa / 正常 / 偏低 / 严重偏低 / 偏高。
- 未返回有效 Health 数据时显示“未知”，绝不冒充 ALL GOOD。

### 保养
- 默认间隔：365 天、20,000 km。
- 本地按车辆分别保存上次保养日期与里程。
- 显示：距上次保养、已超期时间/里程、剩余天数、剩余公里。
- 新增两个剩余百分比：时间剩余 %、里程剩余 %。
- 可选加入主页数据网格与 Widget。

### Widget
- 保留透明/半透明设计，默认改为透明风格；默认透明度 58%、磨砂模拟 28%。
- Lato 字体延续当前设计规范，标题/控制采用 Bold，数据采用 Regular。
- 4×2：最多 8 项；2×2：最多 4 项；4×1：最多 4 项；2×1：最多 2 项；Data 4×4：8 项。
- 字体根据实际选择的数据数量动态缩放，数据越少越突出，数据越多自动压缩。
- 全部 Widget 增加小字“更新 MM-dd HH:mm / Updated MM-dd HH:mm”。
- 移除“旧 / Old / Stale”这种不直观的数据显示后缀；数据是否需要刷新由具体更新时间判断。
- 默认字段加入胎压、保养剩余时间/里程；用户仍可自由选择。

## 3. 验证等级

- 胎压 Proto 字段：`SOURCE_VERIFIED`（上游 Hisingen + Mac China KB 对齐），Android 实现为 `IMPLEMENTED_NEEDS_DEVICE_VERIFICATION`。
- 胎压 UI / Widget / 本地保养算法：`IMPLEMENTED`，由本地单元测试与 CI 验证。
- China 实车胎压读数：在用户车辆实际回读前保持 `PROJECT_DEVICE_VERIFICATION`。
- 官方 Health 剩余保养字段：保留研究映射，不替代用户指定的 365 天 / 20,000 km 规则。

## 4. 安全与状态规则

- 胎压是只读状态，不发送车辆写指令。
- 缓存胎压仅用于 Widget/显示，完整 VIN 与 Token 不进入显示缓存。
- 缓存或缺失数据不会自动判断“正常”。
- 车窗、鸣笛/闪灯仍沿用强生物识别；瞬时鸣笛/闪灯只报告服务器接受/未确认，不伪造持续车况确认。


## 5. WindowControl / HonkFlash request correction

Cross-check with upstream Hisingen `PolestarGRPCRemote.swift` and its unit tests confirms:
- `WindowControl`: field 2 = 1 open all windows; field 2 = 2 close all windows.
- `HonkFlash`: a **single field-2 enum**, not two booleans: 0 = honk + flash, 1 = honk only, 2 = flash only.
- Both use the invocation VIN envelope in field 1.

Android v0.7 has been corrected to this mapping. China vehicle execution remains `IMPLEMENTED_NEEDS_DEVICE_VERIFICATION` until the user's authorized vehicle confirms each command.


## 2026-10-27 v0.7.8 当前增量

本文件是 v0.7 历史记录；当前改为优先显示车辆 Health 剩余保养字段，缺失才本地回退，百分比仍相对于用户配置周期估算。当前主 RPC GetLatestHealth，胎压数值仍实验性。 具体来源、字段和限制见 [v0.7.8 车况、位置与行程](25_V078_CONDITION_LOCATION_TRIP.md)。
