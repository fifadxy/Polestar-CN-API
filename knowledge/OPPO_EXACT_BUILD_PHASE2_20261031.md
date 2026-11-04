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


### Exact ShelfProvider client contract in AssistantScreen 17.14.60

Static bytecode now identifies `com.pantanal.server.content.decision.cardsupport.shelf.b` as the OEM Shelf decision DAO.

It queries:

```
content://com.oplus.pantanal.ums.card.support.shelf/shelf_services
```

with null projection / selection / sort. The cursor parser requires exactly:

- `serviceId` as String
- `score` as float

and builds a `ServiceInfo`. The same code logs the resulting serviceId and score together with default/derived ServiceInfo fields.

Therefore beta10's Shelf probe now records a bounded `serviceId@score` index for all rows in addition to optional package/name-like columns. This is stronger and more OTA-tolerant than assuming package/component columns exist in ShelfProvider.

The next runtime comparison is: capture the service index before opening/adding "My Vehicle", then repeat during/after the flow. Candidate service IDs can then be resolved through the separate CardConfiguration/CardService clients instead of guessing from labels.


## 9. SceneService account-binding layer confirmed

AssistantScreen 17.14.60 does not implement every third-party account OAuth flow itself. The exact account-binding path delegates to SceneService AccountSDK.

Confirmed provider URI:

```
content://com.oplus.sceneservice.AccountSDKInitProvider
```

Confirmed read method and input:

```
method_get_account_bind_list
Bundle: useDB=true
```

Confirmed result keys:

- `accountBindCount`
- `accountBindList`

The same contract is used by AssistantScreen lifecycle/advice code. Existing checks for codes such as `HANGLV` and `ZSGT` prove the returned list contains short service-provider codes in at least non-vehicle domains. These travel codes are examples of the generic account framework and are **not** vehicle-provider evidence.

SceneService launch actions observed in the exact AssistantScreen build:

- `oplus.intent.action.sceneservice.REQUEST_SHOW_ACCOUNT_BINGDING_DIALOG`
- `oplus.intent.action.sceneservice.ACCOUNT_BIND`

The first action keeps the OEM spelling `BINGDING`.

### Provider catalog

The exact coroutine
`AdviceSettingCompatViewModel$queryProviderInfoListFromDB$1`
loads `ServiceProviderEntity` rows and converts them into `ProviderInfo`.

Confirmed mappings:

- `ServiceProviderEntity.getSpCode()` -> `ProviderInfo.spCode`
- `ServiceProviderEntity.getBrandName()` -> `ProviderInfo.name`
- `ServiceProviderEntity.getBrandIcon()` -> `ProviderInfo.iconUrl`

The resulting provider list is passed to SceneService with Intent extra `providerInfo`.

This gives a much stronger candidate architecture for OEM "My Vehicle" binding:

```
Pantanal serviceId / CloudCardConfig
        |
        +-- instantProviderId / instantProviderName / instantAccountBindUrl
        |
AssistantScreen provider catalog
        |
SceneService AccountSDK
        |
provider code + account bind state
```

This relationship still needs runtime correlation on the target phone. Do not inject a provider into SceneService's database until the exact ownership/write contract is proven.

### beta10 read-only probes

Bridge 0.9.0 now adds two exact-build AssistantScreen 17.14.60 probes:

1. `SceneAccountBindProbe`
   - calls the same read-only SceneService account-bind provider used by the OEM
   - retains only `accountBindCount` and short code-shaped provider tokens
   - does not persist account identifiers, credentials, URLs or arbitrary payload

2. `AccountProviderCatalogProbe`
   - observes the OEM provider catalog after the exact ViewModel coroutine fills field `o`
   - records only `spCode=name`
   - ignores brand icon URLs and all account data
   - exact-version gated and fail-closed

### New solution direction

If a vehicle provider appears as a generic `spCode` + Pantanal service mapping, the preferred future integration becomes:

```
PsAssistant vehicle backend
  -> narrow Polestar provider registration
  -> OEM provider catalog / account-bind presentation
  -> Pantanal/Shelf card service mapping
```

rather than impersonating an existing supported car brand.

The first acceptance gate is read-only correlation:

- record Shelf `serviceId@score`
- record provider catalog
- enter/add "My Vehicle"
- compare any provider/binding changes
- identify the exact service/provider pair

Only after that should a registration shim be designed.

## 10. Next implementation gates

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
- beta10 / Bridge 0.9.0 adds a read-only probe inside AssistantScreen 17.14.60
- query the OEM-authorized `content://com.oplus.pantanal.ums.card.support.shelf/shelf_services` path from the Shelf host identity
- retain only bounded service metadata: serviceId, package/component/name candidates, column names and row count
- do not read account state, tokens or card body data and do not create/update/delete services
- correlate the candidate service with CardConfiguration / CardService / authorization owner
- only then decide between a normal provider, a Pantanal adapter, or a narrow LSPosed registration shim

P2:
- Xiaobu semantic command handoff
- Watch X2 normal + endurance-mode event routing

Digital-key provisioning stays outside this route.
