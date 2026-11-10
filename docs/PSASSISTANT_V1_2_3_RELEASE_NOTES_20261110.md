# PsAssistant 1.2.3 — Energy, map, help and menu density

2026-11-10 | Android version **1.2.3 / versionCode 168**

## Changes

1. Energy/cost summary on the main vehicle-data page and its secondary popup now uses compact rows for TM distance and estimated TM total energy, with a clearly highlighted VI-orange selected state for energy, climate, distance, cost and history-period chart switches.
2. Below total trip cost, tap the rate row to toggle **CNY/100 km** and **CNY/km**. Formula: TM average consumption (kWh/100 km) × configured electricity price (CNY/kWh), divided by 100 for per-kilometre cost. Missing trip consumption or unset tariff is clearly indicated, rather than fabricated as zero.
3. Adds **TA trip cost estimate** using reported TA trip distance × **TM average consumption** / 100 × configured tariff. The display labels its data source and estimated, not metered, status; this is not a charging invoice.
4. The portrait parking-map popup presents its map, vehicle-location refresh, phone-location settings/refresh and the collapsed explanation in one compact, non-scrolling panel. Ultra-short landscape layouts retain a scrolling fallback to avoid hiding controls.
5. Help labels appear as *说明* followed by an orange circled question mark. Explanations start collapsed and expand in place; previous “? 说明” prefix removed.
6. Menu row and section spacing is reduced globally. Settings → Appearance and interaction → **Menu row spacing** offers 2, 4 (default), 6, 8 and 12 dp. The setting is included in settings backup/restore; full-screen home vehicle layouts are deliberately unaffected.
7. Oil (where supported) is pinned under **Vehicle checks**, including its unknown/unreported state. Maintenance interval summaries remain limited to service days, distance, engine hours and reference percentages.

## Safety and release boundaries

- Continue from PsAssistant 1.2.2. China-region session, real remote control, OPPO Bridge and LSPosed code are unchanged.
- Real TM and TA readings are never substituted with invented history; unknown values remain “--”. Rates use user-configured electricity tariff, not an assumed purchase or charge price.
- CI validation Debug APKs deliberately do not embed China login client compatibility resources. Independently usable local compatibility APKs require the previously approved client asset and original signing identity, checked off-repository. A verified APK signature does **not** substitute for on-device China login/vehicle control testing.
