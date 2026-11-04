# beta28 OPPO 4×2车辆直控链路（2026-11-03）

主APK：`0.9.7-beta28-standalone /142`；集成及独立模块：`OPPO Bridge 1.14.0 /26`。脱敏静态证据见[evidence](evidence/oppo-car-direct-beta28-20261103.json)。本页记录的是代码、静态契约和本地构建结论；只有安装到用户设备后完成真实车辆状态回读，才能升级为`PROJECT_DEVICE_VERIFIED`。

## 为什么此前“有卡片但不能直接控车”

MyDevices 17.4.15 的4×2卡片包里已经存在四个车辆事件ID：`kongtiao`、`chesuo`、`shanguang`、`mingdi`。但同一包的`eventSummary`没有把这些ID纳入主页事件集合，转换器白名单也会过滤未知的Provider快捷项。因此只注入Provider行、车型或二级详情页，最多能得到“查看设备”，不能形成主页四按钮直控。

另一个历史问题是集成模块仍有固定的独立包authority。主APK内置模块实际属于`io.github.polestarcn.companion`，若快捷项继续指向`io.github.polestarcn.oppobridge.devices`，系统会看到目录行但命令落不到当前Provider。beta28改为运行时双身份解析，集成和独立模块共用同一套源码，安装时只启用其中一份。

## beta28实现的闭环

1. Provider同时识别集成和独立authority；发布`CAR / POLESTAR_2 / Polestar 2`等本地车型字段，以及不可逆车辆键，不保存或上报完整VIN。
2. 只对`com.heytap.mydevices 17.4.15`、固定RPK入口、`1,405,170`字节和固定SHA-256启用内存补丁。任何版本、大小、哈希或唯一锚点不匹配都关闭补丁，不改磁盘APK/RPK。
3. 内存补丁只把系统包里已经存在的四个车辆ID加入`eventSummary`；转换器仍以精确版本门禁添加相同四项，不注入任意脚本或任意命令。
4. 卡片数据层发布四个OEM快捷项；MyDevices点击后由自有控制器接管，映射为`lock-toggle`、`climate-toggle`、`flash`、`honk`，再进入PsAssistant已有确认/生物识别/直接发送策略。
5. 主程序分别记录宿主查询、RPK补丁、转换器、快捷项数量、OEM点击、控制器匹配、命令受理和命令入队。`lastCommandSent`只证明进入PsAssistant命令队列；后续真实车况匹配才证明车辆执行。
6. Root导出扫描CarLink的base APK及最多16个split APK，只提取类名、action、authority和manifest契约；单文件上限100MB、总导出仍≤2MB，不导出数据库、Token、VIN或方法体。

## 两条链路不能混为一谈

| 链路 | beta28能做什么 | 仍然缺什么 |
|---|---|---|
| MyDevices本地4×2卡片 | 精确放行已有车辆ID、注入本地Polestar车型行、四按钮转入PsAssistant车控 | 用户设备、系统版本和车辆真实回读验收 |
| OPPO官方CarLink/Smart Controls | 延续SDK客户端、回调、状态和定向诊断 | OPPO为本应用分配`AUTH_CODE/companyId/typeCode`、云端车型技能、签名权限 |
| 数字钥匙/Watch PAK | 只显示真实框架/配对状态 | 车企PAK、硬件和服务端配对；不伪造已绑定 |

LSPosed/Root可以改变本机进程看到的目录、脚本门禁和控制路由，不能合法生成OPPO服务端应用身份、IOT签名权限或车企数字钥匙凭据。因此beta28优先打通可由本机证据闭环的MyDevices卡片路径，同时保留官方路径诊断，未把本地注入冒充云端正式绑定。

## 真机使用与判定

1. 覆盖安装beta28 FULL；LSPosed只启用主APK内置模块或独立Bridge其中一份。
2. 作用域至少勾选MyDevices、AssistantScreen、SmartEngine和Instant Platform；设备存在CardService时一并勾选。重启手机或由主程序Root工具重启这些目标进程。
3. 在主程序登录并刷新真实车辆，打开OPPO互联、设备卡Provider和“17.4.15 4×2车辆直控实验”。先确认主程序内锁车/空调/闪灯/鸣笛可用。
4. 在设备空间/MyDevices重新添加4×2设备卡。若系统保留旧缓存，先用“修复Provider注册并恢复设备卡”，再重启MyDevices进程。
5. 诊断顺序必须依次出现：`hostQueryObserved → rpkPatchApplied → converterWhitelistApplied → selectedShortcutCount=4 → oemClickSeen → controllerMatched → lastCommandAccepted → lastCommandSent → 车辆状态回读`。断在哪一步就只处理该层，不再用Widget实例数或“模块已加载”替代验收。

`rpkPatchApplied=false`且原因是exact-gate-mismatch，表示设备内MyDevices包与当前静态证据不同；模块会保持关闭。此时导出新的≤2MB轻量资料包再适配，不能扩大成通配补丁。

## 耗电边界

Provider只读已有快照；卡片查询不触发车辆联网。RPK补丁每个目标进程只报告一次，卡片点击才提交命令。CarLink按需读取继续默认关闭且遵循长停省电，当前车辆五分钟最多一次；无常驻秒级轮询、无自动重复写入。手机耗电和车辆12V影响仍需在真机长时间观察，不能从单测推定。
