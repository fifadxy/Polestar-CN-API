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

### 2x2 versus 4x2 registry

The exact 2x2 `deviceCardRegistry.js` only registers:

- ability (lamp/camera/air conditioner)
- TV
- pencil
- headset

There is no active `CAR` registry slot in the 2x2 template.

The exact 4x2 `deviceCardRegistry.js`, however, **does register CAR**:

```js
{
  slot: 'car',
  match(d) {
    return d.type === 'CAR';
  }
}
```

and the 4x2 bundle imports a dedicated `components/car/index.ux`. Its click handler calls the same OEM `com.oplus.mydevices.clickEvent` contract with `kongtiao / chesuo / shanguang / mingdi`.

Therefore the 4x2 native CAR template already exists and is selectable on this build. The remaining blocker for its controls is primarily the top-level `eventSummary` filter, which removes those four CAR shortcut IDs before the car component receives `deviceInfo.shortcuts`.

Practical consequence:

- 2x2: requires both a CAR registry/template solution and shortcut-gate solution; keep NativeHomeCard fallback.
- 4x2: CAR registry/template already exists; focus research on `eventSummary` / widget replacement routing instead of rebuilding the whole card.

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

## Exact CAR TSL contract from MyDevices DEX

Exact DEX instruction-reference analysis of MyDevices 17.4.15 identifies the obfuscated converter `aa.ya`. Its own log tag is `CarTslConverter`.

The nested mapper `aa.ya$a.a(String)` maps ColorOS instruction types to vehicle TSL property IDs:

```
InstructionType.CAR_DOOR
  -> lockStatus_23296_5

InstructionType.ALL_WINDOW
  -> windowOnOff_23296_4

InstructionType.AC_TEMP
  -> power_1536_1

InstructionType.CAR_HORN_AND_FLASH
  -> findCar_23296_7
```

Both the `-0` and `-1` variants of the instruction ID resolve to the same property key. This proves the property identity but does not by itself prove the numeric polarity, so the bridge must not infer that 0 always means off/locked or that 1 always means on/unlocked.

The main `aa.ya.b(DeviceModel)` converter parses `DeviceModel.data` into `CarStatusInfo` and publishes:

```
totalEnduranceMileage
  -> remainingRange_23296_1

batteryEnduranceMileage
  -> elecRemainingRange_23296_3

oilEnduranceMileage
  -> oilRemainingRange_23296_2

BatteryInfo.charge
  -> batteryCharging_1280_2

BatteryInfo.value
  -> batteryLevel_1280_1
```

The exact `CarStatusInfo` model is:

```
batteryEnduranceMileage : int
oilEnduranceMileage     : int
totalEnduranceMileage   : int
companyImg              : String
isRefreshing            : boolean
state                   : CarState
msg                     : CarMessage
```

`CarState` is a UI/action state object rather than the physical lock/window state itself:

```
description
buttonName
extra
disable
needLoading
buttonAction
buttonComponent
componentExtra
```

Therefore Bridge 1.2.0 continues to use PsAssistant's authoritative vehicle snapshot and command/readback path for lock, windows and climate. The OEM TSL property names are treated as a compatibility contract for future native-template integration, not as permission to guess write values.

## Exact widget replacement observation target

DEX reference analysis of MyDevices 17.4.15 confirms these provider methods consume `replacedWidgetCode`:

```
DeviceQuickAppCardWidgetProvider.handleGetCardData(String, Bundle)
DeviceQuickAppCardWidgetProvider.handleCardShow(String, Bundle)
```

The 4x2 RPK itself also reads:

```
oldWidgetCode
replacedWidgetCode
```

and sends the selected code back through its ContentResolver calls.

Bridge 1.2.x therefore adds a read-only `QuickAppReplacementProbe` in `com.heytap.mydevices:cards`. The next runtime capture should establish whether an active 4x2 card receives a replacement code and which concrete widget path is selected before any in-memory RPK replacement is attempted.

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
