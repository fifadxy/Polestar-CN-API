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
