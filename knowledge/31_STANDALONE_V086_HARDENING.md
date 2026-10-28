# 31｜Standalone v0.8.6 独立运行加固

日期：2026-10-28  
分支：`feature/android-standalone-v086-hardening-20261028`

## 目标

确保 PsAssistant 的完整分发 APK 在手机上不依赖任何其他 APK，同时降低私有客户端配置被误打进公开验证包或以明文 assets 存放的风险。

## 结构

```text
private build input
   |
   +-- PSA_CLIENT_CONFIG
   +-- persistent signing identity
           |
           v
prepareClientConfig
           |
           v
48-byte PSACFG2 client-signing.bin
           |
           v
signed Release / Preview
           |
           v
ClientSignatureProvider
           |
           +-- bundled v2
           +-- bundled legacy
           +-- encrypted old-install migration fallback
```

## 关键约束

- 手机端不扫描其他包。
- Manifest 不查询第三方参考应用。
- 不存在 APK 文件选择器或运行时导入入口。
- Debug 公开验证包不能带私有客户端配置。
- 完整 Release / Preview 缺配置或签名时直接不构建。
- 客户端配置不是用户密码、Token 或 APK 私钥，但也不宣称不可逆。
- 登录会话仍使用 Android Keystore AES-GCM 保存。

## v2 资产

`client-signing.bin`：

- 8-byte magic；
- 32-byte transformed payload；
- 8-byte SHA-256 check；
- 总长度 48 bytes。

运行时还会对恢复出的 32 字符配置做完整 SHA-256 指纹校验。

## 验收

- unit tests；
- Android Lint；
- assembleDebug；
- Debug APK 不含私有配置；
- Release/Preview 完整构建检查 48-byte asset；
- clean-install 登录仍需本人账号实测；
- 不把编译通过等同于真实车辆功能验证。
