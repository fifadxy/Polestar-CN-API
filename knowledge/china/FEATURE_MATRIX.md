# Hisingen Global → China feature mapping and acceptance tests

**Baseline:** Hisingen upstream `v2.0.10`, China Android `4.25.0-262`, native Mac Preview `0.19.5`. Updated 2026-10-26.

Legend: **GLOBAL-IMPLEMENTED** = upstream source supports it; **SOURCE-VERIFIED** = exact official-client contract observed; **IMPLEMENTED-UNVERIFIED** = native Mac code exists but lacks a real-account/vehicle proof; **BRIDGE-ONLY** = historical 0.18 Android-process path; **MAPPING_REQUIRED** = direct method remains unmapped; **DISABLED** = intentionally not executable; **NOT_APPLICABLE** = an EU-only interface cannot be assumed in China.

| Feature / original Hisingen path | Global | China verified evidence | China implementation / status | Required next validation |
| --- | --- | --- | --- | --- |
| Polestar account login | GLOBAL-IMPLEMENTED | Phone/SMS request, login and refresh contracts SOURCE-VERIFIED from official app 4.25.0 | Native Mac phone/SMS + dedicated Keychain; IMPLEMENTED-UNVERIFIED | Real SMS login, expiry and refresh |
| Polestar command-client grant | GLOBAL-IMPLEMENTED | CN `key_*` local names observed; audience unknown | MAPPING_REQUIRED | Authorized issuer/client/scope/refresh verification |
| EU Data Portal (Data Act) | GLOBAL-IMPLEMENTED | No CN EU M2M equivalence established | NOT_APPLICABLE as CN substitute | Identify separate supported CN data API |
| Augmented (consumer + M2M) | GLOBAL-IMPLEMENTED | No compatible CN M2M path | MAPPING_REQUIRED | Define valid CN merge after state sources verified |
| Vehicle discovery | GLOBAL-IMPLEMENTED | Public discovery → dynamic `<c3.url>/car-information/car` and response fields SOURCE-VERIFIED | Native multi-shape/vehicle decoder; IMPLEMENTED-UNVERIFIED | Authorized real-account list and selection |
| C3 discovery | GLOBAL-IMPLEMENTED | China root and unauthenticated request SOURCE-VERIFIED | China root injected; bearer omitted only for China; IMPLEMENTED-UNVERIFIED | Production response and dynamic hosts |
| PCCS/Chronos | GLOBAL-IMPLEMENTED | China PCCS root and Proto found | China root injected; read methods IMPLEMENTED-UNVERIFIED | Authorized method-by-method reads |
| Battery SOC | GLOBAL-IMPLEMENTED | Android C3 model and 2021 P2 wire mapping observed | Native latest-battery parser; IMPLEMENTED-UNVERIFIED | Same-minute official-app comparison |
| Estimated range (km/mi) | GLOBAL-IMPLEMENTED | `clicatch` / `cliclass` and C3 mapping observed | Native C3 projection; IMPLEMENTED-UNVERIFIED | Units and model-year coverage |
| Charging state/time/consumption | GLOBAL-IMPLEMENTED | China battery model fields observed | Native C3 projection; IMPLEMENTED-UNVERIFIED | Charging/unplugged/error fixtures |
| Exterior doors / windows / lock state | GLOBAL-IMPLEMENTED | C3 exterior controller and live path observed | Native latest + stream parsers; IMPLEMENTED-UNVERIFIED | Real response semantic comparison |
| Availability / online | GLOBAL-IMPLEMENTED | China C3 family observed | Native optional read; IMPLEMENTED-UNVERIFIED | Real response and unknown-state behavior |
| Odometer | GLOBAL-IMPLEMENTED | C3 vehicle-state family in official client | Native optional read; IMPLEMENTED-UNVERIFIED | Timestamp and unit comparison |
| Health & maintenance | GLOBAL-IMPLEMENTED | C3 vehicle-state family in official client | Native optional read; IMPLEMENTED-UNVERIFIED | Real response semantics |
| Climate state and timers | GLOBAL-IMPLEMENTED | Max runtime 30 observed; direct-start settings false | Native optional C3/PCCS reads; IMPLEMENTED-UNVERIFIED | Real state and timer reads |
| Live C3 gRPC stream | GLOBAL-IMPLEMENTED | Android app C3 live observed | Native Hisingen C3 battery/exterior streams; IMPLEMENTED-UNVERIFIED | Tokenized stream, reconnect, timestamps |
| Target charge SOC | GLOBAL-IMPLEMENTED | CN Chronos target SOC proto exists | Native read only; IMPLEMENTED-UNVERIFIED | Authorized response; write disabled |
| Charging amp limit | GLOBAL-IMPLEMENTED | CN PCCS host and service family observed | Native read only; IMPLEMENTED-UNVERIFIED | Authorized response and model limits |
| Global charging timer | GLOBAL-IMPLEMENTED | Chronos service family observed | Native read only; IMPLEMENTED-UNVERIFIED | Authorized response semantics |
| Climate start/stop | GLOBAL-IMPLEMENTED | CN request class observed | DISABLED | Explicit end-to-end permission and vehicle confirmation |
| Remote lock/unlock | GLOBAL-IMPLEMENTED | CN exterior controller has method identifiers | DISABLED | Explicit end-to-end permission and vehicle confirmation |
| Windows/tailgate/honk/flash | GLOBAL-IMPLEMENTED | C3 `windowControl`, `tailgateControl` and `honkFlash` request models SOURCE-VERIFIED | DISABLED | Command-specific authorization, ACK and vehicle-state confirmation |
| Charging-port flap | Model-dependent | Generic Android UI type `2` exists, but Polestar 2 capability excludes it and C3/PCCS/VOCMO controller paths are unsupported | DISABLED for Polestar 2 | Do not expose for Polestar 2; re-evaluate only with a separately verified model |
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
| Separate China Keychain and no Bridge runtime | YES | Sign-in/sign-out persistence check |
| China direct C3/PCCS token exchanges | Compiles and source-checked | Approved real authentication integration |
| Native C3 live stream | Compiles and launch-tested | Real vehicle values, timestamps and reconnect |
| China remote commands | DELIBERATELY DISABLED | Later separate permissioned vehicle test |

## Minimum acceptance criteria

**P0 (UI/account):** First real launch dialog says `Select Polestar Region` with exactly China/Global; choosing persists; Hisingen app menu and Settings permit switches; Global and China login forms do not mix; Region switch rebuilds active provider; China does not inherit Global cached SOC/session.

**P1 (CN native source):** The Mac app owns the China session, vehicle discovery and read-only C3/PCCS client; no Android/localhost dependency is packaged; unsupported optional reads remain unavailable; remote writes are hidden and rejected.

**P2 (CN native verified):** A real Mac user completes legitimate CN login; vehicle discovery, token refresh and authorized C3 reads work without Android; values and timestamps match the official app. Do not claim P2 until redacted end-to-end evidence exists.

**P3 (CN commands):** Individual control buttons become available only after verified CN method, scope, parameter validation, explicit user confirmation and vehicle-side outcome. No batch enablement by copying Global commands.

## Known current limitations

0.19.5 tests prove source contracts, patch application, App Bundle integrity, process launch, Global source isolation and absence of a Bridge runtime in the active tree/artifact. They do **not** establish that SMS login, token refresh or China C3/PCCS succeeds with a real account/vehicle. China remote commands remain disabled.

