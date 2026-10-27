# Digital Key V2.1 — authorized-flow compatibility harness

## Goal
Bring the research APK closer to the original Polestar China 4.25.0 flow without cloning an existing paired key or bypassing OEM/vehicle trust.

## Included runtime resources from the user-provided original APK
Build-time injection should include all non-secret protocol assets related to the observed flow:

- messages/passive/passive_key_request.proto
- messages/passive/passive_key_response.proto
- messages/passive/passive_key_status.proto
- messages/devicepairing/car_pairing_request.proto
- messages/devicepairing/car_pairing_response.proto
- messages/devicepairing/car_to_device.proto
- messages/devicepairing/device_pairing_request.proto
- messages/devicepairing/device_pairing_response.proto
- messages/devicepairing/device_to_car.proto
- entities/device_pairing_id.proto
- entities/caraccessstate/BasicVehicleData.proto
- entities/caraccessstate/CarAccessStateStatus.proto
- messages/caraccessstate/BasicVehicleDataUpdate.proto
- messages/caraccessstate/CarAccessStateUpdate.proto
- messages/caraccessstate/CarStandbyStateUpdate.proto
- messages/caraccessstate/DataSharingUpdate.proto
- messages/caraccessstate/GetBasicVehicleDataRequest.proto
- messages/caraccessstate/GetBasicVehicleDataResponse.proto
- messages/caraccessstate/GetCarAccessStateRequest.proto
- messages/caraccessstate/GetCarAccessStateResponse.proto
- messages/caraccessstate/GetDataSharingRequest.proto
- messages/caraccessstate/GetDataSharingResponse.proto
- messages/caraccessstate/WorkshopModeUpdate.proto
- paired_devices_request.proto
- paired_devices_response.proto
- lib/arm64-v8a/libvocmo-lib.so
- lib/arm64-v8a/libglble.so

These resources are injected only from the exact user-provided APK after SHA-256 verification and are not committed to GitHub.

## Explicitly excluded security-sensitive material
- existing paired digital-key data
- user/session tokens and passwords
- private keys / key caches / challenge-response payloads
- libAppSecret.so / libwhite-box.so
- mock PFX/keystore passwords
- production certificate caches copied from another app instance

The V2.1 goal is to create a NEW authorized device identity, not impersonate the official app or clone an existing key.

## Account integration
Use the existing China implementation:

ChinaPolestarApi.login(phone, password)
-> POST /user/api/v2/oauth/login/password
-> global access/refresh session
-> SecureSessionStore
-> vehicle discovery

The password is never stored by the digital-key module. The existing China session and VehicleCoordinator remain the account source.

Important: successful China account login does NOT imply Phone-as-Key entitlement. The original APK's PAK UserModel.identifier / OEM-scoped identity must be mapped through an authorized API or SDK before device enrolment.

## Original-flow stage model shown by V2.1

1. ACCOUNT_LOGIN
   - Existing phone + password login
   - Result source: ChinaPolestarApi
2. VEHICLE_DISCOVERY
   - Existing authorized vehicle list
   - Result source: ChinaPolestarApi / VehicleCoordinator
3. OEM_IDENTITY
   - Obtain the Phone-as-Key/OEM account identity from an authorized source
   - Must not guess from phone number, VIN or unrelated token
4. SDK_CONFIGURATION
   - Load packaged native SDK prerequisites
   - Environment + library result only
5. DEVICE_ENROLMENT
   - New app/device UCC/USEC-style identity through authorized OEM flow
6. VEHICLE_PAIRING_APPROVAL
   - User + vehicle normal pairing confirmation
7. ECDHE_CONFIRMATION
   - SDK-owned encrypted session; only stage/error is exposed to UI
8. CERTIFICATE_EXCHANGE
   - New Actor/Near-Field identity; private key material remains inside authorized SDK/store
9. CATALOG_ENROLMENT
10. PASSIVE_ENTRY_PROVISIONING
11. P319_PAIRING
12. VEHICLE_VERIFIED

Only VEHICLE_VERIFIED may be shown as a usable digital key.

## Implementation rule
The V2.1 APK may faithfully reproduce the UI/state/error ordering and load all required non-secret assets, but DEVICE_ENROLMENT and later stages must be backed by an authorized provider. A local fake/simulated success state cannot unlock subsequent real stages.

## Why this is closer to the original APK
The original APK evidence already indicates the same dependency chain:
account identity -> VOCMO/device enrolment -> vehicle-confirmed certificate exchange -> catalogue -> PassiveEntry -> P319 Phone-as-Key.

V2.1 now mirrors the whole sequence rather than stopping at BLE/native-library checks, while preserving the trust boundary that makes a new vehicle key legitimate.
