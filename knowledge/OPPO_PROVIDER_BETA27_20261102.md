# beta27 Provider修复与验证边界（2026-11-02）

主APK：0.9.7-beta27-standalone /141；集成及独立模块：OPPO Bridge1.13.0 /25。本轮以用户新上传的`PsAssistant-OPPO-light.zip`和既有同版本系统APK静态契约为依据；不把用户原始资料、APK、SDK库或账号数据提交仓库。脱敏证据索引见[evidence](evidence/oppo-provider-beta27-20261102.json)。

## 资料包实际能证明什么

输入为beta26/140、Android16/API36；桥接开关开启。MyDevices主进程与cards进程上报模块1.12.0；主进程phase为hooks-installed，cards为quickapp-provider。这证明模块路径被经过，不能证明当前Provider查询、显示或车辆操作成功。

这份schema4轻量包没有Provider查询结果、账号Provider目录观测和Root静态契约。CarLink状态是未初始化、未取得授权/订阅回调，并列出正式companyId、AUTH_CODE、typeCode、HTTPS图片缺项。**缺少Provider诊断不等于Provider目录必定为空**，SDK缺项也不是独立设备卡Provider必定不能工作的理由。

## 三个不同层次

| 层 | 本轮修复 | 仍需验证 |
| --- | --- | --- |
| 设备卡ContentProvider | 主程序转发身份、URI/selection契约、空卡原因、注册/隐藏恢复 | 真机重新发现、显示缓存、一次控车回读 |
| 系统车辆/账号Provider目录 | 空目录三String构造器、不可变列表副本、正确传入Hook替换参数、类型边界 | OEM页面是否消费替换后的目录；导出区分observed与injected |
| 官方CarLink身份 | 沿用beta26客户端、状态与Root诊断 | 本应用正式身份、后台车型技能及手机/手表端到端验收 |

## 设备卡Provider

MyDevices17.4.15静态源码确认：ConfigurationManager通过DEVICE_INFO action发现Provider，读取oplus.devicecards.provider配置；查询路径为device_info_table_new，单卡筛选为`device_id = ?`，读取device_data字符串。配置XML的version、previewImage、supportStyle保持原协议，不猜测替换成其他SDK格式。

- Provider先检查原始调用UID/宿主，再在try/finally中清理和恢复Binder调用身份，内部转发使用本应用身份。此前无线设置宿主虽然通过卡片层白名单，却不在主桥白名单中，存在转发拒绝路径。没有扩大外部任意应用调用权限。
- 接受同authority、同表路径的URI查询参数与尾随斜线；拒绝其他authority/表。selection按device_id/device_mac/authority各自匹配，不再把任意SQL表达式和第一个参数当作相同条件。
- 空Cursor附带固定reason：no-snapshot、demo-excluded、hidden-by-user、selection-miss等。只读缓存，不自动刷新，不把未知电量/续航变成0。
- 旧/未知状态不生成猜测型快捷控件；提交时再次检查车况新鲜度。Provider队列受理与车辆回读成功分开。
- BridgeClient对已有Provider的拒绝、异常或空回复不再用广播重试同一写请求，避免首次可能已受理后重复发送。旧主程序不声明authorizationPolicyVersion=1时，新模块要求先更新主程序再控车；缓存兼容读取继续保留。
- 程序启动核对自身组件开关与用户总开关；新增“修复Provider注册并恢复设备卡”。只修复本应用组件并清除本卡hidden标记，不改系统数据库、全局权限或用户总开关。

Binder身份切换依据Android官方[Binder API](https://developer.android.com/reference/android/os/Binder#clearCallingIdentity())。ContentProvider自己的clearCallingIdentity方法从API29才可用；本项目minSdk26，因此采用Binder API并完整恢复身份，不使用未门禁的高版本方法。

## Provider目录

AssistantScreen17.14.60 DEX声明确认：`com.oplus.advice.settings.ui.viewmodel.ProviderInfo`有三String构造器、getSpCode/getName/getIconUrl及copy$default，**没有零参数构造器**。

- 对已识别的ProviderInfo空目录构建Polestar项，避免必须先从另一个品牌借模板。未知空列表不注入。
- 列表返回值及UI输入生成新副本，保留原有品牌和原始列表，不依赖向不可变列表add。
- libxposed102的Chain参数替换通过`proceed(Object[])`完成；不再修改getArgs列表后仍执行无参数proceed，避免副本未传入真正调用。
- 取消跨列表共用模板，避免把SmartApp对象混进ProviderInfo列表。全局UI列表只接受明确Provider结构或多品牌车型列表，不向任意空列表添加项目。
- 总开关关闭时停止注入。新诊断保留目录观测时间及来源observed/injected，**注入目录不代表官方SDK授权或屏幕已经渲染成功**。

## 控车与验证统一

Provider/native card/快捷入口沿用主程序每个操作的direct/confirm/biometric设置。默认仍为生物识别。非direct操作生成60秒有效、不可变、一次性的内部验证PendingIntent，不从外部Intent读取指令；换车、退出账号、关闭桥接、验证方式改变或取消都会拒绝旧任务。仅主程序验证成功后发送；不把“等待验证”记成车辆成功。

移除宿主层重复确认，避免设置direct后仍强制多弹一层。Android若阻止验证页启动，应打开主程序进行验证；不会通过Root强制后台弹窗，也不会为可用性跳过已选生物识别。

修复结果确认和OEM快捷切换的子串比较：UNLOCKED不能当作LOCKED，INACTIVE不能当作ACTIVE；支持主程序实际“未锁”和“全部关闭”。

## 小体积诊断与使用

轻量包升级为schema5，增加deviceProvider和独立provider-diagnostics.json；仅包含注册/发现/查询布尔量、行数、固定原因、隐藏状态、会话/快照是否存在，以及目录观测元数据。保留模块reason，仍限制2MB；不导出device_data、车辆状态数值、账号、令牌、VIN或完整系统APK。

1. 安装beta27 FULL，仅启用一份集成或独立LSPosed模块，使用建议作用域并重启宿主/手机。
2. 主程序登录并刷新一次，打开OPPO桥接。若卡片丢失，点击“修复Provider注册并恢复设备卡”，重新进入设备空间添加。
3. 按需要在主程序为各控车操作选择直接发送、确认或生物识别；Provider使用同一设置。
4. 如仍不显示，导出≤2MB轻量包。先判断componentEnabled/discoveredByAction，再看querySucceeded/rowCount/queryReason，最后看catalogObservationAvailable/catalogSource；不要拿Widget实例数或hooks-installed代替Provider验收。
5. querySucceeded且rowCount=1证明本机缓存Provider能返回行，不证明OEM实际渲染；目录injected不证明CarLink服务器注册，手机成功也不证明Watch X2长续航模式成功。

本轮没有连接用户手机做更新后的端到端验收。测试、编译、签名和体积结果在交付验证中记录；遇到系统版本新契约时继续依据实际诊断调整。
