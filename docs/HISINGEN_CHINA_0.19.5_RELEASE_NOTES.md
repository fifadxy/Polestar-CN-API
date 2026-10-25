# Hisingen China 0.19.5 — verified-contract integration candidate

Evidence basis: all 16 DEX files from the official Polestar China Android 4.25.0 (build 262) application and the consolidated 0.19.x source branches. The Android app is not required at runtime.

## Corrected account contract

The verified sign-in chain is:

1. Open the official H5 slider at `https://h5.app.polestar-auto.com/account-components/auth-slider/index.html`.
2. Receive `sessionId`, `sig` and `token` through the `chromium` JavaScript bridge.
3. `POST /user/api/v1/credcode/send` with the slider fields and `SIGNIN_BY_PHONE`.
4. `POST /user/api/v2/oauth/login/phone` with the phone, SMS credential and `autocrt=true`.
5. Store the separate Local and APK-named `GlobleToken` families in the China-only Keychain item.

The Pscn account Retrofit client adds `Content-Type`, `Version`, `Device` and `X-User-Agent`. It does not use the separate baselinelibrary `SignInterceptor`. 0.19.5 therefore removes 0.19.4's invented signed-query envelope, nonce/device identifier and MD5 dependency.

## Vehicle and session corrections

- China C3 discovery is an unauthenticated `GET` of `https://cnepmob.volvocars.com.cn/`.
- Linked vehicles are fetched from dynamic `<c3.url>/car-information/car` with the China Global access token; the old fixed REST base and `psoneid` header were removed.
- China gRPC discovery also omits authorization, while Global Hisingen retains its original authenticated default.
- Vehicle-list decoding accepts the observed nested/alternate response shapes and uses advertised vehicle capabilities to decide which optional PCCS reads to call.
- Rotating refresh responses may omit an unchanged refresh or ID token; 0.19.5 preserves the previous value in that case.
- A stored-session verification action now tests Keychain restore, required refresh and authenticated vehicle discovery.
- Remote commands remain fail-closed.

## OAuth refresh material

Initial SMS sign-in needs no pre-provisioned secret. Only the local-token refresh request requires the official Android OAuth client secret. The value is not stored in source, the app bundle, release notes or logs.

The artifact includes `provision_cn_oauth_secret.py`. On macOS, a user may run it against their own official DEX dump:

```bash
python3 provision_cn_oauth_secret.py /path/to/Polestar-China-Dex.zip
```

The helper validates the exact Android local-refresh method, does not print the extracted value, and writes it to:

```text
io.kheirallah.hisingen.polestar-cn.oauth / local-refresh-client-secret-v1
```

It does not import account tokens, passwords, SMS codes, cookies, VINs, Digital Key material or device identifiers.

## Verification boundary

Local tests validate the static contract, DEX extractor, patch application, absence of embedded 32-character material, and source isolation. The macOS workflow additionally builds/signs the app and launches the packaged artifact on the configured runners.

Real-account validation is still required for slider completion, SMS delivery, Local + `GlobleToken` issuance, the exact dynamic discovery response, linked vehicles, C3/PCCS telemetry, expiry refresh and restart recovery. Until those checks pass, 0.19.5 remains an integration candidate.
