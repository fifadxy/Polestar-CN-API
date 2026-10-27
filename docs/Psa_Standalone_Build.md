# Psa 独立安装包构建

v0.8.5起，手机不再读取已安装模板、不再打开APK导入选择器。完整分发包内置维护者提供的账户客户端配置，正常手机号/密码登录与服务端授权不变；不会因移除导入提示而绕过账号验证。

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
