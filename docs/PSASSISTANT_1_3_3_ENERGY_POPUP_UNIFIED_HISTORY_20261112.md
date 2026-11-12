# PsAssistant 1.3.3 — Energy popup consistency and three-step chart interaction

**2026-11-12 — 1.3.3 / versionCode 178**

1. The pinned kWh/km/¥ Y-axis does not paint a solid page-color rectangle. Bars, grid, dates, and other panning content are clipped right of the fixed gutter. The actual secondary dialog's translucent gradient glass remains visible in both themes.
2. The four history ranges shown in the popup are **1 month / 3 months / 6 months / All**, independent of display aggregation. The old 3d, 7d, 31d, and custom range tabs are removed from the popup.
3. The underlying history query now always starts from daily local observations. The same source is then regrouped by day/week/month. Energy, climate, mileage and estimated cost totals are preserved across the three display groupings. Missing observations remain missing, never interpolated.
4. Trend cycles **Automatic (matches period) → last 7 days vs prior 7 days → last 30 days vs prior 30 days → Off**. Automatic mode for All explicitly compares last 1 month against the preceding 1 month; a previous full-retention window does not exist. Every trend uses the selected metric, tariff when applicable, and non-overlapping source intervals. Unsupported or under-sampled comparisons show no invented percentage.
5. Repeated bar taps cycle **focused bar value → all values with X/Y dashed reference crosshair at the tapped bar → hide both values and reference**. When the plot resizes, focus scrolls toward the tapped position.
6. Regression coverage includes day/week/month totals from one source, history-range selection, trend windows, dark/light popup glass axis transparency, and three-stage bar interaction.

**Security & deliverables**: This public repository contains no personal account credentials, account client asset, signing private key or token. Public CI generates *validation* APKs only. Independent China login FULL APK packages require the previously user-provided authorized standalone client asset and the original 1.3.1 signing identity; static packaging checks do not substitute for actual on-device login and vehicle control testing.
