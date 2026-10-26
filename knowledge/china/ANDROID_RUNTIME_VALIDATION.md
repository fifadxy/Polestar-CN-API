# Android emulator runtime validation

**Updated:** 2026-10-26. **Target APK:** official Polestar China `4.25.0-262`. **Host:** x86_64 Linux without `/dev/kvm`.

This record separates emulator facts from application-runtime claims. It contains no account, token, phone, VIN, location or request payload.

## Verified environment facts

The Android Emulator cannot start the installed ARM64 AVD on this x86_64 host. The usable path is an Android 11 / API 30 Google APIs x86_64 `userdebug` AVD with Android Native Bridge:

```text
ro.product.cpu.abilist=x86_64,x86,arm64-v8a,armeabi-v7a,armeabi
ro.dalvik.vm.native.bridge=libndk_translation.so
ro.enable.native.bridge.exec=1
```

The 64-bit translator exists at `/system/lib64/libndk_translation.so`. `adb root` provides a `uid=0` shell. These facts establish an ARM64 translation path for host-driven smoke tests; they do not establish a true ARM64 CPU/kernel or Magisk-style app-facing root.

The API 35 Google APIs x86_64 image also exposed `arm64-v8a` and `libndk_translation.so`, but its `system_server` repeatedly restarted under unaccelerated TCG. API 30 is the current reproducible baseline.

## Official APK observations

The unchanged input APK has SHA-256:

```text
99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef
```

During a standard PackageManager attempt, Android selected:

```text
package=com.polestar.china.android
versionName=4.25.0
versionCode=262
primaryCpuAbi=arm64-v8a
secondaryCpuAbi=null
```

The staged `/data/app/.../base.apk` SHA-256 matched the input. This proves byte-for-byte staging and ARM64 ABI selection. It does **not** prove a completed install or a working application process.

## Measured failure boundary

On the no-KVM host, the default `speed-profile` install invoked `dex2oat32` for the 16-DEX APK. After 570 seconds ART had not finished; Android's Watchdog killed `system_server` while PackageManager was blocked in `installd.dexopt`. An interrupted transaction left an incomplete package record: `pm path` and ABI metadata existed, but the launcher component did not, and the explicit splash launch returned `Error type 3`.

The `verify` install filter also spent minutes verifying the unusually large DEX set and did not complete before another framework restart. Changing `ro.hw_timeout_multiplier` did not change Android 11 `Watchdog.DEFAULT_TIMEOUT`; it is not a valid solution for this failure.

Android 11 ART source confirms `extract` is a valid compiler filter with AOT, JNI compilation, quickening and verification disabled. A clean AVD was prepared with `pm.dexopt.install=extract`, but the final install was intentionally stopped pending user confirmation. The clean AVD contained no Polestar package afterward. This property is boot-scoped and must be set again after restarting the emulator.

Therefore the current status is:

| Claim | Status |
| --- | --- |
| Root ADB API 30 environment | VERIFIED |
| ARM64 ABI advertised through Native Bridge | VERIFIED |
| Original APK staged unchanged and classified `arm64-v8a` | VERIFIED_PARTIAL |
| Package installation completed | NOT YET VERIFIED |
| Polestar process entered | NOT YET VERIFIED |
| ARM64 JNI libraries loaded successfully | NOT YET VERIFIED |
| Reqable / Frida / PCAPdroid observed Polestar traffic | NOT YET VERIFIED |

OverlayFS system preloading was also attempted, but the AVD remained offline for more than 15 minutes after verity/remount changes. It is not the recommended path on this host.

## Root and tooling boundary

- `adb root` is sufficient to push and run an x86_64 Frida server from the host after application startup is proven.
- It is not Magisk's `su -c` contract. PCAPdroid's in-app root mode remains unverified.
- Reqable can be installed as an x86_64 VPN client, but no Polestar traffic result exists yet. Do not disable certificate pinning or record credentials to force coverage.
- No real account, SMS attempt, vehicle, token or private application data was used in this emulator work.

## Resume point

After explicit confirmation, resume from a clean API 30 AVD only:

1. wait for PackageManager, ActivityManager, private storage and boot animation completion;
2. set `pm.dexopt.install=extract`;
3. perform one normal installation of the original APK;
4. verify input/device APK hashes, version, ABI, launcher resolution, process entry and absence of `UnsatisfiedLinkError`;
5. only after startup succeeds, add Frida or Reqable smoke validation.

The operational scripts and commands are in [the emulator runbook](../../docs/ANDROID_ARM64_BRIDGE_EMULATOR.md).
