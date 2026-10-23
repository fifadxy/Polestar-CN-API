# Polestar-CN-API

Research and interoperability tooling for China-region Polestar vehicle data and the **Hisingen China** native macOS preview.

> **Status (2026-10-23):** Hisingen China 0.19.0, based on upstream Hisingen v2.0.10. Mac now has an independent native China phone/SMS session, dedicated Keychain item, linked-vehicle discovery and China C3/PCCS read path. China C3 discovery uses the public configuration document without a bearer header, and PCCS reads follow per-vehicle capability fields from `car-information`. The retired Python Bridge source has been removed from this branch. Real-account/vehicle end-to-end validation is still required, and China remote commands remain disabled.

## Start here

- **Independent China API knowledge base:** [knowledge/china/README.md](knowledge/china/README.md)
- **Native Mac China session:** [knowledge/china/NATIVE_MAC_SESSION.md](knowledge/china/NATIVE_MAC_SESSION.md)
- **Complete Global → China capability matrix:** [knowledge/china/FEATURE_MATRIX.md](knowledge/china/FEATURE_MATRIX.md)
- **Network hosts and RPC reference:** [knowledge/china/ENDPOINTS_RPC.md](knowledge/china/ENDPOINTS_RPC.md)
- **Historical China App model and 0.18 bridge:** [knowledge/china/STATE_AND_BRIDGE.md](knowledge/china/STATE_AND_BRIDGE.md)
- **AI developer handoff:** [knowledge/china/AI_HANDOFF.md](knowledge/china/AI_HANDOFF.md)
- **Machine-readable mapping:** [knowledge/china/capabilities.json](knowledge/china/capabilities.json)
- **Public/official references:** [knowledge/china/OFFICIAL_REFERENCES.md](knowledge/china/OFFICIAL_REFERENCES.md)
- [Historical API comparison](docs/HISINGEN_GLOBAL_CN_API_COMPARISON.md)
- [0.19.0 native Mac release notes](docs/HISINGEN_CHINA_0.19.0_RELEASE_NOTES.md)

## User experience

On first real launch: **Select Polestar Region** → **China** / **Global**. You can switch afterward from the Hisingen app menu, the menu-bar popover footer and Settings.

When Global is selected, original Hisingen provider functionality remains in place. When China is selected, the original account card becomes a native phone/SMS form and the active provider is rebuilt against the separate China session.

The native Mac provider wires vehicle discovery, C3/PCCS reads and live streams without an Android app or localhost service. Compilation and protocol mapping do not prove real-account authorization: China login and telemetry still need a redacted end-to-end vehicle test. All China writes remain fail-closed.

## Build

GitHub Actions workflow: `.github/workflows/build-hisingen-china-019.yml`. It pins Hisingen `v2.0.10`, applies `Scripts/apply_hisingen_china_019.py`, builds on Xcode 27, verifies the ad-hoc signature, checks that the artifact contains no Bridge runtime, and launches the same packaged app on macOS 15/26 ARM64. The artifact is not Developer ID notarized.

**Do not store credentials, authorization headers, SMS codes, cookies, Digital Key material, full VIN, phone numbers or precise vehicle locations in this repository.**

