# 分支盘点、合并结论与 0.19.5 开发入口

盘点日期：2026-10-26。远端完整 refspec 已抓取；以下结论基于提交图、逐分支文件树、差异和契约测试，不仅依据分支名。

## 1. 远端分支与源码状态

| 分支 | 头提交 | 文件 / Swift | 角色 | 处理结论 |
|---|---:|---:|---|---|
| `main` / `android-polestar-china-api` | `6548b2f` | 2 / 0 | 早期 API 文档线，两者同提交 | 文档证据已纳入集成线 |
| `chatgpt/hisingen-china-build-0.16.1` | `2ed1b18` | 2 / 0 | 最早安装/构建尝试，无 Swift 源码 | 仅保留历史 |
| `chatgpt/hisingen-china-build-0.17.0` | `9d6f7a6` | 8 / 1 | 单文件 China integration patch | 已由后续版本包含 |
| `mac-hisingen-china-0.18.0` | `9d6f7a6` | 8 / 1 | 名称虽为 0.18.0，实际与 0.17.0 同提交 | 不重复合并 |
| `chatgpt/hisingen-china-build-0.18.0` | `16ac8b9` | 25 / 2 | Bridge 架构与首轮完整构建 | 仅保留历史，Bridge 已退役 |
| `chatgpt/hisingen-china-build-0.18.1` | `15828e8` | 17 / 2 | 从较早 0.18 节点分出的 Bridge doctor 支线 | 不合入 native 主线 |
| `chatgpt/hisingen-china-native-0.19.0` | `d85ca02` | 32 / 6 | 从 `3390375` 分出的加固支线 | 不整支合并；经核对的加固逐项移植 |
| `chatgpt/hisingen-china-native-0.19.1` | `c55a347` | 44 / 10 | native 产品演进线 | 已被 0.19.4 包含 |
| `chatgpt/hisingen-china-native-0.19.2` | `640aaa3` | 54 / 15 | native 产品演进线 | 已被 0.19.4 包含 |
| `chatgpt/hisingen-china-native-0.19.3` | `7b31e31` | 69 / 20 | native 产品演进线 | 已被 0.19.4 包含 |
| `chatgpt/hisingen-china-native-0.19.4` | `0e2d2f4` | 81 / 26 | 文件最完整的产品基线 | 已通过非快进 merge 纳入集成线 |

0.19.4 与加固版 0.19.0 在 `3390375` 后分叉，没有可直接判定为等价的 cherry-pick。整支合并会把后来的挑战页、安装器和构建流水线回退到旧接口，因此选择“0.19.4 骨架 + 经验证的逻辑移植”。

## 2. 当前源码布局

- `Patches/0.17.0`、`0.18.0`、`0.19.0`–`0.19.4`：归档 patch，便于追溯旧构建；0.19.0–0.19.3 的旧硬编码 OAuth 常量已在当前树脱敏，不再视为可发布构建。
- `Patches/0.19.5`：唯一的新开发入口；包含 Runtime、Session、API、Views、H5 challenge 与高级诊断窗口。
- `Scripts/apply_hisingen_china_0195.py`：把 0.19.5 patch 应用到上游 Hisingen `v2.0.10`。
- `Scripts/install_hisingen_china_0195.command`：验证、备份并安装打包后的 macOS App。
- `Tests/test_native_mac_contract_0195.py`：与 Android 4.25.0 静态证据对齐的离线契约测试。
- `.github/workflows/build-hisingen-china-0195.yml`：Xcode 27 构建、签名、打包和兼容性 smoke workflow。
- `Tools/auth_probe/provision_cn_oauth_secret.py`：只为本地 token 刷新导入 OAuth client secret；不参与首次登录。
- `Tools/auth_probe/*arm64_bridge*` 与 `prepare_tcg_emulator_install.sh`：API 30 x86_64 Native Bridge / root-ADB 模拟器搭建和 TCG 安装准备；不属于 Mac App 运行时。
- `knowledge/china`：长期维护的能力、认证、端点和交接知识库。
- `docs/POLESTAR_CHINA_ANDROID_AUTH_EVIDENCE.md`：16 DEX 的可审计证据链，是修订网络契约的首要依据。
- `knowledge/china/ANDROID_RUNTIME_VALIDATION.md`：模拟器实测边界；当前只证明 root、Native Bridge、APK 字节和 ARM64 包分类，尚未证明 App 进程/JNI。

`Tools/china_bridge` 已删除。它属于 0.18 历史架构，会让读者误以为 native 0.19.x 仍需常驻 Android/localhost bridge；历史说明仍保留在文档和旧提交中。

旧远端提交曾包含已分发 Android 客户端中的 OAuth constant。当前树已脱敏，但没有改写远端既有 Git 历史；该值应按已公开处理并在服务端轮换。0.19.5 不嵌入该值。

## 3. 0.19.5 已吸收的必要改动

从 0.19.4 产品线保留：完整 native UI、独立 China Keychain、H5 滑块、Global 隔离、动态区域切换、C3/PCCS read path、远程命令 fail-closed、构建/安装/打包流程。

从加固版 0.19.0 选择性移植：

- 无认证 China C3 discovery，同时保留 Global 默认行为；
- Keychain → refresh → vehicle-list 的“验证会话”操作；
- refresh token / ID token 轮换兼容与本地 token 被服务端提前撤销后的单次重试；
- 多种车辆列表 JSON 形态、VIN 去重；
- 按 `car-information` 能力字段选择可选 PCCS reads；
- 删除退役 Bridge 工具。

根据 Android 4.25.0 证据纠正：

- SMS 为 `/user/api/v1/credcode/send`；
- Pscn 登录只有 `Content-Type`、`Version`、`Device`、`X-User-Agent` 公共头，不套用另一网络栈的签名器；
- 首次登录不依赖预置材料；OAuth client secret 只用于 `uc.../oauth/token` 本地刷新；
- 车辆列表使用无 bearer 的 discovery 返回的动态 `c3.url`，随后以 Global access token 请求 `/car-information/car`；
- car-information 请求不发送 Local `psoneid`。

## 4. 开始下一轮开发

```bash
git switch integrate/hisingen-china-0.19.5
python3 -m unittest discover -s Tests -p 'test_native_mac_contract_0195.py' -v
python3 -m py_compile Scripts/apply_hisingen_china_0195.py Tools/auth_probe/provision_cn_oauth_secret.py
HISINGEN_SRC=/path/to/Hisingen-v2.0.10 python3 Scripts/apply_hisingen_china_0195.py
```

修改顺序应为：先更新证据文档和契约测试，再改 `Patches/0.19.5`，最后同步 apply script、workflow、安装器和 release notes。不要继续改写历史 patch。

## 5. 发布前阻塞项

静态实现并不等于账号授权已通过。0.19.5 发布前仍需使用自有账号/车辆做脱敏动态验证：滑块回调、单次 SMS、Local + `GlobleToken` 返回、discovery 的真实 `c3.url`、车辆列表、至少一项 C3 telemetry、至少一项 PCCS read、Local/Global token 到期刷新和应用重启恢复。任何日志必须删除手机号、VIN、位置、Cookie、Authorization 与所有 token。
