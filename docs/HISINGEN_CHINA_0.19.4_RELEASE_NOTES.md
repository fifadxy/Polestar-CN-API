# Hisingen China 0.19.4 — Native China account authentication candidate

Static analysis source: user-supplied dump of the official Polestar China Android 4.25.0 application.

## Authentication correction
0.19.4 restores the official China phone + SMS account flow. The previous "missing common parameters" error was traced to the Android network interceptor: GW/GWO requests receive a signed common query envelope before being sent. The Mac implementation mirrors the observed field structure and canonical MD5 signing while generating its own pseudonymous per-install device ID instead of copying an Android identifier.

Observed common query fields: appId, timestamp, nonce, deviceId, deviceModel, brand, clientType, systemVer, appVer and sign. Headers include both version/Version, Device and X-User-Agent. User tokens are never logged.

## Interactive verification
When the gateway returns challenge code 2002016, Hisingen opens the same official account slider H5 observed in the Android app:
https://account.app.polestar-auto.com/account-components/auth-slider/index.html

The window implements the observed chromium.postMessage compatibility bridge. It accepts sessionId, sig and token only in memory, retries the SMS request, then discards the challenge result. It does not bypass verification and does not read cookies.

## Token and vehicle chain
Successful phone verification returns the local China account token family plus the APK-named GlobleToken family. Hisingen stores the session in its separate China Keychain item, uses the global access token for China vehicle discovery, preserves local/global refresh, then proceeds to China C3/PCCS read-only state.

The Android dump also contains a separate Polestar ID/AppAuth flow at https://polestarid.polestar.cn with custom callback polestar-china://china.polestar.com. The generic public website is no longer treated as vehicle authorization; the Polestar ID presenter remains diagnostic-only until that client registration/callback contract is independently validated.

## Isolation and verification
Global Hisingen files remain unchanged. China Remote Commands remain disabled. Android Bridge is not used at runtime. SMS codes and slider results are not persisted.

CI validates source contracts, Swift build, packaging and macOS 15/26 launch. Real China account validation is still required for SMS delivery, challenge completion, token issuance, vehicle discovery, C3 telemetry, refresh and restart.
