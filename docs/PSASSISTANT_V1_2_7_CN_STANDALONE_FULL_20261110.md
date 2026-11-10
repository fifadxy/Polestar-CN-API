# PsAssistant 1.2.7 · China Standalone FULL (2026-11-10)

## Scope and source
- Mainline source commit: `5eac27d0b7e2549f0198dc56aae3c214de0463b5`
- Version: `1.2.7`, versionCode `172`, application ID `io.github.polestarcn.companion`
- Popup: restore nested scrolling but collapse extra checks on every opening; show status summary, report time, upward-facing car and four door/window callouts without requiring an initial swipe
- Vehicle diagram: enlarge art by approximately 20% while honoring side-card and front/rear label clearances
- Vehicle data: TA distance, mean speed and mean consumption always appear in the overview; detailed TA/TM/since-charge section is retained
- Energy: separate kWh KPI `TA能耗估算` and monetary `TA费用估算`; keep the previous TM-based estimate calculation
- Parking map and OPPO Bridge transport are unchanged

## CI verification
- [Android Companion APK — success](https://github.com/fifadxy/Polestar-CN-API/actions/runs/42780297885): 632 unit tests, 0 failures
- [Android 1.2.7 Stable Regression — success](https://github.com/fifadxy/Polestar-CN-API/actions/runs/42780298127): 644 unit tests, 0 failures/errors
- App source, static analysis, packaging and regression checks pass
- Robolectric image preview was inspected for vehicle orientation, callout spacing and collapsed details layout

## Offline standalone FULL verification
An offline-wrapped, original-signer FULL package was created privately from the CI validation APK and the historically approved standalone login resource. This is **not** the public Actions `VALIDATION-debug` artifact and does not depend on another installed APK at runtime.

- Output: `PsAssistant-v1.2.7-CN-Standalone-FULL.apk`
- Size: 9,740,063 bytes
- SHA-256: `9241736c324852a05d3c5218363cb01597d5c5807ff86258bb1fc7ad08d57167`
- Login compatibility asset: `assets/client-signing.bin` in approved 48-byte PSACFG2 format; private value not stored in repository
- Signer certificate SHA-256: `d4cd1b06701f9a8829c5ecc5bbee1398a0f8a655703b6cc356398efa30218a7e`, equal to previous FULL
- APK v2/v3 signatures verified; `android:debuggable=false`; no bytecode/resources changed from the tested validation APK
- Verified ZIP integrity and under-10-MB target

**Scope of verification:** Verified as a self-contained client-resource package with original signing identity; no real-account session, vehicle command or OPPO native CarLink invocation was tested on a physical device in this build environment.

**Security:** Never commit the FULL binary, private compatibility asset, original APK, keystore, passwords, tokens or VINs to a public repository. Keep GitHub artifacts designated `VALIDATION-debug` separate from signed private FULL packages.
