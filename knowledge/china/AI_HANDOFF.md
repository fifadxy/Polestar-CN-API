# AI handoff: what another agent needs to continue Polestar China integration

Read this document before editing the project. Updated 2026-10-23.

## Project, branches and sources

- User repository: `https://github.com/fifadxy/Polestar-CN-API`.
- Active native Mac branch: `chatgpt/hisingen-china-native-0.19.0`. Android/APK work belongs on a different future branch. Do not modify `main` unless the user explicitly requests it.
- Upstream native macOS project: `https://github.com/NicolasKheirallah/Hisingen`, pinned `v2.0.10`.
- Native source patch: `Patches/0.19.0/{ChinaPolestarRuntime,ChinaPolestarSession,ChinaPolestarAPI,ChinaPolestarViews}.swift`.
- Deterministic patch application: `Scripts/apply_hisingen_china_019.py`.
- Build automation: `.github/workflows/build-hisingen-china-019.yml`, Xcode 27, ad-hoc signature, macOS 15 + 26 ARM64 runtime smoke tests.
- `Tools/china_bridge` is 0.18 historical material. The 0.19 workflow does not validate, copy, start or package it.
- Documentation: `knowledge/china/` plus historical `docs/HISINGEN_GLOBAL_CN_API_COMPARISON.md`.

## What is already known

1. Global and China identity systems are not token interchangeable. Global has a user/session grant, separate command-client audience, and independent EU Data Portal M2M.
2. Official China Android `4.25.0-262` source confirms native phone/SMS endpoints, local + nested `globleToken` families, both refresh routes and `GET car-information/car` vehicle discovery. See [NATIVE_MAC_SESSION](NATIVE_MAC_SESSION.md).
3. The native provider now uses a dedicated Keychain item and injects China C3/PCCS roots into Hisingen's Swift implementation. The app has no Android/localhost runtime dependency.
4. Source inspection and CI do not establish successful real-account authentication. Phone/SMS, refresh, linked vehicles and each telemetry family are IMPLEMENTED_UNVERIFIED pending a consenting real-account/vehicle run.
5. Frida observations remain useful comparison evidence for battery values/timestamps but are no longer the Mac transport architecture.
6. `parkingClimatizationMaxRuntime=30`; `supportsParkingClimatizationDirectStartSettings=false`. Do not add custom duration 5–30 minute functionality.

## Reliable constraints

- The user's Global Hisingen provider and Volvo code must remain unchanged when Global is selected.
- First real launch must show exactly `Select Polestar Region` with `China` and `Global`.
- Region selection must remain changeable from the native app menu AND Settings/Welcome.
- Changes must actively rebuild the provider; cannot merely save a preference.
- China region must not show Global cached vehicle snapshots or treat Global Keychain sessions as China sessions.
- No real account credentials, bearer grants, OTP, full VIN, car GPS or private keys in source, logs, documentation or test fixtures.
- Unmapped remote commands must be disabled at **both** UI capability and provider execution layers.

## Development sequence and acceptance evidence

### A. Account and native API validation

Use the implemented native flow. Capture only redacted outcomes: request status, token-family presence, expiry behavior and vehicle count. Never export token values, phone number or VIN. Confirm refresh after expiry and Keychain deletion at sign-out.

### B. Read-only provider validation

- Confirm linked-vehicle discovery and stable multi-vehicle selection.
- Compare same-minute Battery, Exterior, Availability, Odometer and Health values with the official app, recording units and timestamps without identifiers.
- Exercise expiry, unauthorized, absent service, stream disconnect/reconnect and stale-reading behavior.
- Keep optional service failures isolated from the required battery snapshot.

### C. PCCS

Validate China PCCS **read** methods for target SOC, amp limit, charging timers and parking climate; record actual transport response formats and service versions. Write methods remain disabled. Do not assume Globally named methods are the same in China.

### D. Controls (only if individually authorized)

For a single proposed control: confirm proper access permission, command method, protobuf request, safe parameter range, authorization, backend ACK, and subsequent vehicle-side state. Never treat method/class reflection or a capability flag as sufficient proof.

### E. Regression matrix

At a minimum exercise the following cases, distinguish `mock` and `real account`:

| Case | Required assertion |
| --- | --- |
| First launch | China / Global chooser visible, exactly once per chooser version |
| Change region | Native application menu and Settings in sync; active refresh coordinator rebuilt |
| Global | Polestar ID / Data Portal / Augmented UI remains original; commands preserved when legitimately authorized |
| China | Original credential form switches to CN account UI; no Global OIDC in China flow |
| Volvo | Original Volvo account surface unchanged |
| No Android device | China Mac login and provider have no Android/localhost dependency |
| Native sign-in | Dedicated Keychain item; no codes or tokens in logs |
| Vehicle discovery | Correct vehicle count and stable redacted selection |
| Reconnect | Fresh C3 data resumes without using old Global credentials |
| CN remote commands | All disabled until separate verification |
| macOS 15 and 26 | Actual ARM64 app starts, valid signature |

## How to build

```bash
git clone --depth 1 --branch v2.0.10 https://github.com/NicolasKheirallah/Hisingen.git
HISINGEN_SRC="$PWD/Hisingen" python3 Scripts/apply_hisingen_china_019.py
cd Hisingen
swift package resolve
NOTARIZE=never make app IDENTITY=-
```

The patch tool must be run from the user's `Polestar-CN-API` repository; `HISINGEN_SRC` points at the separately cloned upstream. In CI, the working source is in `$RUNNER_TEMP/Hisingen`.

## Git hygiene / docs update

Whenever code or findings change, update [FEATURE_MATRIX](FEATURE_MATRIX.md), [ENDPOINTS_RPC](ENDPOINTS_RPC.md), [STATE_AND_BRIDGE](STATE_AND_BRIDGE.md), `capabilities.json`, and [CHANGELOG](CHANGELOG.md) as applicable. Capture the observed app/version and testing method. Do not promote a method to VERIFIED solely because it compiles or a mock responds.

## Open blockers

- Native Mac phone/SMS and refresh flows are not yet validated with a real account.
- China vehicle list decoder and multi-vehicle selection are not yet validated with a real account.
- China C3 gRPC token audience and dynamic host discovery are implemented but not authenticated end to end from Mac.
- Exterior/odometer normalized state has not been compared on a real vehicle.
- China PCCS method-level real authorized reads not verified.
- China write controls not validated; stay disabled.
- Apple Developer ID signing/notarization requires owner-provided legitimate signing authorization/credentials, not temporary CI certificates.

