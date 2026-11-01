# PsAssistant beta14 standalone China login repair — 2026-11-01

## Root cause

Public validation APKs deliberately omitted the private China account client compatibility asset. The runtime login code was present, but `ClientSignatureProvider` could not resolve the authorized client configuration, so those APKs were not capable of clean-install standalone China login.

This was a packaging/configuration defect, not a dependency on an installed third-party APK.

## Code fixes

- `ClientSignatureProvider` now exposes explicit readiness diagnostics.
- Login fails before transmitting credentials when the standalone client configuration is absent.
- Missing-config errors clearly distinguish public CI validation builds from complete standalone builds.
- HTTP login errors preserve a bounded, redacted server message instead of only `HTTP 4xx`.
- Regression coverage was updated for the explicit standalone-build behavior.
- Version advanced to `0.9.7-beta14-standalone`.

## Complete private packaging

The complete APK is produced without committing the private client configuration to the public repository.

Private packaging input was recovered from the user's own previously built complete PsAssistant APK, decoded using the maintained v2 asset format, and verified against the approved client-config fingerprint already pinned in source.

The compatibility value itself is never printed or committed.

Final private APK verification:

- artifact: `PsAssistant-v0.9.7-beta14-standalone-full.apk`
- size: 9,772,961 bytes
- SHA-256: `da7df849c887d0711d2139526945b16afbd4a0f876acc8c07310a4314ebd4459`
- `assets/client-signing.bin`: present exactly once, 48 bytes
- client configuration fingerprint: PASS
- asset checksum: PASS
- APK ZIP integrity: PASS
- APK Signature Scheme v2: PASS
- APK Signature Scheme v3: PASS
- integrated libxposed metadata: retained
- final package remains below the 10 MB project limit

## Runtime boundary

Static/build verification proves that the missing standalone client configuration is repaired. A real China-account clean-install login still requires testing with the user's own authorized account and network environment. No credential or token was available or stored during packaging.

The app remains single-APK at runtime: no reference APK import and no pre-existing PsAssistant session is required for the login path.
