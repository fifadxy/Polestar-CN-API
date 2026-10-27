# Polestar 2 Digital Key / PAK Research

**状态：STATIC_EVIDENCE_CONFIRMED / EMULATOR_NOT_RUN / VEHICLE_NOT_VERIFIED**  
**基线：** 极星中国官方 Android APK 4.25.0（versionCode 262），SHA-256 `99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef`。  
**分支：** `research/polestar2-digital-key-pak-20261027`，自 `android-polestar-assistant-main` 分出；不改变 Android 正式主线或 Mac 工程。

## 研究目标

确认 Polestar 2 Phone-as-Key 数字钥匙模块的架构、许可与授权依赖，评估新 Android App 在车主授权下能否通过支持的途径独立配对新钥匙。不是复制用户钥匙、私钥、官方签名或模拟车端授权。

## 当前关键发现

- APK 中已静态确认 `Ps2PairingFragment`、`ICupDigitalKeyController`、`com.volvocars.phoneaskey` SDK、`pak.p319.sdk` 及 CCC 另一条技术路线。
- DEX 方法调用关系说明 Polestar 2 控制层通过 Volvo PhoneAsKey 管理层访问 P319 `PAKService` / `VehicleManager` / `Vehicle`。
- APK 本身包含 arm64-v8a 原生库；x86_64 模拟器并不自动兼容。当前分析环境缺少 ADB / Emulator / KVM，未完成 Root 模拟器运行及实车配对。
- 尚未验证独立 SDK 的合法分发、授权、服务端许可证、车辆注册或应用签名依赖。

## 导航

见 [知识库索引](knowledge/00_INDEX.md)、[静态证据](knowledge/01_STATIC_EVIDENCE.md)、[方案判定](knowledge/02_PORTABILITY_DECISION.md)、[Root 模拟器计划](knowledge/03_ROOT_EMULATOR_RUNBOOK.md)、[安全与验收](knowledge/04_SECURITY_AND_ACCEPTANCE.md)、[公开来源](knowledge/05_PUBLIC_SOURCES.md)。

## 原则

不提交原始 APK、DEX、SO、用户 VIN、登录信息、数字钥匙数据、任何密钥与认证报文。研究代码只能使用清洗过的类名/方法名/权限元数据；实验状态机不执行开锁或启动车辆。
