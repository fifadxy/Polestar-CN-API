# PsAssistant × ColorOS 16/17 Native Vehicle Bridge Research

Updated: 2026-10-30

## Goal

Integrate PsAssistant with OPPO / OnePlus China ColorOS native surfaces without duplicating Polestar credentials or vehicle API logic.

Target surfaces:

- ColorOS native device / vehicle binding UI
- Shelf / negative-one-screen cards
- Xiaobu voice vehicle commands
- OPPO Watch command handoff
- Fluid Cloud / Pantanal live cards
- Car+ only where it is actually useful

The Polestar China session and all remote writes remain inside PsAssistant. Root/Xposed code is an optional adapter layer.

## High-value upstream projects

| Project | ColorOS relevance | Reusable knowledge | PsAssistant use |
| --- | --- | --- | --- |
| Mubai-meng/FKColorOS | broad ColorOS modification ecosystem | package discovery, feature gates, OEM behavior changes | reference only; do not copy broad system overrides |
| luckyzyx/LuckyTool | active ColorOS Xposed toolkit; includes OPlus app scopes such as MyDevices | package-scoped hooks, modular settings, OPlus package map | package map and compatibility reference |
| Andrea-lyz/ColorOS-Assistant-Restore | verified ColorOS 16/17 CN; libxposed API 102 | structural resolution, per-process hooks, protective exception mode, narrow diversion-point repair | primary modern hook architecture reference |
| Xposed-Modules-Repo/io.github.hao1196561270.CarPlusUnlocker | modern hook for com.oplus.ocar | libxposed remote preferences, CertifiedApps injection, Car+ restart workflow | proves Car+ can be extended; Car+ is not treated as the vehicle-account source |
| MiaM1ku/Miband-OPlusBridge | ColorOS 16/17 bridge into com.heytap.mydevices and OPPO Health | DeviceApp injection, observer registration, shape-based method discovery across obfuscation | strongest template for native provider/device registration |
| ByteJuno/fcmfix-coloros | ColorOS 16/17 CN system/background policy | Hans/battery/network restrictions, diagnostics, fail-open OTA behavior | background reliability reference |
| whitewhale0612/Oplus-Hans-Policy | OPlus Hans/OFreezer system_server policy | freeze/thaw paths, Binder/packet wake behavior, OTA risk | only add selective exemption after evidence |
| Rove24/ColorOS-Archive-To-Open | ColorOS 17 / Android 17, libxposed API 102 | minimal modern module build, Java 17, fail-safe framework integration | build reference |
| SANWU5/c17-statusbar | ColorOS 17 SystemUI / Fluid Cloud research | Pantanal UMS service IDs, Seedling card/data-source mapping | Fluid Cloud adapter research |
| Costben/coloros-feiniu-bridge | modern bridge using com.heytap.mydevices | API 102 and host-specific token/bridge patterns | secondary MyDevices reference |
| ByteJuno/HuaweiPods-ColorOS | OPlus device-space accessory integration | MyDevices / accessory / Bluetooth scopes | confirms device-space integration model |
| 113324561/iccoa-carplay-sink | reverse engineering of com.oplus.ocar / CarLink on Android 17 | Car+ native transport and library boundaries | supports treating Car+ as projection/container layer |
| MFJip612/Oneplus13Locational | region component restoration | package inventory: cardigitalkey, opluscarlink, ocar, Pantanal UMS, travelengine, sceneservice | confirms subsystem separation |

## Important ColorOS package map

### Native device space

- `com.heytap.mydevices`
- Key known classes from public ColorOS 16/17 bridge work:
  - `com.heytap.mydevices.core.config.DeviceAppConfigManager`
  - `com.heytap.mydevices.core.agent.AppAgentManager`
  - `com.oplus.mydevices.domain.entities.config.DeviceApp`

ColorOS 16 can heavily obfuscate method names. ColorOS 17 builds have restored some stable names such as `getSupportDeviceApplications`, `addDeviceObserver`, `onDeviceAdd`, `onDeviceRemoved`, and `onDeviceUpdated`.

