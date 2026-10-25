# Hisingen China 0.19.2 — Official China Web Sign-In

Native macOS fork of upstream Hisingen 2.0.10, build 28. Mac and Android remain separate.

## Login correction
- 0.19.1's direct phone/SMS UI failed with **缺少公共参数**; its unverified handcrafted request is no longer used.
- A dedicated WKWebView login window now opens the official China URL: https://www.polestar.cn/zh-cn/auth/login/ . Browser fallback is also available.
- The official site handles sign-in challenges. The app does not inject scripts, extract cookies, intercept credentials, or bypass verification.
- Website login is **not** vehicle API authorization. A legitimate independently verified China C3/PCCS grant and callback are still required before vehicle telemetry can be called. UI states make this distinction explicit.
- Previously stored China API sessions remain in the separate Keychain record, untouched; Global sources remain isolated. China remote commands stay disabled.

## Verification
Automated CI checks isolated WebKit creation, official URL, read-only boundary, Swift build, code signature, packaged application launch on macOS 15/26. Actual China website login and vehicle API access require consenting-user manual testing.

## Installation
From GitHub Actions download Hisingen-China-0.19.2-Native-Mac. Verify SHA256SUMS.txt, then run install_hisingen_china_0192.command. The previous China app is backed up. This preview is ad-hoc signed and not notarized.
