# PsAssistant beta8：原生卡片契约修复

目标：OnePlus PJZ110 / Android 16 / 16.0.10.501(CN01)；MyDevices 17.4.15、负一屏 17.14.60、桌面 16.6.17。
证据来自用户完整研究包；APK 与反编译结果摘要见 evidence/oppo-mydevices-17.4.15-native-contract.json。仓库不上传 OEM APK 或反编译实现。

## 已确认的静态规则

- DeviceModel.shortcuts 使用 compat.ShortcutMenu，输入 ID 字段是 shortcutId；不能改成下游 Shortcut.id。
- ShortcutMenuKt.toShortcut 保留 style、componentType、componentExtra；isShowOnHomeCards 只有电视电源入口默认为真。
- Shortcut.isSupportShowOnHomeCards 接受 isShowOnHomeCards 或 style 的第 8 位。LocalPackerParser.f 与 DeviceCardWidgetProvider 的单快捷入口都按这个条件选择操作。旧 CAR style=0 无法通过该条件。
- CardsUtils.p 的 componentType=2 路径调用 CardsUtils.b，经 ContentResolver.call 发送 componentExtra 中的 authority/URI、method、extra。arg 与 location 是另外的参数；现有桥接 transport 与此吻合。
- AbstractC1308fi.d 从 CAR 的 DeviceInfo.statusData.data 字符串解析 CarStatusInfo；C2841ya.b 从 DeviceModel.data 解析同一模型供车辆 TSL 转换使用。仅填写顶层电池数组不等于填写原生车辆续航。
- LocalPackerParser.p 平时显示 totalEnduranceMileage，电池正在充电时显示电量百分比；v 读取 msg.text；更新时间来自原始数据。不能以卡片重新查询的时间替换源时间。

## beta8 / 119，桥接 0.7.0

- 快捷项 style 改为 8，保留明确的 provider 指令入口。
- data 填入 JSON 字符串形式的 CarStatusInfo：真实已知电续航/总续航、油续航 -1、明确标注缓存的摘要。
- 续航未知或非法时保持 data 为空，避免 OEM 把未知数据渲染为 0km 或 -1km；真实 0km 保留。简版卡片继续保留名称中的缓存摘要。
- 不伪造原生授权、车辆操作成功、当前刷新状态；修正仍报 0.5.0 的模块诊断版本。
- 回归覆盖：首页过滤条件、provider extras、data 字符串类型、未知/零续航、原始时间。

## 真机验证

1. 覆盖安装集成版 beta8，保持集成开关打开；检查 LSPosed 作用域并重启设备空间相关进程。
2. 在 PsAssistant 手动刷新一次，重开卡片。若宿主缓存旧布局，只删除并重新添加 PsAssistant 车辆卡片，不清空全部设备空间数据。
3. 分别检查小卡与展开卡。小卡模板可能只挑第一个满足条件的按钮，不承诺四按钮同时显示。
4. 对照源数据检查续航、充电百分比和更新时间。
5. 用户主动点击一次上锁，观察实车并读回；accepted/queued 不代表车辆成功。
6. 再导出轻量包检查版本和模块历史。该包不含车况与指令日志，不能单独证明操作链路成功。

## 下一步研究

- 追踪 QuickAppCardAction 使用的 C0792Yh 工厂与控制器 Map 的语义；现有代理返回空 Map 仍是另一条路径的待查点，不填猜测键名。
- 查清实际负一屏卡片选择原生还是快应用模板，确认数据转换后哪些操作可见。
- 独立区分刷新回调与详情页导航，不把任意单参数回调映射为车控。
- 负一屏“我的车辆”品牌绑定与设备空间卡片是不同入口；本次不代表官方品牌注册成功。
- Watch X2 长续航模式需要单独采集手表双模式资料；手机 APK 无法证明低功耗系统的指令通路。

本次是有精确版本证据的兼容修复；显示、点击、实车响应与手表支持仍需逐项真机验证。
