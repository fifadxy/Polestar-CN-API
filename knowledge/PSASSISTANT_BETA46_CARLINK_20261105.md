# PsAssistant Beta46 · OPPO CarLink 双方案技术知识库

2026-11-05 · 主程序 v1.0.0-beta46 /160 · Bridge v2.4.0 /37

**确认决策：1B、2A、3A、4A、5A、6A、7A、8A、9A。** 详细功能、代码文件、安全边界和每项验收在 [Beta46融合说明](../docs/PSASSISTANT_BETA46_CARLINK_FUSION_20261105.md)。

- **OPPO Watch:** 仅主 APK 手机端实验；不再构建独立 watch APK，不能声称表盘指标已通过。
- **LSPosed:** Stable（默认）/ NEW / Mock，单 Hook 安装入口按宿主加载时选择；切换后重启宿主生效；已选择/已观测分别展示。
- **动态适配:** AssistantScreen 反射检查唯一零参 ArrayList 方法、MyDevices HomeCard 精确结构与已知本应用车辆行。新系统变化不匹配则返回原 OEM 结果，不崩溃、不硬改。
- **RPK:** 保留 Exact 固定 SHA 校验，NEW 可选 Feature 锚点唯一校验；仅 4×2/2×2 受限脚本，不改磁盘包。独立 Java fixture 测试已通过。
- **控车:** 仅拦截本应用 VIEW URI，真实状态需近 5 分钟且有有效 vehicleKey；超时刷新不可用则取消。通过主程序原授权/结果回读；fallback Activity 必须用户确认。
- **安全隔离:** Mock 不安装写入/控车 Hook；NEW 不执行 PMS 全局授予、数据库伪 VIN、假已绑定或假指令成功。历史 Stable 原有兼容模拟仍为本地路径，不能当官方 CarLink 授权。
- **单项诊断:** MyDevices 主/卡进程、AssistantScreen、CarLink、RPK、会话、IPC、运行模式；一键全部只读检查和恢复 Stable/Exact。
- **源码测试:** 本地静态 26/26 校验，Feature RPK 实际 Java 编译与正/负用例通过；这不是 Android SDK Gradle、Robolectric 或真车验证。请运行 `.github/workflows/android-beta46-carlink.yml` 完整 CI 后再编译/签发 FULL APK。

正式签发独立登录 FULL：使用相同签名 `PSA_SIGNING_KEYSTORE` 和前一用户批准 FULL APK 输入 `PSA_PRIOR_PSASSISTANT_APK`，由 `scripts/build_beta46_full.sh` 私有构建。不得提交 keystore、客户端资源、Token 或真实账号至 GitHub。
