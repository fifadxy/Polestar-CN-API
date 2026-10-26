# Android Root validation tooling

**Research baseline:** 2026-10-25; official Polestar China `4.25.0-262`; Hisingen China `0.19.5`.

This runbook covers a device and account owned by the tester. Its purpose is to verify non-secret request structure and runtime sequencing, not to extract a session, bypass a challenge, or enable a remote vehicle command.

## Recommended stack

| Priority | Tool | Project use | Data boundary |
| --- | --- | --- | --- |
| P0 | Frida 17.24.3 | Existing OkHttp metadata hook correlates route templates, header names, body-field names and status codes | No values, bodies, cookies, tokens, phone, VIN or location |
| P0 | PCAPdroid 2.0.2 root mode | Confirms per-app DNS/host/connection activity even when a call is outside the hooked OkHttp path | Root capture, `app_filter`, no dump, no TLS decryption, no full payload |
| P0 | scrcpy 5.0.1 | Local-only visual timestamp for slider, SMS, linked vehicles and refresh | The recording may show private UI; never publish or attach it |
| P1 | Reqable 3.3.5 | Optional comparison when the unmodified app normally accepts the installed system CA | Stop if normal trust fails; do not unpin TLS or record credentials |
| P1 | Perfetto / simpleperf | Startup, JNI, CPU and reconnect diagnosis on the real ARM64 target | Performance trace only; review process/thread names before sharing |
| P1 | PID-filtered logcat | Crash and transport error diagnosis | Sanitize logs; do not publish raw logcat |
| P2 | JADX 1.5.6 / Apktool 3.0.3 / Ghidra 12.2 | Java, resources and ARM64 native-library cross-reference | Static distributed application material only |
| P2 | App Manager 4.1.1 | Package, split APK, component, permission and AppOps inspection | Do not export private app data |

Root-only PCAPdroid capture can coexist with a VPN-based tool such as Reqable. This is useful for flow correlation, but the safe default remains the metadata-only Frida probe plus PCAPdroid with no packet dump.

## Why a real ARM64 phone is still required

The distributed `4.25.0-262` application contains ARM64 native libraries but no x86/x86_64 equivalents. A `google_apis_x86_64` Android 11 AVD with `libndk_translation` can enter the official Java/UI process when process-start timeouts and unaccelerated dex verification are handled. It has reached `HomeActivity` and `PscnLoginActivity`, loaded all 16 DEX files and accepted a metadata-only Frida attachment. See [ANDROID_CLOUD_RUNTIME_EVIDENCE](ANDROID_CLOUD_RUNTIME_EVIDENCE.md).

That result does not validate the ARM64 native paths. In the cloud run, `libpag.so` failed the x86_64 ELF check and `libvocmo-lib.so` was not loaded. Pure QEMU TCG also required test-only isolation of JPush components. The emulator is useful for Java/Kotlin, Activity/View, ClassLoader and OkHttp metadata work; real ARM64 hardware remains required for native-library behavior and account/vehicle end-to-end acceptance.

The target device must meet all of these conditions:

1. `ro.product.cpu.abi` is `arm64-v8a`.
2. `adb shell su -c id` returns root after an explicit device-side grant.
3. The unmodified official app can sign in and show the owner's vehicle before instrumentation starts.
4. PCAPdroid is installed from its official release channel.
5. The tester performs only one normal login/refresh sequence to avoid repeated SMS or challenge requests.

Do not hide root, bypass root detection, disable certificate pinning, re-sign the application, export its private storage, or dump process memory. If the unmodified app refuses the rooted device, use a suitable owned test device rather than changing the app's integrity behavior.

## Reproducible session

On the Mac, install ADB, Frida tools and optionally scrcpy. `ANDROID_SERIAL` can select the intended phone when more than one device is attached.

```bash
cd Tools/auth_probe
chmod +x *.sh
./root_device_preflight_mac.sh
./collect_device_info.sh
./setup_root_frida_mac.sh
./capture_root_metadata_mac.sh
```

`capture_root_metadata_mac.sh` requests PCAPdroid root capture with:

- package filter `com.polestar.china.android`;
- network interface `@inet`;
- `pcap_dump_mode=none`;
- TLS decryption disabled;
- full payload disabled.

PCAPdroid deliberately requests device-side consent. The helper does not pass or store a PCAPdroid API key. After consent, reopen the official app and perform one normal slider → SMS → login → vehicle-list → status-refresh sequence. Ctrl+C stops PCAPdroid through the cleanup trap.

If a local visual timeline is necessary, run scrcpy separately. Do not share the screen recording; report only manually written timestamps and redacted observations.

After the session:

```bash
./stop_pcapdroid_root_mac.sh
./stop_root_frida_mac.sh
```

Only `CN_AUTH_META` lines and the non-secret environment summary may be shared for diagnosis. Review even those files before transfer.

## Acceptance questions

The session should answer only the remaining `0.19.5` questions:

1. Does the official slider return the expected three field names before `/credcode/send`?
2. Which fixed public header names exist on send-code and phone-login requests?
3. Does a successful login proceed directly to public C3 discovery and then the dynamic vehicle-list host?
4. Does the vehicle list use the Global access-token path while discovery remains unauthenticated?
5. Which read-only C3/PCCS calls occur for SOC, range, exterior state and reconnect?

The result must not include an OTP, token, cookie, authorization value, phone number, full VIN, precise location, response body or Digital Key material.

## Official references

- Frida Android guide: <https://frida.re/docs/android/>
- PCAPdroid application API: <https://github.com/emanuele-f/PCAPdroid/blob/master/docs/app_api.md>
- scrcpy: <https://github.com/Genymobile/scrcpy>
- Perfetto: <https://perfetto.dev/docs/>
- simpleperf: <https://developer.android.com/ndk/guides/simpleperf>
- JADX: <https://github.com/skylot/jadx>
