# Polestar-CN-API

Research and interoperability tooling for China-region Polestar vehicle data and the **Hisingen China** macOS preview.

> **Status (2026-10-22):** Hisingen China Preview 0.18.0, based on upstream Hisingen v2.0.10. The original Global account / Data Portal / Augmented / C3 / PCCS / command source path is retained. China has its own UI, region switcher and experimental **read-only Android-app bridge**, but **native China vehicle API authentication and remote commands are not verified**.

## Start here

- **Independent China API knowledge base:** [knowledge/china/README.md](knowledge/china/README.md)
- **Complete Global → China capability matrix:** [knowledge/china/FEATURE_MATRIX.md](knowledge/china/FEATURE_MATRIX.md)
- **Network hosts and RPC reference:** [knowledge/china/ENDPOINTS_RPC.md](knowledge/china/ENDPOINTS_RPC.md)
- **China App model and bridge:** [knowledge/china/STATE_AND_BRIDGE.md](knowledge/china/STATE_AND_BRIDGE.md)
- **AI developer handoff:** [knowledge/china/AI_HANDOFF.md](knowledge/china/AI_HANDOFF.md)
- **Machine-readable mapping:** [knowledge/china/capabilities.json](knowledge/china/capabilities.json)
- **Public/official references:** [knowledge/china/OFFICIAL_REFERENCES.md](knowledge/china/OFFICIAL_REFERENCES.md)
- [Historical API comparison](docs/HISINGEN_GLOBAL_CN_API_COMPARISON.md)
- [0.18.0 release notes](docs/HISINGEN_CHINA_0.18.0_RELEASE_NOTES.md)

## User experience

On first real launch: **Select Polestar Region** → **China** / **Global**. You can switch afterward from the Hisingen app menu, the menu-bar popover footer and Settings.

When Global is selected, original Hisingen provider functionality remains in place. When China is selected, the original account form displays the CN sign-in/bridge surface; it does **not** pretend the China website session grants C3/PCCS access.

China SOC/range is currently accessible only via an already signed-in official Android app plus the optional read-only local bridge and needs on-device confirmation. Native direct Mac China gRPC, PCCS operations and remote commands remain **MAPPING_REQUIRED / DISABLED**.

## Build

GitHub Actions workflow: `.github/workflows/build-hisingen-china-018.yml`. It pins Hisingen `v2.0.10`, applies `Scripts/apply_hisingen_china_018.py`, builds on Xcode 27, verifies ad-hoc signature, and launches on macOS 15/26 ARM64. Offline tests validate knowledge-matrix and bridge freshness policies. The production app is not Developer ID notarized.

**Do not store credentials, authorization headers, SMS codes, cookies, Digital Key material, full VIN, phone numbers or precise vehicle locations in this repository.**

