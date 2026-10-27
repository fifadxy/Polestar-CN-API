# 05｜官方与公开参考文献（2026-10-27 复核）

## 1. 极星官方：数字钥匙用户行为

1. [极星中国 Polestar 2 App 与数字钥匙 FAQ](https://www.polestar.cn/zh-cn/support/faq/polestar-2-app-and-digital-key/)  
   **可确认：** 极星 2 采用加密蓝牙与云连接，而非极星 3/4 钱包模式；首次管理员关联需要全部实体钥匙；访客配对需要车内至少一把实体钥匙；车辆用户管理可撤销设备。

2. [Polestar 2：激活 Digital Key（2025 手册）](https://www.polestar.cn/us/manual/polestar-2/2025/article/212ec99cb18f734fc0a801517dee3fba/)  
   **可确认：** 官方 App 需要先关联车辆，首次激活时需要车辆/手机有网络，用户在车机与官方 App 中完成正常操作。

3. [Polestar 2：Digital Key 功能说明](https://www.polestar.com/us/manual/polestar-2/2024/article/4eac08088b6b96c1c0a801510d70abdc/)  
   **可确认：** 配对后的手机数字钥匙能实现锁定、解锁与车辆启动；仅适用于符合车型和系统要求的设备。

## 2. Android 平台约束

4. [Android 12+ Bluetooth Permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)  
   **用于：** BLUETOOTH_SCAN / CONNECT / ADVERTISE、低版本兼容权限、neverForLocation 的 beacon 过滤风险。

5. [BLE in the background](https://developer.android.com/develop/connectivity/bluetooth/ble/background)  
   **用于：** 进程死亡、背景重连与系统约束；BLE 连接不保证进程存活。

6. [Companion device pairing](https://developer.android.com/develop/connectivity/bluetooth/companion-device-pairing)  
   **用于：** Android companion association 和位置/后台权限的替代方案。**注意：** Android companion pairing ≠ Polestar OEM 授权钥匙。

7. [Android 14 Foreground service types](https://developer.android.com/about/versions/14/changes/fgs-types-required)  
   **用于：** 未来合法的 connectedDevice FGS 类型与权限设计。

8. [Android 15 behavior changes](https://developer.android.com/about/versions/15/behavior-changes-15)  
   **用于：** 开机广播、前台服务和后台行为回归。

9. [Android Keystore](https://developer.android.com/privacy-and-security/keystore)  
   **用于：** 本项目自己的安全存储和密钥生命周期。模拟器有 KeyMint 服务不代表具备真机相同硬件信任根。

## 3. 公开开发者平台的适用边界

10. [Volvo Cars Developer Portal](https://developer.volvocars.com/) 与 [官方 API 示例](https://github.com/volvo-cars/developer-portal-api-samples)  
    目前可公开核查到的示例集中于联网车辆数据与 OAuth2，**不能据此认定已开放 Polestar 2 Phone-as-Key 设备注册 SDK**。官方示例提供支持渠道 developer.portal@volvocars.com；是否接受 Polestar China 数字钥匙第三方集成咨询需另行确认。

11. [Volvo Cars Developer Portal 条款](https://developer.volvocars.com/terms-and-conditions/)  
    使用 API/软件/授权内容有相应条款和附加许可。不能把公开 API 访问资格等同于 OEM PAK SDK 的重新分发/集成权。

12. [Polestar 2 HMI Emulator](https://www.polestar.com/global/developer)  
    极星官方的 Polestar 2 Android Automotive HMI Emulator 是**车机 UI 测试环境**，不是把 Android 手机变为真实数字钥匙的工具；其软件镜像不能证明钥匙系统与真实车辆 BLE/TCAM 认证兼容。

## 4. 用户提供样本与外部文献的界限

- PAK/VOCMO/P319 具体类名、签名与顺序由**上传官方 APK 4.25.0 的静态分析**支持，不是从公共开发门户获授权而来。
- “客户端没有明显签名校验”来自有限静态扫描，是不能证明服务器不存在应用白名单的**弱负面证据**。
- 官方用户操作手册说明配对必须得到真实车端和用户确认，不可用模拟器结果替代。
- 所有实际开发建议必须标注：研究建议 / 代码已编译 / AVD 已验证 / OEM 已授权 / 实车已验证。
