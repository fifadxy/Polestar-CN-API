# Digital Key Native V2 — bundled OEM native-library experiment

Branch: `feature/android-digital-key-native-v2-20261027`

Purpose: package two native libraries extracted from the **user-provided Polestar China 4.25.0 APK** into an isolated PsAssistant research APK and test whether Android can load their JNI entry points under the independent package.

Bundled at final packaging time only:
- `lib/arm64-v8a/libvocmo-lib.so`
- `lib/arm64-v8a/libglble.so`

Explicitly excluded:
- `libAppSecret.so`
- `libwhite-box.so`
- mock keystores/passwords
- account tokens, VIN, certificates, private keys, cached digital keys
- CCC assets unrelated to Polestar 2 PAK research

This experiment performs only `System.loadLibrary` and class-presence checks. It does not call pairing, certificate enrolment, PassiveEntry, P319 vehicle commands, unlock or start.

The extracted proprietary binaries are **not committed to the GitHub repository**. The source branch builds a loader APK; the final V2 APK is assembled from the user's original APK bytes in the local research environment and separately signed as an isolated debug/research application.
