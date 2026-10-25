# Architecture, login, and token families

**Baseline:** upstream Hisingen tag `v2.0.10`; official China Android app `com.polestar.china.android`, version `4.25.0-262`; native Mac preview `0.19.5`. Last reviewed 2026-10-25.

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
  ├─ gw.app.polestar-auto.com
  │   ├─ phone/SMS code
  │   ├─ local + globleToken login
  │   └─ global-token refresh
  ├─ uc.app.polestar-auto.com
  │   └─ local-token refresh
  ├─ cnepmob.volvocars.com.cn
  │   └─ public C3 discovery
  ├─ dynamic c3.url
  │   └─ linked vehicles with Global access
  └─ api.pccs-prod.origin.polestar.cn
      └─ PCCS reads
```

### Source-verified token families

The official client exposes a local account token family and a nested, misspelled `globleToken` family. It also contains these local preference-key labels:

```text
globle_access_token
globle_refresh_token
globle_id_token
key_token
key_refresh_token
```

The `globle` spelling belongs to the official App. Exact phone-login and refresh routes are SOURCE_VERIFIED; successful entitlement for an independent Mac remains IMPLEMENTED_UNVERIFIED. Observing a key never authorizes using a value with an unrelated host. These names are **not** real token values.

Hisingen 0.19.5 performs the phone/SMS exchange directly over TLS and stores the resulting China session in a dedicated macOS Keychain item. It does not extract cookies/tokens from a website or Android process. The optional DEX helper provisions only the OAuth client constant needed for future local-token refresh; it never imports a user session.

### China application session vs website session

| Concept | Established | Not yet established |
| --- | --- | --- |
| Official client source | Phone/SMS, refresh and vehicle-list protocol contracts | Independent Mac entitlement and live values |
| Signed-in Android app | Own app may read C3 state; runtime battery model verified | Native Mac authorization |
| Historical 0.18 bridge | Read-only Android-model transport | Not packaged or used in 0.19 |
| Native China Mac API | Dedicated session, Keychain, vehicle discovery and read-only C3/PCCS implementation | Redacted real-account/vehicle end-to-end proof |

Never re-use Global user/command grants or EU M2M credentials as China vehicle grants without explicit, reproducible verification.

## 3. Provider selection in Hisingen China

```text
Select Polestar Region
  China  -> original AccountCredentialsForm displays ChinaPolestarAccountView
         -> ChinaPolestarSession (dedicated Keychain)
         -> ChinaPolestarAPI (native HTTP + C3/PCCS)
         -> VehicleState with source timestamps/freshness
  Global -> original Polestar ID / Data Portal / Augmented selector
         -> Global original provider, C3, PCCS and original controls

Volvo -> upstream unchanged
```

The region chooser is displayed once per chooser version. The native application menu, menu-bar popover and Settings permit region switches. The active Polestar session coordinator stops and rebuilds against the correct provider; China and Global VIN preferences and Keychain sessions remain separate.

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
- Current China 0.19.5 implementation: `Patches/0.19.5`, `Scripts/apply_hisingen_china_0195.py`.
- Complete Android authentication evidence: `docs/POLESTAR_CHINA_ANDROID_AUTH_EVIDENCE.md`.
- China 0.18.0 Bridge implementation remains historical reference only.
- Source-verified contracts may still change in a later official application version; every upgrade needs revalidation.

