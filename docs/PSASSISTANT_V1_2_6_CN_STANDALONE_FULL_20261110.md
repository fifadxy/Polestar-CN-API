# PsAssistant 1.2.6 China Standalone FULL — 2026-11-10

## Summary

Private offline FULL package generated from the CI-validated UI-fix source commit `78ac73825442abf11b4484c65ed1b9ebe8742041`.

The final APK includes the previously approved, bundled China account client compatibility asset from the Beta40 Standalone FULL. This is *not* a public GitHub Actions validation APK and does not depend on another installed app to read the client configuration.

## Package verification

- Application ID: `io.github.polestarcn.companion`
- Version: `1.2.6` / versionCode `171`
- Size: `9740060` bytes (under 10 MB)
- SHA-256: `a65c9d5bbd6cc98ccb2c22af61d3942b9bf89e7a8e3a8a9049fbe78606d937bb`
- China login asset: `assets/client-signing.bin`, 48-byte PSACFG2 format; approved client fingerprint verified (not recorded in this document as raw configuration)
- APK signatures: v2 and v3 verified
- Signer certificate SHA-256: `d4cd1b06701f9a8829c5ecc5bbee1398a0f8a655703b6cc356398efa30218a7e`, same as previously delivered FULL
- Android `debuggable=false`
- Compiled DEX, resources, integrated OPPO Bridge and LSPosed module metadata unchanged from the CI-validated 1.2.6 UI-fix APK
- Only AndroidManifest debug mode, bundled standalone asset, and APK signatures differ from the validation package

This deliverable is a signed *offline-wrapped* FULL based on the public CI bytecode, not a new fully private-SDK `assembleRelease` build. Real China account authentication, vehicle discovery and commands require on-device verification.

## Security and repository policy

Do not commit the FULL APK, client-signing asset, previous APK or keystore to the GitHub repository. No user passwords, vehicle identifiers, sessions or tokens were copied into the standalone package. Build inputs stay private. Public GitHub Actions validation packages remain separate and must not be represented as independently login-capable FULL releases.
