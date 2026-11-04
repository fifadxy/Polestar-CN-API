# Beta40 FULL build verification — 2026-11-04

Source branch: `feature/oppo-beta40-lsposed-car-control-20261104`  
Compiled source commit: `d227e6a119d219d52e91f0102e258597bcf1bcfe`  
GitHub Actions validation run: `41807629202`

## Deliverable

- Main APK: `PsAssistant-v0.9.7-beta40-lsposed-car-Standalone-FULL.apk`
- Bytes: `9,587,187`
- SHA-256: `742d3ddd377b6b13fcd72ec8a6e460262dad3f069570be0480a1c017e3556f0c`
- APK Signature Scheme v2: verified
- APK Signature Scheme v3: verified
- Signing certificate SHA-256: `d4cd1b06701f9a8829c5ecc5bbee1398a0f8a655703b6cc356398efa30218a7e`
- Certificate matches Beta39 FULL: yes
- Standalone-login asset: approved bundled v2, fingerprint verified
- Integrated LSPosed entry: present
- Beta40 ColorOS vehicle scopes: verified

## Build method

The application payload comes from the successful Beta40 CI build at commit `d227e6a...`.
The tested compiled APK payload is preserved byte-for-byte. The FULL packaging step adds only the previously approved standalone-login asset and regenerates APK signatures with the same PsAssistant test identity used by the Beta38/Beta39 FULL builds.

No OPPO cloud company identity, digital-key credential, vehicle token, account password, VIN, or third-party signing secret is committed to GitHub.

## LSPosed vehicle integration included

The integrated module contains the Beta40 local ColorOS vehicle routing for PsAssistant, including MyDevices, AssistantScreen, CarLink, SceneService, SmartEngine, CardService and optional Car+ host compatibility. Vehicle writes still route through PsAssistant authorization and vehicle-state readback.

## Validation boundary

Build-time verification passed. Real-account login and physical-vehicle command execution require installation by the account owner and are not claimed as build-environment tests.
