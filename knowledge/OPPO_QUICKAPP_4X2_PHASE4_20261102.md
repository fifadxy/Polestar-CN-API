# OPPO QuickApp 4x2 phase 4 — exact runtime data capture

Date: 2026-11-02

Exact phone evidence supplied from the lightweight research bundle:
- OnePlus PJZ110, Android 16 / SDK 36
- MyDevices 17.4.15
- AssistantScreen 17.14.60
- PsAssistant bridge protocol 1
- live Polestar 2 snapshot is available to the bridge
- MyDevices main and :cards hooks are both active
- AssistantScreen hook is active

## Key correction

The immediate negative-one-screen route is already present in AssistantScreen's local card catalog:

- type 10000581
- group: 设备空间
- name: 自选设备
- size: 2
- package: com.heytap.mydevices
- component: DeviceQuickAppCardProvider
- serviceId: 536878706

Therefore a brand-new Pantanal/SceneService vehicle service is not required merely to render the current 4x2 card. Official brand/provider registration remains a secondary track.

## Current blocker

MyDevices 17.4.15 has a CAR visual component in the 4x2 RPK, but two shortcut gates were confirmed:

1. Java QuickCardDeviceDataConverter allow-list does not contain PsAssistant IDs or the dormant CAR IDs.
2. The captured RPK eventSummary window does not contain those CAR IDs.

The 4x2 code also contains a smartHomeDefault[type] fallback after shortcut filtering. The previous export did not include the shared config/registry files, so whether CAR has a built-in fallback set was still unproven.

## beta16 / Bridge 1.3.1 instrumentation

The next build records only a sanitized summary of the final QuickApp Bundle:
- numeric widget route -> exact 2x2/4x2 mapping
- Bundle key names
- deviceList present/count
- selected device type/package
- whether the selected row is the PsAssistant card
- shortcut count and shortcut IDs only
- isAuth / isBind / hasDevice
- whether a data field is present

It never persists raw deviceList, vehicle name, MAC, VIN, account, token, password, cookie or raw API payload.

The research ZIP also adds:
- oppo-official-bind-status
- RPK entry-name index
- bounded keyword windows for smartHomeDefault, eventSummary, carEventIconName and deviceCardRegistry
- explicit diagnostics when AssistantScreen's provider catalog is genuinely empty

## Decision gate after next capture

If final QuickApp data is CAR + PsAssistant and smartHomeDefault contains CAR controls, use the dormant OEM CAR route and controller mapping.

If CAR is correct but no CAR fallback exists, implement a targeted 4x2 card-data/RPK configuration shim for this exact MyDevices build rather than broad SceneService mutation.

If the final device type/package is normalized away before the RPK, fix that normalization first.

Official POLESTAR provider registration, Xiaobu semantic routing, Watch X2 endurance routing and digital-key provisioning remain separate workstreams.
