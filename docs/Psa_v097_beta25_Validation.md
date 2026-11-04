# PsAssistant v0.9.7-beta25 /139 交付验证

日期：2026-11-02。内置 Bridge 1.11.0 /23。主 APK 可独立运行；LSP接入为可选功能。

## 交付与验证

- 文件：`PsAssistant-v0.9.7-beta25-Standalone-FULL.apk`
- 包名：`io.github.polestarcn.companion`；versionCode 139；minSdk 26。
- 大小：**9,463,412 bytes（9.46 MB，小于10,000,000 bytes）**。
- SHA-256：`3094852d6f4e47f2a20aa7d97e074bf5a2a68c5a8e4d6d1183e1743b0fd29c6e`
- 签名证书 SHA-256：`d4cd1b06701f9a8829c5ecc5bbee1398a0f8a655703b6cc356398efa30218a7e`，与先前完整包一致。
- APK v2签名、内容摘要、ZIP完整性和未压缩条目对齐均通过检查；已复核manifest中的包名、版本和最低系统版本。
- 完整包内有经校验的已有PsAssistant客户端配置，不需要导入其他APK；私有配置和签名文件未提交GitHub。
- 打包前后逐项比较CI APK，除客户端资产和签名信息外，DEX、资源、模块元数据等未改变。
- 已验证内置 `OppoBridgeModule`、libxposed API 102元数据和宿主作用域。

## 编译与回归

构建源码：[d3149fe](https://github.com/fifadxy/Polestar-CN-API/commit/d3149fe61f1a90758b2dd1c2947c0cdef8357490)。
[最终GitHub Actions](https://github.com/fifadxy/Polestar-CN-API/actions/runs/41403735676) 成功：

- 79个测试套件，**430项测试，0失败、0错误、0跳过**。
- 主APK `lintDebug`、`assembleDebug` 与独立Bridge `assembleDebug` 通过。
- Java模型、源码结构、知识索引和地图测试通过。
- 已查看PS默认、VoLike简洁、简洁2的本轮极简截图；另有所有皮肤及320×568/640×320的控件边界回归。
- 公开CI的VALIDATION包不包含独立登录配置；本次交付的是另行完成本地配置封装和固定签名的FULL包。

## 使用

1. 安装FULL主APK；同签名旧版可覆盖升级。
2. 长按首页车图1.5秒，打开车辆外观菜单，选择皮肤、日夜车图或切换极简模式。
3. 极简模式关闭的按钮隐藏下方反馈，开启后显示状态；空调有计时依据时显示如`15m`，净化在确认启动后显示如`5:00`。
4. 使用OPPO桥接时，在设置的统一OPPO互联页打开总开关，LSPosed仅启用内置或独立模块中的一种，采用建议作用域，更新后重启手机。
5. 真机反馈可在该页面一键导出轻量资料（≤2MB）。卡片绑定数量不证明它位于负一屏，也不证明手表或官方SDK已完成授权。

## 验收边界

本轮完成界面修改、命令后本地显示计时、后台计时停止、OPPO路由检测纠错及原生自有车辆行构建。
OPPO负一屏直接控制、Smart controls官方授权和Watch X2长续航仍需目标设备端到端验证；CI与构造器静态证据不能替代实机结果。
研究结论及下一步见 [beta25知识库](../knowledge/BETA25_APPEARANCE_CONTROLS_OPPO_20261102.md)。
