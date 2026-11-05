# PsAssistant Beta46 — 构建、源码与签名验收报告

2026-11-05 | Android v1.0.0-beta46 (versionCode 160) | OPPO Bridge v2.4.0 (code 37)

## 源码与 CI

- GitHub 源码仓库：`fifadxy/Polestar-CN-API`
- 完成测试的提交：`628050eef41ea6cf866a21f053645f20c93d1ac2`
- CI：https://github.com/fifadxy/Polestar-CN-API/actions/runs/41999321739 （第 2 次运行尝试；第一次 Gradle CDN 返回 504，重试成功）
- Android App `assembleDebug`、Bridge `assembleDebug`、`lintDebug`、两模块单元测试：全部 PASS。
- 107 份 JUnit XML：**总 558 测试，0 失败、0 错误、0 跳过**。其中 App 546、Bridge 12。

## APK 验证

| 文件 | 字节 | SHA-256 | 备注 |
|---|---:|---|---|
| `PsAssistant-v1.0.0-beta46-Standalone-FULL.apk` | 9,660,780 | `96975b1f220e03effe7e057e228bf1109f4a7d6a8d5dc15e6e3f8e957ab7353d` | Beta46 CI Debug 二进制离线封装（`debuggable=false`）、加入已核准的 China Standalone 登录资源，v2/v3 原签名校验通过 |
| `PsAssistant-v1.0.0-beta46-VALIDATION-debug.apk` | 9,984,974 | `b92b5842adc7393d8d8236e0c04f14380888befc1d683cb5eb32e25de38c` | 公共 CI 原始 Debug APK，未包含私有登录资源 |
| `PsAssistant-OPPO-Bridge-beta46-debug.apk` | 2,131,239 | `d6b15468465efea3bcf616021e2dd18cac1955144728d33ec1be35dfadc1cbe1` | 独立 OPPO Bridge / LSPosed 调试模块 |

- FULL 签名证书 SHA-256：`d4cd1b06701f9a8829c5ecc5bbee1398a0f8a655703b6cc356398efa30218a7e`
- 与 Beta45 FULL 签名证书完全一致；Beta46 的 versionCode=160 大于 Beta45 的 159，可满足同包名/同证书覆盖升级的基本条件。
- `apksigner verify`：**v2=true、v3=true**；ZIP CRC 完整性正常。源 APK 文件内容不变，唯一原始条目差异是 AndroidManifest.xml 的 `debuggable` 置为 false；另增私有客户端兼容资源与 APK 签名条目。
- 独立登录配置来自此前用户已批准的 Beta45 FULL APK，不包含用户账号、密码或车辆 Token。所有密钥及私有二进制仅用于本地离线签名，未推送公开 GitHub。

## 本次 Beta46 实现

已按确认的 9 项：`1B 2A 3A 4A 5A 6A 7A 8A 9A`。

- 移除独立 Watch 测试应用的 Gradle 构建，主 APK 保留手机端 OPPO Watch Indicator。
- LSPosed `Stable / NEW / Mock` 双方案、RPK `Exact / Feature` 校验与回退，按宿主进程独立安装。
- NEW 动态结构验证、有限域内存补丁、负一屏自有 URI 安全处理与 Activity 人工确认后备。
- Mock 隔离，真实命令必须先检查非 Demo、同 VIN 的新鲜快照，后续远控结果仍须车端回读。
- OPPO 系统兼容中提供模式设置、实际宿主回报、只读诊断及一键恢复 Stable。

## 未验证事项

1. 真实的中国区账号登录/车辆列表/指令结果：需要用户在有合法授权的手机和车辆上验收；软件静态签名验证不能替代实测。
2. OPPO 官方 CarLink SDK、safe.IOT 特殊权限、OEM 系统绑定：本次没有官方私有授权证据；模拟会话和 Hook 生效不意味着真实授权。
3. NEW RPK 特征补丁的不同 ColorOS OTA 与负一屏布局：仍需在目标设备上逐版本验证。
4. FULL 目前是**关闭可调试标志的 Debug 构建二进制封装**，**不是经 R8/ProGuard 生成的正式 Release**。不可把它误认为生产级安全强化已完成。
5. 主 APK 内置同一 Bridge：除非测试明确需要，不要在 LSPosed 同时启用主 APK Hook 与独立 Bridge Hook。