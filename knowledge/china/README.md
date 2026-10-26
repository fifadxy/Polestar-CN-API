# Polestar China API knowledge base

Updated: 2026-10-26. Scope: Polestar China Android 4.25.0-262, Hisingen v2.0.10, China native Mac Preview 0.19.5.

This directory is the **source-of-truth handoff** for subsequent AI/developers. It records observations and verification gaps; it is not a collection of working session secrets.

## First principles

- Global and China login, token audience, GraphQL, C3 and PCCS are **not interchangeable**.
- China official website login is a **website session**, not an established C3/PCCS session.
- EU Data Portal M2M credentials do not authorize China vehicle APIs.
- A vehicle capability flag does not prove the current state or a working command.
- The 0.18 read-only Android bridge is historical and is not packaged or used by the 0.19 Mac branch.
- A native path that compiles is still unverified until a consenting account and vehicle complete it end to end.
- Remote command methods stay **disabled** until host, authorization, request and vehicle-state confirmation are independently verified.
- Do not store actual credentials, tokens, cookies, SMS codes, full VIN, private keys, phone numbers or vehicle GPS.

## Reading order

1. [Architecture and authentication](ARCHITECTURE_AUTH.md)
2. [Endpoints and method inventory](ENDPOINTS_RPC.md)
3. [Native macOS China session](NATIVE_MAC_SESSION.md)
4. [Historical vehicle models and Android bridge](STATE_AND_BRIDGE.md)
5. [Android emulator runtime validation](ANDROID_RUNTIME_VALIDATION.md)
6. [Global-to-China capability matrix](FEATURE_MATRIX.md)
7. [AI handoff and validation protocol](AI_HANDOFF.md)
8. [Maintenance log](CHANGELOG.md)

The [16-DEX authentication evidence](../../docs/POLESTAR_CHINA_ANDROID_AUTH_EVIDENCE.md), [ARM64 Native Bridge emulator runbook](../../docs/ANDROID_ARM64_BRIDGE_EMULATOR.md), [branch inventory](../../docs/BRANCH_INVENTORY_AND_0195_PLAN.md), and [comprehensive comparison](../../docs/HISINGEN_GLOBAL_CN_API_COMPARISON.md) provide the implementation and history context.

**Evidence definitions:** VERIFIED (confirmed source or actual runtime), SOURCE_VERIFIED (exact official-client implementation observed), IMPLEMENTED_UNVERIFIED (native code exists but no real-account proof), OBSERVED (host/key/class exists without proven end-to-end access), BRIDGE_ONLY (historical 0.18 path), MAPPING_REQUIRED (not usable directly), DISABLED (safety gate), and NOT_APPLICABLE (region-specific behavior cannot be transferred).

## Update contract

Whenever an API host, token type, RPC, data field, workflow, app version or verification status changes, update this directory **in the same commit** with evidence, date, test method and remaining limitations. Other AIs must never promote OBSERVED or MAPPING_REQUIRED to VERIFIED without a reproducible, redacted verification record.
