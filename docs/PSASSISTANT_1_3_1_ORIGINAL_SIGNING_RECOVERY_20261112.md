# PsAssistant 1.3.1 原签名身份恢复与 1.3.2 P2 同签名交付

日期：2026-11-12  
仓库：`fifadxy/Polestar-CN-API`  
历史 FULL 应用包名：`io.github.polestarcn.companion`

## 1. 原签名恢复结果（已验证）

- 私有签名文件已从用户的 **ChatGPT 项目文件库** 恢复：`/Polestar2 Widget&API/PsAssistant-test-signing.jks`
- 文件确为 JKS，包含一个 `PrivateKeyEntry`，别名 `androiddebugkey`，证书主题 `CN=PsAssistant Test, OU=Development, O=PsAssistant, C=CN`。
- JKS 文件 SHA-256：`fd713e09a89cb525c9d19676a8ee4f89e382ac5b70b7f67df5028ab385b50b8a`
- 原签名证书 SHA-256：`d4cd1b06701f9a8829c5ecc5bbee1398a0f8a655703b6cc356398efa30218a7e`
- 签名方案：APK V2/V3；RSA 2048 位，`SHA256withRSA`。
- 与用户库中的 `PsAssistant-v1.3.1-CN-Standalone-FULL.apk` **证书指纹逐字节一致**。
- 公开证书：`docs/certs/psassistant-1.3.1-original-public.pem`。它不含私钥，不能单独签 APK。

**安全规则：严禁将原 JKS、口令、登录客户端配置、个人信息及带私钥的备份提交公开 GitHub。** 用户私人文件库中的 JKS 为实际签名来源，GitHub 仅存放公开证书与本说明。原签名是历史**测试/开发证书**，不应称作正式生产级密钥。

## 2. 1.3.2 P2 同签名恢复

- 源码：`main` 提交 `c6517e109529ba4adcf2c68f893ea5af4d80ecc1`（`1.3.2 / versionCode 177`）。
- 使用此前成功构建的 1.3.2 P2 FULL 安装包，保留其中全部编译后代码和 `assets/client-signing.bin`，并**用恢复的原 JKS 离线重新签名**；并非重新跑带私有 OPPO SDK 的完整 Gradle Release。
- 新 APK 通过 V2/V3 签名校验、ZIP 完整性校验；原签名证书 SHA-256 与 1.3.1 FULL 一致。
- 登录兼容资源和原 1.3.1 FULL 中相应资源的 SHA-256 一致；未复制账号、密码、Token 或 VIN。
- 包名保持 `io.github.polestarcn.companion`，版本号高于 1.3.1，具备**同签名覆盖升级的必要条件**。仍需在设备上实测登录、车辆控制及 OPPO 兼容。

## 3. 以后发布时使用原签名身份

构建机私有环境提供：

```bash
export PSA_SIGNING_KEYSTORE=/private/path/PsAssistant-test-signing.jks
export PSA_SIGNING_STORE_PASSWORD="$PRIVATE_STORE_PASSWORD"
export PSA_SIGNING_KEY_ALIAS=androiddebugkey
export PSA_SIGNING_KEY_PASSWORD="$PRIVATE_KEY_PASSWORD"
export PSA_PRIOR_PSASSISTANT_APK=/private/path/PsAssistant-v1.3.1-CN-Standalone-FULL.apk
cd android-app
gradle --no-daemon :app:assembleRelease
```

私有 OPPO SDK 仍需按现有构建规定提供。中国区独立登录资源可以用 `PSA_PRIOR_PSASSISTANT_APK` 从自己已授权的 FULL APK 恢复，或使用 `PSA_CLIENT_CONFIG` / `PSA_CLIENT_CONFIG_FILE` 私有输入；三者不是 APK 签名私钥。参考 `docs/Psa_Standalone_Build.md`。

GitHub Actions 已存在四个**私有 Secrets**输入：`PSA_SIGNING_KEYSTORE_BASE64`、`PSA_SIGNING_STORE_PASSWORD`、`PSA_SIGNING_KEY_ALIAS` 和 `PSA_SIGNING_KEY_PASSWORD`。必须由有仓库 Secrets 管理权限的人在 GitHub 私密设置中上传，不能在源码提交时写入。工作流额外核对证书指纹，避免误用新密钥发版。即便配置签名，公开 CI 缺少登录配置和私有 OPPO SDK 时仍只会生成 VALIDATION 验证包，不能称为 FULL。

## 4. 安装与验收注意

- 对于已安装 **1.3.0 / 1.3.1 原签名 FULL** 的手机，应优先测试同签名 1.3.2 P2 覆盖升级；勿先卸载丢失本地缓存。
- 如果手机装的是此前 **P2 临时新签名**（指纹 `1807305df8800a1ad423e1c496882db7f80da15a1fe2787ad81ffcce9bca4dce`），它不能直接被原签名包覆盖。
- APK 静态签名校验、自动化测试与真实车端登录/命令回读是不同的验收层级，不以离线校验等同真机验证。
