# Hisingen Global → China feature mapping and acceptance tests

**Baseline:** Hisingen upstream `v2.0.10`, China Android `4.25.0-262`, native Mac Preview `0.19.0`. Updated 2026-10-23.

Legend: **GLOBAL-IMPLEMENTED** = upstream source supports it; **SOURCE-VERIFIED** = exact official-client contract observed; **IMPLEMENTED-UNVERIFIED** = native Mac code exists but lacks a real-account/vehicle proof; **BRIDGE-ONLY** = historical 0.18 Android-process path; **MAPPING_REQUIRED** = direct method remains unmapped; **DISABLED** = intentionally not executable; **NOT_APPLICABLE** = an EU-only interface cannot be assumed in China.

| Feature / original Hisingen path | Global | China verified evidence | China implementation / status | Required next validation |
| --- | --- | --- | --- | --- |
| Polestar account login | GLOBAL-IMPLEMENTED | Phone/SMS request, login and refresh contracts SOURCE-VERIFIED from official app 4.25.0 | Native Mac phone/SMS + dedicated Keychain; IMPLEMENTED-UNVERIFIED | Real SMS login, expiry and refresh |
| Polestar command-client grant | GLOBAL-IMPLEMENTED | CN `key_*` local names observed; audience unknown | MAPPING_REQUIRED | Authorized issuer/client/scope/refresh verification |
| EU Data Portal (Data Act) | GLOBAL-IMPLEMENTED | No CN EU M2M equivalence established | NOT_APPLICABLE as CN substitute | Identify separate supported CN data API |
| Augmented (consumer + M2M) | GLOBAL-IMPLEMENTED | No compatible CN M2M path | MAPPING_REQUIRED | Define valid CN merge after state sources verified |
| Vehicle discovery | GLOBAL-IMPLEMENTED | `GET car-information/car`, metadata and capability fields SOURCE-VERIFIED | Native multi-vehicle decoder accepts empty/wrapped lists, VIN aliases and duplicate rows; IMPLEMENTED-UNVERIFIED | Authorized real-account list and selection |
| C3 discovery | GLOBAL-IMPLEMENTED | Public China v1/v2 discovery document observed | China root injected; discovery deliberately omits Authorization while Global retains it; IMPLEMENTED-UNVERIFIED | Real telemetry authorization after host resolution |
| PCCS/Chronos | GLOBAL-IMPLEMENTED | China root, Proto families and official car capability fields found | China root injected; optional reads gated by per-vehicle capabilities; IMPLEMENTED-UNVERIFIED | Authorized method-by-method reads by vehicle |
| Battery SOC | GLOBAL-IMPLEMENTED | Android C3 model and 2021 P2 wire mapping observed | Native latest-battery parser; IMPLEMENTED-UNVERIFIED | Same-minute official-app comparison |
| Estimated range (km/mi) | GLOBAL-IMPLEMENTED | `clicatch` / `cliclass` and C3 mapping observed | Native C3 projection; IMPLEMENTED-UNVERIFIED | Units and model-year coverage |
| Charging state/time/consumption | GLOBAL-IMPLEMENTED | China battery model fields observed | Native C3 projection; IMPLEMENTED-UNVERIFIED | Charging/unplugged/error fixtures |
| Exterior doors / windows / lock state | GLOBAL-IMPLEMENTED | C3 exterior controller and live path observed | Native latest + stream parsers; IMPLEMENTED-UNVERIFIED | Real response semantic comparison |
| Availability / online | GLOBAL-IMPLEMENTED | China C3 family observed | Native optional read; IMPLEMENTED-UNVERIFIED | Real response and unknown-state behavior |
| Odometer | GLOBAL-IMPLEMENTED | C3 vehicle-state family in official client | Native optional read; IMPLEMENTED-UNVERIFIED | Timestamp and unit comparison |
| Health & maintenance | GLOBAL-IMPLEMENTED | C3 vehicle-state family in official client | Native optional read; IMPLEMENTED-UNVERIFIED | Real response semantics |
| Climate state and timers | GLOBAL-IMPLEMENTED | Max runtime 30 and `supportsParkingClimatizationTimerSettings` observed | Native C3 state plus capability-gated PCCS timer read; IMPLEMENTED-UNVERIFIED | Real state and timer reads |
| Live C3 gRPC stream | GLOBAL-IMPLEMENTED | Android app C3 live observed | Native Hisingen C3 battery/exterior streams; IMPLEMENTED-UNVERIFIED | Tokenized stream, reconnect, timestamps |
| Target charge SOC | GLOBAL-IMPLEMENTED | CN Chronos proto and `supportsTargetChargeLevelSetting` observed | Capability-gated native read only; IMPLEMENTED-UNVERIFIED | Authorized response; write disabled |
| Charging amp limit | GLOBAL-IMPLEMENTED | CN PCCS service and `supportsGlobalChargeAmperageLimitSetting` observed | Capability-gated native read only; IMPLEMENTED-UNVERIFIED | Authorized response and vehicle limits |
| Global charging timer | GLOBAL-IMPLEMENTED | Chronos service and `supportsGlobalChargeTimer` observed | Capability-gated native read only; IMPLEMENTED-UNVERIFIED | Authorized response semantics |
| Climate start/stop | GLOBAL-IMPLEMENTED | CN request class observed | DISABLED | Explicit end-to-end permission and vehicle confirmation |
| Remote lock/unlock | GLOBAL-IMPLEMENTED | CN exterior controller has method identifiers | DISABLED | Explicit end-to-end permission and vehicle confirmation |
| Windows/tailgate/honk/flash | GLOBAL-IMPLEMENTED | Methods/capability indicators observed | DISABLED | Command-specific mapped request and test |
| Charging override / write settings | GLOBAL-IMPLEMENTED | Protobuf write request structures observed | DISABLED | Verified permission, model limits and outcome |
| OTA schedule/install/cancel | GLOBAL-IMPLEMENTED | Global OTA service known; CN unverified | DISABLED | Official compatibility and full consent workflow |
| Vehicle location / weather / air quality | GLOBAL-IMPLEMENTED | CN LBS/C3 families observed | Native optional reads; IMPLEMENTED-UNVERIFIED | Authorized responses; redact GPS |
| Volvo functionality | GLOBAL-IMPLEMENTED | No China modifications to Volvo provider | Original upstream maintained | Regression with own Volvo account if available |

