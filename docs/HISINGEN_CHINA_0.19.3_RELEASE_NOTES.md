# Hisingen China 0.19.3 — Native Mac Login Diagnostics

Based on upstream Hisingen v2.0.10 build 28. No Android/Python Bridge runtime.

## Important: China API login is NOT yet complete

0.19.2 can open the China official website but does not obtain a China C3/PCCS vehicle API grant from that browser session. Earlier direct phone/SMS calls returned the China gateway error "缺少公共参数". This release does not claim that a browser login grants access to vehicles.

## What's improved

- China Website Login uses its own WKWebView and remains isolated from original Global Polestar login.
- Mac Settings now reports non-sensitive WebKit stage, most recent allowlisted host and HTTP response status, with a Copy Redacted Diagnostics button.
- Reports never include the full URL/query string, OAuth code, phone, VIN, tokens, cookies or request headers.
- Current China token storage is still independently held in macOS Keychain. Previously stored sessions are not overwritten. Incomplete direct-SMS sign-in remains disabled.
- A metadata-only observation kit for a user-owned rooted Android phone is available under Tools/auth_probe.
- Native Mac China Remote Commands remain disabled; original Global implementation stays unchanged.

## Android kit

Review Tools/auth_probe/README.md and, if authorized, collect only the official app's endpoint templates and JSON/HTTP header FIELD NAMES with Tools/auth_probe/okhttp_metadata_only.js. The kit does not read response bodies, disable TLS pinning, bypass challenges or replay credentials.

## Gates to a real China login release

1. Document a China client authorization mechanism permitting a separate macOS client.
2. Validate callback and state/PKCE when applicable, or another legitimate supported account grant.
3. Obtain scoped China vehicle API credentials without extracting another application's tokens.
4. Verify actual account vehicle discovery; real C3 battery/range/odometer/exterior reads.
5. Test refresh expiry, app restart, China/Global switching, error handling, and Keychain isolation against a consenting account/device.

Until then, Website Login and Vehicle API Authorized remain distinct states.

## Build and install

GitHub Actions compiles native Swift, validates contract tests, and smoke-tests packaged app launch on macOS 15 and macOS 26. CI cannot establish real website account sign-in or vehicle API authorization. Ad-hoc signed preview only, not notarized.

Download build artifact Hisingen-China-0.19.3-Native-Mac from the completed workflow. Verify checksums and run install_hisingen_china_0193.command. Old China app is backed up before replacement.