Rule: resolve by class/method shape first, then use stable names as hints. Never pin a single obfuscated name without build fingerprint validation.

### Shelf / negative-one-screen

- `com.coloros.assistantscreen`

Public package maps identify this as the ColorOS Shelf / negative-one-screen host. The exact "My Vehicle" provider path is not yet proven from public sources and must be captured from the target phone.

### Pantanal / Fluid Cloud / Suggestions

- `com.oplus.pantanal.ums`
- related components seen in region restore research:
  - `com.oplus.travelengine`
  - `com.coloros.sceneservice`

ColorOS 17 reverse engineering shows UMS assets containing service ID, host-less card and service priority mappings. Pantanal Seedling classes are used by SystemUI, Shelf, launcher, AOD, speech assistant and Car+.

### Xiaobu

- `com.heytap.speechassist`

Treat it as a command/assistant dispatch host. Do not replace the entire assistant stack or globally spoof region flags.

### Car+

- `com.oplus.ocar`
- `com.heytap.opluscarlink`

CarPlusUnlocker injects apps through Car+ certified-app lists. Reverse engineering also places projection/transport native libraries here. Therefore Car+ is an optional secondary integration surface, not our assumed vehicle-account binding source.

### Digital key

- `com.oplus.cardigitalkey`

This is deliberately outside phase 1. Remote vehicle control and digital-key provisioning are separate trust/security domains.

## Architecture adopted in this branch

```
ColorOS native host
  MyDevices / Shelf / Pantanal / Xiaobu / optional Car+
                |
        libxposed adapter
                |
      narrow local IPC contract
                |
       PsAssistantBridgeProvider
                |
        VehicleCoordinator
                |
         ChinaPolestarApi
                |
           Polestar CN
```

### Trust boundary

The Xposed module never receives:

- phone/password
- access token
- refresh token
- raw account session
- full VIN
- private client signing material

The bridge exposes only:

- sanitized vehicle snapshot
- refresh request
- explicit allow-listed vehicle command
- bridge capability/version metadata

All writes go through the existing `VehicleCoordinator.command()` path and therefore retain PsAssistant's existing readback verification.

## Existing PsAssistant behavior reused

The current v0.9.5-beta2 line already supports:

- lock / unlock
- tailgate open / close
- climate start / stop
- purifier start / stop
- windows open / close
- honk / flash / honk+flash

Remote writes are verified by multiple LIVE-state readbacks. The bridge must not create a second command implementation.

## Phase plan

### Phase 0 — current branch

Implemented:

- `PsAssistantBridgeProvider`
- separate `oppoBridge` libxposed module
- API protocol v1
- bridge diagnostics activity
- static Xposed scopes
- structural MyDevices probe
- CI build target for both APKs
- fail-closed behavior: no OEM state mutation until the target shape is verified

### Phase 1 — target phone capture

Collect from the target ColorOS 16/17 Chinese phone:

1. package versions and APK paths
2. Shelf "My Vehicle" open/bind flow logcat
3. Xiaobu command flow logcat
4. OPPO Watch command flow while phone is connected
5. target APK/JAR class maps for:
   - `com.coloros.assistantscreen`
   - `com.oplus.pantanal.ums`
   - `com.heytap.mydevices`
   - `com.heytap.speechassist`
   - `com.oplus.ocar` only as a secondary surface

### Phase 2 — MyDevices adapter

Adapt the proven Miband-OPlusBridge pattern:

- inject a PsAssistant provider entry
- verify support checks by structural signature
- register/update observer
- render a native Polestar device/vehicle card only after real target behavior is known

### Phase 3 — Shelf vehicle adapter

Trace the exact "My Vehicle" data provider and command dispatcher. Add a version-gated adapter only when both state reads and writes can be observed.

### Phase 4 — Xiaobu / Watch handoff

Map native semantic commands to PsAssistant actions:

