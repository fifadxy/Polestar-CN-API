# 极星中国 Android 构建身份与 APK 验证

> 核对日期：2026-10-24
> 目标包：`com.polestar.china.android`
> 目标版本：`4.25.0`（versionCode `262`）

## 1. 当前可以确认的构建身份

真机进程导出的 16 个 DEX 在两个独立 BuildConfig 类中包含一致的构建信息：

```text
applicationId = com.polestar.china.android
versionName   = 4.25.0
versionCode   = 262
buildType     = release
```

应用初始化代码还把相同的包名和版本号写入崩溃报告配置。完整 APK 已从腾讯应用宝公开 CDN 取得并完成签名及内容校验。

| 核对项 | 状态 | 证据 |
| --- | --- | --- |
| 包名 | **STATIC-VERIFIED** | 两个 BuildConfig 与应用初始化代码一致 |
| versionName | **STATIC-VERIFIED** | `4.25.0` |
| versionCode | **STATIC-VERIFIED** | `262` |
| release 构建 | **STATIC-VERIFIED** | BuildConfig `release` |
| APK 容器 SHA-256 | **VERIFIED** | `99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef` |
| 应用宝公布 MD5 | **VERIFIED** | 下载文件计算值与商店记录一致；仓库不保存 32 位原文，避免触发潜在密钥规则 |
| APK 签名证书 SHA-256 | **VERIFIED** | `ae2e24b9dabb4caf85ca8d8b40ecf1bce04b7ebde66ab236dc05ff77b087038f` |
| 独立公开商店记录 | **VERIFIED** | 腾讯应用宝与小米应用商店均列出包名和 `4.25.0` |
| 真机导出一致性 | **VERIFIED** | APK 内 16 个 DEX 与 Layout Inspect 导出逐个完全一致 |

签名校验结果为单一 RSA 2048 位签名者，证书 DN 为 `CN=polestar, OU=polestar, O=polestar, L=Shanghai, ST=Shanghai, C=200000`。证书名称本身不是来源证明；这里的结论同时依赖应用商店页面、应用宝公布 MD5、CDN 对象、有效 v2 签名和真机 DEX 精确一致性。

## 2. 公开来源与 APK 容器

