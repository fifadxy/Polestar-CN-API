# OPPO lightweight capture findings — 2026-11-01

Target capture:
- OnePlus PJZ110
- Android 16 / SDK 36
- MyDevices 17.4.15
- AssistantScreen 17.14.60
- Pantanal 17.22.6
- Quick App Engine 11.2.0
- PsAssistant 0.9.7-beta11

## Runtime findings

The uploaded lightweight bundle proves that the PsAssistant bridge itself is healthy:

- cardEnabled=true
- protocolVersion=1
- vehicle available=true
- SOC=64%
- range=270 km
- odometer=115442 km
- lock=已上锁
- doors=全部关闭
- windows=全部关闭
- tailgate=已关闭
- climate=开启
- air=关闭
- charging=空闲

However the active host-hook observations are stale:

- MyDevices main hook last seen: 2026-10-30 22:45:40 +08
- MyDevices cards hook last seen: 2026-10-30 22:45:39 +08
- reported module version: 0.5.0
- vehicle snapshot: 2026-11-01 18:56:53 +08
- current installed PsAssistant: 0.9.7-beta11

Therefore the current MyDevices / AssistantScreen processes had not reloaded the new Bridge generation when the capture was produced. This is sufficient to explain why the old generic vehicle row can remain visible while the new native direct-control view and new probes do not execute.

Bridge 1.1.0 adds:
- current-generation AssistantScreen hook report
- stale-hook/version mismatch warning
- optional root force-stop of MyDevices + AssistantScreen to reload LSPosed/Vector
- export of the active SceneService owner

## Exact MyDevices 17.4.15 QuickApp findings

The exported public static assets confirm two independent shortcut gates.

### Java-side whitelist

`assets/support/card_short_cut.json` contains the normal phone/PC/headset/smart-home/watch shortcut IDs but does not contain:

- kongtiao
- chesuo
- shanguang
- mingdi

### 2x2 RPK

`SingleDeviceCard2x2/index.js` defines:

```
carEventIconName = {
  kongtiao: 'kongtiao',
  chesuo: 'chesuo',
  shanguang: 'shanguang',
  mingdi: 'mingdi'
}
```

but its `eventSummary` whitelist still omits these four vehicle IDs.

This means the 2x2 RPK contains dormant vehicle icon support but filters vehicle shortcuts before rendering them.

### 4x2 RPK

The captured `SingleDeviceCard4x2/index.js` does not expose the same vehicle icon mapping and its `eventSummary` also does not allow those vehicle shortcut IDs.

Conclusion: merely changing PsAssistant shortcut IDs to the OEM vehicle names is insufficient on MyDevices 17.4.15. The current native HomeCardView replacement remains the preferred direct-control path.

## Exact AssistantScreen 17.14.60 findings

Target DEX strings confirm the following OEM contract:

SceneService package references:
```
com.coloros.sceneservice
```

Account binding provider authority:
```
content://com.oplus.sceneservice.AccountSDKInitProvider
```

Account query:
```
method_get_account_bind_list
accountBindCount
accountBindList
```

Binding actions:
```
oplus.intent.action.sceneservice.ACCOUNT_BIND
oplus.intent.action.sceneservice.REQUEST_SHOW_ACCOUNT_BINGDING_DIALOG
oplus.intent.action.account_bind_state_notify
```

Provider catalog database:
```
service_provider_list
brandCode
brandName
brandIcon
appType
appPkg
appInstall
spCode
version
```

Relevant exact classes / routes:
```
com.oplus.assistantscreen.card.car.SearchCarProvider
com.oplus.assistantscreen.card.car.setting.CarSettingActivity
com.oplus.advice.settings.ui.viewmodel.ProviderInfo
com.oplus.advice.frameworks.drivers.entity.ServiceProviderEntity
```

Therefore there is no contradiction between the actual package being `com.coloros.sceneservice` and the account provider authority retaining the older `com.oplus.sceneservice.*` namespace.

Bridge 1.1.0 now queries both package identities and the account-provider authority explicitly.

## Pantanal direction

The installed Pantanal package is 17.22.6. Public/system component evidence continues to show:

- ShelfProvider
- CardConfigurationProvider
- CardServiceClientProvider
- CardServiceServerProvider
- AssistantProvider
- AppCardEventProvider
- Seedling/Fluid Cloud support
- watch support metadata

This keeps the next deep-integration order as:

1. reload current AssistantScreen hook generation
2. obtain Shelf serviceId baseline/diff
3. obtain SceneService account/provider catalog
4. correlate SearchCarProvider/CarSettingActivity with serviceId + spCode
5. only then implement a Polestar provider-registration shim
6. connect Xiaobu / Watch through the same command bus

## Immediate acceptance test for Bridge 1.1.0

1. Enable the current module for PsAssistant, MyDevices and AssistantScreen.
2. Use the new root reload button or manually force-stop MyDevices and AssistantScreen.
3. Re-open Device Space and Shelf.
4. Diagnostics must show Bridge 1.1.0, not 0.5.0.
5. MyDevices generic row should be replaced by the native direct-control view when the exact HomeCardView hook matches.
6. Create a baseline, enter the Shelf car-binding flow, run diff.
7. Export another lightweight bundle.

Do not treat a stale host report as evidence that the newest hook failed.
