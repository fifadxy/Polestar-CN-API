# Psa v0.8.6 beta1 更新说明

版本：**0.8.6-beta1 / versionCode 88**  
分支：`feature/android-standalone-v086-hardening-20261028`

## 单 APK 独立运行加固

本版本继续以“手机只安装一份 PsAssistant”为目标，不恢复任何参考 APK 导入流程。

- 不查询、安装或导入 `app.c3garage.companion`。
- 中国区手机号/密码登录继续使用本应用自己的客户端配置与加密会话。
- 登录会话由 Android Keystore 保护，密码不保存。
- Debug 验证包缺少私有客户端配置时明确提示，不伪装为可直接登录的完整包。
- 完整 Release / Preview 只有在私有客户端配置与持久签名同时存在时才构建。

## ClientSignatureProvider v2

`ClientSignatureProvider` 新增版本化客户端配置容器：

- v2 magic：`PSACFG2\0`；
- 最终资产固定 48 bytes；
- 配置格式与 SHA-256 指纹双重检查；
- 运行时校验 magic、长度、截断校验和和完整配置指纹；
- 继续兼容 v0.8.5 旧 32-byte 内置格式；
- 继续兼容旧版 PsAssistant 已经由 Android Keystore 保存的迁移配置。

v2 不是“不可提取”的安全承诺。客户端必须持有的配置仍可能被逆向；本改动主要避免误配置、错误打包和 assets 明文暴露。

## CI / 发布隔离

公开验证 Debug 不注入 `PSA_CLIENT_CONFIG`，并强制检查 APK 内不存在 `assets/client-signing.bin`。

完整签名包流程：

1. GitHub Secret 只在私有配置步骤可见；
2. 写到 runner 临时文件，权限 0600；
3. Gradle 生成 v2 资产；
4. Release / Preview 构建；
5. 仅检查资产长度为 48 bytes，不输出内容；
6. 上传完整签名 APK。

## UI

登录页和设置页更新为准确状态：

- “独立版本 · 无需其他 APK”；
- 完整包显示“v2 客户端配置已内置”；
- 旧版兼容路径会显示“旧版内置配置”或“已迁移旧版本地配置”；
- 缺配置的开发验证包明确提示安装完整签名版。

## 验证边界

本次只改变客户端配置封装、构建隔离和独立安装可靠性，不提升车辆 API 或 Digital Key 的证据等级。真实账号、真实车辆、远程控制和数字钥匙仍分别按既有实车验证门槛验收。
