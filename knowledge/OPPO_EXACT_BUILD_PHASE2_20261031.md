# OPPO exact-build phase 2: MyDevices 17.4.15 + AssistantScreen 17.14.60

Evidence date: 2026-10-31

Target phone evidence:
- OnePlus PJZ110 / Android 16
- MyDevices 17.4.15
- AssistantScreen 17.14.60
- Pantanal 17.22.6 package/component inventory
- Quick App Engine 11.2.0 package/component inventory
- MyDevices APK SHA-256: `4b777b6c2b9c05535e8750a49068ce41d6bdffd6d03083796722eadfc2beffba`
- bundled quick-card RPK SHA-256: `b0f2cf9ac46b3dfe29a0923b2ac7e04cf9d002a343e8939ec641029b3e95a870`

This document separates static reverse-engineering evidence from real-device acceptance. No OEM APK is committed.

## 1. Native shortcut provider transport is confirmed

MyDevices 17.4.15 `CardsUtils.onShortcutClick` is obfuscated as the six-argument `CardsUtils.p(...)` path.

The provider branch ends in `CardsUtils.b(...)`:

1. parse `Shortcut.componentExtra` as `ProviderExtra`
2. use explicit URI or build it from authority
3. copy `ProviderExtra.extra` into Bundle key `extra`
4. copy card host into Bundle key `location`
5. call `ContentResolver.call(uri, method, null, bundle)`

This matches PsAssistant's current componentType=2 / authority / method / extra design. It does not prove the OEM renderer will expose the shortcut.

## 2. QuickApp click routing is confirmed

`DeviceQuickAppCardWidgetProvider.handleClickEvent(widgetCode, bundle)` reads:

- `deviceId`
- `shortcutId`
- `valueBoolean`, otherwise `valueInt`

and forwards them to `QuickAppCardAction.q(deviceId, shortcutId, value, widgetCode)`.

`QuickAppCardAction.q`:

1. resolves the device
2. reads `device.getDeviceAppPackage()`
3. reads `device.getDeviceType().name()`
4. asks the controller registry for `packageName + deviceType`
5. calls the selected controller's three-argument callback `controller.b(device, shortcutId, value)`

The controller contract is:

```
Map a()                         // packageName -> List<deviceType>
void b(device, String, Object)  // shortcut click
void c(device)                  // detail navigation
```

Therefore the existing PsAssistant controller proxy is architecturally correct: it advertises only its own package + CAR and accepts only explicitly emitted PsAssistant shortcut IDs.

## 3. QuickApp has two independent shortcut allow-lists

### Gate A: native converter

`QuickCardDeviceDataConverter.q(device, deviceType, connectState)` merges:

- TSL items
- DeviceModel standard shortcuts

then de-duplicates them and filters every ID through `QuickCardDeviceDataConverter.r()`.

`r()` is backed by `assets/support/card_short_cut.json`.

The exact 17.4.15 list contains headset, lamp, air-conditioner, camera, TV, phone/pad/computer and watch IDs. It contains no `ps_*` ID.

Result: PsAssistant's explicit IDs are removed before the QuickApp renderer receives the data.

### Gate B: bundled RPK

The bundled 2x2 and 4x2 root cards then do a second filter:

```javascript
deviceData.shortcuts.filter(item => new Set(eventSummary).has(item.id))
```

Neither RPK `eventSummary` contains a `ps_*` ID.

Hashes:
- SingleDeviceCard2x2/index.js: `f63dfc8554ef4465800dae5264ceab0b7c873d924cd37436867b9db854ec5483`
- SingleDeviceCard4x2/index.js: `69bdfacad2cd7ad7a65da678b9435eaa54fa7011debb7381164e1d699cfb86e2`

Conclusion: changing only style=8, only the controller registry, or only one allow-list cannot make PsAssistant's custom shortcuts appear in the stock QuickApp card.

## 4. What the bundled CAR component actually proves

The 4x2 RPK includes:

- DeviceType `CAR`
- car component and default car artwork
- four semantic icon names:
  - `kongtiao`
  - `chesuo`
  - `shanguang`
  - `mingdi`

