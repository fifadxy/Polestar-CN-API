# Network endpoints, transports and RPC inventory

This file is a **reference map**, not a list of working authenticated CN endpoints. Updated 2026-10-22. Upstream Hisingen tag v2.0.10; official CN Android 4.25.0-262.

## A. Global endpoints verified in upstream Hisingen

| Service | Address | Role | Authorization model |
| --- | --- | --- | --- |
| Polestar OIDC issuer | `https://polestarid.eu.polestar.com` | `/.well-known/openid-configuration`, authorization/token/userinfo/revocation | OIDC Global |
| Mystar v2 GraphQL | `https://pc-api.polestar.com/eu-north-1/mystar-v2/` | Vehicle discovery and telemetry | Global user session |
| Mystar public | `https://pc-api.polestar.com/eu-north-1/mystar-public/` | Public image metadata | Public image API mechanism |
| App backend / VDMS | `https://pc-api.polestar.com/eu-north-1/app-backend/api/graphql` | Vehicle enrichment | Command-client session |
| C3 discovery | `https://cnepmob.volvocars.com` | Discover dynamic gRPC host/port | Global user/vehicle authorization |
| PCCS Chronos | `https://api.pccs-prod.plstr.io:443` | Charge target, amp limit, timers and climate-related functions | Global scoped grant |
| EU Data Portal M2M | `https://pc-api.polestar.com/eu-north-1/data-portal/m2m/` | Official EU Data Act REST | M2M grant + `x-client-id` |

### Global vehicle discovery

```graphql
query GetConsumerCarsV2 {
  getConsumerCarsV2 {
    vin
    internalVehicleIdentifier
    modelName
    modelYear
    registrationNo
    pno34
    structureWeek
  }
}
```

The query is documented for API analysis only: do not log returned VIN/registration data.

### C3 discovery

Upstream Hisingen requests the discovery service with `Accept: application/volvo.cloud.cnepmob.v2+json` or the v1 variant and uses the server-provided `c3.grpcHost` and `c3.grpcPort`. The resulting gRPC endpoint is dynamic. Do not hardcode one inferred gRPC address as universally correct.

### EU Data Portal M2M

The M2M route has `POST /token` and `GET /v1/vehicles`; read-only telemetry families include battery, exterior, health, availability, odometer, location, parking-climatization and pre-cleaning. Charging resources include target-soc, amp-limit, charge-locations, global-charge-timer, parking-climate-timer and charge-now. Hisingen tracks an application-side allowance of 10,000 calls/day. **This provider is not a China authentication alternative.**

## B. China endpoints observed in official App / project

| Service | Host/address | Evidence scope | CN state |
| --- | --- | --- | --- |
| Official China web login | `https://www.polestar.cn/zh-cn/auth/login/` | Official website login page | VERIFIED website URL; vehicle token exchange UNKNOWN |
| C3 static/discovery | `https://cnepmob.volvocars.com.cn` | China app C3 static path | OBSERVED / API session MAPPING_REQUIRED |
| C3 token-related | `cepmobtoken.prod.c3.volvocars.com.cn` | Runtime host | OBSERVED_HOST_ONLY, exact RPC/credential UNKNOWN |
| C3 LBS-related | `cepmob.lbs.prod.c3.volvocars.com.cn` | Runtime host | OBSERVED_HOST_ONLY |
| C3 signature-related | `cepmobsig.prod.c3.volvocars.com.cn` | Runtime host | OBSERVED_HOST_ONLY |
| PCCS China | `https://api.pccs-prod.origin.polestar.cn` | China app/backend configuration | OBSERVED, method-level auth MAPPING_REQUIRED |
| App package | `com.polestar.china.android` | Version 4.25.0-262 | VERIFIED application identifier |

**Do not** substitute `.cn` for `.com` automatically. Global and China may use different issuer, audience, discovery, certificate and service capability requirements.

## C. Global C3 read RPC → China mapping

Paths below are source-backed Hisingen Global service paths. China columns mean mapping **not yet verified** unless otherwise stated.

