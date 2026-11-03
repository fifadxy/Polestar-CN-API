# OPPO Bridge 1.15.0 standalone验证

日期：2026-11-03。该包是beta29阶段一的独立LSPosed模块，尚未合并到PsAssistant主APK。

## APK

- 文件：`PsAssistant-OPPO-Bridge-v1.15.0-standalone.apk`
- 包名：`io.github.polestarcn.oppobridge`
- versionCode：27；versionName：1.15.0
- 大小：1,599,486字节（约1.60MB），低于10,000,000字节限制
- SHA-256：`a10eb8fecce2feafc9493a4d0ee6c07bef08f3bfaa548b4db971e1c53f287209`
- APK v2签名验证通过；开发测试证书SHA-256：`f496549510ffa8cd991724c5011ebbbbb6790a70e35f974503bdfcf8472dcf62`
- Xposed入口：`io.github.polestarcn.oppobridge.OppoBridgeModule`

## 自动验证

- Robolectric模块测试：2项，0失败、0错误、0跳过。
- Android Lint：0错误、45警告；警告主要为LSPosed反射、导出只读Provider及现有硬编码界面文字。
- Debug与Release构建成功；ZIP完整性、包版本、Xposed入口和14个scope均完成只读校验。
- 仓库源码结构、知识库和纯Java状态模型检查通过。

## 使用边界

- 测试时只启用独立Bridge 1.15.0，停用主APK内置Bridge scope，避免同一宿主加载两代模块。
- 模拟链路仅在模块私有进程运行，不发送车辆命令，也不写OPPO真实鉴权或绑定数据库。
- `Polestar 极星`入口来自本机内存目录与Provider；真实控制仍由PsAssistant车辆会话执行。
- CarLink数据库诊断只读取数据库文件大小和SQLite schema对象名，不读取表行、不修改数据库。

## 真机步骤

1. 安装独立APK，在LSPosed启用模块并选择建议scope，重启手机。
2. 打开模块，执行“模拟完整链路”，应全部通过。
3. 执行“一键修复并重载宿主”，重新打开设备空间和负一屏。
4. 执行“一键检测全部方案”，确认MyDevices两进程和负一屏均回报模块1.15.0。
5. 进入车辆绑定目录检查 `Polestar 极星`；重新添加4×2卡片。
6. 通过“诊断与导出”复制统一摘要；若仍无入口，再导出小于10MB的同源ZIP。