| Native intent | PsAssistant action |
| --- | --- |
| lock vehicle | lock |
| unlock vehicle | unlock |
| start A/C | climate-start |
| stop A/C | climate-stop |
| start purification | air-start |
| open trunk | trunk-open |
| close windows | windows-close |
| find vehicle | honk-flash |

The Watch path should remain a phone-mediated command when possible; no need to modify the watch firmware.

### Phase 5 — Pantanal / Fluid Cloud

Prefer official/normal app-facing Seedling paths when available. Root injection is reserved for missing registration gates.

Possible cards:

- charging + SOC + remaining time
- climate running + remaining duration
- vehicle locked/unlocked
- command result

## OTA compatibility policy

Every private ColorOS hook must have:

1. package version/build fingerprint gate
2. class existence check
3. method-shape check
4. ambiguity rejection
5. protective exception mode
6. original behavior retained on failure
7. clear diagnostics
8. independent kill switch

No global `isExpRegion=true` style modification.

## Background reliability

ColorOS Hans/OFreezer can freeze app/service processes and break an otherwise correct Binder/assistant flow.

Policy:

- first use ordinary battery/background exemptions
- keep PsAssistant resident behavior narrow
- collect freeze/thaw evidence
- only then add a dedicated Hans exception
- never use broad system_server hooks as the first solution

## Device capture commands

```bash
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.opporom
adb shell getprop ro.build.fingerprint

adb shell pm list packages | grep -Ei 'oplus|oppo|heytap|pantanal|ocar|vehicle|speechassist|mydevices'
adb shell dumpsys package com.heytap.mydevices | head -120
adb shell dumpsys package com.coloros.assistantscreen | head -120
adb shell dumpsys package com.oplus.pantanal.ums | head -120
adb shell dumpsys package com.heytap.speechassist | head -120
adb shell dumpsys package com.oplus.ocar | head -120

adb logcat -c
adb logcat | grep -Ei 'vehicle|car|bind|mydevices|pantanal|seedling|speechassist|xiaobu|ocar|PsOppoBridge'
```

With root, obtain package paths only after identifying the exact installed version:

```bash
adb shell su -c 'pm path com.heytap.mydevices'
adb shell su -c 'pm path com.coloros.assistantscreen'
adb shell su -c 'pm path com.oplus.pantanal.ums'
adb shell su -c 'pm path com.heytap.speechassist'
adb shell su -c 'pm path com.oplus.ocar'
```

## Current conclusion

The most defensible integration order is:

1. PsAssistant backend IPC
2. MyDevices/provider registration
3. Shelf "My Vehicle" discovery and injection
4. Xiaobu / Watch semantic command bridge
5. Pantanal / Fluid Cloud
6. optional Car+
7. digital key as a separate future project

This order follows the strongest public ColorOS 16/17 evidence and keeps the existing Polestar credential and remote-control security boundary intact.

## 2026-10-30 integrated APK: v0.9.6-beta1 / bridge v0.3.0

The main APK now packages Device Card Provider, diagnostics, the control detail
panel, and the modern libxposed module entry together. A second module APK is
optional and remains available for older installed PsAssistant clients.

### User switch and lifecycle

Settings → OPPO device space and module → Enable OPPO vehicle device card.
Default: OFF. The Provider component is disabled in the manifest. Enabling saves
the preference, enables the Provider without killing the main app, and notifies
observers. Disabling closes the data/command gate before disabling the component;
already-bound Provider objects also return no card and reject calls. Runtime
fallback consults the same switch whenever the installed client has this gate.
Legacy clients without the switch retain their previous standalone behavior.

Native Provider mode does not itself require Xposed, but OEM allow-lists may
prevent automatic discovery. For MyDevices registration assistance, enable the
main PsAssistant APK as a module in an API-102-compatible framework and scope
`com.heytap.mydevices`. Restart the host or phone after first setup. A normal app
setting cannot silently turn on an LSPosed module or grant its scope.

Do not enable the old standalone bridge and integrated module simultaneously;
this can create two provider entries. The standalone package remains for old
clients; new integrated installs use only the main APK module.

### Protocol and source arrangement