| Global gRPC method | Data domain | China evidence | Status for direct China Mac API |
| --- | --- | --- | --- |
| `/services.vehiclestates.battery.BatteryService/GetLatestBattery` | Battery latest | C3BatteryController call chain observed | MAPPING_REQUIRED |
| `/services.vehiclestates.battery.BatteryService/GetBattery` | Battery current/live | China Android C3 live model observed | MAPPING_REQUIRED |
| `/services.vehiclestates.exterior.ExteriorService/GetLatestExterior` | Locks/doors/windows | C3ExteriorController observed | MAPPING_REQUIRED |
| `/services.vehiclestates.exterior.ExteriorService/GetExterior` | Exterior stream | China Stage12 C3 live observer >0 | MAPPING_REQUIRED |
| `/services.vehiclestates.availability.AvailabilityService/GetLatestAvailability` | Vehicle availability | Limited app capability evidence | MAPPING_REQUIRED |
| `/services.vehiclestates.health.HealthService/GetHealth` | Health | No end-to-end CN semantic mapping | MAPPING_REQUIRED |
| `/services.vehiclestates.odometer.OdometerService/GetOdometer` | Odometer | No end-to-end CN field mapping | MAPPING_REQUIRED |
| `/services.vehiclestates.dashboard.DashboardService/GetLatestDashboard` | Dashboard | No CN method test | MAPPING_REQUIRED |
| `/services.vehiclestates.parkingclimatization.ParkingClimatizationService/GetLatestParkingClimatization` | Climate state | CarInformation capabilities partially observed | MAPPING_REQUIRED |
| `/services.vehiclestates.precleaning.PreCleaningService/GetPreCleaning` | Precleaning | No CN method test | MAPPING_REQUIRED |
| `/services.vehiclestates.precleaning.PreCleaningService/GetLatestPreCleaning` | Precleaning latest | No CN method test | MAPPING_REQUIRED |
| `/services.vehiclestates.airquality.AirQualityService/GetLatestAirQuality` | Air quality | No CN method test | MAPPING_REQUIRED |
| `/services.vehiclestates.airquality.AirQualityService/GetAirQuality` | Air quality stream | No CN method test | MAPPING_REQUIRED |
| `/services.vehiclestates.location.LocationService/GetLatestLocation` | Location | LBS host observed, no field exchange documented | MAPPING_REQUIRED |
| `/services.vehiclestates.location.LocationService/GetLocation` | Location current | Not verified | MAPPING_REQUIRED |
| `/services.vehiclestates.weather.WeatherService/GetLatestWeather` | Weather | Not verified | MAPPING_REQUIRED |

## D. Global PCCS Chronos → China mapping

| Global gRPC method | Semantics | China status |
| --- | --- | --- |
| `/pccs.chronos.services.v1.TargetSocService/GetTargetSoc` | Read target SOC | MAPPING_REQUIRED |
| `/pccs.chronos.services.v1.AmpLimitService/GetAmpLimit` | Read current limit | MAPPING_REQUIRED |
| `/pccs.chronos.services.v2.GlobalChargeTimerService/GetGlobalChargeTimerStream` | Read charge schedule | MAPPING_REQUIRED |
| `/pccs.chronos.services.v1.ChargeLocationService/GetChargeLocations` | Charging locations | MAPPING_REQUIRED |
| `/pccs.chronos.services.v1.ParkingClimateTimerService/GetTimers` | Climate timers | MAPPING_REQUIRED |
| `/pccs.chronos.services.v1.LocationService/GetLocation` | Fallback location | MAPPING_REQUIRED |

China proto introspection has observed `com.volvocars.conncar.digitaltwin.chronos.proto.messages.targetsoc.v1.SetTargetSocRequest` with `batteryChargeTargetLevel_`, `chargeTargetLevelSettingType_`, and `request_` fields. This **does not** prove an authenticated China `SetTargetSoc` RPC is supported or permitted.

## E. Global controls inventory — China disabled

Upstream Hisingen implements remote commands using `/invocation.InvocationService`, including `ClimatizationStart`, `ClimatizationStop`, `Lock`, `Unlock`, `TailgateControl`, `WindowControl`, `HonkFlash` and `PreCleaning`. Global PCCS supports `TargetSocService/SetTargetSoc`, `AmpLimitService/SetAmpLimit`, `ChargeNowService/StartOverrideChargeTimer`, `StopOverrideChargeTimer`, `GlobalChargeTimerService/SetGlobalChargeTimer`, and parking-climate timer write/delete methods. Global OTA scheduler includes `Schedule`, `InstallNow`, `CancelSchedule`.

All of the above are **DISABLED for China** in the current Hisingen provider and command catalog. For each proposed China command, first verify the issuer/audience, gRPC or REST method, request/response proto, permissions, errors, and vehicle-side acknowledgement. The China app exposed some control methods by reflection; presence of a method is not evidence that the independent Mac client may invoke it.

## F. Research and safety rules

- Host reachability (`HTTP 200/404`) confirms only transport, not authorized state reads.
- No authentication bypass, no TLS interception/pinning disablement, no storage of live auth headers.
- For telemetry, store source timestamp, retrieval timestamp, units, source label, and cache age separately.
- For actions, never infer success from request acceptance; verify fresh state and obtain explicit user consent for every test.
- Exact real tokens, VINs, phone numbers, passwords, private keys and location must be removed from test fixtures and logs.