The component renders every surviving shortcut and sends:

```
clickEvent(deviceInfo.id, item.id, Number(!item.state))
```

However these four IDs are not present in the same RPK's `eventSummary`, and they are not present in the converter's `card_short_cut.json`.

Therefore their presence is evidence that the OEM package contains a CAR visual component, not evidence that MyDevices 17.4.15 exposes an unrestricted third-party vehicle-control contract.

Do not map these IDs to PsAssistant commands until the state/value semantics and upstream source are independently verified.

## 5. Controller registry findings

The exact registry uses `Map<packageName,List<deviceType>>` with exact and wildcard matching.

Observed built-in controller package declarations include:
- com.heytap.health
- com.heytap.smarthome
- com.oplus.melody
- com.realme.linkcn
- com.oplus.linker / com.oplus.padconnect
- com.oplus.ebadge
- com.heytap.mydevices
- com.heytap.opluscarlink
- com.oplus.ipemanager

The built-in `com.heytap.opluscarlink` three-argument click callback in this build is a no-op. A generic `com.heytap.mydevices` wildcard controller also has a no-op click callback.

This is additional evidence that CarLink/Car+ should not be treated as the primary Polestar cloud-control implementation.

## 6. Why beta9 native home remains the preferred MyDevices route

`NativeHomeCardHook` is exact-build scoped to MyDevices 17.4.15 and only replaces the content of the PsAssistant CAR row.

It bypasses both QuickApp shortcut allow-lists while keeping:
- MyDevices card discovery and row lifecycle
- CAR identity
- PsAssistant credential boundary
- existing VehicleCoordinator command/readback flow
- fail-closed behavior for other devices and unknown host versions

The beta10/bridge 0.9.0 work adds a read-only runtime probe for the QuickApp double-gate. It deliberately does not widen OEM allow-lists.

## 7. AssistantScreen car-setting result

The static AssistantScreen 17.14.60 class:
`com.oplus.assistantscreen.card.car.setting.CarSettingActivity`
opens a preference panel whose keys include:

- `car_setting_jump_manage_bluetooth`
- `car_setting_ignore_switch`

This is a vehicle/Bluetooth scene setting surface, not proof of the official Shelf "My Vehicle" remote-control binding flow.

The APK also contains generic map route vehicle models; those remain excluded from vehicle-control evidence.

## 8. New hypothesis for Shelf "My Vehicle"

The target package inventory shows Pantanal 17.22.6 exposes service/card infrastructure including:

- `com.oplus.pantanal.ums.card.support.shelf`
- `ServiceStoreProvider`
- `CardServiceClientProvider`
- `CardServiceServerProvider`
- `AssistantProvider`
- UPK / card file providers
- decision and configuration providers

AssistantScreen itself contains broad dynamic card/store integration but no statically confirmed OEM-brand remote-vehicle implementation in the inspected car-setting package.

Working hypothesis: the user-visible Shelf "My Vehicle" remote-control service may be delivered/configured through Pantanal/service-store/UPK or another dynamically registered provider rather than being fully implemented in AssistantScreen.apk.

This is not yet runtime proof. The next capture should trace Shelf add/bind/control with Pantanal provider calls and service IDs instead of searching map VehicleInfo classes.

## 9. Next implementation gates

P0 completed statically:
- CardsUtils provider transport
- QuickApp click callback contract
- controller registry key semantics
- converter shortcut whitelist
- RPK shortcut whitelist
- CAR component click shape

P0 runtime:
- beta10 exact-build native-home rendering
- one explicit lock request
- actual vehicle readback
- QuickApp probe report confirming the expected filter on device

P1 Shelf:
- capture Shelf "My Vehicle" add/bind flow
- correlate AssistantScreen with Pantanal ShelfProvider / ServiceStoreProvider / card service calls
- identify service ID / provider identity / authorization owner
- only then decide between a normal provider, a Pantanal adapter, or a narrow LSPosed registration shim

P2:
- Xiaobu semantic command handoff
- Watch X2 normal + endurance-mode event routing

Digital-key provisioning stays outside this route.
