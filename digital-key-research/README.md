# PsAssistant v0.8.3-beta1 数字钥匙集成分支（只读阶段）

**分支**：feature/android-digital-key-lab-v083-20261027（基于 android-polestar-assistant-main / 0.8.3-beta1）。

本分支只集成已经验证可编译的 Android 研究组件：
- 只读 DigitalKeyResearchActivity，用户点击后申请 Nearby Devices 权限；
- DigitalKeyEnvironment，防止在权限未授权时错误访问 BluetoothAdapter；
- DigitalKeyProvider、ResearchDigitalKeyProvider、DigitalKeyStatus；
- 设置页入口和 Android Manifest 权限；
- Root Android 14 AVD 诊断 UI 冒烟测试。

**不会连接车辆、不执行证书注册或 P319 配对，不包含原厂 SDK AAR/SO 和任何密钥。**

完整的技术分析、证据、实施路线、OEM 授权/身份差距及测试矩阵，统一维护在：
[原数字钥匙研究分支知识库](https://github.com/fifadxy/Polestar-CN-API/tree/research/polestar2-digital-key-pak-20261027/digital-key-research/knowledge)。

首先阅读：
1. [08 移植实施方案](https://github.com/fifadxy/Polestar-CN-API/blob/research/polestar2-digital-key-pak-20261027/digital-key-research/knowledge/08_MIGRATION_EXECUTION_PLAN.md)
2. [09 Android 权限与后台](https://github.com/fifadxy/Polestar-CN-API/blob/research/polestar2-digital-key-pak-20261027/digital-key-research/knowledge/09_ANDROID_PERMISSIONS_BACKGROUND_AND_SECURITY.md)
3. [10 China 身份与 OEM 授权缺口](https://github.com/fifadxy/Polestar-CN-API/blob/research/polestar2-digital-key-pak-20261027/digital-key-research/knowledge/10_IDENTITY_AND_OEM_AUTHORIZATION_GATES.md)
4. [11 证据测试矩阵](https://github.com/fifadxy/Polestar-CN-API/blob/research/polestar2-digital-key-pak-20261027/digital-key-research/knowledge/11_EVIDENCE_TEST_MATRIX.md)

验收前不得将本研究 UI 宣称为已移植数字钥匙，也不把本分支自动合并到主线。

## 2026-10-27 实际 CI/模拟器验收

- 源码 commit（本次 APK）：`a46a9f318cfcff4b952be6929eb4f7ebc3dc84f1`
- Workflow: https://github.com/fifadxy/Polestar-CN-API/actions/runs/40474095279
- Gradle :app:assembleDebug + :app:testDebugUnitTest（3 个 DigitalKeyContractTest 安全单元测试）：PASS。
- Android 14 / API34 Root AVD: ADB uid=0、install Success、Activity topResumed、UI XML assertion、PNG 截图：PASS。
- 安装包 applicationId: `io.github.polestarcn.companion.digitalkeylab`，**不覆盖正式 PsAssistant**。
- Debug APK SHA-256: `a2b111804ab1e4f733b02ae1ad340e3ce05b0b9e87b4edb27fab41b6cc811c27`
- 诊断页面只读，不加载私有 SDK、不配对、不发送任何实体车钥匙命令。
