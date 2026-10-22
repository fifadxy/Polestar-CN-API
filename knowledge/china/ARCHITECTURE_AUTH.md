# Architecture, login, and token families

**Baseline:** upstream Hisingen tag `v2.0.10`; official China Android app `com.polestar.china.android`, version `4.25.0-262`. Last reviewed 2026-10-22.

## 1. Global / upstream Hisingen

```text
Global Polestar ID (OIDC, browser-assisted)
    ├─ user session access + refresh credentials
    │   ├─ Mystar v2 GraphQL: vehicle discovery/telematics
    │   ├─ C3 gRPC discovery and state reads
    │   └─ PCCS Chronos, where authorized
    └─ separate command-client credential family
        ├─ C3 Invocation remote operations
        └─ app-backend / VDMS account enrichment

Independent EU Data Portal provider:
    Account ID + Client ID + Client Secret
      -> short-lived M2M access grant
      -> Data Portal REST endpoints, request x-client-id
```

Global authentication authority: `https://polestarid.eu.polestar.com`, using the OIDC discovery document `/.well-known/openid-configuration`. Actual authorize/token/userinfo/revoke paths are obtained from discovery, **not guessed**. Hisingen protects persisted user and command session material in the macOS Keychain. Treat the web-user, command, and M2M audiences as three distinct grants.

The Global user path, EU Data Portal mode, and Augmented mode are original upstream behavior and remain available **when Region = Global**. Augmented combines the EU Data Portal telemetry path with consumer-only identity/commands; do not assume it works in China.

### Global authorization categories

| Credential family | Purpose | Storage/refresh | Confidence |
| --- | --- | --- | --- |
| OIDC user access and refresh | Mystar discovery / selected C3/PCCS reads | Keychain; OIDC refresh lifecycle | VERIFIED upstream |
| Independent command access and refresh | C3 Invocation and VDMS enrichment | Separate command-client authorization; Keychain | VERIFIED upstream |
| EU Data Portal M2M client credentials | Data Act M2M API, not a consumer mobile app login | Account ID + Client ID + Client Secret; reacquire access grant | VERIFIED upstream |
| Public image/API-key material | Image metadata where supported | Not vehicle-control authorization | VERIFIED upstream |

**Do not put credential values in documentation or diagnostic output.**

## 2. China official account

```text
Official China Polestar Account
  ├─ www.polestar.cn/zh-cn/auth/login/
  │   └─ Website session in browser/WKWebView
  │       └─ NOT an established macOS China vehicle API session
  └─ Official China Android app
      ├─ CN account/mobile sign-in (SMS/verification when required)
      ├─ app-private token/session families
      ├─ C3 Connected Car Digital Twin runtime
      ├─ PCCS backend
      └─ VOCMO secondary/fallback path
```

### Observed Android token **preference keys** (names only)

The following are exact local preference-key labels observed during source/runtime investigation:

```text
globle_access_token
globle_refresh_token
globle_id_token
key_token
key_refresh_token
```

The `globle` spelling belongs to the official App. Its exact OAuth audience, entitlement, issuer, refresh semantics, and relation to `key_*` remain **MAPPING_REQUIRED**. Observing a preference key does not establish that a particular value can be used with any host. These names are **not** real token values.

No session/cookie/token extraction is part of the Hisingen China web sign-in UI; credentials and any SMS verification remain entirely on the official site.

### China application session vs website session

| Concept | Established | Not yet established |
| --- | --- | --- |
| Official website sign-in | Official login web page opens with persistent WKWebsiteDataStore | Vehicle API entitlement or token exchange |
| Signed-in Android app | Own app may read C3 state; runtime battery model verified | Native Mac gRPC authorization |
| Mac local read-only bridge | Can observe permitted Android model state on the user's connected device | Direct China C3/PCCS login, multi-car discovery |
| Direct China Mac API | Proposed dedicated `ChinaPolestarAPI` | Official auth flow, token audience, refresh and C3 discovery negotiation |

Never re-use Global user/command grants or EU M2M credentials as China vehicle grants without explicit, reproducible verification.

## 3. Provider selection in Hisingen China

```text
Select Polestar Region
  China  -> original AccountCredentialsForm displays ChinaPolestarAccountView
         -> ChinaPolestarAPI (currently read-only local Android bridge)
         -> VehicleState with provider timestamp/freshness
  Global -> original Polestar ID / Data Portal / Augmented selector
         -> Global original provider, C3, PCCS and original controls

Volvo -> upstream unchanged
```

The 0.18.0 region chooser is displayed once for existing/upgrading installs using a chooser-version flag, even if 0.17 wrote Global as the implicit default. The native application menu and Settings permit region switches. The active Polestar session coordinator must stop and rebuild against the correct provider when region changes; displaying the region label alone is not sufficient.

**Security boundary:** China commands remain fail-closed in both provider execution and UI command catalog. Neither a command capability flag nor a successful HTTP HEAD probe enables write commands.

## 4. Native China implementation gates

To promote a specific function from MAPPING_REQUIRED to VERIFIED, document:

1. Official authentication mechanism and session scope using an approved flow;
2. Vehicle discovery and ownership/access authorization without publishing user identifiers;
3. Exact C3/PCCS host discovery, transport/gRPC path and applicable protobuf version;
4. Request and response field mapping, units, timestamps, failure codes;
5. Retriable refresh lifecycle, TLS verification and rate-limit handling;
6. For controls only: explicit user-initiated consent, success/failure and **fresh vehicle-side state confirmation**.

This repository must not circumvent authentication, disable TLS validation, or store Android digital-key material.

## 5. Source of truth

- Hisingen `Sources/Hisingen/Services/API/PolestarAPI+Authentication.swift`, `PolestarAPI.swift`, `PolestarGRPC.swift`, `PolestarDataPortalAPI.swift`, `PolestarGRPCRemote.swift`, `ProviderRegistry.swift`.
- China Android 4.25.0-262: passive Stage 3 battery object read and Stage 12 C3/PCCS/VOCMO introspection.
- China 0.18.0 implementation: `Patches/0.18.0/ChinaPolestarIntegration.swift`, `Tools/china_bridge`.
- All China host and token findings are limited to observed structure; official documentation may differ.