## Test levels

| Test | CI automated | Needs user/device |
| --- | --- | --- |
| Upstream v2.0.10 still compiles with China patch | YES | No |
| Global provider still compiled and selected for Region Global | Static source routing + startup | Actual Global account authentication/vehicle refresh |
| China selected on first real launch | Static UI wiring; interactive dialog requires GUI test | Visual first-start verification |
| China/Global menu + Settings switching | Static selection + rebuild logic checks | Manual GUI switch with provider state isolation |
| Native phone/SMS, refresh and vehicle-list contract | YES, source/static | Real consenting account |
| Separate China Keychain and no Bridge source/runtime | YES | Sign-in/sign-out persistence check |
| China direct C3/PCCS token exchanges | Compiles and source-checked | Approved real authentication integration |
| Native C3 live stream | Compiles and launch-tested | Real vehicle values, timestamps and reconnect |
| China remote commands | DELIBERATELY DISABLED | Later separate permissioned vehicle test |

## Minimum acceptance criteria

**P0 (UI/account):** First real launch dialog says `Select Polestar Region` with exactly China/Global; choosing persists; Hisingen app menu and Settings permit switches; Global and China login forms do not mix; Region switch rebuilds active provider; China does not inherit Global cached SOC/session.

**P1 (CN native source):** The Mac app owns the China session, vehicle discovery and read-only C3/PCCS client; no Android/localhost dependency is packaged; unsupported optional reads remain unavailable; remote writes are hidden and rejected.

**P2 (CN native verified):** A real Mac user completes legitimate CN login; vehicle discovery, token refresh and authorized C3 reads work without Android; values and timestamps match the official app. Do not claim P2 until redacted end-to-end evidence exists.

**P3 (CN commands):** Individual control buttons become available only after verified CN method, scope, parameter validation, explicit user confirmation and vehicle-side outcome. No batch enablement by copying Global commands.

## Known current limitations

0.19.0 tests prove source contracts, compilation, App Bundle integrity, process launch, Global source isolation and absence of Bridge source or runtime in the branch and artifact. They do **not** establish that SMS login, token refresh or China C3/PCCS succeeds with a real account/vehicle. China remote commands remain disabled.

