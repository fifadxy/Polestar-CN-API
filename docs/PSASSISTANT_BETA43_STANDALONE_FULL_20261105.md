# PsAssistant v1.0.0-beta43 Standalone FULL | 2026-11-05

**Delivery type:** Offline-packaged, independently login-capable Android APK based on the CI-validated Beta43 binary. It is *not* a native Gradle release variant. The source Android `debuggable` flag is disabled in the packaged manifest, and the original executable code is preserved.

## Verification

| Item | Result |
|---|---|
| File | `PsAssistant-v1.0.0-beta43-Standalone-FULL.apk` |
| App ID | `io.github.polestarcn.companion` |
| Version | `1.0.0-beta43` / `157` |
| Size | 9,619,819 bytes (below 10,000,000 byte limit) |
| SHA-256 | `065e7299eebaf6bb6fd94e2b86da982174d3854a358b0227ff051244ff63decb` |
| Source | Android CI run `41951830602`, commit `8d4033ab8aa560ec022c23ec2c860b298e5ca506` |
| Standalone China client resource | Bundled, validated against approved Beta40 FULL fingerprint |
| Java bytecode/resources | All original ZIP entry contents preserved |
| Manifest | Only `application android:debuggable` changes from true to false |
| Signing | APK v2 and v3 verified; same certificate as Beta40 Standalone FULL |
| Signing certificate SHA-256 | `d4cd1b06701f9a8829c5ecc5bbee1398a0f8a655703b6cc356398efa30218a7e` |
| Android and Bridge regression | CI suite passed (534 app tests, 12 Bridge tests) |
| Real China account login | Not run without the account owner and device |
| OPPO native SDK | Private SDK inputs unavailable in public CI; no real CarLink authorization claim |

## How to build again privately

Run `scripts/package_standalone_full.py` on a private workstation with the CI-validation APK, a **previous approved PsAssistant FULL APK**, the **matching original signing keystore**, and the official Android `apksigner.jar`. Export `PSA_SIGNING_STORE_PASSWORD` and optionally `PSA_SIGNING_KEY_PASSWORD` without putting passwords in commands, logs or GitHub.

Example (file paths are placeholders only):

```bash
python3 scripts/package_standalone_full.py \
  --validation-apk /private/beta43-VALIDATION-debug.apk \
  --previous-full /private/beta40-Standalone-FULL.apk \
  --keystore /private/PsAssistant-test-signing.jks \
  --apksigner-jar /private/apksigner.jar \
  --output /private/PsAssistant-v1.0.0-beta43-Standalone-FULL.apk
```

Validation rejects a missing/mismatching standalone resource, incompatible package/version, unaligned stored entries, changed DEX/resources, certificate drift, and APK size above the limit. Private client data and signing keys are **not** committed to GitHub. The app continues to require the user's legitimate account, password, and vehicle authorization.

## Important limits

1. The independently packaged China login resource is present and structurally validated. **A real account login was not attempted** and cannot be guaranteed by a static package check.
2. Signature identity matches the previously issued Beta40 FULL. An in-place upgrade is possible from that same certificate/app ID if versionCode and Android system requirements permit; back up settings first.
3. This package was produced from debug CI bytecode rather than the minified official release variant, even though Android's debuggable manifest flag is disabled. Use for authorized testing, not a hardened commercial distribution.
4. OPPO CarLink/LSPosed feature status remains dependent on device firmware, permitted SDK inputs, system authorization and real callbacks. A successful APK signature is not vehicle control confirmation.
5. The APK is provided directly in the user's private conversation. It is intentionally **not** published on the public GitHub repository because it contains standalone client compatibility material.
