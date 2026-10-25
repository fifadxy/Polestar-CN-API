# Native macOS China session

Updated: 2026-10-23. Baseline: official China Android app `4.25.0-262`, Hisingen `v2.0.10`, Hisingen China `0.19.0`.

This document records the source-backed native Mac implementation. It supersedes the Android bridge as the current Mac architecture, but it does not turn source inspection or CI compilation into a claim of successful real-account authentication.

## Runtime boundary

China and Global are separate providers:

| Concern | China | Global |
| --- | --- | --- |
| Account UI | Mainland phone + SMS | Original Hisingen Polestar ID / Data Portal |
| Persistent credentials | Keychain service `io.kheirallah.hisingen.polestar-cn`, account `native-session-v1` | Original Hisingen Keychain items |
| Selected VIN | China-specific preference, restored on region switch | Original Global VIN, restored on region switch |
| C3 discovery | `https://cnepmob.volvocars.com.cn` | `https://cnepmob.volvocars.com` |
| PCCS | `https://api.pccs-prod.origin.polestar.cn` | `https://api.pccs-prod.plstr.io:443` |
| Remote commands | Disabled | Original Hisingen behavior |

The Mac artifact does not require an Android device, ADB, Frida, a localhost listener or a companion process.

## Authentication sequence

1. Normalize the mainland mobile number to 11 digits.
2. Request a code with `POST https://gw.app.polestar-auto.com/user/api/v1/credcode/send`.
3. Exchange phone + code with `POST https://gw.app.polestar-auto.com/user/api/v2/oauth/login/phone`.
4. Parse the local account token and the nested `globleToken` token family.
5. Persist the session only in the dedicated macOS Keychain item.
6. Discover linked vehicles with `GET https://cepmobtoken.prod.c3.volvocars.com.cn/car-information/car`.
7. Supply the global token to China C3 discovery and the read-only telemetry client.

Observed request constants:

| Field | Value |
| --- | --- |
| `clientId` | `polestar-app-android` |
| SMS `type` | `SIGNIN_BY_PHONE` |
| SMS `aliScene` | `ic_login_h5` |
| Login `autocrt` | `true` |
| Protocol `Version` | `4.25.0` |
| Protocol `Device` | `Android` |

`Device: Android` and the client ID describe the public mobile protocol observed in the official client; they do not make this a Bridge architecture. HTTP, token lifecycle, persistence, vehicle discovery and telemetry all run in the Mac process.

## Token lifecycle

- Local refresh: `POST https://uc.app.polestar-auto.com/oauth/token` with the observed mobile-client query parameters.
- Global refresh: `POST https://gw.app.polestar-auto.com/user/api/v2/oauth/refresh-global-token`.
- A five-minute margin refreshes before expiry.
- JWT `exp` is used when available; otherwise the server `expiresIn` value is used.
- One actor-owned refresh task prevents concurrent refresh fan-out.
- Tokens, SMS codes, authorization headers, phone numbers and full VINs are never written to diagnostics.

The mobile-client secret is a protocol constant embedded in the distributed official application, not an end-user secret. Hisingen does not store it in Keychain or log it.

## Vehicle reads

The China provider reuses Hisingen's typed C3/PCCS parsers with injected China roots. Battery is the required core read. Optional feature reads fail independently and are marked unavailable rather than discarding a usable battery snapshot.

The implementation wires:

- vehicle discovery and metadata;
- battery, range and charging state;
- availability;
- exterior state;
- odometer, trips, health and maintenance;
- software and OTA capability reads;
- target SOC, amp limit, charging locations and schedules;
- climate state and timers;
- connectivity, air quality, location and weather;
- battery/exterior live streams.

Compatibility of a named China service is still an end-to-end question. A compiled request path is `IMPLEMENTED_UNVERIFIED` until a consenting real account and vehicle confirm authorization, response shape, units and timestamps.

## Failure policy

- Missing or expired credentials produce an authentication-required state.
- A failed global-token request performs one refresh and retry.
- HTTP and response sizes are bounded; response bodies and tokens are not logged.
- Alibaba challenge code `2002016` is surfaced as a blocking account-protection challenge.
- Every China remote command returns `RemoteCommandError.unsupported`, while the command catalog hides the controls.

## Evidence needed before promotion to verified

Record a redacted run showing:

1. SMS request and login outcome;
2. local and global refresh after expiry;
3. linked-vehicle count and stable VIN selection without recording the VIN;
4. same-minute SOC/range/odometer/exterior comparison with the official app;
5. live-stream reconnect and source timestamps;
6. sign-out deleting only the China Keychain item;
7. China ↔ Global switching without credential or cached-state crossover.

Remote commands require a separate, command-by-command consent and verification plan.

