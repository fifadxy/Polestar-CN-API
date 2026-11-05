# Beta46 GitHub 同步与 FULL APK 编译流程

截至当前离线交付：**GitHub 连接器读取/推送持续超时，尚不能确认 Beta46 代码已经同步；Beta46 Android APK 尚未编译，不能提供真实 FULL 文件。** 此文档给出准确的后续可复现步骤，且不会公开密钥。

## 一、将 `.patch` 应用到已授权 GitHub Android 主线

本次补丁基线：`android-polestar-assistant-main` 的 Beta45 提交 `5af208b3ebdcbb919996d97d0e481a48c5d3b7a0`。请先核对仓库的当前提交；若已前进，应在独立分支执行三方合并并检查冲突，不要强制覆盖他人提交。

```bash
# 从仓库的 Beta45 基线创建独立分支
git fetch origin
git checkout -b feature/psassistant-beta46-dual-carlink-20261105 5af208b3ebdcbb919996d97d0e481a48c5d3b7a0
# 将下载补丁文件放置为下面路径（命令中的路径为示例）
git apply --check /private/PsAssistant-Beta46-CarLink-DualMode-Fusion.patch
git apply /private/PsAssistant-Beta46-CarLink-DualMode-Fusion.patch
git add -A
git commit -m "feat(beta46): OPPO CarLink stable/new/mock dual mode and guarded compatibility"
git push origin feature/psassistant-beta46-dual-carlink-20261105
```

CI `.github/workflows/android-beta46-carlink.yml` 会编译 Android 主程序与独立 OPPO Bridge 并运行两组单元测试。**先确保 CI 全绿，再合并/快进到 `android-polestar-assistant-main`**。旧版 Beta44/Beta45 的手表工作流已经从 main 的自动触发中移除，避免因 1B 取消 Watch 子模块而失败。

## 二、FULL 构建（保持原客户端资源/签名）

有 Android SDK 35、JDK17、Gradle8.7 的私有构建环境上：

```bash
export ANDROID_HOME=/private/android-sdk
export PSA_PRIOR_PSASSISTANT_APK=/private/PsAssistant-v1.0.0-beta45-Standalone-FULL.apk
export PSA_SIGNING_KEYSTORE=/private/PsAssistant-test-signing.jks
# 以下密码由私有终端安全输入，不要写进脚本、GitHub 或聊天日志
read -r -s -p "签名密码: " PSA_SIGNING_STORE_PASSWORD
export PSA_SIGNING_STORE_PASSWORD
bash scripts/build_beta46_full.sh
```

预期文件：`android-app/app/build/outputs/apk/release/app-release.apk`。编译任务会校验 `assets/client-signing.bin` 已根据过去批准的 FULL 版本导入，并使用相同的私有签名密钥，保留 `versionCode=160`、`versionName=1.0.0-beta46`。如 release 混淆引起功能回归，必须先修复，再签发；不可把旧 Beta45 APK 修改显示版本号来替代。

**注意：** 上述操作只用于有权使用对应 Polestar 账户和车辆的场景；它不自动解决 OPPO 官方 CarLink SDK、safe.IOT、官方设备绑定或其他签名权限问题。真正的功能完成仍需目标手机的 ColorOS 17 + LSPosed + 真车回读记录。
