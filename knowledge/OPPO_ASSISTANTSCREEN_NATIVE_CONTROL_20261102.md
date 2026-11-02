> 2026-11-02更新：本文是历史阶段记录。当前实现与纠错以 [beta25](BETA25_APPEARANCE_CONTROLS_OPPO_20261102.md) 为准，特别是QuickApp检测假阳性、无自有原生行的处理及1.5秒外观菜单。

# OPPO AssistantScreen native vehicle controls — beta18 / Bridge 1.5.0

Date: 2026-11-02

Exact target:
- AssistantScreen 17.14.60
- MyDevices 17.4.15
- Pantanal 17.22.6
- Android 16 / OnePlus PJZ110

## Why the 1.4.0 QuickApp experiment is not enough on this phone

Static inspection of the exact MyDevices 17.4.15 RPK confirms:
- the 4x2 registry contains DeviceType.CAR and CAR icon names
  `kongtiao / chesuo / shanguang / mingdi`
- the same RPK's `eventSummary` does not include those CAR IDs
- `smartHomeDefault` has LAMP / AIR_CONDITIONER / CAMERA only; it has no CAR entry

Therefore the adaptive 1.4.0 shim correctly evaluates the dormant OEM CAR QuickApp route as unsafe and falls back. We do not bypass the signed RPK or disable its integrity checks.

## Exact AssistantScreen native path

AssistantScreen 17.14.60 contains a separate native MyDevices model/render path:

```
com.oplus.assistantscreen.card.mydevices.data.a.c()
  -> ArrayList<DeviceInfo>
  -> j50/a RecyclerView adapter
```

Confirmed model APIs:
- `DeviceInfo.setMDeviceType(DeviceType)`
- `DeviceInfo.setMConnectState(ConnectState)`
- `DeviceInfo.setMTemplateType(TemplateType)`
- `DeviceInfo.setMTemplateList(ArrayList)`
- `DeviceInfo.setSwitchMenuList(List)`
- `SwitchMenu(String id, String name, String highlightName, int icon, int iconHighlight,
  boolean check, String deepLinks)`

Exact adapter mapping:
- CONTROL -> holder type 0
- SINGLE_BATTERY -> 1
- MULTI_BATTERY -> 2
- NOISE_CANCELING -> 3

For NOISE_CANCELING with a non-empty SwitchMenuList, holder `m50/g` renders battery/device content plus switch controls. The holder name is an OEM implementation detail; beta18 does not expose any noise-control wording to the user.

The switch click path `m50/e.onClick` reads `SwitchMenu.deepLinks`, appends `check=`, and starts ACTION_VIEW.

## beta18 implementation

`AssistantScreenNativeVehicleHook` is exact-version gated to AssistantScreen 17.14.60.

It modifies only rows whose:
- device ID begins with `psassistant_`, or
- authority is the PsAssistant/Bridge device provider.

For those rows only it:
- normalizes type to CAR
- marks the native view state CONNECTED
- selects the OEM switch-control holder
- inserts 4 native switch controls:
  - 车锁
  - 空调
  - 净化
  - 尾门
- keeps real check state from PsAssistant's cached vehicle snapshot

All other OEM rows and unknown host versions are left unchanged.

## Command boundary

Switch controls open package-specific deep links:
- standalone bridge: `psassistant-oppo-bridge://command?... `
- integrated PsAssistant: `psassistant-companion://command?... `

`VehicleShortcutActivity` never trusts the OEM `check` value to decide a command.
It reads the latest PsAssistant snapshot and resolves:
- lock-toggle -> lock/unlock
- climate-toggle -> climate-start/climate-stop
- air-toggle -> air-start/air-stop
- trunk-toggle -> trunk-open/trunk-close

Unlock and tailgate movement keep an explicit user confirmation dialog. Actual commands still pass through PsAssistant's existing session, busy-state and vehicle-readback flow.

## Standard OPPO CardWidget route

OPPO's public `cardwidgetsupport_android_demo` confirms the supported third-party card contract:
- `AppCardWidgetProvider`
- intent action `android.appcard.action.APPCARD_UPDATE`
- meta-data `android.card.provider`
- `CardWidgetAction.postUpdateCommand(...)`

The public dependencies `com.oplus.card.widget:cardwidget` and
`com.oplus.smartengine:smartenginehelper:1.0.23` are available through Maven Central.
This remains the preferred next POC for a normal, non-LSPosed ColorOS card provider. It is kept separate from beta18 so the proven standalone-login/main APK build is not destabilized.

## Acceptance

1. Install beta18 FULL and Bridge 1.5.0.
2. Enable LSPosed/Vector scope for PsAssistant, MyDevices and AssistantScreen.
3. Root-reload the OPPO host processes.
4. Open the negative-one screen.
5. The PsAssistant device row should render native switch controls if the native MyDevices card path is active.
6. Test lock first. The click must resolve from the current cached lock state.
7. Test climate and purifier.
8. Test tailgate in a safe environment.
9. If the QuickApp 4x2 route remains the active visible card, export the light research bundle; native-card subscription/service routing is then the remaining issue, not vehicle authentication.

No digital-key cryptography or secure key provisioning is modified by this work.