| 来源 | 公开记录 |
| --- | --- |
| [腾讯应用宝](https://sj.qq.com/appdetail/com.polestar.china.android) | App ID `54028161`、`4.25.0`、`135944423` 字节；商店 MD5 已在隔离分析区核对 |
| [小米应用商店](https://app.mi.com/details?id=com.polestar.china.android) | App ID `1042285`、`4.25.0`、129.65 MB、开发者“极星汽车销售有限公司” |
| 应用宝 CDN 对象 | 从应用宝产品页解析；`Content-Type: application/vnd.android.package-archive`；`Last-Modified: 2026-07-24 04:02:15 GMT`；带 32 位文件标识的下载 URL 不写入仓库 |

完整 APK 保存在分析工作区，不进入 Git：

```text
file    com.polestar.china.android_4.25.0.apk
size    135944423 bytes
SHA-256 99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef
signing APK Signature Scheme v2
```

## 3. DEX 构建指纹

以下 SHA-256 来自重新打包前逐个校验通过的 DEX 038 文件。完整商店 APK 解压后，16 个哈希全部一致，证明 Layout Inspect 导出与该 APK 属于同一代码构建，并且本次导出没有改写 DEX header。

| 文件 | SHA-256 |
| --- | --- |
| `classes.dex` | `3ce2e2057f3a481588708011219a188660ec531baa66a53a46ba817f7d25dcf7` |
| `classes2.dex` | `7c04657b5976d391e6356b8d87ef8de368be801364000e57829c5d48044c0e53` |
| `classes3.dex` | `cfc408dfed34481bd099c1be894470c5374126617b609c8c1da902120d605802` |
| `classes4.dex` | `a646d7b65bf6b8c4304fd8fb2c5b8f9cfd038fc05b9eed7d6fc8b3ad0ff143e8` |
| `classes5.dex` | `64f9c9bd05ed28eb10852553233a76fd51296a68e45f529fc8f0aad8a008ab10` |
| `classes6.dex` | `605cabd4593c64ef02ebce8ca7058d05bc8bc4be127478bca4319591c7a5aa89` |
| `classes7.dex` | `85cbd621ab3fe539f2efafce9e29483e93e84a8c19acae8b2280fba8ca42b399` |
| `classes8.dex` | `5c81207dcfe22450e8f5320ea745168b369b26e4268f3760e62378a9a6176d73` |
| `classes9.dex` | `cfac43fe851dc7647423abef243e01735a6e820f104930fdf54b9d8c7ca56140` |
| `classes10.dex` | `9a2f4a99af5a950bd24af2fe78942c885f850a59be8f3d55449721eff2992ae4` |
| `classes11.dex` | `28cc86e93a7414421308d21a01087518c42051a2ca7b18e5dc823e94e9c19d59` |
| `classes12.dex` | `d975a39a90acd51a7e4a936eb99ef2ead50481c5509e74ad1140f83c1c6d228f` |
| `classes13.dex` | `7d94b1816b282c39b855702078f9b1df507c9bbb9379d167b998ded61286b4b7` |
| `classes14.dex` | `17273b8563bfff21b856497b62ac4ca99f153d731b5e792ccb91a8732e369aad` |
| `classes15.dex` | `170bd54c791e42f422dafe3369d43e3b911f15dabdd8203f60771aa856d14ef9` |
| `classes16.dex` | `4961e91bc6f0974dc5670e8af2811ad00d82e85c605e17dca40461d1339f58e4` |

后续版本若出现哈希差异，应先核对包版本、签名证书及类/方法内容，不能只凭单个哈希判断来源。

## 4. 为什么无法从 DEX 直接恢复 APK 下载地址

本构建的更新地址不是静态常量。`JpushReceiver` 接收极光推送自定义消息，解析：

```text
action=update
url=<apk-url>
version=<file-name/version>
tips=<release-notes>
isMandatoryUpdate=<true-or-false>
```

随后 `AppUpdateAction` 把 `url` 和 `version` 交给 Android `DownloadManager`，并把下载文件命名为 `<version>.apk`。这条调用链证明官方更新 URL 由服务端推送动态下发；DEX 只能恢复字段与下载行为，不能恢复某次发布使用的实际 URL。

## 5. 候选 APK 的接受条件

仓库提供 [`Tools/apk/verify-polestar-cn-apk.sh`](../Tools/apk/verify-polestar-cn-apk.sh)。候选文件至少必须满足：

1. ZIP 结构完整；
2. `applicationId` 为 `com.polestar.china.android`；
3. `versionName` 为 `4.25.0`；
4. `versionCode` 为 `262`；
5. APK v2 或 v3 签名校验成功；
6. 签名证书 SHA-256 与已安装官方包或另一个可信版本一致；
7. 16 个 DEX 与 [`Tools/apk/polestar-cn-4.25.0-dex.sha256`](../Tools/apk/polestar-cn-4.25.0-dex.sha256) 全部一致。

验证工具会自动执行全部 7 项检查；签名步骤要求单一签名者、v2/v3 验证成功且证书 SHA-256 与本版基线一致。任何不一致都返回非零状态。文件名、图标、页面标题和第三方下载站的“官方版”标签都不能替代上述校验。

## 6. 为后续版本建立签名连续性

连接运行该 App 的授权 Android 设备后，可执行：

```bash
adb shell pm path com.polestar.china.android
adb pull '<上一步返回的 base.apk 路径>' ./polestar-cn-installed.apk
Tools/apk/verify-polestar-cn-apk.sh ./polestar-cn-installed.apk
```

若 `pm path` 返回多个 split APK，签名基线以 `base.apk` 为主，同时保存全部 split 的文件名和 SHA-256。仓库只记录证书指纹、包元数据和文件哈希，不提交 APK、Token、Cookie、手机号、VIN 或客户端密钥。
