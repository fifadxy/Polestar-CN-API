# OPPO native vehicle control phase 3 — beta11 / Bridge 1.0.0

Date: 2026-11-01  
Exact targets retained:
- MyDevices 17.4.15
- AssistantScreen 17.14.60
- Pantanal 17.22.6
- Quick App Engine 11.2.0

## What changed

### 1. Truthful command completion

The native MyDevices CAR view and the bridge detail panel no longer treat "request accepted" as "vehicle completed".

For observable commands the UI now waits for a newer PsAssistant vehicle snapshot and confirms only when the requested state is present:

- lock / unlock
- climate start / stop
- purifier start / stop
- tailgate open / close
- windows open / close

The wait is bounded to 32 seconds. Timeout is shown as unconfirmed rather than success.

Honk / flash / honk+flash have no reliable vehicle-state readback in the current snapshot model. They are reported only as accepted and require physical confirmation.

### 2. Full bridge control surface

The detailed control panel now exposes:

- lock / unlock
- climate on / off
- purifier on / off
- tailgate open / close
- windows open / close
- honk
- flash
- honk + flash vehicle finding
- explicit refresh

Unlock, tailgate movement and opening windows keep an explicit confirmation dialog.

### 3. Exact MyDevices native-home fix

MyDevices 17.4.15 native content replacement already targets:

```
com.oplus.mydevices.deviceui.devicecard.view.homecard.HomeCardView
HomeCardView.D(QuickCardDeviceData)
```

The exact host fields `j` and `i` are private in the target shape. Bridge 1.0.0 resolves them once and enables reflective access before use. Failure remains fail-closed and falls back to the OEM card.

This specifically addresses the symptom where the generic CAR row renders SOC/range and "View device" but the custom direct-control content does not appear.

### 4. One-tap lightweight research export

Bridge Diagnostics now includes:

```
一键导出 OPPO 轻量研究包（<10MB）
```

The export is intentionally bounded:
- final ZIP must be below 9 MB
- raw collected payload is capped at 4.5 MB
- individual extracted text assets are capped
- no account/password/token/VIN/raw account payload is exported

Included:
- device/Android/build fingerprint
- relevant package versions
- APK sizes and SHA-256 fingerprints
- bounded relevant component names
- sanitized bridge capabilities and cached vehicle state
- MyDevices hook status
- Shelf/Pantanal service index status
- SceneService account-bind/provider-catalog status
- baseline diff result
- MyDevices `card_short_cut.json`
- selected 2x2/4x2 QuickApp RPK JavaScript/config entries when available

The package is written only after the user taps the button and is shared through a temporary read-only FileProvider URI.

## Acceptance sequence

1. Install beta11 integrated APK or Bridge 1.0.0.
2. Ensure the module scope includes PsAssistant and MyDevices.
3. Force-stop/restart MyDevices.
4. Confirm the PsAssistant CAR row still appears.
5. Confirm the row changes from generic "View device" content to native direct controls.
6. Test lock first and verify the card says vehicle-confirmed only after an updated snapshot reports locked.
7. Test climate and purifier.
8. Test tailgate/windows only in a safe environment.
9. If native controls do not render, open Bridge Diagnostics and export the lightweight research ZIP.
10. For Shelf "My Vehicle" research, record baseline, enter/add/bind My Vehicle, return, run diff, then export the ZIP.

## Still not claimed complete

- official OPPO "My Vehicle" brand registration
- SceneService provider injection
- Pantanal service registration
- Xiaobu semantic command handoff
- Watch X2 normal/endurance routing
- digital-key provisioning

The next registration shim should only be designed after runtime correlation identifies the actual vehicle `serviceId` and SceneService provider code.
