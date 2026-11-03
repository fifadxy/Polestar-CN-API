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