- Discovery action: `oplus.devicecards.action.DEVICE_INFO`.
- Integrated authority: `io.github.polestarcn.companion.devices`.
- Query path: `device_info_table_new`; columns: device_id, device_mac,
  device_data, authority.
- Main bridge state/control authority remains
  `io.github.polestarcn.companion.bridge`.
- Device card data excludes demo snapshots, credentials, tokens and raw VIN.
- Serialization preserves the source timestamp; querying a card no longer
  changes the revision or presents stale data as newly captured data.
- The main Gradle task generates its bridge Java sources from `oppoBridge` and
  substitutes only application/provider/resource identities. The Java package
  and Xposed entry class remain `io.github.polestarcn.oppobridge`. There is one
  maintained bridge implementation, not two manually copied codebases.
- Keep rules retain the module and reflection-based bridge classes in release.

### Research evidence and limits

Primary implementation reference:
https://github.com/MiaM1ku/Miband-OPlusBridge

This project supplies a wearable-device integration pattern, not proof that
ColorOS accepts a `CAR` enum or renders all vehicle shortcut fields. Our vehicle
schema and callback details remain experimental until tested on the target
MyDevices version. Success means separately verifying provider discovery,
DeviceModel parsing, native card rendering, command acceptance, and vehicle-state
readback. Compilation alone proves none of these on a physical phone.

Xiaobu, Shelf, Pantanal/Seedling, Car+ and Watch still have probe/research entries;
they are not advertised as working vehicle integrations. Digital-key enrollment
is separate from this remote-control bridge.

### Build and checks

CI builds the integrated v0.9.6-beta1 main APK and separate bridge v0.3.0 APK,
runs unit tests and lint, and verifies packaged Xposed metadata. Tests cover
default-off command rejection, persisted opt-in/component enablement, closing an
already-bound provider after disable, and stable source-based card revisions.
Public CI builds omit the private account client configuration unless supplied
through the existing private build input. Persistent update signing still needs
the existing signing identity; never describe an unsigned/developer build as a
verified in-place update or a verified vehicle connection.

### Private standalone assembly after CI

`scripts/package_private_client.py` can combine an unchanged CI APK with the
approved `client-signing.bin` from an earlier standalone PsAssistant. It validates
the existing asset format/checksum/fingerprint, preserves every other APK entry,
realigns uncompressed ZIP entries, and signs/verifies with Android's apksigner.
The client asset and signing key never go to the public repository. Supply the
signing identity explicitly through existing signing environment variables. A
new test key does not allow an in-place update over an APK signed by another key;
use the original key for a seamless update. No account session or token is
extracted by this packaging workflow.

## v0.9.7-beta1 / bridge v0.4.0 coordination review

- Move all direct-panel Binder/provider calls and runtime fallback waits off the UI thread.
- Serialize panel operations and disable buttons while submitting, preventing rapid duplicate submissions.
- Keep request feedback separate from snapshots. Accepted means queued, never vehicle-confirmed success.
- Stop queued work when the panel is destroyed; delayed callbacks check activity lifetime.
- Add a standard Quick Settings tile to both APKs: 极星车控. This opens the control panel after system unlock and does not issue commands. It does not depend on MyDevices registration or Xposed. The existing opt-in gate still applies.
- Android 14+ uses the PendingIntent TileService launch API; earlier supported versions use the Intent API.
- Primary reference: https://developer.android.com/reference/android/service/quicksettings/TileService
- Next research: ControlsProviderService for a native system controls surface; live notifications for charging/climate, then version-gated Xiaobu command routing. None are marked implemented.
- Physical OPPO native-card discovery, host callbacks, framework API 102 loading and actual vehicle commands still require device testing.

## Base correction for v0.9.7-beta1

Merged android-polestar-assistant-main commit 5d9a8b3 (v0.9.6-beta6, versionCode 111) into the bridge branch. The deliverable versionCode is 112, retaining beta6 UI, map, energy, icons, authorization and regression tests. Previous OPPO builds based on v0.9.5-beta2 must not be described as the latest main app.

