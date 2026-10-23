# Historical vehicle-state research and Android read-only bridge

**Last updated:** 2026-10-23. Verified device: official China Android app `4.25.0-262`, installed package `com.polestar.china.android`. Frida framework version `17.24.3`.

> This file preserves 0.18 research evidence and the retired Bridge design. Hisingen China 0.19.0 uses the independent native Mac session documented in [NATIVE_MAC_SESSION](NATIVE_MAC_SESSION.md); the Mac artifact does not package or use this bridge.

## 1. Observed battery flow on the official China App

```text
CarMainFragment
  -> CarMainViewModel.requestUpdate
  -> CarBatteryInteractorImpl.requestBatteryStatus
  -> hybrid client layer
  -> C3BatteryController
  -> Volvo Connected Car Digital Twin battery gRPC
  -> GetBatteryResponse
  -> ChargeStatusResponseModel (obfuscated r9.clibyte)
  -> UI
```

The same observation identified `C3BatteryController` as the primary active path and `VocmoBatteryController` as the secondary side of the hybrid. Stage12 passive checks found C3 Exterior/Climate/Battery/ChargeNow controllers present and their PCCS counterparts absent in that observed runtime window. This does **not** mean PCCS is universally unused, nor does it prove that VOCMO can serve as a general fallback for every feature.

## 2. Observed `ChargeStatusResponseModel` mapping

| Observed obfuscated field | Semantic model name | Type/units | Evidence |
| --- | --- | --- | --- |
| `cliif` | `batteryChargeLevelPercentage` | Double, percent | Stage3 Android model inspection |
| `clicatch` | `estimatedDistanceToEmptyKm` | int, km | Stage3 |
| `cliclass` | `estimatedDistanceToEmptyMi` | int, miles | Stage3 |
| `cliint` | `averageEnergyConsumption` | Double, app-defined consumption units | Stage3 |
| `clido` | `updatedByCarAt` | epoch milliseconds | Stage3 |
| `clifor` | `estimatedChargingTimeInMinutes` | int, minutes | Stage3 reflection |
| semantic model string | `batteryChargingState` | e.g. Unplugged | Stage3 |
| semantic model string | `batteryChargingType` | e.g. Unknown | Stage3 |
| semantic model string | `chargerPowerStatus` | e.g. NoPower | Stage3 |

The last three were observed in the model's semantic `toString()` output; the bridge parses only those named battery-state values. Obfuscation names may change with China Android app releases. Every upgrade must revalidate this map before assigning VERIFIED.

### Historical read-vs-cache divergence

One real investigation observed a **current Android interactor model** showing battery ~72% and range ~300 km while a local Room persistence record contained ~44% / ~180 km. Therefore cached DB state is never assumed to be live.

**Important additional caveat:** Reading a model currently stored in app memory also does not, by itself, prove it is fresh. Use `updatedByCarAt` and mark older samples `Cached`. `fetchedAt` is the bridge retrieval time; `vehicleReportedAt` is vehicle/provider time. Do not substitute one for the other.

## 3. China exterior / odometer / other states

- `com.polestar.remotecarservice.remotefunction.exterior.c3.C3ExteriorController` observed at runtime.
- A passive Stage12 check observed a China C3 exterior live instance and no PCCS counterpart in that same sample.
- The China exterior protobuf field-to-`ExteriorSnapshot` mapping is **MAPPING_REQUIRED**. A method or class name alone does not prove door or lock current state.
- China odometer source and normalizing `odometerKm` require verified read response. No actual odometer sample has been verified for the bridge yet.
- Health, availability, climate state and charging preferences also require read-only end-to-end data mapping before being marked available.
- Vehicle `supports...` flags are **capabilities**, not measured states.

## 4. China parking climate limitations

Observed CarInformation:

```text
parkingClimatizationMaxRuntime = 30
supportsParkingClimatizationDirectStartSettings = false
```

Introspected `ClimatizationStartRequest` has invocation/start and some temperature/seat/steering-wheel fields. No user-controllable arbitrary duration field was verified. Do not create a 5–30-minute custom duration selector. Native climate on/off and confirmed state/remaining-time read would be eligible only after China-specific authorization and RPC verification.

## 5. Mac local read-only bridge architecture

Historical 0.18 experimental read-only path:

```text
Signed-in official Android app on your rooted phone
  -> Frida read-only `CarBatteryInteractorImpl.clicase` observation
  -> local Python HTTP server, 127.0.0.1:28765
  -> Hisingen `ChinaPolestarAPI` actor
  -> unified `VehicleState` (with provenance and freshness)
```

The bridge is optional, not a direct Mac China vehicle API session. It does not copy user bearer grants or Digital Key material. It should run only on your Mac and bind only to loopback. It requires your already-running official app and an attached USB/ADB/Frida environment. The app must be running and signed in under its own normal authentication; no bypass occurs.

### Local API contract (read only)

- `GET http://127.0.0.1:28765/v1/status`: protocol version, readiness, per-domain availability.
- `GET http://127.0.0.1:28765/v1/state`: sanitized single-vehicle object, battery, optional exterior/odometer, and provenance.
- No POST/PUT/DELETE vehicle operations. No unlock/lock/start-climate control surface.
- `--mock` fixtures are for CI, **not** proof of real phone connectivity.

The bridge uses an **opaque placeholder identifier**, not the full VIN. It is currently one-vehicle only; multi-vehicle discovery is MAPPING_REQUIRED.

### Sanitized schema

```json
{
  "version": 1,
  "session": { "ready": true, "source": "official-android-app" },
  "vehicle": {
    "id": "china-primary",
    "title": "Polestar China",
    "modelName": "Polestar 2",
    "modelYear": null
  },
  "battery": {
    "reportedAtMs": 1792472861622,
    "percentage": 72.0,
    "rangeKm": 300,
    "rangeMi": 180,
    "averageConsumption": 19.1,
    "chargingState": "Unplugged",
    "chargingPowerWatts": null
  },
  "exterior": null,
  "odometerKm": null,
  "source": "Cached"
}
```

This historical data illustrates structure only. `source` must reflect age at the time of reading; don't label historical samples Live.

### Bridge test statuses

| Test | What PASS proves | What it does NOT prove |
| --- | --- | --- |
| Python mock contract | JSON interface / fields parse | Real phone / C3 session |
| Frida attach and model read | Live process has a battery model object | The model was updated by car now |
| Timestamp age check | Sample age and cache labeling are sound | Authenticated Mac C3 grant |
| Hisingen macOS startup | Swift app starts without immediate crash | China account or original Global command execution |
| Original Global restored with separate account | Existing provider path still works if credentials valid | China remote control permissions |

## 6. Freshness policy to enforce

1. Parse `updatedByCarAt` as milliseconds, reject implausible future timestamps.
2. Compare `vehicleReportedAt` to **current wall clock**.
3. If missing/too old (initially > 10 minutes), label `Cached`; never show a green Live marker.
4. Do not assume HTTP response freshness means vehicle freshness.
5. Carry source and reported timestamp through to the Hisingen view; if data is unavailable, show `Not connected` rather than fictitious zeros.
6. On Frida detachment/permission or app logout, mark bridge unready and preserve only explicitly labeled cached observations.

## 7. Consent and privacy

Only observe telemetry in your own authorized account. Do not log or export the user's phone/plate/VIN/precise vehicle location, raw headers, cookies, access/refresh tokens or vehicle digital-key secrets. Do not disable TLS verification or bypass sign-in. All China control commands remain disabled until separately tested with explicit vehicle-side confirmation.

