# Hisingen China 0.19.0 — Native Mac preview

Hisingen China 0.19.0 is based on upstream Hisingen `v2.0.10` (build 28). This release replaces the 0.18 Android bridge runtime with an independent native macOS China session. Android/APK work is not part of this branch or artifact.

## What changed

- Native mainland China phone + SMS sign-in inside Hisingen Settings.
- China credentials and both China token families are stored in a dedicated macOS Keychain item.
- Local-token and global-token refresh are independent of Hisingen's Global Polestar session.
- Linked vehicles are discovered through the China `car-information/car` service.
- The China provider injects the China C3 discovery and PCCS roots into Hisingen's existing Swift telemetry implementation.
- Battery, range, charging, exterior, availability, odometer, health, software, climate, schedules, connectivity, air quality, location and weather reads are wired where the China backend exposes the compatible service.
- Native C3 battery and exterior streams use Hisingen's existing stream engine.
- First launch, the native app menu, the menu-bar popover and Settings all expose China / Global selection.
- The Settings UI uses the original Hisingen card, typography, spacing, color and motion system.
- The distributable contains only the Mac app, installer, release notes and checksums. It does not contain or start an Android bridge or localhost service.

## Safety boundary

All China vehicle-changing operations remain disabled in both the capability catalog and the provider executor. This includes locks, climate, charging writes, windows, tailgate, honk/flash and OTA actions. Observing a method in the official app is not enough evidence to execute it against a vehicle.

## Verification status

| Area | 0.19.0 status |
| --- | --- |
| Source patch against Hisingen 2.0.10 | Automated |
| Native Mac contract / no-Bridge checks | Automated |
| Xcode 27 compilation and app launch | GitHub Actions |
| macOS 15 / macOS 26 packaged-app launch | GitHub Actions |
| Global provider source isolation | Automated |
| Real mainland China SMS sign-in | Requires a consenting account; not claimed by CI |
| Real linked-vehicle discovery and C3 values | Requires a consenting vehicle; not claimed by CI |
| China remote commands | Disabled |
| Developer ID notarization | Not included; artifact is ad-hoc signed |

The official service can request an Alibaba verification challenge (`2002016`). Hisingen stops and reports that condition; it does not bypass the challenge.

## Install

Keep these files together:

- `Hisingen-China-0.19.0-Native-Mac.app.zip`
- `install_hisingen_china_019.command`
- `SHA256SUMS.txt`

Run the installer. It verifies checksums, the ad-hoc signature and patch version, backs up an existing `Hisingen China.app`, installs the new app in `/Applications`, then opens it.

