# beta26 /140：CarLink优先接入、Root定向诊断与协议纠错

日期：2026-11-02（香港）。内置/独立Bridge同源1.12.0 /24。保留beta25的1.5秒外观菜单、所有皮肤极简模式及空调/净化文字、倒计时。

## 结论与本轮证据

优先CarLink是合理方向：官方明确把“离车管家”远控建立在CarLink SDK或云端对接上。但“本地车辆行”“SDK初始化”“用户授权”“OPPO后台车型技能”是不同状态。修改其中一个已绑定布尔值不能建立其他状态。

本轮重新读取OPPO文档前端公开JS所使用的匿名正文接口，取得CarLinkUnit目录内**33项文档（30篇有实质正文、3项为目录容器）**。解决此前10978页面前端空壳无法读取的问题。来源ID、标题、正文摘要校验和见 [抓取索引](evidence/oppo-carlink-official-docs-20261102.json)。正文以2026-11-02读取结果为准；有些内容仍列2022机型，不能据此保证ColorOS17或Watch X2兼容。

同时核对本次上传的CarAppDemo_v2.0.1、SignUtils、DigestUtils和截图/接入指南。Demo内实际库是**CarLink 1.4.0-SNAPSHOT + base 1.0.9**，与官方接入示例的1.4.8/base1.0.16及更新表中的1.4.9不同。文档Maven仓库匿名访问返回401；没有使用示例仓库密码或Demo身份冒充本应用。

## 各层关系

| 层 | 职责与现有事实 | 对本项目的含义 |
| --- | --- | --- |
| PsAssistant中国区后端 | 已有登录、车辆、车况、命令与回读 | 唯一真实控车后端，OPPO入口必须回到这里 |
| MyDevices设备空间 | 本地Device Card Provider、原生设备页和QuickApp卡片 | 设备卡可见不等于CarLink注册、授权或控车成功 |
| CarLinkUnit / com.heytap.opluscarlink | SDK服务、车企回调、账号授权、车辆状态、异步任务 | 本轮新增实际SDK客户端，不再只有合同预览和品牌列表Hook |
| 负一屏“我的车辆” | 官方离车管家入口，要求车企授权与车型技能配置 | SDK或云端路线可以承载；与MyDevices“自选设备”QuickApp是不同卡片 |
| Smart controls | 当前实机存在车企支持目录，Polestar曾缺席 | 推断依赖车辆能力/目录及绑定链；具体分发服务仍需宿主和点击证据，不能用品牌行注入证明控制路由完成 |
| Car+ / com.oplus.ocar | 连接车机后的导航、音乐/投屏生态 | 与离车管家远控有不同入口，不应把投屏连接成功当作远控注册成功 |
| Watch X2长续航 | 手表RTOS侧入口及手机侧分发均需确认 | SDK打通会提供可用的手机端后端，但不能保证手表自动出现Polestar或支持净化等全部技能 |

