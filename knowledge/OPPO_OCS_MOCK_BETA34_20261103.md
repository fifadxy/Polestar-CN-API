# PsAssistant beta34 Mock OCS TestLab

Date: 2026-11-03

## Purpose

beta34 adds an isolated, locally implemented OCS 1.4.0-compatible test transport for end-to-end system panel integration tests. It does not contain or redistribute OPPO proprietary AAR binaries and does not authenticate to OPPO production services.

## Build modes

- OFFICIAL: private original OCS AARs + PsAssistant-issued production/test credentials.
- MOCK_TESTLAB: project `:carlinkMockSdk`; in-process virtual OCS service.
- VALIDATION: public API contract stubs only; transport unavailable.

Select MOCK_TESTLAB with `PSA_OPPO_MOCK_SDK=true`.

## Mock identity

- companyId: `990001`
- AUTH_CODE: `TESTLAB_MOCK_AUTH_20261103`
- typeCode: `POLESTAR_2_TESTLAB`

These values are accepted only by `MockCarLinkServer.credentialsValid()` inside the TestLab APK. They have no OPPO production meaning.

## Simulated lifecycle

`CarLink.init -> onServiceConnected -> onCarControlSdkInitComplete -> CarControl.auth -> onAuthResult(1) -> inquireCarInfo -> sendCarInfo(List) -> CarControl.subscribe -> onSubscribeResult(1) -> inquireCarStatus -> sendCarStatus(List) -> hasCarBinding/checkCarBind -> onControl -> ControlTask -> CONTROL_EXECUTING -> local fixture update -> CONTROL_SUCCESS`

The synthetic vehicle is `Polestar 2 TestLab`. Control writes are dry-run and never call the real Polestar command backend in MOCK_TESTLAB mode.

## Supported simulated controls

LOCK-CAR, CAR-DOOR, AC-TEMP, CAR-BOOT, ALL-WINDOW, CAR-HORN, DOUBLE-FLASH, CAR-HORN&&DOUBLE-FLASH.

## Safety boundary

Mock mode never grants OEM permissions, edits OPPO databases, changes PMS authorization, generates OPPO-valid tokens, or claims official OPPO binding. Production OCS remains a separate build path.


## Final verified build

GitHub Actions run: `41608167739` — SUCCESS.

- PsAssistant: `0.9.7-beta34-mock-ocs / 148`
- Integrated OPPO Bridge: `1.19.0 / 31`
- Mock OCS AAR: `PsAssistant-OCS-Mock-1.4.0-TestLab.aar`

Verified deliverables:

- Standalone APK SHA256: `9f83cb6a6d335749d14604c62ec6e0cd79f635842057d257d15f87aed573f7b0`
- Mock OCS AAR SHA256: `770b017da3761b71539d66b8d3d21d1740eecff32fac2650fcee3133dd84a53f`
- OPPO Bridge APK SHA256: `7ae54a9cfa35a14d4677195baf52e37cf863104c236c19403d2e0a5e1419f24a`

The standalone APK keeps the existing PsAssistant test signing certificate and the same 48-byte standalone login asset used by beta33.
