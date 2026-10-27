# Digital Key Native V2 build report — 2026-10-27

## Result

Second isolated Android research application produced locally from:
- PsAssistant v0.8.3 Native V2 loader source branch
- the **user-provided** Polestar China 4.25.0 APK
- two OEM native libraries plus three PassiveEntry protobuf schemas

Final local artifact:
- filename: `PsAssistant_v0.8.3_DigitalKey_NativeV2.apk`
- size: **9,540,444 bytes**
- SHA-256: `afb569adab505e2fb1cc4f2be1cef083d8725a1df50b60a515bdfa719532f549`
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

## JNI bridge reconstruction

`libglble.so` JNI_OnLoad registers 13 native methods against `com/iwall/tech/security/GLAlgorithmJni`. The method names and signatures were independently recovered from the original APK's `classes12.dex` and native string/registration table. V2 adds a minimal declaration-only Java class containing those 13 native methods; it does not copy OEM implementation code and does not invoke the algorithms.

Final V2 DEX verification confirms the bridge has exactly 13 native methods with matching descriptors:
`aes_decrypt`, `aes_encrypt`, `aes_gcm_decrypt`, `aes_gcm_encrypt`, `crc16`, `crc32`, `crc8`, `getDeviceFingerprint`, `hmac_sha1`, `hmac_sha256`, `sha1`, `sha256`, `version`.

`libvocmo-lib.so` JNI_OnLoad disassembly only calls Djinni `jniInit(JavaVM*)` and returns JNI 1.6; it does not statically require a specific application package in JNI_OnLoad. Its VOCMO Java/Djinni API classes are still not bundled, so V2 only tests native loading, not VOCMO API invocation.

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
- Final injected APK has not yet been transported back into the GitHub-hosted AVD; actual JNI `LOADED/LOAD FAILED` status remains runtime-pending. Static JNI registration prerequisites for `libglble.so` are now present in the final APK, and the final bridge descriptors have been checked against the original DEX.

## Repository policy

OEM binaries are intentionally **not committed to GitHub**. Use `scripts/package_native_v2_from_original.sh` with the exact source APK to reconstruct the V2 package.
