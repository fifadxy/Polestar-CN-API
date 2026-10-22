# Hisingen China Preview 0.18.0

## What changed from 0.17.0

0.17.0 proved the separate China provider could compile, but it did not fully satisfy the requested UX or runtime behavior. 0.18.0 fixes those gaps.

### Region selection

On the first real launch of 0.18.0:

```text
Select Polestar Region
[ China ] [ Global ]
```

The choice is explicit. A legacy 0.17.0 default write does not suppress the 0.18.0 chooser.

At runtime:

```text
Hisingen
  └─ Polestar Region
       ├─ China
       └─ Global
```

Settings also retains the Global / China region control.

Changing region now forces `VehicleSessionController` to tear down and rebuild its active provider. This fixes the 0.17.0 bug where changing the stored region could leave the old provider running until relaunch.

### Original Hisingen account UI integration

China is no longer only a parallel engineering card.

Inside the original `AccountCredentialsForm`:

- Global Polestar: original Polestar ID / Data Portal UI and behavior.
- China Polestar: China account surface with Official China Sign-In plus local read-only vehicle bridge status.
- Volvo: original UI and behavior unchanged.

### Original Global functionality

When Region = Global, Hisingen still uses the upstream v2.0.10 providers, session model, C3/PCCS mapping and remote controls. The China provider is not selected.

### China operational path

The first operational China vehicle-data path is:

```text
Official China Android app (already signed in)
        ↓
read-only Frida observation
        ↓
localhost 127.0.0.1:28765
        ↓
ChinaPolestarAPI
        ↓
Hisingen VehicleState
```

The bridge deliberately does not export:

- access or refresh tokens
- cookies
- SMS / 2FA codes
- phone number
- full VIN
- precise location
- Digital Key secrets

### Feature status

| Feature | 0.18.0 status |
| --- | --- |
| Global original Hisingen | Preserved |
| First-launch China / Global chooser | Implemented |
| Runtime menu China / Global switching | Implemented |
| Settings region switching | Implemented |
| China provider rebuild on region change | Implemented |
| China website sign-in | Implemented; website session only |
| China vehicle session | Operational through Android official-app bridge; native macOS China OAuth still MAPPING_REQUIRED |
| China vehicle discovery | Single bridge-backed opaque vehicle identity; native multi-vehicle discovery still MAPPING_REQUIRED |
| China SOC / range | Operational through verified ChargeStatusResponseModel mapping |
| China charge state / average consumption | Operational through the same live model where reported |
| China exterior | Provider/data contract prepared; semantic Android mapping still MAPPING_REQUIRED |
| China odometer | Provider/data contract prepared; semantic Android mapping still MAPPING_REQUIRED |
| China live stream | Hisingen VehicleLiveStreaming implemented as 5-second bridge live polling; direct C3 gRPC stream still MAPPING_REQUIRED |
| China PCCS | Hosts/protobuf evidence documented; method-level authenticated mapping still MAPPING_REQUIRED |
| China remote commands | Disabled/fail-closed |
| Global remote commands | Original upstream behavior retained |

## Why China remote commands remain disabled

Capability flags in CarInformation show that the vehicle/account may support locks, windows, trunk, climate and charging features, but a capability flag is not proof that Hisingen has the correct China endpoint, token audience, request proto and vehicle acknowledgement semantics.

Remote commands will only be enabled individually after all of these are verified.

## Verified battery mapping used by the bridge

China Android app 4.25.0-262:

```text
CarBatteryInteractorImpl.clicase -> ChargeStatusResponseModel (r9.clibyte)

cliif    -> batteryChargeLevelPercentage
clicatch -> estimatedDistanceToEmptyKm
cliclass -> estimatedDistanceToEmptyMi
cliint   -> averageEnergyConsumption
clido    -> updatedByCarAt
```

Charging state/type/power state are read from the model's semantic string representation, without reading authentication material.
