# China API knowledge-base changelog

This is a living log. Entries describe **what was learned or changed**, the source/verification level, and remaining evidence gaps. Never paste raw auth records or private vehicle identifiers.

## 2026-10-26 — KB version 1.3 / Android Native Bridge emulator

### Verified environment

- Built an Android 11 / API 30 Google APIs x86_64 `userdebug` AVD for a no-KVM x86_64 host.
- Verified root ADB, an ABI list containing `arm64-v8a`, enabled `libndk_translation.so` and the 64-bit translator file.
- Verified that PackageManager staged the unchanged official 4.25.0-262 APK with a matching SHA-256 and selected `primaryCpuAbi=arm64-v8a`.

### Measured limits

- The default `speed-profile` install was blocked in `installd.dexopt`; ART reported that `dex2oat` had not finished after 570 seconds, and Watchdog restarted `system_server`.
- `verify` remained too expensive under TCG. Interrupted transactions produced incomplete component registration and are not counted as successful installation or runtime proof.
- Confirmed from Android 11 ART source that `extract` is a valid no-AOT/no-verification install filter. A clean AVD was prepared with it, but final installation was paused for user confirmation.
- Polestar process entry, ARM64 JNI loading, Reqable, Frida, PCAPdroid root mode and all account/vehicle behavior remain NOT YET VERIFIED in the emulator.
- Recorded the distinction between ADB-root userdebug and Magisk app-facing `su -c`; no real account or private data was used.

## 2026-10-25 — KB version 1.2 / Hisingen China Native Mac 0.19.5

### Full Android evidence and branch consolidation

- Analyzed all 16 DEX files from official China Android 4.25.0-262 and recorded the end-to-end static evidence chain.
- Merged the complete 0.19.4 product history into `integrate/hisingen-china-0.19.5`; selectively ported validated hardening from the divergent 0.19.0 line instead of overwriting newer files.
- Corrected SMS to `/user/api/v1/credcode/send` and removed the unsupported signed-query/MD5/common-parameter implementation. The Pscn account client uses the verified `Content-Type`, `Version`, `Device` and `X-User-Agent` headers.
- Limited locally provisioned OAuth material to `uc.../oauth/token` refresh. Initial SMS login no longer depends on it.
- Implemented public China C3 discovery followed by dynamic `<c3.url>/car-information/car` with Global access; removed the fixed REST base and Local `psoneid` header.
- Added rotating-token fallbacks, stored-session verification, multi-shape vehicle decoding, VIN de-duplication and capability-gated PCCS reads.
- Removed the retired `Tools/china_bridge` runtime tree and added a single 0.19.5 development/build/test entry point.
- Redacted the legacy embedded OAuth constant from the current 0.19.0–0.19.3 archive files. Existing remote Git history was not rewritten; the old value must be treated as public and rotated server-side.

### Verification boundary

- Offline tests and DEX extraction validate implementation contracts without publishing the OAuth client secret.
- Real slider, SMS, token, discovery, vehicle and telemetry behavior remains IMPLEMENTED_UNVERIFIED until a redacted consenting-account run succeeds.
- China remote commands remain disabled.

## 2026-10-23 — KB version 1.1 / Hisingen China Native Mac 0.19.0

### Source-verified protocol

- Recorded the official China Android 4.25.0 phone-code and phone-login request contracts.
- Recorded separate local and nested `globleToken` families, local/global refresh routes, client headers and Alibaba challenge code `2002016`.
- Recorded `GET /car-information/car` and its linked-vehicle metadata fields.
- Confirmed the China C3 discovery and PCCS roots used by the official client.

### Native Mac implementation

