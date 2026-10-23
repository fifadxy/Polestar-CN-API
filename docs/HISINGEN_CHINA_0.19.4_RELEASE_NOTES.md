# Hisingen China 0.19.4 — Native China account authentication candidate

Static-analysis basis: the user-supplied DEX dump of the official Polestar China Android 4.25.0 app. This build does not use the Android app at runtime.

## What the Android DEX proved

The China account flow is not the same as Global Hisingen OAuth and is not a public-web-cookie login.

Observed account chain:

1. Mainland China phone number.
2. Official H5 verification slider.
3. The slider returns sessionId / sig / token through a JavaScript bridge named chromium.
4. SMS request: POST /user/api/v1/credcode/send.
5. Phone-code login: POST /user/api/v2/oauth/login/phone with autocrt=true.
6. The response contains the local China token family plus the APK-named GlobleToken family.
7. Global China access token is then used for vehicle discovery and China C3/PCCS read-only telemetry.

The earlier "缺少公共参数" failure was also traced to the Android common network interceptor: GW/GWO requests receive a signed common query envelope before transmission.

Observed common query names: appId, timestamp, nonce, deviceId, deviceModel, brand, clientType, systemVer, appVer and sign.

## 0.19.4 changes

- Restores phone + SMS login using the observed request bodies.
- Uses the verified official slider URL:
  https://h5.app.polestar-auto.com/account-components/auth-slider/index.html
- Implements a WKWebView chromium.postMessage compatibility bridge. sessionId, sig and token are held only in memory and discarded after the current SMS request.
- Sends the slider challenge before requesting an SMS code, matching the Android flow instead of first probing the gateway with null Ali challenge fields.
- Implements the observed GW/GWO common-query canonicalization and MD5 signing structure.
- Corrects account gateway vs non-gateway client-header behavior.
- Preserves separate China Keychain token storage, China vehicle discovery, C3/PCCS read-only state, live-stream token refresh, and Global isolation.
- China Remote Commands remain disabled.

## Protocol material is not embedded in the app

The official Android client contains protocol-level material used by both the gateway signature and local refresh grant. 0.19.4 deliberately does not place that material in Swift source or the packaged app.

The artifact includes:

  provision_cn_protocol_material.py

Run it locally on macOS against your own official Android DEX dump before first China login:

  python3 provision_cn_protocol_material.py /path/to/Polestar-China-Dex.zip

The helper validates the exact Android gateway signing method, extracts the required protocol material without printing it, and writes it into a dedicated macOS Keychain item:

  io.kheirallah.hisingen.polestar-cn.protocol / gateway-client-material-v1

It does not import Android account tokens, passwords, OTPs, cookies, VINs, Digital Key material, or device identifiers.

## Verification boundary

CI validates the static auth contract, source does not contain a 32-character embedded protocol material value, the native Swift build, code signature, packaged application launch, and macOS 15/26 compatibility.

Real account validation is still required for:
- official slider completion on WKWebView,
- SMS delivery,
- local + GlobleToken issuance,
- China vehicle discovery,
- C3 SOC/range/odometer/exterior,
- token refresh after expiry and application restart.

Until those real-account checks pass, 0.19.4 is an authentication candidate rather than a fully validated production release.
