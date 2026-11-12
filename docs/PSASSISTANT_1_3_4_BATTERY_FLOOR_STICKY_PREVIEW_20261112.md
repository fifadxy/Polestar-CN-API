# PsAssistant 1.3.4 — Under-car battery floor visibility and fixed preview

Date: 2026-11-12. Version: 1.3.4 / versionCode 179.

## Verified root causes

1. `VehicleHeroView.setSnapshot(snapshot)` previously ignored the supplied snapshot. Many dashboard skins explicitly bind `model.snapshot` through this method; a separate coordinator could have null or stale telemetry, preventing the floor from displaying. The view now stores/uses the supplied snapshot, including when null (no stale fallback), and invalidates on coordinator changes.
2. `BatteryFloor.draw(...)` rendered *before* drawing the chosen vehicle WebP. Several of the supported images are opaque from ~73% to 90% of image height, covering the narrow ground strip. The perspective floor now renders visibly after the original car image, in a larger ground-level band. The car retains the original bitmap and aspect ratio; an enabled floor reserves slight proportional space.
3. A charging state with unknown SOC used to suppress the entire floor, even when charging was confirmed. Now it displays an empty grid with “--%” rather than inventing a battery value. The idle floor still requires a valid percentage and explicit opt-in.
4. Previously the whole settings screen, *including the live preview*, was inside one ScrollView. The editor now has a fixed preview pane above an independently scrolling settings pane. The preview remains visible when changing colors, grid density, opacity, size or individual corners. The preview uses mock 81% only, never changes live vehicle data. Settings refresh the preview immediately.

## Regression and safety

- Unit tests verify live-snapshot binding, charging with missing SOC, preset colors, corner constraints, and fixed-preview view hierarchy.
- Corrects the 1.3.3 localized range-tab test to set Chinese language explicitly.
- Existing energy UI fixes and controls, login, OPPO Bridge, vehicle telemetry requests and remote commands remain otherwise untouched.
- Private signing key and China client asset must **not** be committed to GitHub. Original-signed FULL is an offline artifact, distinct from public validation CI, and requires device login/vehicle-control testing.