## v0.9.7-beta2: limited gallery trim for a sub-10-MB integrated APK

Retain beta6 main features, black/white default assets, front/side views and the embedded API-102 module v0.4.0. Delete only optional blue/silver overhead and three-quarter WebP files (four files, 3,472,434 bytes in the previous signed APK). Remove their menu entries and rendering references. Legacy saved selections migrate to black/white within the same view family according to the active day/night theme. Direct drawable lookup also normalizes old keys. Existing assets are not resized or recompressed. The CI checks the integrated debug APK against 10,000,000 bytes; private final signing must be checked again before delivery.

## v0.9.7-beta3 / bridge v0.5.0 native registration review

Primary comparison: MiaM1ku/Miband-OPlusBridge MyDevicesHook.java (stable-name resolution and supported-app list). Add hooks for getSupportDeviceApplications as well as the cached table. Resolve all observer methods before mutating host state; stable method names take priority over ambiguous structural candidates. Membership gates track provider enablement. Observer sessions use generation checks, bounded read retries and cleanup on registration failure. Provider-access failures remove the previous card rather than leaving stale state.

Add authenticated MyDevices host reports, independent of the integration switch, and read-only diagnostics showing last observed main/cards installation or card-added phase with module/host versions. These are historical observations, not proof of current module enablement, native rendering or vehicle success. No credential or VIN is recorded. Only the actual MyDevices UID can submit IPC reports.

Unknown host methods now fail explicitly. Queue acceptance returns vehicleConfirmed=false. Unknown/idle/planned/completed charge states are not displayed as charging. Native CAR schema and shortcut callbacks remain target-phone dependent. Keep the signed integrated APK below 10,000,000 bytes and retain the trimmed gallery.

### Phone and parked-vehicle power audit

- Card query, host diagnostics, card-show/connect, rendering and bounded local observer retries read only cached state; card-show/connect no longer start cloud vehicle refreshes.
- Explicit OPPO refresh is rate-limited to one admission per 60 seconds; ordinary command readback is separate.
- Optional long-parking saver (default OFF to preserve explicit existing refresh preferences) stops foreground automatic reads, Widget/background jobs, map timers and opening-location probes. It does not stop manual refresh, requested-command verification or active camping supervision. Existing intervals are preserved when disabled again.
- Existing foreground default is 5 minutes; Widget/background default 30 minutes when enabled. Android batching changes actual execution. Display ticks/notification redraws are local, not vehicle API reads. No new wake lock or always-on polling is added.
- GetLatest cloud state calls cannot be assumed to wake the car, nor assumed wake-free, without target-backend/vehicle evidence. No 12V voltage or sleeping-current measurement is available. The policy reduces requests; it is not a guarantee against 12V discharge. Do not apply Polestar 1 TJ35542 wake/charging figures to Polestar 2.
- Primary reference for the role of the Polestar 2 12V battery: https://www.polestar.com/uk/manual/polestar-2/2021/article/308b0a98bfc63e62c0a8015172fd2960/ . Android task batching reference: https://developer.android.com/develop/background-work/background-tasks/optimize-battery .

## v0.9.7-beta4 Shelf setup correction

MyDevices integration is not evidence of AssistScreen native vehicle support. Add an in-app local setup page with standard Android provider inventory, bound-instance counts, launcher/Shelf/MyDevices versions, an OPPO standard picker shortcut and the official launcher pin request. Pin acceptance is only a request and does not select Shelf placement; bound instance count is not proof of Shelf placement. Native My Vehicle brand-list registration remains unverified. No additional cloud polling is introduced.

Primary observed picker reference: https://github.com/Nielk74/oppo-widget-launcher — explicit com.android.launcher/com.android.launcher.Launcher, extra state=ToggleBar_widget, no action/category. Project verification is on PMA120/ColorOS16.1; do not generalize it to every China firmware.

