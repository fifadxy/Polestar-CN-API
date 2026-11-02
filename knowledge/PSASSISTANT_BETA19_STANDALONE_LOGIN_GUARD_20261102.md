# PsAssistant beta19 standalone-login packaging guard — 2026-11-02

Beta18 is a linear descendant of beta15; no beta15 login code was lost. The beta15 and beta18 Git blobs for ClientSignatureProvider, ClientConfigFormat, SecureSessionStore and ChinaPolestarApi are identical.

The verified beta15 FULL and beta18 FULL packages use the same 48-byte assets/client-signing.bin and the same PsAssistant signing certificate. The public GitHub Actions #496 Debug package is different: PSA_CLIENT_CONFIG and persistent signing inputs were absent, so that Debug APK cannot perform clean-install China standalone login.

beta19 fixes the release-process ambiguity:
- private standalone assets are Release/Preview only;
- Debug is explicitly named VALIDATION and must contain no private login material;
- Release/Preview are checked for the approved 48-byte v2 client asset before being treated as FULL;
- CI can no longer silently present a non-login Debug artifact as a standalone deliverable.

If a verified beta18 FULL still fails on-device, capture the exact displayed error or HTTP status; the audit does not show a beta15-to-beta18 login-source regression.
