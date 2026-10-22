# Hisingen China read-only bridge

This bridge is the currently verified way for Hisingen China Preview to consume **live read-only data** from the already signed-in official China Polestar Android app.

## Current verified data

- SOC
- estimated range (km / mi)
- charging state when present
- average consumption when present
- source timestamp
- 5-second Hisingen live polling

The Hisingen provider contract also has fields for exterior and odometer, but their Android semantic mapping is still **MAPPING_REQUIRED** and must not be treated as live until verified.

## Requirements

- rooted Android device with the official China Polestar app installed and signed in
- package: `com.polestar.china.android`
- USB debugging enabled
- `adb` available on the Mac
- Frida Python module available, preferably in `~/Desktop/PolestarAnalysis/frida-venv`
- matching Frida server/client environment already configured on the rooted Android device

## Diagnose

```bash
chmod +x china_bridge_doctor.command
./china_bridge_doctor.command
```

The Doctor checks ADB, Frida, the official app package, app process status and the localhost bridge.

## Start

```bash
chmod +x start_china_bridge.command
./start_china_bridge.command
```

The launcher will:

1. find Python with the `frida` module;
2. verify ADB connectivity;
3. verify the official China Polestar package;
4. start the official app if it is not already running;
5. start the localhost bridge;
6. expose only read-only sanitized state at `127.0.0.1:28765`.

## Security boundary

The bridge does **not** export access tokens, refresh tokens, ID tokens, cookies, SMS/2FA codes, phone numbers, full VINs, precise location or Digital Key secrets/private keys.

No remote vehicle commands are sent.

## Endpoints

```text
GET /v1/status
GET /v1/state
```

Both bind to loopback only.

## Feature boundary

| Capability | Status |
| --- | --- |
| Original Global Hisingen | preserved |
| China vehicle session | usable through already signed-in official Android app bridge |
| China single-vehicle discovery | usable through opaque bridge identity |
| SOC / range | verified and usable |
| charging state / average consumption | usable when the app model reports them |
| live updates | usable through 5-second bridge polling |
| exterior | contract ready, Android semantic mapping pending |
| odometer | contract ready, Android semantic mapping pending |
| native macOS China OAuth/C3 auth | MAPPING_REQUIRED |
| PCCS authenticated method mapping | MAPPING_REQUIRED |
| remote commands | disabled/fail-closed |