Usage: Settings → OPPO device space and module → Shelf/home widget setup. Open the standard widget list and locate PsAssistant. If Shelf provides third-party widgets, add there manually; otherwise use the home-screen widget or existing Quick Settings tile. Copy the setup diagnosis to distinguish an unregistered provider from a host catalog limitation. LSP module is only needed for MyDevices hooks, not ordinary Android widgets.


## v0.9.7-beta5: target evidence and bounded improvement

User target: launcher 16.6.17, AssistScreen 17.14.60, MyDevices 17.4.15. Screenshots prove a selectable device card is placed on Shelf and opens our detail activity. They do NOT prove native My Vehicle protocol support or command execution. User confirms the CAR card omits supplied shortcuts and vehicle state. Existing schema already sends battery fields and four shortcuts; these are ignored by this rendered template. No unsupported cardStyle/enum guesses or host-wide hooks are introduced.

Use the rendered mDeviceName as a compatibility fallback: known valid battery/range first, explicitly marked cached, then original vehicle name. Stable deviceId remains based on original identity, not changing state. Rendering/truncation still requires target validation. Missing values never become zero. Full cache source timestamp appears on the redesigned high-contrast, two-column detail page. A lifecycle-scoped content observer updates cached state without a polling timer or cloud read; it is removed on stop. Queue acceptance remains distinct from vehicle completion. Unlock and tailgate operations require explicit confirmation.

This release improves cached visibility and detail usability. It does not solve first-level native control: the target AssistScreen APK/protocol is still required to implement a verified richer template. Standard Android widgets remain the existing direct-control option on supported widget hosts. Update APK, reopen Shelf; if the host retains the old name, remove and re-add only this device card. No vehicle refresh is needed for card re-add.

Setup now offers an explicit user-initiated SAF export of the installed AssistScreen and MyDevices base/split APK files, versions and SHA256 manifest. It never reads private app directories, account/session storage, logs or vehicle cache; it uses no root, no cloud requests, no storage permission. ZIP streams on a worker thread with compression level zero for already-compressed APKs. Export may fail if OEM source paths cannot be read; do not claim success on partial output. The user chooses the destination and manually shares the archive.

## Direct-control code research — 2026-10-30

### New primary-source leads and evidence limits

- Hush `DeviceCard.kt`: https://github.com/dmitthedazed/hush/blob/main/app/src/main/java/com/occaecat/hush/DeviceCard.kt (blob cd464dcc7051fb6f79ff898da4763b159ee12808). Documents MyDevices 17.4.10 and `remotecards/utils/CardsUtils.b`: `componentType=2` routes a shortcut through ContentResolver.call using `componentExtra.authority`, `method`, and `extra`; type 1 is described as requiring a signature-only permission. This is a headset implementation, not proof of CAR/Shelf rendering on 17.4.15/17.14.60.
- Hush `xposed/HushModule.kt`: https://github.com/dmitthedazed/hush/blob/main/app/src/main/java/com/occaecat/hush/xposed/HushModule.kt . `QuickAppCardAction` uses a controller registry keyed by package and device type. Its fallback controller returns an empty Map and opens a panel on the one-argument callback. Our `MyDevicesVehicleHook.installCardProcess` currently has this same limited routing shape. The Map semantics are unverified; do not fill arbitrary action names and expect buttons to appear.
- CarPlusUnlocker `HOOKS.md`: https://github.com/hao1196561270/CarPlusUnlocker/blob/main/HOOKS.md (blob 3bcde896dcba2796a03b9eedcbb952b94599fa65). Hooks `com.oplus.ocar.carcontrol.response.CertifiedApps.getList` and injects media app entries. The `carcontrol` package substring here is not evidence of lock/climate/tailgate control or Shelf vehicle registration.

### What our existing code already implements

`VehicleDeviceCardProvider.shortcuts` emits the same provider shortcut shape, using method `ps_vehicle_command` and extras `lock`, `unlock`, `climate-start`, `climate-stop`, `air-start`, `air-stop`, `trunk-open`, `trunk-close`. In the integrated APK authority is `io.github.polestarcn.companion.devices`; standalone authority is `io.github.polestarcn.oppobridge.devices`. `call` forwards accepted commands to BridgeClient and the main app coordinator, without requiring openPanel. This is a candidate direct-control transport already implemented; it does not prove any native button is rendered or invokes it.

