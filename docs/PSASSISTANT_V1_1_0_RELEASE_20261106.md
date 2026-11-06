# PsAssistant 1.1.0 / 164 — 正式主版本更新与独立登录交付
2026-11-06 · 适用 China 区 / Android / ColorOS 16–17

## 1. 主线发布
- App：`1.1.0`、versionCode `164`，不再使用 Beta 标签；原包名 `io.github.polestarcn.companion`
- 内置与独立 OPPO Bridge：`2.6.0`、versionCode `39`
- Android 开发主线 `android-polestar-assistant-main`；完成 CI 后以 fast-forward 方式同步默认 `main`（不覆盖其他平台历史）
- 维持 Beta49 的 OPPO 自定义模板、Widget Check、All Good 提醒、净化排版、0.6s 长按、地图弹窗和已验证车辆指令回读

## 2. OPPO 动态刷新管理
入口：**设置 → 数据刷新管理 → OPPO 插件 · 动态刷新 / 操作后加速**。

| 选项 | 默认值 | 作用 |
|---|---:|---|
| 动态刷新开关 | 开 | 根据最近门锁操作、运行中状态调整 TTL |
| OPPO 常规前台 | 10 分钟 | 无操作时共享车况的前台最小刷新间隔 |
| OPPO 常规后台 | 30 分钟 | 与其他后台数据需求合并，交给 Android JobScheduler |
| 空调／净化／托管运行中前台 | 2 分钟 | 仅适用相应活跃状态；控制读回不受此限制 |
| 运行中后台 | 15 分钟 | 服从 Android 的 Doze、JobScheduler 和网络条件 |
| 开锁／关锁后加速窗口 | 3 分钟 | 成功的车辆回读确认后开启短窗口 |
| 加速窗口前台 | 30 秒 | 只在短窗口内提高 CORE 请求资格，仍经过合并与状态校验 |
| 加速窗口后台 | 60 秒资格 | 不保证每分钟唤醒；实际后台定时仍受系统最短 15 分钟限制 |
| OPPO 卡片手动刷新冷却 | 60 秒 | 可选 30／60／120／300 秒；限流不等于后台定时 |
| 动态状态显示 | 开 | 倒计时由缓存或本地已确认开始时间计算，不因每分钟显示变化发起网络请求 |

以上皆为**请求资格/最低间隔**，不是承诺车辆云端在指定时刻返回，也不保证 OPPO 宿主主动调用。用户关闭自动读取/未登录/退出/跨车辆时不得绕过安全门禁。后台加速不得伪装成前台长驻轮询。

## 3. OPPO 卡片状态
- 常态默认：`88% · 370KM`（数字示例，实际以缓存为准）
- 开启空调：`88% · 空调30m`；净化开启：`88% · 净化4m`；托管中：`88% · 托管45m`
- 同时开启时按 **托管 → 空调 → 净化** 顺序优先显示（不轮流闪烁）
- 到时且无新车端确认时显示「运行中」或「待确认」，**不能自动断言车辆已关闭**
- `{lock}` OPPO 短状态：`开锁` / `关锁`；仅展示，不会提交门锁指令
- 用户使用自定义模板时不会被自动覆盖；需要动态状态可插入 `{dynamic}`
- 设置 → OPPO 卡片显示：动态状态开关默认开启，实时预览可勾选模拟运行中的空调；**预览不执行车辆控制**。

## 4. 一次请求多项数据的实际刷新同步
普通定向读取调用 `ChinaPolestarApi.readVehicleScoped(..., observed)`。它追踪成功返回的 RPC 家族，交给 `VehicleRefreshManager`，**只顺延本次有实际结果的 TTL**：

| 实际成功的数据服务 | 顺延哪类 TTL | 备注 |
|---|---|---|
| Battery | CHARGING；成功的 Exterior + Battery 才计 CORE | 电量、续航、充电可在同一 Battery RPC 返回 |
| Exterior（CORE 调用内） | CORE（同时需 Battery 可用） | 门锁/门窗可与电池一起读取，失败保留旧状态且不延长 CORE |
| ParkingClimatization | CLIMATE | CORE 发现外部启动空调时也能一起顺延 |
| PreCleaning | AIR | CORE 顺带读取净化的实际返回也可顺延 |
| Odometer | MILEAGE | 成功获得里程值才顺延 |
| Health | HEALTH + MAINTENANCE | 共享同一组 Health 数据；成功的新结果或仍有效的健康缓存才顺延，失败陈旧值不顺延 |

网络超时、缺少字段或业务未确认时，不能凭一次“请求发出”推断数据已经更新；车辆上报时间保持原值。所有缓存键使用 VIN 哈希，不记录原始 VIN；车控的 P0 全量验证独立于普通队列。

## 5. 中国区独立登录配置与公开 GitHub
已同步 `config/polestar-china.public.json` 和 `config/polestar-china.env.example`，它们仅包含区域、服务入口、登录所需资源名称、批准的兼容指纹和构建说明，**不包含账号、手机号、密码、Token、VIN、车钥匙或私有客户端认证材料**。

中国区客户端兼容资源 `assets/client-signing.bin` 虽不含个人资料，但仍属于认证材料；不应以明文、可逆编码、Git 提交或公开下载地址传播。实际 Standalone FULL APK 在**私有离线打包**时从已核验的历史 FULL APK 复用批准格式的资源，使用历史同签名密钥签名，产物仅提供为安装包下载，不回传 GitHub。

## 6. 发布验收
- CI Debug 编译、Lint、主程序/Bridge 全量单元测试、ZIP/Manifest/包体校验。
- FULL 本地再封装：批准资源指纹一致、versionCode164、非 debuggable、原有证书一致、APK v2/v3、安装包与原 CI 字节码仅 Manifest/签名/资产差异。
- 功能验收边界：中国区手机号真实登录、Token 刷新、车辆发现、实际远控、ColorOS 原生宿主状态与频率仍需真机、实车和授权后台确认；通过离线静态测试并不代表上述在线测试已经完成。
