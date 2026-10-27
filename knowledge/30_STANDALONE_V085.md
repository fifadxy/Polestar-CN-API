# 30｜PsAssistant Standalone v0.8.5：单 APK 独立运行

日期：2026-10-27  
分支：`feature/android-standalone-v084-20261027`  
版本：`0.8.5-beta1-standalone / 87`

## 目标

安装到 Android 手机后只需要 **一个 PsAssistant APK**。运行时不再：

- 安装 `app.c3garage.companion`；
- 从文件选择器导入参考 APK；
- 扫描其他 APK 的 DEX；
- 查询参考 APK 是否已安装。

China 登录、车辆发现、C3 读取、远控、Widget、后台任务和 Digital Key V2.1 诊断均从同一个应用入口提供。

## China 登录独立化

历史版本的 China 登录依赖参考 APK 中的一项固定兼容材料。Standalone 将此依赖从“运行时导入”改为“构建时封装”：

```text
exact verified reference APK (build machine only)
        ↓ SHA-256 gate
extract one compatibility value without printing it
        ↓
opaque account_compat.bin
        ↓
Standalone APK assets
        ↓
ReferenceSignatureProvider
        ↓
existing ChinaPolestarApi login / refresh
```

源码仓库不提交该原始兼容材料。最终 APK 安装后不需要参考 APK。

旧版 PsAssistant 已经完成过模板导入的用户升级时，可继续读取本应用自己已经用 Android Keystore 加密保存的旧兼容数据；不再弹出导入界面。

## Digital Key V2.1

Standalone 同时包含 Digital Key V2.1 的安全诊断框架：

- Android BLE / Nearby Devices 环境；
- Native V2 library presence/load diagnostics；
- AuthorizedFlowAssessment；
- account session / selected vehicle / OEM identity / SDK prerequisite 分级；
- 首个阻塞阶段显示。

OEM Native 库和协议 schema 可由用户提供的官方 Polestar China 4.25.0 APK在**构建阶段**加入最终 APK。不会复制账号 token、VIN、已有数字钥匙、私钥、证书缓存或认证报文。

真正的新设备数字钥匙仍必须通过正常 OEM/车辆授权流程。Native 库加载成功不等于车辆钥匙已配对。

## 构建

```bash
sh scripts/build_android_standalone.sh \
  /path/to/app-release.apk.1.1\(1\).1 \
  /path/to/极星Polestar-com.polestar.china.android-4.25.0.apk
```

第一参数 SHA-256 必须为：

`8cae07a3b5c6050487443914ebb55b1d2f2f26e258f4b369837f2b88de052735`

第二参数（可选）SHA-256 必须为：

`99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef`

最终输出：

`dist/PsAssistant-v0.8.5-beta1-standalone.apk`

## 当前验证门槛

- Java / Robolectric unit tests；
- Android Lint；
- assembleDebug；
- CI 明确检查 MainActivity 不再包含 `ACTION_OPEN_DOCUMENT` / “Import reference APK”；
- Manifest 不再查询 `app.c3garage.companion`；
- 最终 clean-install China 登录仍必须使用构建时封装了兼容 asset 的 APK 进行本人账号实测；
- Digital Key 的真实车辆配对必须单独达到 `VEHICLE_VERIFIED`。
