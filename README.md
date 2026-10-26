# Polestar-CN-API

Research and interoperability tooling for China-region Polestar vehicle data and the **Hisingen China** native macOS preview.

> **Status (2026-10-25):** The historical 0.17.0–0.19.4 patch sets have been consolidated on the 0.19.5 integration line, based on upstream Hisingen v2.0.10. The Mac client has an independent native China phone/SMS session, dedicated Keychain item, linked-vehicle discovery and China C3/PCCS read path. It is **not Bridge mode**. Real-account/vehicle end-to-end validation is still required, and China remote commands remain disabled.

## Start here

- **Independent China API knowledge base:** [knowledge/china/README.md](knowledge/china/README.md)
- **Native Mac China session:** [knowledge/china/NATIVE_MAC_SESSION.md](knowledge/china/NATIVE_MAC_SESSION.md)
- **Complete Global → China capability matrix:** [knowledge/china/FEATURE_MATRIX.md](knowledge/china/FEATURE_MATRIX.md)
- **Network hosts and RPC reference:** [knowledge/china/ENDPOINTS_RPC.md](knowledge/china/ENDPOINTS_RPC.md)
- **Historical China App model and 0.18 bridge:** [knowledge/china/STATE_AND_BRIDGE.md](knowledge/china/STATE_AND_BRIDGE.md)
- **AI developer handoff:** [knowledge/china/AI_HANDOFF.md](knowledge/china/AI_HANDOFF.md)
- **Machine-readable mapping:** [knowledge/china/capabilities.json](knowledge/china/capabilities.json)
- **Public/official references:** [knowledge/china/OFFICIAL_REFERENCES.md](knowledge/china/OFFICIAL_REFERENCES.md)
- **Verified Android 4.25.0 authentication evidence:** [docs/POLESTAR_CHINA_ANDROID_AUTH_EVIDENCE.md](docs/POLESTAR_CHINA_ANDROID_AUTH_EVIDENCE.md)
- **Rooted ARM64 validation tooling and runbook:** [docs/ANDROID_ROOT_VALIDATION_TOOLING.md](docs/ANDROID_ROOT_VALIDATION_TOOLING.md)
- **ARM64 Native Bridge emulator setup:** [docs/ANDROID_ARM64_BRIDGE_EMULATOR.md](docs/ANDROID_ARM64_BRIDGE_EMULATOR.md)
- **Branch inventory and 0.19.5 development entry:** [docs/BRANCH_INVENTORY_AND_0195_PLAN.md](docs/BRANCH_INVENTORY_AND_0195_PLAN.md)
- [Global / China API comparison](docs/HISINGEN_GLOBAL_CN_API_COMPARISON.md)
- [0.19.5 release notes](docs/HISINGEN_CHINA_0.19.5_RELEASE_NOTES.md)

## User experience

On first real launch: **Select Polestar Region** → **China** / **Global**. You can switch afterward from the Hisingen app menu, the menu-bar popover footer and Settings.

When Global is selected, original Hisingen provider functionality remains in place. When China is selected, the original account card becomes a native phone/SMS form and the active provider is rebuilt against the separate China session.

The native Mac provider wires vehicle discovery, C3/PCCS reads and live streams without an Android app or localhost service. Compilation and protocol mapping do not prove real-account authorization: China login and telemetry still need a redacted end-to-end vehicle test. All China writes remain fail-closed.

## Build

Start new work in `Patches/0.19.5` and validate it with `Tests/test_native_mac_contract_0195.py`. The matching workflow pins Hisingen `v2.0.10`, applies `Scripts/apply_hisingen_china_0195.py`, builds on Xcode 27, verifies the ad-hoc signature, checks that the artifact contains no Bridge runtime, and launches the same packaged app on macOS 15/26 ARM64. The artifact is not Developer ID notarized.

**Do not store credentials, authorization headers, SMS codes, cookies, Digital Key material, full VIN, phone numbers or precise vehicle locations in this repository.**

