# MyDevices 17.4.15 OEM CAR QuickApp protocol

Date: 2026-11-01

Target:
- `com.heytap.mydevices` 17.4.15
- exported 2x2/4x2 QuickApp RPK from the user's exact device

## Confirmed native QuickApp provider protocol

The bundled RPK talks to:

```
content://com.oplus.mydevices.authority.quickappcards/
  com.oplus.mydevices.quickapp.provider.DeviceQuickAppCardWidgetProvider
```

Main methods:

```
com.oplus.mydevices.getCardData
com.oplus.mydevices.onCardShow
com.oplus.mydevices.onCardHide
com.oplus.mydevices.onCardDestroy
com.oplus.mydevices.onCardCreate
com.oplus.mydevices.jumpDeviceHome
com.oplus.mydevices.jumpDeviceDetail
com.oplus.mydevices.bindDevice
com.oplus.mydevices.clickEvent
```

The OEM click call carries:

```
deviceId: String
shortcutId: String
valueBoolean: bool
  or
valueInt: int
```

The URI adds a timestamp suffix and the resolver call uses the QuickApp widget instance ID as its args value.

Bridge 1.2.0 adds a read-only runtime probe for this exact provider contract. It never changes OEM provider results.

## Dormant OEM CAR IDs

The 2x2 RPK contains a native car icon map:

```
kongtiao   -> climate
chesuo     -> vehicle lock
shanguang  -> flash
mingdi     -> horn
```

The 4x2 RPK also contains a dedicated `components/car` implementation whose click handler uses the same four IDs.

Bridge 1.2.0 pre-routes these exact IDs through the existing PsAssistant command bus:

- `shanguang` -> `flash`
- `mingdi` -> `honk`
- `chesuo` -> lock/unlock toggle from the current vehicle snapshot
- `kongtiao` -> climate start/stop toggle from the current vehicle snapshot

For lock and climate, the bridge deliberately ignores the OEM `valueInt/valueBoolean` semantic and uses the live PsAssistant snapshot. This avoids guessing whether an OEM numeric value means "current state" or "requested state".

## Why the OEM CAR UI is not currently visible

There are multiple independent gates.

### Java converter whitelist

`assets/support/card_short_cut.json` does not include:

```
kongtiao
chesuo
shanguang
mingdi
```

### QuickApp RPK eventSummary

The 2x2 RPK defines the CAR icons but its top-level `eventSummary` removes all four CAR IDs before rendering.

The 4x2 RPK also filters `deviceData.shortcuts` through `eventSummary`.

### 2x2 registry

The exact 2x2 `deviceCardRegistry.js` only registers:

- ability (lamp/camera/air conditioner)
- TV
- pencil
- headset

There is no active `CAR` registry slot in this build.

Therefore merely changing shortcut IDs or widening the Java whitelist cannot fully enable the OEM CAR UI.

## CAR coordination state

The OEM 2x2 bundle contains explicit CAR behavior for:

```
deviceInfo.type === 'CAR'
deviceInfo.connectState === 'COORDINATED'
```

and its icon/color logic also treats `COORDINATED` as the active car state.

Bridge 1.2.0 therefore changes the PsAssistant CAR model from:

```
CONNECTED / DEFAULT
```

to:

```
COORDINATED / COORDINATED
```

This is limited to the synthetic MyDevices presentation model; it does not change Polestar connectivity/session semantics.

## Current integration strategy

Stable path:

```
PsAssistant
  -> VehicleDeviceCardProvider
  -> MyDevices registration
  -> NativeHomeCardHook (exact 17.4.15)
  -> NativeVehicleHomeView
  -> VehicleCoordinator
```

Experimental OEM path:

```
MyDevices QuickApp CAR RPK
  -> DeviceQuickAppCardWidgetProvider
  -> com.oplus.mydevices.clickEvent
  -> custom controller hook
  -> CardShortcutCommand
  -> VehicleCoordinator
```

The experimental path is prepared but not claimed as rendered until a future exact-build adapter safely unlocks both the RPK registry and eventSummary gates.

## Next research target

Do not patch the full system APK.

Prefer one of these scoped approaches:

1. identify MyDevices' internal RPK replacement/update path (`oldWidgetCode/replacedWidgetCode`) and test whether the OEM card supports an in-memory replacement;
2. identify the exact provider/converter stage that can select a CAR-specific card without modifying unrelated devices;
3. keep NativeHomeCardHook as the fail-closed fallback for 17.4.15.

Any RPK mutation must be version-gated to the exact APK fingerprint and must restore the OEM path on mismatch.