Screenshot evidence shows missing controls and battery/range fields. It does not distinguish JSON deserialization drops, conversion drops, template gating, shortcut filtering or another hosting path. Earlier wording that the CAR template "ignores" fields describes the observed result, not a proven internal cause. Likewise, the current empty controller Map is a lead to inspect, not a confirmed cause of missing buttons.

### Required trace on the exact OEM APKs

1. Follow the `device_info_table_new` row into DeviceModel, then conversion to DeviceInfo; establish whether shortcuts and battery survive conversion for CAR.
2. Follow the actual Shelf card template selection, including size, device type, cardStyle, package and capability gates. Find views or declarative slots for data and actions. Do not guess new cardStyle numbers or spoof HEADSET as the production solution.
3. Trace `QuickAppCardAction` registry controllers, the Map-returning interface method and all call sites; identify whether it describes supported actions, permissions, links or something else before implementing it.
4. Trace `componentType=2` at the target build's equivalent of CardsUtils.b through to provider call. An individual button must dispatch its explicit action without forwarding to the generic detail callback. Keep click handlers separate from card-background/detail click.
5. If the stock CAR template has no action slots, prototype an LSPosed layout extension scoped exclusively to our package/device ID. First verify the actual render technology and process; a View hook will not automatically work for quick-app, web or RemoteViews surfaces. Reuse the authenticated main bridge for all execution and cached state for all rendering.
6. Validate one explicit lock action, request feedback, bounded readback, unchanged generic device cards, no cloud calls on card show, and behavior after host restart/OTA. Export exact AssistScreen 17.14.60 and MyDevices 17.4.15 using beta5's user-chosen system APK export; no accounts, tokens, logs or private app directories are included.

Research status: a concrete provider transport and a specific controller-registry lead exist. A verified native Shelf vehicle action template, exact action-map semantics and on-phone direct control remain unresolved. No new production hook or feature-complete APK is claimed by this research note.

## v0.9.7-beta6 — one-action research capture, 2026-10-31

The setup screen now starts one process-local background export and opens Android's share chooser after generation. It also offers sharing the completed archive again and saving it via SAF. The worker uses application context, survives setup activity recreation, deduplicates concurrent generation, and does not create a persistent service, vehicle refresh, root shell, polling task or network upload. The share chooser leaves recipient selection and actual sending to the user.

Archive schema 2 contains AssistScreen and MyDevices base/split APKs, per-file byte count and SHA256, versions and signing-certificate fingerprints, public activity/receiver/service/provider declarations and permission names. Launcher, Pantanal UMS and quick-app engine are metadata-only. The environment records Android SDK/security patch, build/model/ABI and UI dimensions/font scale. Build fingerprint is a firmware identifier; serial numbers, Android ID, account tokens, VIN, vehicle snapshots, arbitrary preferences, screenshots and logs are not collected. Module history exports only integration toggle, process readiness/time/phase and host/module versions; raw failure reasons are excluded.

Missing packages or unreadable APKs are recorded per item. Readable APKs and environment metadata remain useful even if one host is unavailable. `requiredApksComplete=false` and per-file `copied=false` prevent treating a partial archive as a complete target capture. Output/disk failures do not return success; intermediate output is removed. Sharing is read-only via a non-exported FileProvider restricted to the dedicated export cache directory. No account or general app-private directory is shared.

Research decision remains: compare target parser/converter fields, CAR template and `QuickAppCardAction` controller interface plus its Map-returning method call sites. The Provider command transport is already present. Missing buttons alone do not locate the loss stage. Do not ship guessed action maps or unrelated media-app allowlist hooks as a solution. Once the exported pair arrives, use its hashes/versions to bind all findings to that build; first demonstrate cache-only state rendering and one explicit lock command, then add remaining actions with bounded readback. This release improves evidence collection and does not claim native first-level control is complete.
