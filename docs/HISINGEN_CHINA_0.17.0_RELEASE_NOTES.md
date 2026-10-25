# Hisingen China Preview 0.17.0

基线：Hisingen v2.0.10  
目标：在不破坏 Global Hisingen 登录/车辆逻辑的前提下，为中国区建立清晰、可验证、可持续扩展的独立架构。

## 本版落实的 9 项优化

1. **Region 正式进入 Settings**
   - Global / Mainland China 分段选择。
   - 默认 Global，不主动打断已有 Global 用户。
   - Global 原 Polestar ID / Data Portal 路径不改。

2. **防止 Sparkle 覆盖 China Patch**
   - Preview 强制关闭自动检查、自动下载。
   - Settings 更新卡替换为 China Preview 更新策略说明。
   - 菜单和设置里的手动 Sparkle Check 也被拦截，避免更新到上游原版。

3. **Website Sign-In 与 Vehicle API 状态分离**
   - Official Web Sign-In 只代表官网网页登录入口可用。
   - Vehicle API 未取得合法、验证过的中国区 session 前明确显示 Not connected。
   - 不把网页登录误判为 C3/PCCS 已连接。

4. **独立 China Provider**
   - 新增 ChinaPolestarAPI。
   - ProviderRegistry 仅在用户明确选择 Mainland China 时路由到 China Provider。
   - 不复用 Global access/refresh/command token 假设。

5. **China 第一阶段只读**
   - China Provider 当前 fail-closed。
   - RemoteCommand 一律拒绝。
   - 未验证的 China command mapping 继续保持 MAPPING_REQUIRED。

6. **Freshness / Source 边界**
   - 预留 Live C3 / C3 Latest / Cached 三种数据来源。
   - 未连接车辆 API 时不显示虚假的“实时”数据或更新时间。

7. **China Backend Diagnostics**
   手动检测以下网络入口，不发送真实 Token：
   - polestar.cn Official Web
   - cnepmob.volvocars.com.cn
   - cepmobtoken.prod.c3.volvocars.com.cn
   - cepmob.lbs.prod.c3.volvocars.com.cn
   - cepmobsig.prod.c3.volvocars.com.cn
   - api.pccs-prod.origin.polestar.cn

8. **签名策略**
   - Preview CI 继续使用 ad-hoc signing，适合自用测试。
   - 不再创建临时 Hisingen Development 证书，避免 hosted macOS runner codesign 卡死。
   - Developer ID + notarization 留作正式发布阶段，当前没有 Apple signing secrets 时不伪造“正式签名”。

9. **多 macOS 版本 CI**
   - Xcode 27：Build + bundle/signature/framework/privacy/launch smoke。
   - macOS 15：下载同一 Artifact 后做兼容性验证与可执行时运行测试（ARM64 runner 时）。
   - macOS 26：同上。

## 安全边界

本版不会读取或提交：

- China access / refresh / ID token；
- SMS / 2FA code；
- Cookie；
- 手机号；
- 完整 VIN；
- Digital Key secret/private key；
- 精确车辆位置。

## 当前未完成

以下项目仍需要真实中国区认证链路继续验证：

- China vehicle API session；
- 车辆发现；
- C3 authenticated discovery；
- SOC / range / charging state 映射到 VehicleState；
- exterior / availability / odometer / health；
- C3 live stream；
- PCCS method-level mapping；
- Token refresh 生命周期；
- macOS Keychain 正式 China session model；
- Remote controls。

这些项目不会因为 Global 接口“看起来类似”而直接标记可用。
