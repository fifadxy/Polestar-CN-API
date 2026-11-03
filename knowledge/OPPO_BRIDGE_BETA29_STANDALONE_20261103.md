# OPPO Bridge 1.15.0 standalone（beta29阶段一）

日期：2026-11-03。范围仅为独立 `oppoBridge` LSPosed模块；主APK仍为beta28，待真机验证后再合并。

## 本轮目标

- 把原来的Provider、4×2实验、基线比较、Root重载和多种导出入口合并为一个模块页面。
- 提供一次性只读全路径检测，以及完全隔离、不会发车的模拟链路。
- 在本地Provider/AssistantScreen目录中发布 `POLESTAR / Polestar 极星` 入口，点击进入本机桥接；不写OPPO真实鉴权或绑定数据库。
- 统一复制文字与轻量ZIP数据源；ZIP硬限制小于10MB，摘要限制约60KB。

## 页面结构

设置只保留总开关和四个作用面：MyDevices、AssistantScreen、Watch/钱包探测、4×2直接控制。操作只保留：

1. 一键检测全部方案（只读、不发车）。
2. 模拟完整链路（不发车）。
3. 一键修复并重载宿主。
4. 诊断与导出（复制统一摘要或导出同源ZIP）。

## 全路径检测

顺序检查Root、相关包版本、PsAssistant桥接、真实车辆只读状态、MyDevices主/卡片进程Hook、17.4.15精确RPK门禁、Converter/4×2快捷项、负一屏Hook、POLESTAR目录观测、Provider行和Watch/钱包宿主观测。检测过程不会提交车辆命令。

## 模拟边界

模拟会在模块私有进程中生成64%/270km的临时Polestar 2状态，复用正式的Provider序列化和锁车/空调/闪灯/鸣笛路由函数。模拟状态不发布给OPPO宿主、不修改 `auth/checkCarBind/hasCarBinding`、不授予 `safe.IOT`，退出测试后不会成为系统绑定状态。

## CarLink数据库

统一诊断可在Root可用时只读列出CarLink数据库文件大小和SQLite schema对象名，不读取表行，不修改数据库，不输出账号、Token、AUTH_CODE、VIN、Cookie或Secret。真实Polestar入口由内存目录注入与Provider产生，可通过停用/卸载模块完整回退。

## 验收顺序

1. 安装独立模块，在LSPosed只启用这一份Bridge并选择scope列表。
2. 重启手机或执行一键修复，重新打开设备空间和负一屏。
3. 先运行模拟测试，再运行一键检测。
4. 车辆绑定目录应出现 `Polestar 极星`；报告中的 `providerCatalog` 应包含 `POLESTAR`。
5. MyDevices 4×2卡片必须依次出现RPK、Converter、shortcut、click、controller回报。
6. 只有后续人工确认后才执行真实闪灯/锁车/空调测试；`lastCommandSent`仍不等于实车已执行。