- Replaced the current Mac runtime's 0.18 Bridge path with native `ChinaPolestarSession` and `ChinaPolestarAPI` actors.
- Added a dedicated data-protection Keychain item, expiry-aware single-flight refresh, native vehicle discovery, read-only C3/PCCS telemetry projection and C3 live streams.
- Kept Global Hisingen endpoint defaults and account implementation intact; China chooses separate injected roots and a separate VIN preference.
- Rebuilt the account and region controls from original Hisingen cards, theme tokens and typography.
- Added source contract tests, no-Bridge artifact gates, Xcode build/launch smoke tests and macOS 15/26 packaged-app checks.
- Kept all China remote vehicle mutations disabled in both UI capability discovery and provider execution.

### Verification boundary

- Authentication routes and payloads are SOURCE_VERIFIED from the official app and IMPLEMENTED_UNVERIFIED on Mac.
- CI can prove compilation, launch, isolation and packaging. It cannot prove a real SMS login, linked-vehicle authorization or correct live vehicle values.
- No real credentials, tokens, phone number, VIN or location were used or stored in this change.

## 2026-10-22 — KB version 1.0 / Hisingen China Preview 0.18.0

### Documented

- Separate Global Polestar OIDC user, command-client and EU Data Portal M2M authentication families.
- Recorded observed China App token preference **key names only**, with unverified audiences clearly marked.
- Recorded China official login URL, C3 static/token/LBS/signature hosts and PCCS host.
- Inventoried upstream Global Mystar GraphQL discovery, Data Portal REST, C3 and PCCS read RPCs, remote Invocation commands, charging writes and OTA.
- Assigned every Global feature a China status: BRIDGE_ONLY, MAPPING_REQUIRED, DISABLED or NOT_APPLICABLE.
- Documented the verified China Android C3 battery call chain, mixed-mode C3/VOCMO observations, obfuscated model field mapping, climate limitations and stale-cache caveats.
- Documented China local read-only Android bridge and its limits: no direct Mac China OAuth/C3 session, one opaque vehicle, no validated exterior/odometer model, no vehicle controls.
- Created an AI handoff guide, reproducible validation matrix and `capabilities.json` for automated consistency checks.

### Technical corrections in 0.18.0 follow-up

- Read-only bridge now classifies app-memory snapshots by vehicle `reportedAtMs` (fresh <= 10 minutes; missing/older/implausibly future is Cached).
- Bridge state projection applies a battery-field allowlist to avoid accidentally exporting unexpected fields.
- China UI now requires a genuinely ready bridge/status response rather than declaring connection solely from HTTP reachability.
- Hisingen marks unknown China availability as Unknown; Android bridge data must not falsely imply vehicle online status.
- Periodic Android object observation displays `App Poll`, not a direct `Live C3` server stream.
- Region selection is accessible from the menu-bar popover footer as well as the app menu and Settings.
- Global Polestar vehicle preference is locally preserved across China selection; Global provider/token logic remains separate.
- China account controls appear in the original account form instead of being duplicated in Settings.
- CI adds offline freshness, privacy and capability tests, plus macOS smoke checks.

### Verification boundaries

- GitHub Actions build and macOS 15/26 startup checks verify that compiled code launches, not that a real account can authenticate.
- Python bridge fixture tests verify only the protocol and policy; real Android device/Frida read must be verified separately.
- The currently observed China Android battery model is a real app observation; native Mac vehicle API auth remains MAPPING_REQUIRED.
- China remote controls remain DISABLED. Do not promote to VERIFIED by interpreting class names/capability flags.

## Procedure for future AI/developers

For each change, append:

```text
Date:
Project/Android app version:
Function / host / class / RPC:
Prior status:
New status:
Evidence source and reproduction method (redacted):
Code/docs changed:
CI test outcome:
Remaining blocker:
```

Update [FEATURE_MATRIX](FEATURE_MATRIX.md), [ENDPOINTS_RPC](ENDPOINTS_RPC.md), [STATE_AND_BRIDGE](STATE_AND_BRIDGE.md), [AI_HANDOFF](AI_HANDOFF.md), and `capabilities.json` as applicable. No claim of real CN login or remote action is allowed without independently reproducible, authorized evidence.

