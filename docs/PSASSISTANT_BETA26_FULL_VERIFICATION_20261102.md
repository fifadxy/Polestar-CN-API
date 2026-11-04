# PsAssistant beta26 FULL 验证与使用方法（2026-11-02）

源码提交：`4ea2b729474852f1bc7212391a09a6cf5a571d9b`。Android `0.9.7-beta26-standalone` / versionCode `140`，集成OPPO Bridge `1.12.0` / `24`，包名 `io.github.polestarcn.companion`。

## 交付文件

- 文件：`PsAssistant-v0.9.7-beta26-Standalone-FULL.apk`
- 大小：**9,557,626 字节（9.56 MB，低于10,000,000字节）**。
- APK SHA256：`86209fde518727b502e6d4e2799c3d2dc67f5ffbaa63913dd06025aaba5c6d51`。
- 签名证书SHA256：`d4cd1b06701f9a8829c5ecc5bbee1398a0f8a655703b6cc356398efa30218a7e`，与beta25完整包相同；APK v2内容摘要及签名验证通过。
- 已包含批准的独立China登录配置；不需要再导入其他APK或另装桥接模块。本轮没有使用真实账号/手机/车辆做登录或车控验收。
- 采用真实用户提供SDK（CarLink 1.4.0-SNAPSHOT / base1.0.9）编译；DEX中确认109个官方CarLink已定义类，包含inner实现。不是公开CI接口桩包，也不宣称所用SDK已经升级为文档中的1.4.9。
- ZIP无损重压缩和重新签名逐项核对：除独立登录配置与签名外，DEX、资源、图片及模块元数据的解压字节不变。

## 提交核对与修复

已保留同分支后续的`8bae900`（过期车况与精确匹配）、`32e8913`（Lint）、`79960a5`（CI签名）三条提交。作者字段只能确认GitHub账号，不能区分由哪个AI生成。`79960a5`的Actions `41428430424`成功；其他应用主分支`5d9a8b3`和v093分支`5720c9b`日期更早。

本次增加`2ff95a3`（隐式服务绑定与旧授权失效）、`2926f65`（受限连接回调）、`4ea2b72`（识别主程序实际“未锁”状态）。完整差异和33项官方文档索引见[研究记录](../knowledge/OPPO_CARLINK_PRIORITY_BETA26_20261102.md)。

## 验证

- 真实SDK：`2926f65`全量449项测试，0失败、0跳过；最后`4ea2b72`改动另跑原生卡片与快捷入口11项定向测试，0失败、0跳过。
- 最终源码真实SDK构建：`:app:lintDebug :app:assembleDebug :oppoBridge:assembleDebug`成功；Lint **0错误、96警告**，并非零警告。
- 源码结构、知识库、纯Java模型、地图距离检查通过；云端签名工具5项测试通过。
- 公开CI：`2926f65`运行`41465647335`成功；最终`4ea2b72`运行[41467050874](https://github.com/fifadxy/Polestar-CN-API/actions/runs/41467050874)也已成功。公开CI不含私有SDK实现与独立登录配置，其VALIDATION APK不能替代本FULL文件。
- 私有SDK原库未发布GitHub。自动审核拒绝该二进制公开发布后，改为公开自编源码、API契约及摘要，本地使用真实SDK构建完整交付。

## 使用方法

1. 安装FULL包。LSPosed只启用内置模块或旧独立模块其中一份；使用推荐作用域并重启相关宿主/手机，避免重复注入。旧CI包若签名不同，不保证覆盖安装；不要为此直接删除现有账号数据。
2. 主程序登录并手动刷新一次真实车况；在OPPO互联设置启用桥接，进入“CarLink接入与Root定向诊断”。车辆长按1.5秒的外观菜单、极简模式及“空调/净化”短名称继续保留。
3. 优先点“一键Root采集并导出（≤2MB）”，授权本机su后分享生成的`PsAssistant-OPPO-light.zip`。只采集已安装CarLink的base APK静态类/方法签名及阶段信息，不附完整系统APK、数据库、令牌、原始日志或VIN。split数量会标明；未发现符号不代表split也不存在。
4. “Root测试官方绑车入口”仅验证固定Activity能否启动，成功不等于绑车/鉴权成功。LSPosed探针同时识别显式和按包名绑定，`binder-service-entry`也只证明入口被经过。
5. 有正式接入参数后，按本包签名私有配置companyId、AUTH_CODE，并填写OPPO后台已登记的typeCode、typeName和HTTPS图片URL；依次初始化、授权、订阅，检查真实回调后再验证一次空调指令和更新后的车辆回读。未配置时页面明确显示缺项，不用Demo车企身份代替。
6. “允许CarLink按需更新车况”默认关闭。开启后，仅当前车辆、SDK本次授权成功、旧缓存且无正在执行的任务时，处理系统`activateCar`连接请求；进程内5分钟最多一次，失败同样占用冷却，长停省电优先。普通读卡只读缓存，没有新增轮询或BLE扫描。

## 尚未通过的终端验收

设备卡片可添加、显示Connected、本地SceneService绑车和模块加载，均不能证明官方CarLink账号授权/后台车型技能、原生设备中心和负一屏直接控制、Smart controls或Watch X2长续航控车已贯通。Root不会生成OPPO服务器授权。净化不是公开标准InstructionType，继续使用主程序已有命令，不能冒充AC-TEMP。

下一步依据本机≤2MB资料包确认真实CarLink服务与接口、各进程的新模块版本和回调，然后先验收手机单次控车，再独立验收手表模式及命令回读。当前安装包可以用于这些步骤；这些待验项目没有被标为已完成。
