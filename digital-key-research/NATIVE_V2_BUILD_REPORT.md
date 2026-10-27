# Digital Key Native V2 build report — 2026-10-27

## Result

Second isolated Android research application produced locally from:
- PsAssistant v0.8.3 Native V2 loader source branch
- the **user-provided** Polestar China 4.25.0 APK
- two OEM native libraries plus three PassiveEntry protobuf schemas

Final local artifact:
- filename: `PsAssistant_v0.8.3_DigitalKey_NativeV2.apk`
- size: **9,536,290 bytes**
- SHA-256: `d06142b694166b2e2483e99414cc1c4f7e3f5a7dc8d9cff3b7f735a8fc082d82`
- applicationId: `io.github.polestarcn.companion.digitalkeyv2`
- signing: APK Signature Scheme **v2 + v3**
- research certificate SHA-256: `763e4c6241fc56f5df24ce1c258394f1031f3d98115942a774c1b57684638325`

## OEM bytes included

| Original APK entry | SHA-256 | Final APK |
| --- | --- | --- |
| `lib/arm64-v8a/libvocmo-lib.so` | `9bce3214a74a883fa9839eb00929f55046ad75dd54aaa387a3685d8ffc777c2f` | exact byte match after extraction |
| `lib/arm64-v8a/libglble.so` | `82f141506a913ddb870309e23d42641f8cbff9c1c559129ca96ef25a08556678` | exact byte match after extraction |
| `messages/passive/passive_key_request.proto` | `e36a1e0d2f8518e4f22af6153871c95a666268dd09874d5ead392ef20d398a5f` | packaged under `assets/oem-native-v2/passive/` |
| `messages/passive/passive_key_response.proto` | `baf86c0e2ee81d2feafd850a8fa14f36eac50eb5f20adf817f4a782f02f85d54` | packaged under `assets/oem-native-v2/passive/` |
| `messages/passive/passive_key_status.proto` | `43c2013c5d6f95bf772c15a51d498bbfac4fc8def510ab9e320d2ed8ffbc4d38` | packaged under `assets/oem-native-v2/passive/` |

Original APK SHA-256 gate:
`99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef`.

The two libraries are compressed in the final APK and the research manifest sets `android:extractNativeLibs="true"`, so Android may extract them into the app native-library directory before `System.loadLibrary()`.

## Explicit exclusions

Not copied:
- `libAppSecret.so`
- `libwhite-box.so`
- VOCMO mock PFX/password assets
- CCC CA certificate assets
- account tokens, VIN, certificates, private keys, existing phone-as-key cache
- original application DEX as a dynamic plugin

## V2 program behavior

`NativeLibraryProbe` checks whether both libraries exist in the app native directory and whether all three PassiveEntry schema assets are packaged. A user-visible button then performs only:
- `System.loadLibrary("vocmo-lib")`
- `System.loadLibrary("glble")`
- presence check for `com.volvocars.vocmo.djinni.CertificateApi$CppProxy`

It does **not** call native certificate generation, device enrolment, PassiveEntry, P319 pairing, unlock, start, or remote vehicle commands.

## CI / runtime status

- Native V2 loader CI run `40476317754`: loader and Android signing tools built successfully.
- Public Xiaomi endpoint `http://app.mi.com/download/1042285` returned a 33,861-byte wrapper response, not the user's 135,944,423-byte APK. Hash mismatch was correctly rejected.
- Therefore the final binary uses only the locally available user-provided APK bytes.
- Static package/signature/hash/resource verification: **PASS**.
- Final APK Signature Scheme v2/v3 verification: **PASS**; isolated RSA-3072 research certificate.
- Final injected APK has not yet been transported back into the GitHub-hosted AVD; actual JNI `LOADED/LOAD FAILED` status remains runtime-pending.

## Repository policy

OEM binaries are intentionally **not committed to GitHub**. Use `scripts/package_native_v2_from_original.sh` with the exact source APK to reconstruct the V2 package.
