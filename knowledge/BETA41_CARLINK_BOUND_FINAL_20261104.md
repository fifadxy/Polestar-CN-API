# Beta41 CarLink Bound / PsAssistant Supported App — final verification

Date: 2026-11-04

## Source and CI

- Feature branch: `feature/oppo-beta41-carlink-bound-app-20261104`
- Tested source commit: `ff12c892c6438e8de8e533ee4109249244a24eb4`
- Successful GitHub Actions run: `41814324276`
- Result: compile, lint, app APK, bridge APK, app unit tests and bridge unit tests all passed.

## Versions

- PsAssistant: `0.9.7-beta41-carlink-bound` / versionCode `155`
- OPPO Bridge: `2.3.0` / versionCode `36`

## Local CarLink test behavior

When PsAssistant has a signed-in selected Polestar vehicle, fresh live state, system vehicle integration enabled and the virtual session is active:

1. CarLink/SceneService provider and vehicle catalogs can receive a POLESTAR / Polestar 2 row.
2. CarLink bind reads are projected as locally bound through manager/session getters, SceneService account-bind list and binding-shaped JSON keys.
3. PsAssistant is accepted by package/app support checks through package list injection, boolean whitelist/support checks, safe-IOT/CarLink permission compatibility, and host-scoped signature compatibility.
4. CarLink package variants supported by the LSPosed scope are:
   - `com.heytap.opluscarlink`
   - `com.oplus.carlink`
   - `com.oppo.carlink`
5. Remote vehicle commands continue through PsAssistant's authorization/gateway/readback path. The hook does not implement a second vehicle API.
6. Explicit requests for another vehicle/app are not redirected to PsAssistant.

This is a local LSPosed compatibility/test projection. It does not create an OPPO cloud company authorization, OCS production AUTH_CODE, digital-key enrollment, or official OEM certification.

## FULL APK

- File: `PsAssistant-v0.9.7-beta41-carlink-bound-Standalone-FULL.apk`
- Bytes: `9,591,276`
- SHA-256: `4b0202e320b7dc96b7bc6f98aaaf90758da066525dfba1a3093bcb4f90d6091e`
- APK v2: verified
- APK v3: verified
- Standalone-login client asset: approved bundled v2
- Client asset fingerprint: `59e1ca066e23a43e3976ded60c6691800dd19c4b173bd94b241bee42be5a7274`
- Signing certificate SHA-256: `d4cd1b06701f9a8829c5ecc5bbee1398a0f8a655703b6cc356398efa30218a7e`
- Integrated LSPosed metadata/scopes: verified
- CI compiled APK payload preserved byte-for-byte except APK signature records plus the approved standalone-login asset.

## Standalone Bridge

- File: `PsAssistant-OPPO-Bridge-v2.3.0-debug.apk`
- SHA-256: `09a471c4acf555fc5c6f4d2b284af27fcbcbdff03f46dabda3eeb53ed39c0db2`

Use either the integrated module inside the FULL PsAssistant APK or the standalone Bridge module, not both simultaneously.

## Device validation

The build environment cannot prove what the target ColorOS CarLink UI renders. Physical-device acceptance is:

- Polestar appears in CarLink as locally bound.
- PsAssistant appears/passes as a supported application where the host exposes the supported-app list.
- Lock and climate actions reach PsAssistant and are confirmed by vehicle-state readback.
- Other vehicle/app requests remain handled by the OEM host.
