# Polestar China API knowledge base

Updated: 2026-10-22. Scope: Polestar China Android 4.25.0-262, Hisingen v2.0.10, China Preview 0.18.0.

This directory is the **source-of-truth handoff** for subsequent AI/developers. It records observations and verification gaps; it is not a collection of working session secrets.

## First principles

- Global and China login, token audience, GraphQL, C3 and PCCS are **not interchangeable**.
- China official website login is a **website session**, not an established C3/PCCS session.
- EU Data Portal M2M credentials do not authorize China vehicle APIs.
- A vehicle capability flag does not prove the current state or a working command.
- Read-only Android bridge snapshots are not native macOS China C3 authentication.
- Remote command methods stay **disabled** until host, authorization, request and vehicle-state confirmation are independently verified.
- Do not store actual credentials, tokens, cookies, SMS codes, full VIN, private keys, phone numbers or vehicle GPS.

## Reading order

1. [Architecture and authentication](ARCHITECTURE_AUTH.md)
2. [Endpoints and method inventory](ENDPOINTS_RPC.md)
3. [Vehicle state, models and Android bridge](STATE_AND_BRIDGE.md)
4. [Global-to-China capability matrix](FEATURE_MATRIX.md)
5. [AI handoff and validation protocol](AI_HANDOFF.md)
6. [Maintenance log](CHANGELOG.md)

The pre-existing [comprehensive comparison](../../docs/HISINGEN_GLOBAL_CN_API_COMPARISON.md) remains additional reference.

**Evidence definitions:** VERIFIED (confirmed source or actual runtime), OBSERVED (host/key/class exists without proven end-to-end access), BRIDGE_ONLY (via observed Android app), MAPPING_REQUIRED (not usable directly), DISABLED (safety gate), and NOT_APPLICABLE (region-specific behavior cannot be transferred).

## Update contract

Whenever an API host, token type, RPC, data field, workflow, app version or verification status changes, update this directory **in the same commit** with evidence, date, test method and remaining limitations. Other AIs must never promote OBSERVED or MAPPING_REQUIRED to VERIFIED without a reproducible, redacted verification record.