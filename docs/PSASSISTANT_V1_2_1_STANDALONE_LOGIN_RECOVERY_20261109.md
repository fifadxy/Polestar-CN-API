# PsAssistant 1.2.1 — 中国区独立登录修复与交付校验

日期：2026-11-09  
仓库：`fifadxy/Polestar-CN-API`  
基线源码提交：`7908fc27de08c4b73891ca1d50761f7f85aff05e`  
包名：`io.github.polestarcn.companion`，`1.2.1 / versionCode 166`

## 问题根因

公开 GitHub Actions 生成的 `PsAssistant-v1.2.1-VALIDATION-debug.apk` 有意**不打包** `assets/client-signing.bin`，使用临时 Debug 签名；它可以验证界面和代码，但无法独立进行中国区账号登录，也不能当作覆盖安装现有 FULL 版的正式更新包。

这并非要求重新编写登录 API：现有 `ClientSignatureProvider` 会从本 APP 的 `assets/client-signing.bin` 读取经兼容指纹校验的 48 字节 `PSACFG2` 资源。缺失时应显示明确诊断，不使用模拟登录。

## 本次离线修复

仅在本地受控环境，使用用户项目文件库中此前可用的 **Beta40 Standalone FULL** 作为已批准客户端兼容资源的来源，并用历史一致的原签名文件重新签名现有 1.2.1 构建。任何资源的实际内容、签名私钥、密码、Token、VIN、个人信息均**不提交 GitHub**。

已完成离线检查：

- 旧 FULL 文件 SHA-256 与历史校验记录一致
- 48 字节客户端资源头、校验和及现有 `ClientConfigFormat.EXPECTED_FINGERPRINT` 校验通过
- APK 包名、`1.2.1 / 166` 保持不变，业务代码/界面资源保持不变
- `android:debuggable=false`，`android:allowBackup=false`
- ZIP 完整性、非压缩资源对齐、APK v2/v3 签名验证通过
- APK 签名证书与历史 FULL 证书一致（公开证书 SHA-256：`d4cd1b06701f9a8829c5ecc5bbee1398a0f8a655703b6cc356398efa30218a7e`）

**交付定性：**该文件是基于 1.2.1 CI 验证代码离线修复、加固和原证书签名的登录恢复安装包，**并非重新通过私有 OPPO SDK 编译的标准 Release**。仅完成离线结构和签名验证；首次中国区账号登录、车辆列表、远控回读和 OPPO 原生兼容仍须在设备上验证。

## 后续避免再次出现

1. **发布包严格分流：**公开 CI `VALIDATION-debug` 不能标记或交付为独立登录 FULL。
2. **标准 Release 私有构建：**复用 Gradle 已支持的 `PSA_PRIOR_PSASSISTANT_APK`（此前可用 FULL）、`PSA_SIGNING_KEYSTORE` 和 `PSA_SIGNING_*` 参数；持久原签名需一致；需私有 OPPO SDK 时在本地受控构建机提供对应 AAR。不要把构建素材加进 Git。
3. **强制校验：**发布前运行 `scripts/verify_standalone_apk.py <apk> --expect present`，同时用 Android SDK `apksigner verify --verbose --print-certs` 检查 v2/v3、证书、包名、版本码与非调试状态；不能以 CI 绿色成功替代私有资源检查。
4. **功能验收：**真机直接登录中国区账号 → 车辆发现/选择 → 电量/里程刷新 → 会话重登 → 授权车控及回读；保留用户明确授权的加密密码存储策略，禁止明文日志。
5. **敏感材料边界：**不上传 FULL APK、JKS、客户端兼容资源原文、手机密码或 Token 到公开仓库。构建完成即清理临时资源和含秘密的日志。

原 `1.2.0` 的账号安全、会话隔离与 OPPO Bridge 逻辑不因此修改。
