# PsAssistant v0.8.9-beta1 · 中国区登录兼容与发行门禁

日期：2026-10-28。基于 v0.8.8-beta1 完整 Android 主线（版本号 90）。不更改已有六套 UI、Widget 规格、手机控制、C3 状态读取和写操作逻辑。

## 根因

公开 CI 缺少私有账户客户端配置时仍生成 Debug 验证包。这个 APK 无法完成 China 手机号/密码认证，登录按钮会在网络请求前提示「安装包配置不完整」。这不是 Token 过期，也不是车辆不在线；不能通过重新输入密码解决。

旧 v0.8.5 Standalone 完整版把经过校验的兼容数据放在 `assets/standalone/account_compat.bin`；随后主线改为 `assets/client-signing.bin`。因此必须在**构建阶段**迁移格式，不应让运行中的手机导入其他 APK。

## 本次修复

1. `ClientSignatureProvider` 同时识别三类内置资源：旧明文 32 位兼容字符串、新版 `PSACFG2` 48 字节资源、v0.8.5 旧 Standalone 32 字节受限格式；另外保留本应用早期 Keystore 加密偏好迁移。所有来源都验证此前批准的配置指纹，错误资源不会伪装成登录成功。
2. 构建提供 `PSA_PRIOR_PSASSISTANT_APK` 环境变量，指向**用户自己的、已能登录的完整 PsAssistant 安装包**（非官方 Polestar APK）。构建机只读取其已有的账户兼容资产，核对指纹后重新编码为 `PSACFG2`；不会导入帐号密码、Token、VIN、数字钥匙或原 APK 的其他文件。
3. 也继续支持 `PSA_CLIENT_CONFIG_FILE` 或 `PSA_CLIENT_CONFIG` 作为已授权私有构建输入。二选一，避免误用不同环境材料。完整 `Release/Preview` 构建必须同时具有正确客户端资源及原有持久 APK 签名。
4. 公共 CI 不添加账户兼容资源。该环境产生的 Debug 包仍只供 UI 和协议回归，**不可作为能登录的完整 APK 交付**。

## 完整本地构建（无需改代码）

已有可用的完整旧版 PsAssistant APK 和原固定签名后：

```sh
export PSA_SIGNING_KEYSTORE="/private/path/psassistant-release.keystore"
export PSA_SIGNING_STORE_PASSWORD="your-local-password"
export PSA_SIGNING_KEY_ALIAS="your-existing-alias"
export PSA_SIGNING_KEY_PASSWORD="your-local-password"
export PSA_PRIOR_PSASSISTANT_APK="/private/path/PsAssistant-v0.8.5-beta1-standalone-full.apk"
cd android-app
gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

也可以用既有私有客户端配置代替 `PSA_PRIOR_PSASSISTANT_APK`。禁止把旧完整 APK、客户配置或签名文件上传到公开 GitHub。本地会话只有在**包名和签名证书都与之前安装的版本一致**时才能覆盖保留。

## 验收边界

Gradle/JVM/Lint及构建内的资源指纹检查可以证明发行包格式正确，但不能替代真实账号登录或车端动作验证。若本地不存在先前可用的完整包或私有配置，本修复仍能让源码在 CI 通过，但无法从公共 Debug 中创造合法有效的客户端签名参数。
