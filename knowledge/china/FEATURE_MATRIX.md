# Hisingen Global → China feature mapping and acceptance tests

**Baseline:** Hisingen upstream `v2.0.10`, China Android `4.25.0-262`, China Preview `0.18.0`. Updated 2026-10-22.

Legend: **GLOBAL-IMPLEMENTED** = upstream source supports it, not that a real account was tested here; **BRIDGE-ONLY** = China live Android process read, not a native Mac API; **MAPPING_REQUIRED** = China direct session/RPC unverified; **DISABLED** = intentionally not executable; **NOT_APPLICABLE** = an EU-only interface cannot be assumed in China.

| Feature / original Hisingen path | Global | China verified evidence | China implementation / status | Required next validation |
| --- | --- | --- | --- | --- |
| Polestar ID user login (Global OIDC) | GLOBAL-IMPLEMENTED | CN official web page, separate identity system | Official China site window; direct CN vehicle session **MAPPING_REQUIRED** | Native CN authorized callback/session, refresh, audience |
| Polestar command-client grant | GLOBAL-IMPLEMENTED | CN `key_*` local names observed; audience unknown | MAPPING_REQUIRED | Authorized issuer/client/scope/refresh verification |
| EU Data Portal (Data Act) | GLOBAL-IMPLEMENTED | No CN EU M2M equivalence established | NOT_APPLICABLE as CN substitute | Identify separate supported CN data API |
| Augmented (consumer + M2M) | GLOBAL-IMPLEMENTED | No compatible CN M2M path | MAPPING_REQUIRED | Define valid CN merge after state sources verified |
| Vehicle discovery (Mystar) | GLOBAL-IMPLEMENTED | China app knows linked vehicles | BRIDGE-ONLY one opaque car; native CN MAPPING_REQUIRED | Multi-vehicle CN discovery, authorization |
| Global C3 discovery | GLOBAL-IMPLEMENTED | CN C3 hosts observed | MAPPING_REQUIRED for native Mac | CN token/audience + discovered gRPC host |
| Global PCCS/Chronos | GLOBAL-IMPLEMENTED | CN PCCS host and Proto found | MAPPING_REQUIRED | Read-only RPC/response schema first |
| Battery SOC | GLOBAL-IMPLEMENTED | Android C3 battery model `cliif` | BRIDGE-ONLY | Real device test and timestamp freshness |
| Estimated range (km/mi) | GLOBAL-IMPLEMENTED | `clicatch` / `cliclass` | BRIDGE-ONLY | Unit/currency conversion and multi-vehicle source validation |
| Charging state/time/consumption | GLOBAL-IMPLEMENTED | China battery model string and fields | BRIDGE-ONLY where data present | Unplugged/charging/error enum fixtures |
| Exterior doors / windows / lock state | GLOBAL-IMPLEMENTED | C3 exterior controller live observed | MAPPING_REQUIRED; bridge contract prepared | Actual response semantic field mapping |
| Availability / online | GLOBAL-IMPLEMENTED | Current CN availability field not verified | MAPPING_REQUIRED | Avoid calling vehicle Available when unknown |
| Odometer | GLOBAL-IMPLEMENTED | No verified CN odometer field | MAPPING_REQUIRED; bridge contract prepared | Extract timestamped model read, unit mapping |
| Health & maintenance | GLOBAL-IMPLEMENTED | No verified CN model | MAPPING_REQUIRED | Read-only proto and error semantics |
| Climate state and timers | GLOBAL-IMPLEMENTED | Max runtime 30 observed; direct-start settings false | MAPPING_REQUIRED | C3 state + PCCS timer reads |
| Live C3 gRPC stream | GLOBAL-IMPLEMENTED | Android app C3 live observed | BRIDGE-ONLY periodic app-object sampling; **not direct C3 stream** | Tokenized native stream, reconnect, per-event timestamps |
| Target charge SOC | GLOBAL-IMPLEMENTED | CN Chronos target SOC request proto exists | MAPPING_REQUIRED | Read first; write disabled |
| Charging amp limit | GLOBAL-IMPLEMENTED | CN PCCS likely analogous but no verified method | MAPPING_REQUIRED | Read-only schema, limits, supported models |
| Global charging timer | GLOBAL-IMPLEMENTED | Chronos service family observed | MAPPING_REQUIRED | Read-only request/response semantics |
| Climate start/stop | GLOBAL-IMPLEMENTED | CN request class observed | DISABLED | Explicit end-to-end permission and vehicle confirmation |
| Remote lock/unlock | GLOBAL-IMPLEMENTED | CN exterior controller has method identifiers | DISABLED | Explicit end-to-end permission and vehicle confirmation |
| Windows/tailgate/honk/flash | GLOBAL-IMPLEMENTED | Methods/capability indicators observed | DISABLED | Command-specific mapped request and test |
| Charging override / write settings | GLOBAL-IMPLEMENTED | Protobuf write request structures observed | DISABLED | Verified permission, model limits and outcome |
| OTA schedule/install/cancel | GLOBAL-IMPLEMENTED | Global OTA service known; CN unverified | DISABLED | Official compatibility and full consent workflow |
| Vehicle location / weather / air quality | GLOBAL-IMPLEMENTED | CN LBS host observed; state fields unverified | MAPPING_REQUIRED | Authorized read-only responses; redact GPS |
| Volvo functionality | GLOBAL-IMPLEMENTED | No China modifications to Volvo provider | Original upstream maintained | Regression with own Volvo account if available |

## Test levels

| Test | CI automated | Needs user/device |
| --- | --- | --- |
| Upstream v2.0.10 still compiles with China patch | YES | No |
| Global provider still compiled and selected for Region Global | Static source routing + startup | Actual Global account authentication/vehicle refresh |
| China selected on first real launch | Static UI wiring; interactive dialog requires GUI test | Visual first-start verification |
| China/Global menu + Settings switching | Static selection + rebuild logic checks | Manual GUI switch with provider state isolation |
| China bridge data schema and basic mock SOC | YES | Real official China Android app/Frida run |
| China C3 live data freshness | Fixture and timestamp tests | Real signed-in app with refresh |
| China direct C3/PCCS token exchanges | NO | Approved real authentication integration |
| China remote commands | DELIBERATELY DISABLED | Later separate permissioned vehicle test |

## Minimum acceptance criteria

**P0 (UI/account):** First real launch dialog says `Select Polestar Region` with exactly China/Global; choosing persists; Hisingen app menu and Settings permit switches; Global and China login forms do not mix; Region switch rebuilds active provider; China does not inherit Global cached SOC/session.

**P1 (CN read-only):** The signed-in Android bridge supplies a battery model with real provider timestamp; SOC/range/charge status appear with accurate freshness; the app handles missing bridge safely and never displays mock data in production. Exterior, odometer and availability show unavailable until validated.

**P2 (CN native):** Mac user can complete a legitimate CN account login; vehicle discovery and authorized C3 read work without Android; Token/session lifecycle is verified. Do not claim P2 until end-to-end evidence exists.

**P3 (CN commands):** Individual control buttons become available only after verified CN method, scope, parameter validation, explicit user confirmation and vehicle-side outcome. No batch enablement by copying Global commands.

## Known current limitations

0.18.0 tests prove compilation, App Bundle integrity, process launch, and the bridge **mock contract**. These tests do **not** establish that the original Global remote commands work with a real account, nor that direct native China C3/PCCS works. UI P0 and China bridge P1 need Mac/device validation beyond CI.

