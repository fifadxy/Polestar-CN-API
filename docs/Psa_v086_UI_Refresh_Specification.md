# Psa UI and Refresh Specification v0.8.6

## Controls and information

- Reuse the confirmed Psa glyph family. PS Default windows/find-car use the existing VI assets; its condition glyph matches vehicle-data navigation. Day/night changes tint only.
- Center the complete icon/title group; feedback occupies a separately bounded line. Fit text to width/height, never overlap adjacent controls. Preserve full values in the accessible description and data page.
- Condition button modes are bar, percentage-only, text-only; percent is the lower available remaining-time/distance reference, not native life. No percentage appears when both source readings are absent.
- Button-label bold, data-value bold, feedback bold and widget bold are independent. Skin font percentage and Android accessibility scale remain active. A requested size is an upper bound in constrained cells.
- Climate default: state, time left, target temperature. Cabin temperature remains available in data/settings but is not the default climate-button field. Running durations use h/m; derived timing provenance belongs in detail text.
- Charge shimmer is restricted to the charging card: 4.8 seconds per left-to-right sweep, soft alpha beneath legible text, reused shader/matrix, redraw throttled to about20fps; no work while hidden, paused, detached or Android animations disabled.
- VoLike has one picker entry and a persisted detailed-view toggle, also activated by a deliberate right swipe. Detailed data occupies the right rail; compact landscape prioritizes sorted data without deleting preferences.

## Widgets

Use the same App launcher glyph for provider icons and picker previews. Keep native RemoteViews text, responsive layout, timestamp and proportional vehicle image. Bind action glyph/tint/status from the same vehicle snapshot; never turn a command response into an optimistic state update. Fixed actions and toggles are distinct, and all writes open biometric verification. At5×1/380dp the layout can show5 controls with48dp cells; narrower launchers show fewer, wider frames may show6. Fields and button choices persist by provider size, styles/typography remain shared preferences.

## Separate refresh scopes

| Scope | Defaults and choices | Boundary |
| --- | --- | --- |
| App foreground |5min; off/1/2/5/10/15 |Only while activity resumed, consented and signed in; no overlapping command/readback. |
| App background |Disabled until user enables;30min; off/15/30/60/120 |JobScheduler, no resident notification required. Existing resident source remains valid. |
| Widget background |30min when widgets exist; off/15/30/60/120 |Shares vehicle snapshot/job with the fastest enabled app source; OS can defer. |
| Vehicle parking point |15min cache; every-open/5/15/30/60 |c3Lbs when opening preview; explicit refresh bypasses cache. No phone-location call. |
| Phone location |Permission off; manual in preview, optional once on preview open |Trusted Amap origin, one-shot request window, no background permission/watchPosition. |
| Command verification |5/10/15/20s |Unchanged read-only verification; no repeated writes. |

No background job requests vehicle parking coordinates or phone location. C3 coordinates stay in memory and are cleared on vehicle switch/logout. No private compatibility configuration or signing key enters source control.
