# PsAssistant Beta47 — UI 优化与构建验收

日期：2026-11-06  ·  Android APK `1.0.0-beta47 / versionCode 161`

## 功能范围

- 净化未运行时不显示“已关闭”，显示从车辆快照读取的 `AQI`、`PM2.5`；数据缺失时显示 `--`，不生成虚假数值。
- 7 套皮肤 × 8 种首页按钮（门锁、空调、净化、尾箱、车窗、寻车、露营、保养），每套每按钮独立配置宽高比例、边框粗细/圆角、图标/文字大小、按钮位置 X/Y、内容焦点 X/Y。
- 设置入口：`设置 → 外观与交互 → 各皮肤按钮布局`；离线实时预览、按钮单独复原、整套皮肤复原、设置备份/恢复支持。
- 按钮始终留在原加权网格内，车辆远控、数据刷新、OPPO Hook 与露营安全判断不受新视觉参数修改。

## 自动化 CI 验收

构建与测试记录：[GitHub Actions 42054020789](https://github.com/fifadxy/Polestar-CN-API/actions/runs/42054020789)。源提交：`e2ad8845621bda1764a759e26c31c46a9338a755`。

| 测试范围 | 通过 | 失败 | 错误 | 跳过 |
|---|---:|---:|---:|---:|
| Android 主程序 | 552 | 0 | 0 | 0 |
| OPPO Bridge | 12 | 0 | 0 | 0 |
| **合计** | **564** | **0** | **0** | **0** |

CI Android 编译、Lint、APK 构建、单元测试及版本检查均成功。

## APK 文件完整性和签名

| 文件类型 | 文件名 | 字节 | SHA256 |
|---|---|---:|---|
| FULL | `PsAssistant-v1.0.0-beta47-Standalone-FULL.apk` | 9,668,973 | `0fc9d41c14e28d57cd93e6fe7243d7997d072dc8d8d962bff9f08a02dd184d1a` |
| Debug | `PsAssistant-v1.0.0-beta47-VALIDATION-debug.apk` | 9,994,154 | `17bfe7dcd86cc61e99a5c811d45718b3ad51d59abb6926d0f8a8736a74167d4a` |
| OPPO Bridge | `PsAssistant-OPPO-Bridge-beta47-debug.apk` | 2,131,263 | `4c8218a0448b6bbfa9419a8fec2acd6b4fcd89d8a21f388a877cedc8849f0073` |

- 完整 FULL 版的 AndroidManifest `debuggable=false`。包名 `io.github.polestarcn.companion`、versionCode 161。
- FULL 复用以前批准的 PsAssistant 独立登录兼容资源（未写入 GitHub），资源长度 48 字节且原始密文指纹一致。
- APK v2 / v3 签名验证通过，证书 SHA-256 与 Beta46 FULL 完全相同：`d4cd1b06701f9a8829c5ecc5bbee1398a0f8a655703b6cc356398efa30218a7e`。
- 对比本轮 CI 编译的主 APK，全部原始 DEX 和资源条目逐字节保持一致；仅有 Manifest `debuggable` 从 true 调整为 false，再加入独立登录资源和签名文件。
- FULL 版约 9.67 MB（低于 10,000,000 字节目标）。
- 此 FULL 为 **CI Debug 编译代码的离线封装并关闭 debuggable**，并非 R8 优化正式 Release 变体。

## 真实设备验收边界

测试没有使用真实中国区账户登录或车辆进行指令回读，也没有通过 OPPO 官方 CarLink 认证。需要在 ColorOS 17 及其它目标设备上进一步检查：各套皮肤在横屏、小屏、大字体的按钮边界，空气读数的新鲜度，以及相同签名的覆盖安装。

为避免泄露，签名私钥、独立登录资源、正式 FULL APK 均只交付给用户，**不进入公开 GitHub 仓库**。