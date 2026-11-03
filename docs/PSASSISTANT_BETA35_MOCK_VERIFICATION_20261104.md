# beta35 Mock 实测包交付验证

验证日期：2026-11-04（中国时间）。本版针对 Mock 与负一屏卡片之间的断点；手机宿主实际运行及真实车辆动作尚未验收。

## 安装包

- 文件：`PsAssistant-v0.9.7-beta35-Mock-TestLab.apk`
- 包名：`io.github.polestarcn.companion`
- 版本：`0.9.7-beta35-mock-shelf /149`；内置 Bridge `1.20.0 /32`。
- 大小：**9,525,037 字节（9.53 MB）**，小于 10,000,000 字节。
- SHA-256：`14f6d17caa0b3bf08552ed6cd43934534187a7f303ba39647e45813f05919c51`。
- 签名证书 SHA-256：`d4cd1b06701f9a8829c5ecc5bbee1398a0f8a655703b6cc356398efa30218a7e`，与既有 PsAssistant Test 升级身份一致。
- minSdk 26、targetSdk 34。此包是可调试的 TestLab 测试构建，包含本地 Mock SDK、内置 LSPosed 模块及既有独立登录配置。
- 系统卡片指令在本包内只更新模拟车辆；Mock 成功不会授予 safe.IOT 或建立 OPPO 云端绑定。

## 已完成验证

构建源码提交：`160b147397ccc4824de07cda4a47a72a00b52682`。

[GitHub Actions 41734647375](https://github.com/fifadxy/Polestar-CN-API/actions/runs/41734647375)：成功。

- 主程序 Debug、独立 Bridge Debug、Mock SDK AAR 编译，以及 Debug Lint 通过。
- Mock 构建：495 项测试，0 失败、0 错误、0 跳过。
- 非 Mock 构建：6 项车控授权回归，0 失败、0 错误、0 跳过。这项验证覆盖本应用命令授权，不代表官方 OCS 服务完成认证。
- 实际 17.4.15 RPK 样本校验：1,405,170 字节，SHA-256 `69bdfacad2cd7ad7a65da678b9435eaa54fa7011debb7381164e1d699cfb86e2`。
- 在隔离 JavaScript 环境中执行该样本 CAR 模块，并使用源码定义的 API 绑定修补：点击能发出设备 ID、按钮 ID、按钮值，并停止整卡点击传播。这不能证明手机上的脚本加载器已应用内存补丁。
- 源码结构与知识库检查通过。
- 独立登录配置指纹、ZIP 完整性、APK v2 签名及内容摘要验证通过。无损压缩后逐项验证原编译代码、资源和模块元数据相同；独立登录配置是打包时额外加入的既有配置。
- 从最终 APK 核对版本、MockCarLinkServer、Xposed 入口和作用域列表，均存在。

## 主要修复

1. Mock SDK、IPC 车况、虚拟会话及命令网关共用同一模拟车辆；无需登录真实账号即可运行系统卡片模拟测试。
2. 根据提供的 MyDevices APK，直接匹配 `DeviceQuickAppCardWidgetProvider.handleClickEvent(String, Bundle): Bundle`。原 Hook 排除了 Bundle 返回值；现在按 `deviceId` 等于当前模拟车辆、以及四个已知 CAR 按钮 ID 接管。
3. 固定哈希的 17.4.15 RPK 补丁同时修复 CAR 事件名单与缺少的 `clickEvent` API 引用；补充顶层连接状态和锁车/空调按钮状态。
4. 一键测试按观察到的回调推进；每条模拟指令按任务 ID 核对结果。重置会取消旧测试及尚未执行的模拟状态修改。
5. 自有卡片失败显示原因，重复点击被消耗，避免继续触发原有页面跳转。复制诊断会保留点击及命令时间，帮助区分历史成功与本次测试。

## 手机使用方法

1. 覆盖安装本测试包。LSPosed 中只启用主 APK 的内置模块，停用另一份独立 Bridge，避免重复加载。
2. 作用域包含“我的设备”`com.heytap.mydevices`、负一屏 `com.coloros.assistantscreen`；保留既有 QuickApp 运行环境作用域。更新后重启手机一次。
3. 主程序打开 **OPPO 互联 → CarLink接入与Root定向诊断 → 一键完整 Mock OCS 测试**。完成首用确认后，应看到初始化、绑定、订阅以及八项模拟命令 PASS。
4. 负一屏设备空间中重新选择 **Polestar 2 TestLab** 的 4×2 设备卡。点击锁车或空调按钮，间隔至少 5 秒再试一次；观察模拟状态变化。
5. 返回同一设置页，点击 **复制本次测试及负一屏诊断**。如按钮仍不响应，保留本次点击时间；可使用现有 **一键Root采集并导出（≤2MB）** 提供宿主/脚本加载诊断。

若初始化通过但负一屏没有新的事件时间，问题仍在卡片渲染、脚本加载或宿主 Hook；若已有事件而无命令受理，按会话、PendingIntent 与拒绝原因继续定位。不要将 Mock 授权或绑定标志解释为官方车辆注册成功。

真实 CarLink 授权、ControlRouter、safe.IOT、OPPO Watch 长续航模式，以及真实车辆执行和 12V 影响，均没有在本次构建环境完成实机验证。
