# OPPO Bridge 1.11.0 / runtime findings and toggle safety (2026-11-02)

Source: the user's 15:26:46 +08:00 lightweight export, MyDevices 17.4.15, AssistantScreen 17.14.60, integrated Bridge 1.11.0. Do not include credentials, VIN, raw accounts, or full OEM APK/RPK.

## Confirmed observations

- MyDevices main and :cards reported `hooks-installed`; AssistantScreen reported `hooks-entered`. These are historical load observations, not proof the native UI and controls were applied.
- The 4x2 QuickApp received a PsAssistant-owned CAR row through `handleGetCardData`, yet reported `selectedHasData=false`, `selectedShortcutCount=0`, and empty old/replacement widget codes.
- CAR icon registry was present. The 4x2 RPK lacked eventSummary CAR entries and smartHomeDefault CAR; the OEM shim was intentionally not applied. The two-stage `card_short_cut.json` and `eventSummary` filters mean arbitrary ps_* controls are not available through the stock 4x2 route.
- SceneService observed local POLESTAR binding, but provider catalog was empty and Pantanal shelf row count was zero. Local account binding is not equivalent to OEM card registration, SDK authorization, or a real BLE/PAK key.
- CarLink host displayed module 1.10.0, versus 1.11.0 on other hosts. A stale diagnostic, cached process, or conflicting module needs verification on a fresh reboot.
- Vehicle `capturedAt` translated to 13:25:45+08; the MyDevices observation was 15:23:13+08, about 117.5 minutes later. A LIVE-labeled source is not proof of fresh telemetry.

## Patch

- Vehicle state comparisons use exact equality; `UNLOCKED` cannot accidentally match `LOCKED` and `INACTIVE` cannot match `ACTIVE`.
- State-dependent external shortcut toggles require a current real snapshot (within five minutes; tolerate 30 seconds of clock skew). If expired, trigger a refresh request instead of guessing a remote command.
- MyDevices native home controls are disabled if their own vehicle state is unknown or cached data has expired. The Refresh control remains enabled and dispatch performs a second freshness guard.
- Robolectric tests cover English antonyms, expired/future/missing timestamps, and stale-state native UI behavior.
- This patch does not bypass `com.oppo.permission.safe.IOT`, register fake Bluetooth keys, alter any signed OEM RPK, or mark OEM direct car controls as verified.

## Device verification

1. Enable only one of standalone and integrated LSPosed modules, restart affected hosts, and compare fresh per-process timestamps and module versions.
2. Check `assistant_native_vehicle applied=` and `native_home_card rendered=` while opening MyDevices; hook-loaded alone does not establish a visible card.
3. Inspect 4x2 `handleGetCardData` metadata and shortcut count separately from the native route.
4. Manually refresh the vehicle, verify `capturedAt` advances, then test one low-risk operation and confirm against a newer vehicle readback; an accepted command alone is insufficient.
5. Official CarLink permissions, public CardWidget publication, and Watch X2 long-battery-mode remain separate end-to-end acceptance tasks.

Release warning: Public CI debug APK lacks the private standalone login client asset. A complete upgrade requires the approved private client configuration and the original persistent signing identity.