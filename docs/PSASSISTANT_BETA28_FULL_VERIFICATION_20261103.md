# beta28 FULL交付验证（2026-11-03）

主APK：`0.9.7-beta28-standalone /142`；内置OPPO Bridge：`1.14.0 /26`。本文件与beta28源码同提交，功能和证据边界见[MyDevices 4×2车辆直控研究](../knowledge/OPPO_CAR_DIRECT_BETA28_20261103.md)。

## 安装包

- `PsAssistant-v0.9.7-beta28-Standalone-FULL.apk`：**6,678,749字节（6.68MB）**，低于10,000,000字节限制。
- SHA-256：`cd0a54f0c54f411f6c41db975b046c09bcbd52accff7d94779e899347d19e436`。
- APK v2签名及内容摘要验证通过；证书SHA-256：`d4cd1b06701f9a8829c5ecc5bbee1398a0f8a655703b6cc356398efa30218a7e`，与既有FULL升级身份一致。
- 包名`io.github.polestarcn.companion`，versionCode 142，minSdk 26，targetSdk 34；Release不可调试。
- 已包含批准的China登录配置、真实OPPO CarLink SDK类、内置LSPosed入口和beta28精确RPK门禁，不需要安装独立Bridge APK。

## 构建与回归

- 473项单元/模型/Robolectric回归：0失败、0错误、0跳过。
- Debug Lint：0错误、106警告；Release Lint Vital通过。
- 主程序Release、独立Bridge Debug、R8、资源压缩及固定签名构建通过。
- 源码结构、知识库、纯Java模型、免Key地图距离及CarLink云端签名5项测试全部通过。
- ZIP完整性、standalone登录资产、APK v2签名/内容摘要均通过只读校验。
- R8 seeds确认保留`OppoBridgeModule`、`QuickAppRpkPatch`、`VehicleDeviceCardProvider`、`PsAssistantBridgeProvider`及`com.oplus.ocs`官方SDK类。包内资源原始体积约5.30MB，与既有FULL资源量级一致；体积下降主要来自依赖代码裁剪，不是删除车辆资源。

## beta28修复内容

1. 集成/独立Provider双身份：运行时选择`companion.devices`或`oppobridge.devices`，卡片authority、图标URI、命令scheme和包名一致，不再出现目录行指向另一安装模式。
2. MyDevices 17.4.15固定RPK入口、大小和SHA-256内存门禁；只把系统包内已有的锁车、空调、闪灯、鸣笛四个ID加入主页事件集合，不修改磁盘文件，不匹配即关闭。
3. 转换器白名单、卡片快捷项和自有控制器共用相同四个OEM ID；点击进入主APK既有直接/确认/生物识别策略，不由LSPosed直接写车。
4. Provider发布本地`CAR / POLESTAR_2 / Polestar 2`车型字段及不可逆车辆键；不导出完整VIN，不伪造OPPO服务器或PAK绑定。
5. 新诊断区分宿主查询、RPK补丁、转换器、快捷项、点击、控制器、命令受理、命令入队和真实状态回读。Root定向扫描同时覆盖CarLink base及split APK，总资料包仍限制2MB。

## 更新后使用

1. 覆盖安装本FULL包。LSPosed只启用**主APK内置模块**；如果此前装过独立Bridge，先停用独立模块，避免同一宿主加载两份。
2. 作用域勾选`com.heytap.mydevices`、`com.coloros.assistantscreen`、`com.oplus.smartengine`、`com.nearme.instant.platform`；系统存在`com.oplus.cardservice`或`com.oplus.assistantscreen`时一并勾选。重启手机，或从主程序Root工具重启目标进程。
3. 主程序登录并刷新一次真实车辆，进入OPPO互联，开启设备卡Provider和页面内的单一开关**MyDevices 17.4.15 · 4×2车辆直控实验**。高级诊断页显示的是同一个开关，不再维护重复状态。先在主程序确认对应车辆命令可用。
4. 点击“修复Provider注册并恢复设备卡”，回到设备空间/MyDevices重新添加4×2设备卡。旧卡片缓存不更新时重启MyDevices进程再添加。
5. 依次核对：`hostQueryObserved → rpkPatchApplied → converterWhitelistApplied → selectedShortcutCount=4 → oemClickSeen → controllerMatched → lastCommandAccepted → lastCommandSent → 新鲜车辆状态回读`。

其中`lastCommandSent`只证明命令进入PsAssistant队列，不代表车辆已经动作；最后的状态回读才是端到端成功。鸣笛/闪灯没有稳定状态字段时，必须现场观察，应用不得自动重发。

## 仍需真机完成

- 当前构建环境没有连接用户手机、OPPO Watch X2或车辆，因此卡片实际渲染、四按钮点击、ColorOS缓存差异、手机耗电和车辆12V影响仍是待验收项。
- OPPO官方CarLink/Smart Controls仍要求平台给本应用分配`AUTH_CODE/companyId/typeCode`、云端车型技能与受保护权限；Root/LSPosed没有伪造这些凭据。
- 数字钥匙PAK、车企配对和硬件可信链没有绕过或伪造。MyDevices本地车型注入只服务本机卡片路由，不等于官方“我的车辆”已绑定。
- 若`rpkPatchApplied=false`且报告`exact-gate-mismatch`，设备上的MyDevices代码与本次样本不同。请导出新的≤2MB轻量包继续适配，不应把固定门禁改为通配Hook。
