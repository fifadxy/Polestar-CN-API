# Psa 独立安装包构建

v0.8.6继续使用单 APK 独立架构：手机不读取已安装模板、不打开 APK 导入选择器，也不查询其他伴侣应用。完整分发包内置维护者提供的账户客户端配置，正常手机号/密码登录与服务端授权不变；不会因移除导入提示而绕过账号验证。

## 本地完整构建

维护者在私有环境提供`PSA_CLIENT_CONFIG_FILE`，指向只含当前有效客户端配置的文件（固定格式由Gradle校验）。也支持`PSA_CLIENT_CONFIG`环境变量；文件优先。不要把值写入源码、文档、命令日志或Git。签名仍通过已有`PSA_SIGNING_*`变量设置。

```sh
PSA_CLIENT_CONFIG_FILE=/secure/client-signing.txt \
PSA_SIGNING_KEYSTORE=/secure/app.keystore \
PSA_SIGNING_STORE_PASSWORD="$PRIVATE_STORE_PASSWORD" \
gradle --no-daemon :app:assembleRelease :app:assemblePreview
```

Gradle将配置写入被忽略的`app/build/generated/standaloneAssets`，再打包为应用资产；正常运行只读取本应用资产，读取一次后在内存复用。客户端资产不是用户密码/token，也不是APK签名私钥；不将它描述为不可提取。保留旧版加密配置兜底以兼容升级。

`prepareClientConfig`每次执行：输入缺失时删除旧生成文件，避免开发验证包意外沿用上次私有配置。完整分发任务缺少配置会失败。不要发布中间任务产生的未完成文件。

## GitHub Actions

可配置`PSA_CLIENT_CONFIG`Secret；CI不会打印其值。有该配置及已有持久签名Secrets时，工作流才上传完整Release/Preview。缺失时仍执行源码、模型、知识库、地图JS、Android测试/Lint并生成Debug开发验证包；Summary与App登录页均说明缺客户端配置，不能把它当作完整登录版。

公开仓库只包含生成逻辑，不包含配置值、模板APK或完整私有分发包。`.gitignore`排除构建目录、签名文件及客户端配置文件。本轮提供给用户的私有签名APK已完成配置，无需用户自己处理这些构建细节。


## v0.8.6 客户端配置容器

完整分发包的 `assets/client-signing.bin` 不再保存 32 字符明文。Gradle 私有构建阶段会：

1. 校验输入必须为 32 位大写十六进制；
2. 校验当前兼容配置 SHA-256 指纹；
3. 写入 `PSACFG2\0` 版本头；
4. 使用固定版本掩码转换 32 字节内容；
5. 写入截断 SHA-256 校验和；
6. 最终得到固定 **48 bytes** 的 v2 资产。

App 运行时只解析本应用自己的这一资产，验证 magic、长度、校验和和配置指纹。旧 v0.8.5 的 32-byte 资产仍可读取，旧版本已经由 Android Keystore 保存的本地配置也保留升级兜底。

这不是把客户端配置描述为“不可逆秘密”：任何必须随客户端分发的配置都可能被逆向。v2 的目的主要是防止误打包、错误输入和 assets 明文扫描，同时保证最终 APK 不依赖任何其他 APK。

## CI 隔离

v0.8.6 的公开 Debug 验证流程**不注入**私有客户端配置，并额外检查 Debug APK 不包含 `assets/client-signing.bin`。

只有以下两项同时存在时才构建完整 Release / Preview：

- 私有 `PSA_CLIENT_CONFIG`；
- 持久签名 `PSA_SIGNING_*`。

完整 Release 构建后，CI 只验证该资产存在且长度为 48 bytes，不输出配置值。


## 从已核验官方 4.25.0 样本生成完整独立包

对于本人测试环境，也可以不单独准备 `PSA_CLIENT_CONFIG` 文件，而使用已核验的官方 Polestar China 4.25.0 APK 作为**构建机输入**：

```sh
PSA_SIGNING_KEYSTORE=/secure/app.keystore \
PSA_SIGNING_STORE_PASSWORD="$PRIVATE_STORE_PASSWORD" \
PSA_SIGNING_KEY_ALIAS=psassistant \
PSA_SIGNING_KEY_PASSWORD="$PRIVATE_KEY_PASSWORD" \
sh scripts/build_android_standalone_official.sh /path/to/极星Polestar-com.polestar.china.android-4.25.0.apk
```

脚本先校验官方 APK SHA-256：

`99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef`

随后仅从该固定官方样本中定位与已知指纹匹配的**非用户客户端配置**，写入临时文件，再走 v0.8.6 的 v2 资产生成流程。临时目录退出时删除。

该脚本不会复制账号密码、Token、VIN、数字钥匙、私钥、证书缓存或已配对状态；官方 APK 也不会进入最终安装包。手机安装最终 PsAssistant 后不需要官方 APK。
