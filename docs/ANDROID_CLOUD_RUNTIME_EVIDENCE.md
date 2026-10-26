# 极星中国 Android 4.25.0 云端运行时证据

> 整理日期：2026-10-26（Asia/Hong_Kong）
> 采集对象：官方 `com.polestar.china.android` `4.25.0-262`
> 环境：Android 11 / API 30 `google_apis_x86_64` userdebug AVD，纯 QEMU TCG，无 KVM
> 结论级别：Java/UI 运行时已验证；ARM64 native、真实账号与车辆端到端仍未验证

本文记录云端 ADB/root、Frida 和界面运行结果。原始 APK、截图、UI XML、`/proc/maps`、logcat、代理 CA、流量原文和 Frida 输出保存在隔离分析区，不进入 Git。本文只保留可复查的非秘密结论。

## 1. 输入身份

运行的 APK 先通过 [`CHINA_ANDROID_APK_PROVENANCE`](CHINA_ANDROID_APK_PROVENANCE.md) 和 [`verify-polestar-cn-apk.sh`](../Tools/apk/verify-polestar-cn-apk.sh) 核对：

| 项目 | 结果 |
| --- | --- |
| package | `com.polestar.china.android` |
| version | `4.25.0` / `262` |
| APK size | `135944423` bytes |
| APK SHA-256 | `99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef` |
| signing | v2；单一 signer；证书 SHA-256 `ae2e24b9dabb4caf85ca8d8b40ecf1bce04b7ebde66ab236dc05ff77b087038f` |
| DEX | `classes.dex`–`classes16.dex` 全部与 Layout Inspect 导出一致 |

## 2. AVD 与 root 边界

AVD 运行属性：

```text
ro.build.type=userdebug
ro.debuggable=1
ro.product.cpu.abilist=x86_64,x86,arm64-v8a,armeabi-v7a,armeabi
ro.dalvik.vm.native.bridge=libndk_translation.so
ro.enable.native.bridge.exec=1
```

`adb root` 返回 UID 0。官方 APK 被 PackageManager 识别为 `primaryCpuAbi=arm64-v8a`。由于宿主没有 `/dev/kvm`，安装期 `speed-profile` dexopt 和首次类校验极慢；运行时使用 `run-from-apk`，并通过 Android 的 `wrap.<process>=logwrapper` 延长调试进程附加窗口。

为隔离纯软件虚拟化导致的 JPush 子进程启动超时，测试设备临时禁用了以下三个组件：

```text
com.polestar.jpush.service.JpushService
cn.jpush.android.service.PushService
cn.jpush.android.service.DataProvider
```

这项改动只用于让主进程在 TCG 下稳定进入 UI。它意味着本次结果不能用于证明推送初始化、后台保活或 JPush 网络行为；账号、车辆和 UI 代码仍来自未重签名的官方 APK。

## 3. 已验证的运行时路径

### 3.1 进程与 DEX

主进程成功启动并进入 `com.polestar.cn.ui.HomeActivity`。`/proc/<pid>/maps` 显示 16 个 DEX 均从已安装的 `base.apk` 解压到内存，包括只有 1,708 bytes 的 `classes2.dex`。Frida 17.24.3 可通过 root server 附加该进程。

一次 `Java.enumerateLoadedClassesSync()` 观察到 27,518 个已加载类；其中可确认以下网络/认证类已实际装载：

```text
okhttp3.internal.connection.RealCall
retrofit2.Response
net.openid.appauth.IdToken
com.polestar.repository.data.account.LoginToken
com.polestar.repository.data.account.RefreshGlobalTokenRequest
com.polestar.explore.core.auth.AuthClientImpl
com.polestar.carcontrol.auth.PolestarIdAuthImpl
com.polestar.remotecarservice.network.RetrofitClientImpl
com.polestar.base.interceptor.MyHttpLoggingInterceptor
```

运行时反射只读取字段和方法签名，没有读取对象值。`LoginToken` 的字段为：

```text
accessToken: String
refreshToken: String
expiresIn: int
tokenType: String
userName: String
userUid: String
globleToken: LoginToken.GlobleToken
```

这与 16 DEX 静态证据中的 Local + `GlobleToken` 双 token 模型一致。`PolestarIdAuthImpl` 运行时字段类型同时引用 AppAuth、`SharedPreferenceManager` 和 `PscnAccountRepo`；`RetrofitClientImpl` 持有 `okhttp3.OkHttpClient` 并按 base URL 创建 Retrofit。

### 3.2 UI

已按正常首次启动路径观察：

1. `SplashActivity`；
2. `HomeActivity` 隐私政策对话框；
3. 同意后进入“极屋”未绑定车辆页面，页面只显示“绑定车辆”；
4. `PscnLoginActivity` 成功显示手机号、短信验证码、获取验证码、密码登录切换和协议勾选 UI。