官方产品说明：[ColorOS智慧出行](https://www.coloros.com/article/A00000015/) 分别列出Car+车联和负一屏离车管家。架构研究不以车载投屏项目替代远控SDK。

## 为什么此前没有真正打通CarLink

1. 旧代码有CarLink宿主发现、品牌目录/本地绑定facade、命令映射和CarStatus预览，却未把官方SDK初始化、registerCallback、inquireCarInfo/Status、onControl任务与真实命令完成事件接起来。
2. 直接绑车Activity受`com.oppo.permission.safe.IOT`限制。原来的普通startActivity回退到本地面板，只解决入口不崩溃，不是官方绑定成功。
3. 官方FAQ [11105](https://open.oppomobile.com/documentation/page/info?id=11105) 明确：必填CarInfo缺项导致“请求参数无效”；后台未配置对应typeCode导致“没有支持的车辆型号”。
4. 官方 [11081](https://open.oppomobile.com/documentation/page/info?id=11081) 还要求openId、carId、currentCar、name、companyId、typeName、typeCode、image。现有本地卡片不具备完整OPPO车型身份。
5. [11083](https://open.oppomobile.com/documentation/page/info?id=11083)、[11084](https://open.oppomobile.com/documentation/page/info?id=11084) 表明：技能、CLICK/SWITCH/SWITCH_OPEN/SWITCH_VALUE展示方式由OPPO后台按车型配置；App返回技能还会被配置过滤。仅发一个CarStatus不能创建后台技能表。
6. 现有资料未提供PsAssistant包名/签名对应的有效AUTH_CODE、OPPO分配的companyId及已登记typeCode。本次FULL包保留明确的“未配置”标记，不伪造通过状态。

## beta26代码实现

### 官方SDK客户端

- 完整APK在本地使用用户提供Demo中的两个SDK库编译，未复制Demo AUTH_CODE、公司身份、账户和测试密钥。原始AAR不公开发布：GitHub同步自编源码和不可初始化真实SDK的API接口桩，公开CI明确标记VALIDATION；FULL必须在私有库存在时重新编译并重跑测试。
- Manifest配置`carapp.feature_support=1`，私有构建参数`PSA_OPPO_AUTH_CODE`和`PSA_OPPO_COMPANY_ID`默认未配置。
- 移除旧库Manifest中错误的`com.coloros...OplusCarReceiver`及宽泛QUERY_ALL_PACKAGES；使用受系统蓝牙权限保护的自有初始化接收器和明确包可见性。
- Application及系统初始化事件均服从用户总开关、首用同意、真实登录/车辆和完整配置预检。只传`FLAG_CAR_CONTROL=2`，不初始化蓝牙钥匙。
- 注册ICarControlCallback，响应inquireCarInfo、inquireCarStatus、onControl、授权/订阅/解绑结果、显示卡片及当前车辆请求；记录真实阶段/错误码。
- SDK授权/订阅成功值是**1**，云端REST成功值是**0**；两者分开处理。初始化成功仍不冒充授权/订阅成功。
- CarInfo使用稳定去标识车辆ID与账号范围ID，不发送原始VIN/车牌；companyId和typeCode必须是分配/登记值，图片为HTTPS URL。
- 补充最后已授权车辆的Keystore加密缓存：冷启动可恢复当前选择，不需要先打开主界面或为查询卡片重新发现车辆；切车、退出或换账号时清理，不建立明文VIN副本。车况仅使用主程序缓存。SOC转为0～1比例；缺少SOC/续航/车锁等必要数据时不上传默认零值。车况时间变化才推送，显式查询直接返回缓存，不触发车辆刷新。

### 命令和安全语义纠错

| CarLink指令 | 映射 |
| --- | --- |
| CAR-DOOR OPEN / CLOSE | 解锁 / 上锁 |
| LOCK-CAR OPEN | **上锁**；不支持CLOSE。旧映射把OPEN当成解锁，已修正 |
| ALL-WINDOW OPEN / CLOSE | 全窗开启 / 关闭 |
| CAR-BOOT OPEN / CLOSE | 尾门开启 / 关闭 |
| AC-TEMP OPEN / CLOSE | 空调开启 / 关闭；数值22等参数不再被忽略后误当“启动” |
| CAR-HORN / DOUBLE-FLASH / CAR-HORN&&DOUBLE-FLASH OPEN | 鸣笛 / 闪灯 / 鸣闪；不把CLOSE重置事件当成新命令 |

- 每个SDK请求立即给唯一taskId和60秒窗口，校验当前车辆、会话、支持指令和并发槽位。
- 按主APK现有逐操作设置执行直接发送、确认或强生物识别；内部验证Activity不导出，不接受外部动作/验证结果。SDK的onVerify回调不自动视作本应用验证通过。
- 主协调器增加每个命令独立完成回调，避免同时操作不同功能时串用全局operation状态。
- 只在回读确认后向SDK报CONTROL_SUCCESS。服务端接受、超时、鸣笛/闪灯未有状态证据时不报已验证成功。
- SDK路径不自动补发锁车写指令；相同车辆/方向有5秒重复写冷却。同一功能已有任务时拒绝重复，不建立周期任务。
- 原主程序手动操作的既有锁车回读/补发策略保留；本轮不把SDK路径再叠加到该补发链上。

净化不在公开InstructionType标准枚举中，不能用AC-TEMP冒充。既有MyDevices/主程序净化仍可走自己的China命令；想加入官方CarLink卡片/手表需另行确认扩展技能及后台配置。

### Root与LSPosed

新增设置：**OPPO互联 → CarLink接入与Root定向诊断**。

- Root测试官方绑定入口：从已安装CarLink的PackageManager信息确定目标Activity，以root启动该固定组件/固定bind URI；不全局授予IOT、不改系统APK/SELinux/鉴权结果。
- Root一键定向导出：仅复制安装包代码到私有临时缓存，解析DEX声明的类/方法原型，完成后删除临时APK。只保留脱敏静态契约，正文上限约180KB。当前扫描base APK，split数量会标明，缺失符号不证明split中也不存在。
- 不读取车企用户数据库、Token、VIN、原始日志、静态字段值或方法体。最终仍走≤2MB轻量ZIP，并包含当前SDK阶段、宿主版本和模块回报。
- CarLink LSPosed作用域新增真实Service绑定入口观察：对ActivityThread.handleBindService的精确形状安装保护式Hook，记录目标服务类名及SDK action。只观察、不修改返回结果；最多记录16种静态入口，避免高频日志。
- “hooks-entered / probe-installed / binder-service-entry”分别表示加载/探针/绑定入口被经过，均不代表SDK鉴权、车辆绑定或指令成功。

Root能解决本机受保护入口测试和静态/运行时观察；OPPO服务器签发应用身份、接受车型和技能配置是独立条件。下一步应根据导出的真实宿主契约决定是否另做仅作用于Polestar的本地协议适配，不能按猜测强行修改未知宿主类。

## 云端三种身份不可混用

来源：[11111](https://open.oppomobile.com/documentation/page/info?id=11111)、[11112](https://open.oppomobile.com/documentation/page/info?id=11112)、[11113](https://open.oppomobile.com/documentation/page/info?id=11113)、[11306](https://open.oppomobile.com/documentation/page/info?id=11306)。

| 身份 | 用途 | 放置位置 |
| --- | --- | --- |
| SDK AUTH_CODE + companyId +签名 | App与系统CarLink SDK鉴权 | 本应用私有构建输入；不是云端token |
| appKey + serverSecret | generate-app-token，SHA256(appKey+serverSecret+毫秒timestamp) | 业务服务端；token24小时，另有10分钟过渡期 |
| appId/appSecret、用户OAuth code/accessToken/refreshToken | 用户级绑定、车况和指令请求 | 经用户授权的业务服务端 |

已新增 [服务端签名工具](../tools/oppo-carlink-cloud/README.md)，分别实现token、用户级SHA1和应用级SHA256/Java表单编码；只做本地摘要计算和测试，没有拿示例凭据发真实请求。

官方REST标准还要求OAuth重定向/换令牌/刷新、绑定解绑、carInfoList、carStatus、carControl，以及`/car/api/v1/msg/notify`异步结果。没有部署这些业务服务，也没有把serverSecret放进APK。

上传工具存在需要联调确认的细节：文档文字称name:value，SignUtils实际为key+value；appSecret与serverSecret不能混用。实现明确跟随对应Java工具，不在验签时擅自重新格式化body。

## 下一步验收顺序

1. 安装beta26 FULL，在LSPosed只启用内置或独立模块中的一份，勾选建议作用域并重启。
2. 主程序登录并刷新一次车况，启用OPPO桥接；打开CarLink页，先看缺项/SDK阶段，不把本地“已绑定”当作SDK结果。
3. 先点“一键Root采集并导出（≤2MB）”，取得本机CarLink 17.x真实服务类、接口签名、版本和入口观察。这个包足以决定下一轮本地适配，避免再上传十几MB系统APK。
4. 如已有本应用正式授权参数，按包名/签名做私有构建，并填写登记的typeCode/typeName/image；依次验证初始化、授权、订阅、车辆信息、车况、一次空调指令回读。
5. 手机负一屏链路验证后，再单独记录Smart controls/Watch点击事件；不可把手机SDK成功当作手表长续航成功。
6. 若考虑云端替代，应先确认应用身份、服务域名和车型技能登记条件，再做完整OAuth与异步结果服务；它不是用root替换一个appKey就能完成的路径。

## 当前验收边界

代码实现/编译和实际终端验收分开记录。本轮没有连接到用户手机或车辆，也没有获配有效OPPO开发者授权，不能标记“CarLink已完全打通”。主APK原有独立中国区功能、Widget、外观菜单和极简模式继续保留。最终测试、签名、大小见交付验证记录。

## 并行提交核对与后续修正

2026-11-02核对远端：OPPO工作分支在`d895dcb`之后新增`8bae900`（过期车况与精确状态匹配）、`32e8913`（三处Lint修复）、`79960a5`（CI临时调试签名）。已完整快进保留；`79960a5`的Actions运行`41428430424`通过。提交作者信息只能确认GitHub账号，不能据此识别具体AI。其他应用主分支最新提交仍为2026-10-30的`5d9a8b3`；v093工作分支为2026-10-28的`5720c9b`，不能据“main”名称判断比本分支新。

- 修复SDK的隐式服务绑定观察：同时识别显式组件和`Intent.setPackage(com.heytap.opluscarlink)`；通过ActivityThread已创建的服务实例解析类名，失败仍记录implicit。只记录静态入口，不读取Intent extras或Binder数据。
- 服务断开/鉴权错误清空上一次授权和订阅结果；重新初始化不继承旧成功。停止后的迟到授权回调不更新状态。收到车控及实际发送前都要求本次真实授权成功。
- Root复制目的文件先由本应用创建，以保留正确SELinux标签；不修改系统SELinux策略。
- 服务端签名校验对非ASCII或非法输入返回失败，不让异常中断处理；增加回归验证。

这些修正改善观察与执行可靠性，不改变前述OPPO服务器授权、车型技能配置和真机验收要求。


构建路径说明：自动审核阻止了将用户上传资料中的SDK二进制公开到GitHub，原因是未明确授权该公开分发。已采用仅同步源码、接口桩及SDK摘要的替代路径；原始SDK只用于本地完整APK构建。
