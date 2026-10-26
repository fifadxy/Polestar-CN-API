# Android ARM64 Native Bridge emulator

**Baseline:** 2026-10-26; official Polestar China `4.25.0-262`; Hisingen China `0.19.5`.

This runbook provides a repeatable rooted emulator when a physical ARM64 phone is unavailable. It uses an Android 11 / API 30 Google APIs x86_64 guest and Android Native Bridge to translate ARM64 application code. The official APK remains unmodified and keeps its original signature.

## What this environment proves

| Capability | Result / boundary |
| --- | --- |
| Root shell for host-driven tests | `adb root` returns a `uid=0` shell on the userdebug image |
| ARM64 application ABI exposure | `ro.product.cpu.abilist` includes `arm64-v8a` |
| ARM64 translation | `ro.dalvik.vm.native.bridge=libndk_translation.so`; 64-bit translator is present |
| Official APK integrity | Use the original APK; do not unpack, patch or re-sign it |
| Intended validation | Package scan, process startup, JNI loading, crash/log diagnosis, Frida and non-account UI smoke tests |
| Not proved | ARM64 kernel/CPU behavior, hardware-backed attestation, production root behavior, real login, linked vehicles or telemetry |

This is an ARM64 **runtime compatibility environment**, not a true ARM64 virtual machine. Release acceptance still requires a tester-owned ARM64 phone and account.

Current measured status is narrower than the intended scope: root ADB and Native Bridge are verified, and PackageManager selected `primaryCpuAbi=arm64-v8a` for the unchanged APK. A completed install, process entry and JNI load are **not yet verified**. See [the knowledge-base evidence record](../knowledge/china/ANDROID_RUNTIME_VALIDATION.md).

## Create and start the AVD

The setup helper pins the image variant proven to contain Native Bridge:

```bash
export ANDROID_SDK_ROOT=/path/to/android-sdk
export ANDROID_AVD_HOME=/path/to/avd-home

Tools/auth_probe/setup_arm64_bridge_emulator_linux.sh
Tools/auth_probe/run_arm64_bridge_emulator_linux.sh
```

Defaults are API 30, AVD `polestar_armbridge_api30`, port `5558`, 3 GiB RAM, four virtual cores, a read-only system image and no snapshots. The runner defaults to `-accel off`, so it also works on a cloud host without `/dev/kvm`. A cold boot there can take several minutes. On a KVM-capable host, set `ANDROID_BRIDGE_ACCEL=auto`. Writable-system mode is opt-in with `ANDROID_BRIDGE_WRITABLE_SYSTEM=1` and is not recommended for this baseline.

In another terminal, select the emulator and verify the facts that matter:

```bash
export ANDROID_SERIAL=emulator-5558
Tools/auth_probe/emulator_native_bridge_preflight.sh
```

The preflight fails closed unless the device is an ADB-root target, advertises `arm64-v8a`, enables Native Bridge execution and has the named 64-bit translator library.

## Install the official application

On a KVM-capable host, use the normal package-manager path directly. On a TCG-only host, first wait for the full Android framework and select ART's `extract` install filter:

```bash
export ANDROID_SERIAL=emulator-5558
Tools/auth_probe/prepare_tcg_emulator_install.sh
```

The helper does not download or install anything. It waits for PackageManager, ActivityManager, private storage and boot animation completion, then sets `pm.dexopt.install=extract`. Android 11 ART defines `extract` as a valid filter with AOT compilation, JNI compilation, quickening and class verification disabled. This avoids a multi-minute install-time `dex2oat` run; ART verifies and compiles code later as needed.

Then use the normal package-manager path:

```bash
adb -s emulator-5558 install -r /path/to/polestar-cn-4.25.0.apk
```

The 130 MiB APK can otherwise make Package Installer exceed `system_server`'s watchdog deadline on TCG. In the measured default attempt, `dex2oat` had not finished after 570 seconds and Watchdog restarted `system_server` while PackageManager was blocked in `installd.dexopt`. That is a host-performance failure, not evidence of an ABI failure. The `extract` path was prepared on a clean AVD but its final installation result is deliberately pending user confirmation.

Do not use the writable-system OverlayFS fallback on this host. After verity/remount changes, the tested AVD stayed ADB-offline for more than 15 minutes. It also changes package classification to a system app and would not prove production install/update behavior.

After boot, verify the installed ABI and launch the explicit entry activity:

```bash
adb -s emulator-5558 shell dumpsys package com.polestar.china.android \
  | grep -E 'versionName=|primaryCpuAbi='
adb -s emulator-5558 shell am start -W \
  -n com.polestar.china.android/com.polestar.home.ui.SplashActivity
```

Treat `UnsatisfiedLinkError`, Native Bridge translation errors and process crashes as failures. Do not report success from `pm path`, ABI metadata or an APK copy alone; launcher resolution and process entry are separate acceptance gates. A login or vehicle result is outside this emulator's acceptance scope.

## Root-tool boundary

The userdebug image supplies host-controlled root through `adb root`. It does not supply Magisk's app-facing `su -c` contract. Frida server can therefore be started directly from the root ADB shell, while PCAPdroid's in-app root mode may not work until a separately reviewed Magisk/rootAVD layer is installed. Reqable's VPN capture and a system CA can be tested independently, subject to the application's normal trust behavior; do not add an SSL-unpinning module.

Stop the emulator after the session:

```bash
adb -s emulator-5558 emu kill
```
