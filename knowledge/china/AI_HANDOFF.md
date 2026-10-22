# AI handoff: what another agent needs to continue Polestar China integration

Read this document before editing the project. Updated 2026-10-22.

## Project, branches and sources

- User repository: `https://github.com/fifadxy/Polestar-CN-API`.
- Active preview branch: `chatgpt/hisingen-china-build-0.18.0`. Do not modify `main` unless the user explicitly requests it.
- Upstream native macOS project: `https://github.com/NicolasKheirallah/Hisingen`, pinned `v2.0.10`.
- Source patch: `Patches/0.18.0/ChinaPolestarIntegration.swift`.
- Deterministic patch application: `Scripts/apply_hisingen_china_018.py`.
- Build automation: `.github/workflows/build-hisingen-china-018.yml`, Xcode 27, ad-hoc signature, macOS 15 + 26 ARM64 runtime smoke tests.
- Android local read-only bridge: `Tools/china_bridge/polestar_china_bridge.py`; launcher `start_china_bridge.command`.
- Documentation: `knowledge/china/` plus historical `docs/HISINGEN_GLOBAL_CN_API_COMPARISON.md`.

## What is already known

1. Global and China identity systems are not token interchangeable. Global has a user/session grant, separate command-client audience, and independent EU Data Portal M2M.
2. China official website `https://www.polestar.cn/zh-cn/auth/login/` gives a website session only. A native China Mac vehicle API session has not been established.
3. Official China Android `4.25.0-262` uses C3 as primary observed battery/exterior path; VOCMO is secondary. CN C3/PCCS hosts are listed in [ENDPOINTS_RPC](ENDPOINTS_RPC.md).
4. Frida 17.24.3 on the user's rooted Android can inspect `CarBatteryInteractorImpl` model without exporting credentials. Stage3 confirmed `r9.clibyte` fields for SOC/range/consumption/timestamp. The model's reported timestamp **must** be used for stale/live display.
5. China direct vehicle discovery, authorized Mac C3/PCCS gRPC exchange, multi-vehicle account mapping, exterior/odometer full schema and remote commands remain MAPPING_REQUIRED.
6. The preview's optional Android read-only bridge provides a limited interim telemetry path. The runtime workflow has been compiled/mock-tested, but not verified end-to-end on the user's phone in GitHub CI.
7. `parkingClimatizationMaxRuntime=30`; `supportsParkingClimatizationDirectStartSettings=false`. Do not add custom duration 5–30 minute functionality.

## Reliable constraints

- The user's Global Hisingen provider and Volvo code must remain unchanged when Global is selected.
- Fresh 0.18 first run must show exactly `Select Polestar Region` with `China` and `Global`.
- Region selection must remain changeable from the native app menu AND Settings/Welcome.
- Changes must actively rebuild the provider; cannot merely save a preference.
- China region must not show Global cached vehicle snapshots or treat Global Keychain sessions as China sessions.
- No real account credentials, bearer grants, OTP, full VIN, car GPS or private keys in source, logs, documentation or test fixtures.
- Unmapped remote commands must be disabled at **both** UI capability and provider execution layers.

## Development sequence and acceptance evidence

### A. Account and native API research

Start with **non-invasive read-only proof**: inspect the official Android client's normal login and vehicle-read call-chain metadata, not token values. Identify legitimate client ID / issuer / scope / callback information from authoritative sources; do not guess an OAuth flow. Validate host, protocol, audiences and read permissions before changing native Mac authentication.

### B. Read-only provider migration

Only after authorization is confirmed:
- Implement ChinaVehicleDiscovery and ChinaC3Discovery.
- Add Battery, Exterior, Availability, Odometer and Health responses to `VehicleState`.
- Compare same-minute values shown in official China Android app vs Mac client, recording units and source timestamps, without storing vehicle identifiers.
- Add backoff, error semantics, stream reconnect and an explicit freshness policy.

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
| No Android bridge | Nonfatal disconnected state, no fake SOC or command ability |
| Android bridge mock | Valid data contract, no user identifiers, no real commands |
| Actual Android bridge | State matches official app and respects vehicle timestamp |
| Old Android sample | Display is Cached, not Live |
| Reconnect | Fresh data resumes without using old Global credentials |
| CN remote commands | All disabled until separate verification |
| macOS 15 and 26 | Actual ARM64 app starts, valid signature |

## How to build

```bash
git clone --depth 1 --branch v2.0.10 https://github.com/NicolasKheirallah/Hisingen.git
HISINGEN_SRC="$PWD/Hisingen" python3 Scripts/apply_hisingen_china_018.py
cd Hisingen
swift package resolve
NOTARIZE=never make app IDENTITY=-
```

The patch tool must be run from the user's `Polestar-CN-API` repository; `HISINGEN_SRC` points at the separately cloned upstream. In CI, the working source is in `$RUNNER_TEMP/Hisingen`.

## Git hygiene / docs update

Whenever code or findings change, update [FEATURE_MATRIX](FEATURE_MATRIX.md), [ENDPOINTS_RPC](ENDPOINTS_RPC.md), [STATE_AND_BRIDGE](STATE_AND_BRIDGE.md), `capabilities.json`, and [CHANGELOG](CHANGELOG.md) as applicable. Capture the observed app/version and testing method. Do not promote a method to VERIFIED solely because it compiles or a mock responds.

## Open blockers

- Native Mac CN Polestar ID/OAuth/session grant not validated.
- China vehicle list and linking semantics not mapped.
- China C3 gRPC token audience and dynamic host discovery not authenticated from Mac.
- Exterior/odometer normalized state not captured from a real app model.
- China PCCS method-level real authorized reads not verified.
- China write controls not validated; stay disabled.
- Apple Developer ID signing/notarization requires owner-provided legitimate signing authorization/credentials, not temporary CI certificates.