测试没有填写手机号、请求短信、输入验证码或登录账号。因此本节证明官方 4.25.0 Java/UI 能在该 AVD 中运行，不证明短信、token 签发、车辆列表或遥控权限。

“极屋”未绑定车辆页没有显示充电口操作。结合真机 Layout Inspect 与 controller 静态矩阵，项目结论保持为：**Polestar 2 不支持、也不应展示充电口按钮**。APK 中 type `2` 只是通用扩展数据模型预留项。

### 3.3 native 边界

日志确认 `libndk_translation` 初始化，但 `libpag.so` 的直接加载失败：

```text
libpag.so is for EM_AARCH64 (183) instead of EM_X86_64 (62)
```

对应的 `PAG.SDKVersion()` JNI 实现因此不可用。采集时 `/proc/maps` 没有 `libvocmo-lib.so`。所以该 x86_64 AVD 适合 Java/Kotlin、Activity、View、ClassLoader 和 OkHttp 元数据分析，不能替代真实 ARM64 设备验证 PAG、VOCMO、Digital Key 或其他 native 路径。

## 4. 脱敏网络观察

Frida hook 只输出 method、host、path、header 名和 HTTP status；不输出 query 值、header 值、body、Cookie、Authorization、手机号、VIN 或 token。未登录页面后台观察到魅族 SDK 的三个请求入口：

```text
POST https://fansclub-api.meizu.cn/broadcast/queryHighestPriority
POST https://fansclub-api.meizu.cn/recommend/recommendApp
POST https://fansclub-api.meizu.cn/carousel/queryByCode
```

这三项在本环境中均进入 error 分支；它们属于第三方 SDK 背景行为，不是 Polestar 账号或车辆 API 证据。由于没有执行短信或登录，本轮没有动态发出 `credcode/send`、`oauth/login/phone`、C3 discovery 或 `car-information/car`。这些路径仍保持 `SOURCE_VERIFIED / IMPLEMENTED_UNVERIFIED` 状态。

mitmproxy 12.2.1 已按 metadata-only 方式配置；其 CA 私钥和原始会话不进入仓库。官方 App 对用户 CA 的信任只出现在 `debug-overrides`，因此未修改 release APK 不能把用户 CA 当作 production TLS 证据。真实 ARM64 验证继续使用 [`ANDROID_ROOT_VALIDATION_TOOLING`](ANDROID_ROOT_VALIDATION_TOOLING.md) 的 Frida + PCAPdroid 元数据方案。

## 5. Layout Inspect / LSPosed 状态

官方工具身份已经固定：

| 组件 | 版本 | 校验 |
| --- | --- | --- |
| Layout Inspect universal | `1.1.6-20` | 官方 release `20-1.1.6`；APK SHA-256 `0a6271e971885fb3b214196383bf7d3976d485c111715e136bc1f98ed25e8348` |
| LSPosed Zygisk | `1.9.2-7024` | 官方 release；ZIP SHA-256 `0ebc6bcb465d1c4b44b7220ab5f0252e6b4eb7fe43da74650476d2798bb29622` |
| Magisk | `25.2` | rootAVD 本地基线；Android 11 ramdisk 已创建原始备份并完成 patch 尝试 |

ADB root、Frida、App 运行和 Layout Inspect/LSPosed 安装材料已准备。Magisk ramdisk 冷启动进入了 `magiskinit`，随后 AVB 报告 `vbmeta digest mismatch`，`/system` 挂载失败并进入重启循环；该补丁已停止使用并恢复原始 ramdisk。Zygisk、LSPosed 模块加载和作用域因此没有通过验收，在修复 AVB 链并核对 `su`、Zygisk 状态和 LSPosed 日志前，不得把云端 Layout Inspect 标为 `VERIFIED`。真机导出的布局创建栈继续以 [`CHINA_LAYOUT_INSPECTOR_RUNTIME_EVIDENCE`](CHINA_LAYOUT_INSPECTOR_RUNTIME_EVIDENCE.md) 为正式证据。

## 6. 对 0.19.5 的影响

本轮没有提升真实账号或车辆能力状态，也不改变 0.19.5 的 fail-closed 策略：

- 确认官方 APK 与 16 DEX 的构建身份和运行时一致；
- 动态确认双 token 相关类、字段形状、AppAuth、OkHttp 与 Retrofit 已加载；
- 确认未绑定车辆、手机号/短信登录页面和 Polestar 2 充电口 UI 边界；
- 证明当前云端 x86_64 AVD 可以进入官方 App 的 Java/UI 进程，修正“完全无法进入进程”的旧描述；
- 保留真实 ARM64、真实账号、C3/PCCS telemetry、刷新与重连为发布阻塞项；
- 中国远程命令继续禁用。
