## Psa v0.9.6-beta2 · Motion preview and glass edges

Charging-card previews belong below the vehicle viewport, never layered across the car. Auto tests use an explicit project selection and send no vehicle requests. Window coverage defaults to 52%; inward feather defaults to 20% (0–100%) and stays inside the glass mask. Cache the mask rather than blur every frame. Climate and charging default to absorption while preserving explicit saved preferences.

## Psa v0.9.6 · Confirmed classic icon and status typography

The user-confirmed black/white double-headlight vehicle glyph is an additional choice, not a replacement for other released styles. Master: [Psa_Confirmed_Classic_Icon.svg](assets/Psa_Confirmed_Classic_Icon.svg). Launcher foreground has 22% inset; status glyph uses white alpha on transparent background for system tint. App and notification styles are independent. Earlier A/B/C preview candidates are not approved assets and are not shipped. Condition text customization changes presentation only; warning/unknown decisions remain data-driven.

## UI Hotfix addendum · v0.9.5-beta2 (2026-10-29)
- Settings: keep labels concise; disclose explanations on tap of a 26dp orange circled question mark.
- Every control tile: icon + action title + live status. Dark mode may use white status text; a single bottom row shows vehicle connection · command feedback. Connection means recently read cloud data.
- Hide all battery SOC progress bars with a user setting, without hiding SOC text or service-life progress.
- Per-control-family command security: biometric(default), explicit confirmation, or direct; a tap on an ongoing same-family command always requires separate repeat confirmation and permits at most one queued repeat.
- Native VI-orange particles (not GIF): adjustable straight fraction 20% default, irregular arc 15% default, size 25–300%, spawn extent 30–150%, fade-in. Top-view absorbs from all sides under the car; oblique/side art absorbs under body and front. These targets are stylistic references, not verified physical charge-port locations.
- Climate airflow shares irregularity controls; window gradient intensity and speed adjustable. Cooling blue, warming VI orange, purification green; mode choice is visual preference where telematics cannot verify heat/cool.
- Optional map backdrop: Amap, OpenStreetMap, OpenTopoMap, zoom 10–19, progressive fade and attributions; availability depends on provider/device/network.
- Camping local stop must be possible offline; remote stop remains explicitly unconfirmed until fresh LIVE read; after reconnect read state first, never blindly replay unknown writes. No bypass of server token expiry.

# Psa — VI/UI Design Specification v5.0 (Optimized Edition)

**English research edition v5.0 — synchronized with the optimized 22-page PDF · 26 October 2026**  
**Coverage:** Stockholm Design Lab visual identity; Polestar official external branding guideline; Google Play Polestar Android app; launcher icon; typography; mobile UI and model-specific vehicle imagery; Android 4×2, 4×1, 2×2, 2×1 and experimental 1×1 widgets; opaque, semi-transparent and fully transparent widget references; legacy wearable evidence; independent Wear OS reference concepts; state and Android implementation handoff.  
**Classification:** **Independent reference study — not an official Polestar brand manual and not an original asset package.**

> **Provenance rule:** Separate what the sources *document* from what the published images *show* and from implementation values *proposed in this guide*. Public marketing images do not supply a pixel-perfect design file. Exact industrial use requires approval of the original logo, licensed font, original screenshots, and a verified master design file.

### Companion PDF and revision parity

**Companion PDF:** Uploaded optimized v5 design-reference PDF (22 pages).

This Markdown is synchronized with the final optimized PDF rather than an earlier widget mockup. The document is intentionally more detailed than the slide-like PDF, with source links, implementation proposals and verification constraints.

| Optimized PDF pages | Design reference | Source status |
|---|---|---|
| 1–5 | Identity, symbol and wordmark guidance | Official sources + explicitly labeled reference trace |
| 6–10 | Typography, palettes, iconography and vehicle assets | Observed official material + proposals |
| 11–13 | Phone UI and responsive dimensions | Official public screenshots + proposed tokens |
| **14** | **4×2 and 4×1 enlarged widget layouts** | Independent proposed layouts |
| **15** | **2×2, 2×1 and 1×1 responsive compact layouts** | Independent proposed layouts |
| **16** | **Opaque / semi-transparent / fully transparent reference** | Independent surface treatment samples |
| **17** | **Widget sizing breakpoints, offline and pending states** | Android platform guidance + proposals |
| 18–19 | Wear OS interfaces and command states | Legacy function evidence + independent UI proposals |
| 20–21 | Cross-platform states and release acceptance | Independent engineering guidance |
| 22 | Official source register and rights statement | Public primary sources |

**What changed in the optimized PDF:** the 4×2 and 2×2 contents were enlarged; 4×1 and 2×1 maintain clear metric/action separation; the 1×1 version is action-only; and the three opacity samples are shown with the *same* content against the *same* example wallpaper so their differences can be compared. This Markdown keeps those decisions consistent across the layout matrix, design tokens and acceptance checks.

## 1. Source hierarchy and evidence definitions

| Grade | Definition | Permitted interpretation |
|---|---|---|
| **A — Documented** | Direct claims by Stockholm Design Lab, Polestar, Google Play, Lineto, or platform design documentation. | A reliable description of that specific source at the date checked; not a blanket claim about every region/model/version. |
| **B — Visually observed** | Traits visible in official published static imagery or Google Play marketing screenshots. | Visual reference, not a verified dp, pixel, density, asset name, animation, or live application layout. |
| **C — Proposed** | Responsive grids, tokens, watch screens, component measurements and behavior suggested here for an implementation. | Useful engineering baseline that **must not** be described as original/official Polestar specifications. |

### What was verified

- **Brand:** Stockholm Design Lab explicitly names the Unica77 Polestar Var typographic system, a Northern Star-derived symbol, typographic marking, minimalist spaces and an overall product-first identity. The brand team describes **Pure / Progressive / Performance**. [S1] [S2]
- **Typeface:** Lineto describes a customized Unica77 deployment with adjusted glyph spacing/kerning, extensive screen testing and multiple scripts. It is not interchangeable with an ordinary off-the-shelf font file. [S3]
- **Official Android product:** Google Play identifies **Polestar**, by **Polestar Performance AB**, package `com.polestar.explore`. The listing inspected on 26 October 2026 displays a **16 October 2026** update date, and describes climate, door locks, location, charging, software, account, orders and service functions. [S4]
- **Published UI:** Google Play shows a **dark car overview** with an orange battery/range cluster and compact status tiles, together with **lighter climate, charge, car management, location and order** marketing captures and a home-widget example. [S5]
- **Official Apple ecosystem:** iOS release 5.1.0 (December 2025) notes Apple Watch (watchOS 26) quick controls; 5.3.0 (February 2026) notes configurable widgets. Subsequent releases expand model-dependent car controls. [S6]
- **Wearable limitation:** The publicly located Polestar 1 manual lists earlier Android Wear/Apple Watch functionality, but does **not** establish a modern official Wear OS UI for the Android Google Play product. [S7]
- **Brand asset permission:** Polestar’s official 2023 external branding guideline states that third-party use of its visual brand assets—including the symbol, wordmark and cars—requires written permission. This document is a research reference, not a license. [S13]

### What was *not* verified

1. Actual original Figma or Sketch design-file dimensions; precise current phone screen grid or component padding.
2. Original official icon SVG paths, vector icon set, or approval to redistribute them.
3. Official production HEX values for the current 2026 app colors.
4. Licensed Unica77 Polestar Var font files, complete CSS/font variable configuration, or typographic tracking tables.
5. The presence of a standalone official **2026 Wear OS** companion app or a screenshot of its actual interface.
6. Which features are enabled for each country, model, vehicle software build and account tier.

The companion optimized v5 PDF uses **official public screenshots and vehicle photographs** where a direct published source is available. It also contains a **vector reference trace** derived from the official public app icon and clearly labeled independent mobile, widget and watch proposals. None of these diagrams is authenticated Polestar source artwork or a pixel-exact official UI file.

---

## 2. Official brand design principles [A]

### 2.1 Pure

A small number of carefully controlled design variables gives the product attention. Preferred characteristics are monochrome surfaces, generous negative space, quiet backgrounds, alignment through a consistent grid and functional labels. Do not add cosmetic skeuomorphism, glow, multiple competing typefaces or gratuitous colorful buttons.

### 2.2 Progressive

Polestar's visual language treats factual markings and engineered details as design elements. Small lines, technical model labels, units and simple iconography can carry brand character. The marking principle originates from product information and extends into communication. A restrained mark and correct unit are more characteristic than a decorative graphic. [S2]

### 2.3 Performance

Vehicle and live product capability are protagonists. A dashboard should organize a clear primary value (battery state, range, charging, or a requested action), then supporting controls. It should not present ten equally weighted numbers. Show status truthfully: the beauty of the interface depends on reliable information.

### 2.4 Brand system versus app history

The older Polestar application/brand imagery includes large **vivid yellow** vehicle-control panels. The current Google Play presentation instead pairs a **dark dashboard**, an **orange metric/accent**, and **light utility views**. These are **different visual moments**, not a single globally binding design theme. See the earlier 90% / yellow panels in the Polestar Journal/SDL project [S1][S2], and newer Google Play imagery [S5]. **Do not combine their components as though they were released together.**

---

## 3. Reference image atlas — original source links

Images below reference **remote publicly published assets**; no redraws are presented as originals. If a reader cannot access these external hosts, open the source pages in [S1] or [S4]. Preserve the supplied image aspect ratios, do not crop out the brand mark, and do not silently substitute another model.

### 3.1 Stockholm Design Lab — brand identity

**Northern Star-inspired symbol and presentation of its geometry** [S1]

![Polestar symbol development and visual identity](https://cdn.sanity.io/images/6uvtz1ot/production/345ec6c4637eb62b0d2b15f3e627939adc35e1f6-2160x2701.svg?auto=format&fm=webp&q=75&w=3840)

**Weight and size behavior of brand typography** [S1]

![Polestar typography in varying sizes](https://cdn.sanity.io/images/6uvtz1ot/production/adf02d13610a37abc8b74bfc1fc453634adf89f1-5118x3412.jpg?auto=format&fm=webp&q=75&w=3840)

**SDL's earlier car-control design image: illustrates a distinct archival app moment** [S1]

![Earlier vehicle battery UI in the Stockholm Design Lab portfolio](https://cdn.sanity.io/images/6uvtz1ot/production/04b8256f283646e2cbc0c70e261117abd5401981-1080x1080.png?auto=format&fm=webp&q=75&w=3840)

### 3.2 Google Play — current published promotional image set

The artwork is published under the official Polestar listing. Numbering below follows the captured Play gallery order, **not** an assertion that all views appeared in this order in-app.

**01. Dark status overview — battery/range, car view, status tiles.** [S5]

![Official Google Play Polestar dark status overview](https://play-lh.googleusercontent.com/Ary_TtqeQzkiZxsqGaXiaU5gNGvPVPOHsBpR-TZz_83hHsr4cAyLLBzF0nDZL2h0Gj6pljiHGdLv_RVdU8HrCw%3Dw526-h296)

**02. Feature typography/promotional list (marketing artwork, not in-app navigation).** [S5]

![Official Google Play Polestar marketing capabilities typography](https://play-lh.googleusercontent.com/HUn44Krrv4OMbGdZrfIEtzQqqV_Y1DdqTGZe7kgiB_B7owb0QBySFOtLI9I_2AwgXQ9yPoUGiefHAGcP0yBQRw%3Dw526-h296)

**03. Preconditioning / climate setting.** [S5]

![Official Google Play Polestar climate control promotional screenshot](https://play-lh.googleusercontent.com/6TYFhFxboqsc82hNanerDEfKGhGvtMyWcMklCn6-sfoOqWXpbzL2U0qz-b9jChCZGcODzOdcpCwk-JKG1wdBsQ%3Dw526-h296)

**04. Charging and scheduling.** [S5]

![Official Google Play Polestar charging screenshot](https://play-lh.googleusercontent.com/u0o1_gFYH352WvgWucNqZzWJoxFQIDXsMS722bKhLPl4pqsQYMGTyeh4lVrAzvbxZU-n8DdozWDWKO6trTvaDw%3Dw526-h296)

**05. Vehicle status / management.** [S5]

![Official Google Play Polestar car status and management screenshot](https://play-lh.googleusercontent.com/aiY9nYy4O0rBJhefcoGEGYXpYYJyqjBnxy6GChInJbxqD0CthPKK_UwcoLzWOHMHHdM95A7bPx2Z7aHnvyB0%3Dw526-h296)

**06. Order / account.** [S5]

![Official Google Play Polestar order tracking screenshot](https://play-lh.googleusercontent.com/HW0ipNGQwpjfT2ysHz3RXTK9qvneSWPqZ7utbHB2f6tHe8qKfG0iexRxdRGt8VIhMHFn-pEbMsDJpV5afn9TZQ%3Dw526-h296)

**07. Home-screen widget / control shortcut.** [S5]

![Official Google Play Polestar widget screenshot](https://play-lh.googleusercontent.com/Mb-f5wAGkjfqUXJOM0Pb1OKCyFCF6tWJn-c45eS2655IwtU-JklXPHEmcLMoe6O3yN95MGTEEYmVosRrdzM1tAc%3Dw526-h296)

**Official launcher icon**, a four-point star rendered on a white square. The thumbnail served to this research workspace is 240 × 240 px; a separate derived *vector reference trace* in the PDF is used for large-screen clarity but is **not** a Polestar-issued vector master. [S4]

![Official Polestar Android launcher artwork](https://play-lh.googleusercontent.com/mqC-GhSPKkk1tUA6D0KHU4VmaAsXG17n83rpAypTZEgbwMSWJkkvsAOnXpqfWobdjGNAulZXUjwWFxOOKQxLZg%3Dw240-h480)

*Image handling:* links above are reference assets served by the source sites, **not an authorization for republication as a logo pack**. For production graphics request the official brand assets.

---

## 4. Brand symbol and launcher icon

### 4.1 Symbol structure [A/B]

- A minimal four-point, opposed-wing optical star, formed by angular tapered elements and their negative space; the image also shows a crisp sense of folded/three-dimensional light and shadow. [S1][S2]
- The mark works in monochrome and may adopt light/dark variants depending on surface. Avoid drawing over it, applying a circle around it, substituting generic star emojis or redrawing the crest by eyeballing it.
- Treat the **Polestar wordmark** and the **star symbol** as two governed master assets. Their proportions must remain fixed by the approved source SVG.

### 4.2 Geometry and safe zone [C + Android platform A]

For Android launcher delivery, follow Google's adaptive icon specifications [S9]. The 108 × 108 dp layer and centered 66 × 66 dp safe region are **Android platform** dimensions, not unique Polestar brand dimensions:

| Layer | Source canvas | Critical safe region | Notes |
|---|---:|---:|---|
| Foreground | 108 × 108 dp | Center 66 × 66 dp | Use the authorized mark; outer 18 dp each side can be affected by masks and movement. |
| Background | 108 × 108 dp | Entire tile | Single neutral field; avoid shadows that imply pre-masking. |
| Monochrome | 108 × 108 dp | Center 66 × 66 dp | Supply a one-color themed variant. |

**Suggested visual checks:** preview on a circle, rounded rectangle, squircle and themed icon. Do not place a production asset beyond the visible safe area. Match pixel/optical alignment after the icon is rendered in actual launchers.

### 4.3 Wordmark discipline [A/C]

Use the authorized wordmark. Never type the word "Polestar" in an approximate font and claim it is the master logo. A sketch may use substitute text, but production and marketing must use the official wordmark export. Allow a consistent protection area based on the symbol's internal reference module **U**, to be defined from the approved master artwork, not invented from a resized web image.

---

## 5. Typography and visual hierarchy

### 5.1 Official typography [A]

**Typeface:** Unica77 Polestar Var. **Principle:** a uniform weight with size-sensitive spacing/kerning; system consistency carries hierarchy. The historic brand typography was unusually restrained in its number of weights and sizes. SDL's case study directly mentions **a single weight with variable letter spacing**. Lineto confirms bespoke alterations and sophisticated screen behavior. [S1][S3]

**Licensing:** this document does not include, convert or redistribute the font. A substitute is for engineering preview only. Do not ship a Polestar-branded app with an unlicensed commercial font or make a general system font appear to be Polestar's exact wordmark.

### 5.2 Prototype replacement font [C]

- **Android prototype:** Roboto regular/light for reliable system layout, or licensed Inter if approved.
- **Multi-script prototypes:** Noto Sans family for missing language coverage.
- **Final:** original licensed Unica77 Polestar Var; match original tracking and kerning with design assets.

### 5.3 Proposed app text tokens — 360 dp reference [C]

| Token | Size / line-height | Tracking | Typical use |
|---|---:|---:|---|
| `displayMetric` | 48 / 56 sp (64 / 70 on very large surfaces) | −1.2 sp | Main SOC figure; avoid several competing oversized metrics. |
| `screenTitle` | 30 / 36 sp | −0.6 sp | Cars, Charge, Climate, Location. |
| `section` | 20 / 25 sp | −0.2 sp | Secondary information heading. |
| `body` | 16 / 23 sp | 0 sp | Menu, tile labels, states. |
| `support` | 12 / 17 sp | +0.1 sp | Range units, last-updated time, helper copy. |
| `micro` | 11 / 15 sp | +0.4 sp | Secondary caption only, not critical small tap labels. |

**Typography rules:** default left-alignment; no automatic all-caps for main body; pair figures with their units; avoid forced line breaks within a number and unit; scale the type with the user's device setting; increase row height when text wraps. All these values are **proposed** and need later measurement against approved snapshots.

---

## 6. Color system, chronology and accessibility

### 6.1 Brand master [A]

Polestar's official identity narrative describes a **white/black/greyscale foundation** with restrained accent colors. The intent is that accent colors highlight rather than dominate. Polestar distinguished material-specific colors from web colors. [S2]

### 6.2 Sample implementation palette [C]

The following values are **art-direction approximations for this document**, not verified official Polestar production HEX codes.

| Role | Sample HEX | Intended use | Provenance |
|---|---|---|---|
| `ink` | `#151617` | Primary dark text, high-contrast icons | C sample |
| `charcoal` | `#27282A` | Dark tiles/surfaces | C sample informed by B |
| `canvas` | `#F3F3F1` | Quiet light surfaces | C sample |
| `white` | `#FFFFFF` | Strong negative space | C sample consistent with A |
| `midGray` | `#777A7C` | Secondary text on light surfaces | C sample |
| `actionOrange` | `#FF6B00` | Current visual accent study | C approximation of B screenshot |
| `archiveYellow` | `#FFE72D` | **Historical** yellow-app reference only | C approximation of B historical image |
| `positive` | `#22764A` | Semantic confirmed/success — not brand | C sample |
| `error` | `#B62E28` | Semantic error — not brand | C sample |

### 6.3 Contrast checks on these sample tokens [C]

- Orange `#FF6B00` on white `#FFFFFF`: **~2.86:1** — **not suitable for regular small body text**. Prefer dark text on orange, orange on charcoal, or use a stronger darker text tone.
- Dark ink `#151617` on archive yellow `#FFE72D`: **~14.43:1**.
- White on charcoal `#27282A`: **~14.76:1**.
- Check actual supplied production palette in the target display color space and with appropriate WCAG contrast tests; bright accent is not automatically an accessible text color.

### 6.4 Historical/current separation

| Context | Observed treatment | How to use this reference |
|---|---|---|
| Earlier Polestar brand-app visuals | Gray field, large yellow status/control rectangles | Archive/history only. |
| Current official Android Play dashboard | Black field, orange SOC/range, compact dark tiles | Reference for the dark main status layout. |
| Current Play utility pages | Mostly bright/light interface and orange CTAs | Reference for charging/climate/service views. |
| Widgets / watch | OS-native surface and color constraints apply | Do not force phone layouts directly into a widget or round display. |

---

## 7. Responsive mobile geometry and proportional fidelity

### 7.1 Implementation baseline [C]

Reference board: **360 × 780 dp** in portrait, approximately **360:780 / 0.4615 width-to-height**. This is **an intentionally chosen design baseline**, not the measurement of Google's published image. Test also 320, 393, 412 dp widths, landscape, split-screen, tablet and font scaling.

| Variable | Proposed value | Rationale |
|---|---:|---|
| Screen margin | 20 dp | Maintain the understated grid and clean left alignment. |
| Internal gutter | 8 dp | Suitable for 2-column status tile systems. |
| 4-column layout | `20 + 4×(74) + 3×8 + 20 = 360 dp` | Explicit calculable baseline. |
| Spacing ladder | 4, 8, 12, 16, 24, 32 dp | Predictable rather than arbitrary margin values. |
| Icon artwork box | 24 × 24 dp | Draw optical shape within a common viewbox. |
| Minimum hit target | 48 × 48 dp | Android accessibility guidance. [S12] |
| Standard divider | 1 dp or 1 device pixel depending on rendering | Technical separation, not heavy boxed design. |
| Corner radius | 0–8 dp inside app; OS-governed corners on widgets | The original Polestar app often uses square/technical surfaces. |

**Proportional control**: avoid globally scaling the entire 360 dp screen. Use width-responsive constraints for columns, dp for physical touch targets, scalable sp for text and independent content heights for text wrapping. Preserve aspect ratio for official car images, the brand logo and vehicle silhouettes. Do not stretch a marketing screenshot to the dimensions of a modern phone.

### 7.2 Primary dark status view [B/C]

The visible published hierarchy is: orange battery/range metric; a vehicle representation; compact supporting vehicle/location text; a 2 × 2 set of status/action zones; bottom navigation. [S5]

Proposed content allocation on a 360 × 780 dp artboard: system+header **~8%**, prominent metric+vehicle **~25%**, context **~10%**, control grid **~47%**, navigation+gesture area **~10%**. This is a **conceptual vertical allocation**, not a measured official screen ratio. Change it based on the device's actual safe insets and content. On shorter screens, the remote controls must scroll or reorganize rather than clip.

### 7.3 Screen family definitions

| Screen | Required first-class information | Secondary detail | Remote action policy |
|---|---|---|---|
| Overview | SOC, range, telemetry freshness, car identity | Locks, climate, charge, location | No false confirmed state. |
| Climate | Current climate mode, target/ETA (if supported), on/off | Timer, purification, temperature | Separate requested, pending and acknowledged statuses. |
| Charging | SOC, range, current charge state, cable state | Schedule, limit, charging history | Model/region capability gate for charge-now or changes. |
| Vehicle | Correct model, identity, image | Statistics, users, software, service | Use model-specific assets. |
| Location | Parked position + timestamp | Map and route/open in map | Respect privacy and freshness. |
| Orders/account | Order identity and high-level progress | Polestar ID, support, configuration | Do not use live car command styles for commerce actions. |

### 7.4 Bottom navigation [B/C]

Google Play screenshots visually suggest a **five-slot lower navigation**, but the screenshots do not prove each screen has precisely the same labels, semantics or order in production. The five domains in the PDF are a **conceptual IA map**, not asserted official tab names. Match actual app captures before coding tab identity.

**2026-10-27 project reference mapping from the user-supplied Polestar China 4.25.0 screenshot:** the **second icon from the left** in the captured lower navigation is adopted as the PsAssistant **Vehicle Control / 车控** tab reference; the **third icon from the left** is adopted as the PsAssistant **Vehicle Data / 车辆数据** tab reference. These are screenshot-derived project reference traces, not claimed original Polestar SVG masters. Preserve their apparent proportions and visual weight instead of substituting unrelated Material symbols.

Implementation assets:
- `R.drawable.ic_nav_vehicle_control_ref` → Vehicle Control / 车控.
- `R.drawable.ic_nav_vehicle_data_ref` → Vehicle Data / 车辆数据.
- Artwork box: **19–21 dp** inside the current bottom-navigation button depending on compact layout; minimum touch target remains **≥48 × 48 dp**.
- Inactive: use the navigation muted semantic color. Active: use the current PsAssistant active accent; do not alter the icon geometry between states.
- Keep icon + label vertically centered; compact layouts may reduce artwork size but must not distort aspect ratio.

---

## 8. Iconography specification

### 8.1 Observed design cues [B]

Line icons are thin, functional, readable without colorful pictorial treatments. Common functions include locked/unlocked door, climate fan, charging, car, parked location, settings, profile, timer, service and external navigation. Exact stroke widths or SVG path commands are **not** disclosed publicly.

### 8.2 Proposed engineering rules [C]

- Viewbox: **24 × 24 dp**. Default visual stroke: **1.5–1.75 dp**; system-proportional large icons may be **2 dp** at 32 dp viewbox.
- Optical margins: **2–3 dp** from typical viewbox edges, adjusted by individual glyph.
- Stroke cap and join: use consistent rounded or minimally softened appearance within one family; do not mix outline and filled icons arbitrarily.
- Do not rely on stroke/color alone to convey critical state. Pair commands with text such as **Locked**, **Off**, **Starting**, **Unknown**.
- Hover/focus/pressed must remain OS-accessible; hit targets should be **≥48 × 48 dp** on phone and watch. [S12][S11]

### 8.3 Suggested icon inventory [C]

`vehicleControlReference`, `vehicleDataReference`, `vehicle`, `doorsLocked`, `doorsUnlocked`, `climateFan`, `charge`, `battery`, `location`, `timer`, `account`, `software`, `vehicleSettings`, `carWindow`, `lights`, `horn`, `service`, `arrowExternal`, `refresh`, `close`, `chevron`, `more`.

**Do not confuse a design set with guaranteed supported features.** A model may lack windows, climate target-setting, or trunk/flash actions. Functional icons must be hidden/disabled according to a verified capability map, not merely because the pictogram exists.

---

## 9. Widgets — Android home screen and lock-screen reference

### 9.1 Supported evidence [A/B]

Google Play's promotional gallery explicitly includes a home/lock-screen widget visual, with a compact horizontal metric card and multiple vehicle shortcuts. [S5] The gallery does not reveal the full underlying widget layout definitions, device-specific rounded-corner values or every launcher size.

The Apple iOS version history also documents widget customization features, but these are **iOS features**, not proof that Android's app has an identical WidgetKit implementation. [S6]

### 9.2 Proposed Android Glance sizes and states [C]

| Responsive bucket | Illustrative launcher span | Suggested display | When space is limited |
|---|---|---|---|
| Expanded card | **4 × 2** | Large SOC, range/progress, up to four actions | Reduce low-priority details; keep metrics and controls in distinct zones. |
| Wide banner | **4 × 1** | Large SOC, range and up to two actions | Maintain a one-row hierarchy and shorten control names. |
| Compact card | **2 × 2** | Enlarged SOC, range and up to two actions | Increase key metric/icon scale while removing optional text. |
| Mini banner | **2 × 1** | SOC, abbreviated range and one action | Keep one clearly separated shortcut. |
| Micro concept | **1 × 1** | One metric **or** one action, not both | Optional launcher-dependent design experiment, not a verified official Polestar variant. |

**Important:** widget grid cell sizes vary by launcher. These spans are *names*, not fixed pixel widths. Android's responsive size layouts and `Glance` size modes are the correct architecture. [S10][S14] The 1 × 1 layout is conceptual and must be gated on actual launcher support; 2 × 1 support also requires verification on each target launcher.

### 9.3 Recommended anatomy [C]

```
+--------------------------------------------------+
| POLESTAR 2                         Updated 2m ago |
| 80%                     315 km                    |
| ========================--------                |
| [Lock]     [Climate]     [Charge]    [Locate]     |
+--------------------------------------------------+
```

This ASCII diagram expresses hierarchy, **not original proportions or approved icon SVGs**. Use system-native widget corner radii, correct content padding and minimum targets.

### 9.4 Widget behavior [C]

- Cached vehicle snapshot may render instantly, **clearly timestamped**.
- On refresh, show refresh indicator without erasing the last trustworthy value; if telemetry is too old or contradictory, mark as **Last known** / **Unavailable**.
- A lock/unlock action must show **Sending** until verified acknowledgement; never optimistically switch the physical lock state to completed.
- If offline, show an explicit recovery path and preserve a safe cached state.
- Keep widgets privacy-aware: support hiding location, plate/VIN and sensitive status on an exposed home/lock screen.
- Avoid needless background polling. Work within Android App Widget/Glance lifecycle rules and launcher budgets.

---

## 10. Smartwatch, Apple Watch and Wear OS

### 10.1 Official platform scope [A]

| Platform | Verified source | What the evidence supports |
|---|---|---|
| **Apple Watch / watchOS 26** | Polestar iOS 5.1.0 release notes [S6] | Supports quick controls such as climate preconditioning and vehicle lock. Not proof of a separate complete Polestar watch navigation. |
| **Earlier Android Wear / Polestar 1** | Official Polestar 1 manual [S7] | Lists battery, lock, location, parking heat, subject to feature availability and older compatibility. |
| **Current Wear OS + Google Play `com.polestar.explore`** | No verified Wear OS page or current official screenshot in the reviewed references | **No claim of an existing official Wear OS 2026 screen set.** This guide defines a proposed adaptation. |

Apple's watchOS 26 implementation can expose controls from an iPhone app to the Watch **even without a separate Watch app**, according to Apple developer guidance. Therefore do not infer a native watch application from a Control Center action. [S8]

### 10.2 Proposed Wear OS interaction hierarchy [C]

**At a glance**: Battery SOC large, range smaller, one or two easy-to-tap actions.  
**Lock screen/action**: One icon, clear text of *observed* lock state, explicit command intent and confirmation.  
**Climate screen**: Available mode, start/stop, status freshness.  
**Pending state**: In-flight progress + cancellation guidance if appropriate; timeout must produce unknown/failed without claiming command success.

### 10.3 Wear geometry [C + platform guidance]

- Primary visual container: circular screen; honor arc margins rather than filling every corner.
- Typical visual sample board: **192–225 dp** diameter. The actual device value and scalable layout define placements.
- Minimum touch target **48 × 48 dp**. Google describes even larger controls in media-oriented Wear patterns. [S11]
- Use one primary action per interaction view; avoid a four-column phone grid on a watch.
- Favor platform fonts for compact legibility until actual licensed typeface rendering is reviewed.
- Use watch haptics only as an acknowledgement of **tap/request**, never as an indication the vehicle command succeeded unless confirmed.

**Designs shown in optimized PDF pages 18–19 are independent design proposals, not screenshots of an official Polestar Wear OS app.**

---

## 11. Interaction states and remote-command truthfulness

A vehicle app's appearance must correspond to its actual execution status. This is crucial for an Android adaptation where authorization/session refresh, background connection and remote commands may respond at different times.

| State | UI message | State ownership | Forbidden behavior |
|---|---|---|---|
| `known` | "Locked · Updated just now" | Verified server/car telemetry | Do not imply freshness when timestamps are unknown. |
| `stale` | "Last known: Locked · 12 min ago" | Cached snapshot | Do not use the same visual state as a fresh confirmed reading. |
| `pending` | "Sending lock command…" | Remote command request | Do not show "Locked" before acknowledgement. |
| `confirmed` | "Vehicle locked" | Acknowledged command + reconciled telemetry where possible | Do not infer from HTTP 200 alone if the API only accepted a job. |
| `unknown` | "Vehicle status unavailable" | No trustworthy observation | Do not transform network/token errors into observed physical changes. |
| `failed` | "Command not confirmed · Retry" | Explicit failure or timeout | Do not falsely promise the command had no physical effect; it may be ambiguous. |
| `reauth` | "Session needs attention" | Auth/session subsystem | Do not erase cached confirmed vehicle data solely due to a refresh attempt. |

**Implementation flow:** `tap → verify capability / session → submit → pending → acknowledge → reconcile → confirmed or unknown`. Record user-facing timestamps. Use distinct telemetry and command-request models so authentication errors do not corrupt displayed vehicle state.

### Important for a China/global companion implementation [C]

Reuse this **design system** across China/Global only at the UI layer. Do **not** assume the Google Play global app authenticates against the same backend, returns the same vehicle fields or permits the same commands as a China-market app. Region-specific APIs, vehicle-capability flags and consent requirements are separate implementation concerns.

---

## 12. Motion, feedback, language, accessibility

### 12.1 Copy style [C]

Short, technical, neutral phrases: **Battery level**, **315 km**, **Doors locked**, **Climate off**, **Charge limit**, **Last updated 2 min ago**, **Starting climate…**. Avoid emotional promotional text in control states. Avoid ambiguous "Done" messages that do not state *what* changed.

### 12.2 Proposed motion timing [C]

| Motion | Proposed range | Purpose |
|---|---:|---|
| Standard text/status fade | 100–180 ms | Reduce visual discontinuity. |
| Tile content state transition | 140–220 ms | Perceived continuity without decorative bounce. |
| In-flight indicator | Duration of true operation | No arbitrary fake completion time. |
| Success feedback | 300–600 ms, then steady state | Make the changed state legible before the toast disappears. |

### 12.3 Accessibility [C, aligned with Android guidance]

- Phone and wearable **touch targets ≥48 × 48 dp**; distinguish icon drawing size from actual touch area. [S12][S11]
- Normal body text target **WCAG 4.5:1**; larger text target **3:1**. Re-check sampled color combinations.
- Support larger font sizes, screen readers, reduced motion and dynamic OS insets.
- Describe icons with `contentDescription` and concrete state labels; do not use only accent color to signal locking/climate/charge.
- Ensure progress, loading, offline and error are announced to accessibility services.

---

## 13. Android implementation tokens (proposed)

```json
{
  "$schema": "https://design-tokens.github.io/community-group/format/",
  "metadata": {
    "name": "Psa reference tokens",
    "status": "PROPOSAL_NOT_OFFICIAL",
    "asOf": "2026-10-26",
    "fontLicense": "Original Unica77 Polestar Var required for production"
  },
  "colors": {
    "ink": "#151617",
    "charcoal": "#27282A",
    "canvas": "#F3F3F1",
    "white": "#FFFFFF",
    "midGray": "#777A7C",
    "actionOrangeApprox": "#FF6B00",
    "archiveYellowApprox": "#FFE72D",
    "successSemantic": "#22764A",
    "errorSemantic": "#B62E28"
  },
  "typography": {
    "productionFamily": "Unica77 Polestar Var — licensed original",
    "prototypeFamily": "Roboto",
    "displayMetricSp": 48,
    "screenHeadingSp": 30,
    "bodySp": 16,
    "supportSp": 12,
    "microSp": 11
  },
  "mobileDp": {
    "referenceWidth": 360,
    "referenceHeight": 780,
    "screenMargin": 20,
    "gridGutter": 8,
    "spacing": [4, 8, 12, 16, 24, 32],
    "iconArtwork": 24,
    "minTouchTarget": 48
  },
  "widget": {
    "sizeMode": "Responsive",
    "buckets": ["4x2", "4x1", "2x2", "2x1", "1x1_experimental"],
    "surfaceModes": ["opaque", "semi_transparent", "transparent"],
    "illustrativeSurfaceAlpha": {"opaque": 1.0, "semi_transparent": 0.76, "transparent": 0.0},
    "showFreshness": true,
    "acknowledgeBeforeSuccess": true
  },
  "wear": {
    "status": "CONCEPT_UNVERIFIED_OFFICIAL_WEAR_OS_UI",
    "referenceDiameterDp": [192, 225, 240],
    "minTouchTargetDp": 48
  }
}
```

**Production implementation guidance:**

1. Replace approximate tokens from audited official assets before release. Keep evidence grade and dates in token comments.
2. Implement Compose UI against a screen-specific `ViewState` and stable reusable components, not screenshots.
3. Use Jetpack Glance `SizeMode.Responsive` or appropriate RemoteViews layouts for Android widgets. [S10]
4. For watchOS, the controls supported via Apple's Control APIs are not the same implementation as a Wear OS app. [S8]
5. Separate visual snapshot tests from API correctness tests; passing one does not validate the other.
6. Provide original files (`.svg`, `.png` or Android vector drawable) only after rights and source verification.

---

## 14. Visual QA and acceptance checklist

| Area | Test | Pass criteria |
|---|---|---|
| Identity | Compare symbol to approved vector | No traced/approximate logo in shipping build; same negative space and optical position. |
| Adaptive icon | Round/squircle/themed masks | All critical geometry within centered safe area; no unintended crop. |
| Font | Licensed font and typographic master | Typeface + tracking + kerning approved; fallback clearly documented. |
| Color | Actual source asset sampling | Palette values tied to source version; contrast passes. |
| Main dashboard | 360/393/412 dp + short heights | No tile clipping, no overlapped figures, states visible. |
| Climate/charge | Feature and region matrix | Unsupported operations hidden or explained. |
| Widgets | 4×2, 4×1, 2×2, 2×1, experimental 1×1 × opaque/semi/transparent | Metric and actions remain readable and tappable; different layouts by size and widget surface treatment. |
| Watch | 192, 225, 240+ dp round screens | Round margins, safe areas, ≥12sp essential type, ≥48dp targets and pending behavior. |
| Accessibility | Larger text and screen reader | Important information never conveys meaning by color alone. |
| Remote actions | Delay, timeout, expired token, no response | UI never claims success from an unconfirmed result. |
| Image assets | Vehicle model and aspect ratio | No distortion, mirroring or model mismatch. |

**Pixel-perfect finalization protocol:** acquire the official vector assets and 1:1 screenshots from the target October 2026 app version; record device size/density, app release, region, and vehicle model; collect screenshots in each state; align Figma frames with real safe insets; inspect original glyph sizes; run screenshot diffs with a documented crop mask excluding clock and dynamic values. Do not treat the diagrams in the PDF as a pixel-equal benchmark.

---

## 15. Optimized edition: visual asset corrections and publication policy

### 15.1 Logo fidelity and large-format reference

- **Primary geometry reference:** the official public Google Play icon and the published Polestar brand case/guideline.
- **Optimized PDF high-resolution rendering:** the logo shown at large size in the companion PDF uses scalable paths derived from the *appearance* of the public icon. This prevents PDF raster pixelation; the derivative paths are not official Polestar artwork.
- **Master asset requirement:** use a Polestar-issued SVG/AI master before production or branding. Do not make the inferred trace a licensed logo.
- **Quality distinction:** at least three labels should be kept separate in a design repository: `PUBLIC_OFFICIAL_SCREENSHOT`, `DERIVED_REFERENCE`, and `APPROVED_PRODUCTION_ASSET`.

### 15.2 Phone and vehicle imagery

- Use source imagery with its original intrinsic aspect ratio. A proportional inset is preferable to cropping a key vehicle detail.
- Phone screenshots shown in the PDF are public *marketing captures* that can have low intrinsic resolution. They remain useful for composition; they do not establish exact runtime layout values.
- A current authenticated Android device capture is required before final pixel-exact QA. Record app build, device density, region, model, screenshot dimensions, font scale and OS insets.

### 15.3 Brand permissions

Polestar's published **Branding guidelines — External parties** (11 October 2023) describes its controlled assets as including the name, wordmark, symbol, cars, pictures, icons, videos and related materials. It states that third-party use of its visual brand assets requires prior written permission. This guide is a study/reference document and does not grant such permission. [S13]

---

## 16. Watch UI: official functional evidence vs proposed layout

### 16.1 Functional scope

Polestar's **Polestar 1** public manual describes the following historic wearable functions: battery level, locking the car, vehicle location and parking heating. It does **not** verify a current official Wear OS visual design for the Google Play `com.polestar.explore` app. [S7]

### 16.2 Proposed watch surfaces

| Surface | Main content | Interaction | Source grade |
|---|---|---|---|
| Overview | SOC + range + two shortcuts | Tap into a dedicated control screen | C based on A functional scope |
| Lock | Verified lock state + one action | Explicit request → pending → confirmation | C |
| Climate | Climate mode + start/stop | Show command progress and stale state | C |
| Location | Vehicle location freshness + navigation handoff | Deep link to phone/map | C |

### 16.3 Wear OS geometry and quality gates

- **Small first:** begin at **192dp** round diameter, then adapt around **225dp** and larger sizes. [S15]
- **Readable type:** essential text **≥12sp**, secondary **≥10sp**, with support for OS font scaling. [S16]
- **Touch targets:** **≥48×48dp** for actionable controls. [S16]
- **Safety:** no clipped content, text overlaps, or crossing the round-screen boundary. [S16]
- **Tiles:** one primary task per tile; avoid hardcoding a Polestar logo in the tile because Wear OS may insert the app icon automatically. [S17]

### 16.4 Command states

`READY → REQUESTED → SENDING → CONFIRMED | NOT_CONFIRMED | NEEDS_REAUTH`

Only authoritative acknowledgement should yield `CONFIRMED`. A timeout may mean **unknown outcome**, not necessarily a physically unchanged car. The watch examples on optimized PDF pages 18–19 are conceptual.

---

## 17. Widget matrix: 4×2 / 4×1 / 2×2 / 2×1 / experimental 1×1

### 17.1 Distinct density classes

| Variant | Hierarchy | Suggested controls | Support status |
|---|---|---|---|
| **4×2** | SOC, range, progress, actions | Lock, climate, charge, location | Android platform widget size class; layout is proposal |
| **4×1** | SOC, range, progress, two actions | Lock, climate | Wide compact banner reference; layout is proposal |
| **2×2** | SOC, range, two actions | Lock, climate | Android platform widget size class; layout is proposal |
| **2×1** | SOC, short range, one action | Example: lock shortcut | Compact banner reference; launcher-dependent |
| **1×1** | **Single quick action** in the PDF example | Lock | **Independent design concept**; launcher-dependent and not verified as a standard/default size |

The current Polestar Google Play listing contains a public widget graphic, but it does not provide all of the widget source layout variants. The five compositions shown in PDF v5 are therefore design proposals based on a shared brand vocabulary and Android widget-sizing guidance. [S5][S14]

### 17.2 Proposed responsive behavior

- **4×2:** expanded controls with the most prominent SOC text, a large charge progress bar, large icons, four distinct actions and a legible range label. Keep the metric and controls in separate visual zones (optimized PDF p.14).
- **4×1:** one-row wide banner with a left metric region and a right two-action region (Lock/Heat in the PDF sample). Do not squeeze a 4×2 layout into one row (p.14).
- **2×2:** enlarged SOC, range and two actions; remove low-priority copy instead of shrinking the core number or icons to unused space (p.15).
- **2×1:** keep one action, a clear SOC and abbreviated range; the sample is a mini banner (p.15).
- **1×1:** in the optimized PDF the card contains a single **LOCK** action. Do not combine that action with an additional numeric metric or a second shortcut (p.15).
- **Scaling rule:** 4×2 and 2×2 should use their available bounds more generously than the former draft. Scale the SOC, range, iconography and spacing as a coordinated system while preserving readable separation; avoid either a tiny central cluster or overlapping controls.
- Use actual `appWidgetOptions` or Glance `SizeMode.Responsive` breakpoints instead of assuming grid cells have uniform dp dimensions.
- Fill the allocated launcher area; do not add nonstandard external container padding. [S14]

### 17.3 Widget surface appearance

| Surface treatment | Example backing alpha in PDF | Intent | Guidance |
|---|---:|---|---|
| **Opaque** | **100%** | Safest default | Stable card backing; consistent text and icon contrast. |
| **Semi-transparent** | **76%** | Controlled wallpaper blend | Illustrative opacity only. Test actual alpha and contrast on multiple wallpapers. |
| **Fully transparent** | **0%** | Minimal chrome / wallpaper-led | No filled backing. Text and icon visibility must be verified on busy, light and dark wallpapers. |

All three variations use the **same content arrangement** and a **shared wallpaper example** in the optimized PDF (p.16), so the comparison isolates background treatment. The **76%** value is an **illustrative design value**, not an official Polestar token. These transparent variants are independent proposals, not verified official releases. Do not assume a high-contrast version on one wallpaper remains legible on a different launcher or wallpaper.

### 17.4 Optimized widget acceptance matrix (PDF p.17)

| Surface / state | Required visible content | Layout or behavior constraint |
|---|---|---|
| 4×2 | SOC, range, four actions | Large type and up to four separate touch targets |
| 4×1 | SOC, range, two actions | Preserve metric/action separation |
| 2×2 | SOC, range, two actions | Increase metric and icon scale; maintain non-overlapping targets |
| 2×1 | SOC, abbreviated range, one action | Abbreviate labels before reducing the main type size |
| 1×1 (concept) | Exactly one action or one metric | Gate on actual launcher support |
| Offline | Last-known sync indicator + unavailable state | Never present old telemetry as currently live |
| Pending command | Sending / awaiting confirmation | Do not show success until acknowledged |

**Cross-device requirement:** dynamic font scaling, localization and long vehicle names should not cause clipped labels or overlap between metrics, icons or buttons. Use adaptive content density; maintain clear action feedback independently of the opacity setting.

### 17.4 Critical state rules

- Cached values must be timestamped, not rendered as live without evidence.
- A command should remain pending until acknowledged.
- A stale/offline widget must offer a clear and safe recovery path.
- Location and other sensitive information should not be exposed unnecessarily on lock screen surfaces.

---

## 18. Layout safety and PDF verification

The optimized 22-page PDF specifically revises the widget pages to avoid collisions between enlarged SOC text, progress bars, action icons, labels and panel notes, while maintaining proportional source screenshots on phone pages. The design review workflow is:

1. Render every PDF page to PNG at high enough DPI to inspect text spacing.
2. Inspect titles, labels, card edges, phone screenshots and circular watch bounds.
3. Programmatically flag intersecting text rectangles and repair flagged pages.
4. Verify that all smartphone source images use proportional sizing, not unintentional `cover` crops.
5. Check the five widget variants (4×2, 4×1, 2×2, 2×1, 1×1) independently at realistic launchers/bounds, and compare all three opacity modes on dark, bright and complex wallpapers.
6. Re-render after changes and keep an audit record.

**All dimension tokens above are engineering proposals**, not claims of a licensed Polestar design source.

---

## 19. Handoff matrix for Android implementation

| Domain | Required asset / component | Verification |
|---|---|---|
| Identity | approved logo master; launcher layers | master parity, adaptive safe region |
| Imagery | official model-specific vehicle render set | model/angle/variant and aspect ratio |
| Typography | licensed production font; prototype fallback | line height, kerning, dynamic type |
| Icons | approved SVG or source drawable exports | 24dp art, 48dp touch, semantics |
| Mobile UI | states, insets, responsive layouts | screenshot diff on target devices |
| Widgets | 4×2, 4×1, 2×2, 2×1, optional 1×1 | viewport-specific density, timestamps, surface opacity variants |
| Wear OS | tile/app states and deep links | minimum target, safe circle, action feedback |
| Remote commands | request status and vehicle reconciliation | no unsupported or unconfirmed success |

---

## 20. Source register

| ID | Source | Relevance |
|---|---|---|
| [S1] | [Stockholm Design Lab — Polestar](https://www.stockholmdesignlab.se/work/polestar) | Authoritative brand case, symbol, marking system, typography, historical visual gallery. |
| [S2] | [Polestar — Journal 1](https://www.polestar.com/global/news/journal-1/) | Brand intent, Pure/Progressive/Performance, history of symbol, Unica77, black/white/grey, marking. |
| [S3] | [Lineto — Polestar bespoke typography](https://lineto.com/bespoke/polestar) | Unica77 customization, screen optimization and multi-script details. |
| [S4] | [Google Play — Polestar official app](https://play.google.com/store/apps/details?hl=en-US&id=com.polestar.explore) | Android package, publisher, current app description, listing update and published materials. |
| [S5] | [Official Google Play screenshot carousel](https://play.google.com/store/apps/details?hl=en-US&id=com.polestar.explore) | Current published Android visuals, feature screens and widget image. |
| [S6] | [Apple App Store — Polestar version history](https://apps.apple.com/sg/app/polestar/id1451196635?platform=watch) | Watch controls and iOS widget behavior, plus evolving model features. |
| [S7] | [Polestar 1 manual — wearables](https://www.polestar.com/us/manual/polestar-1/2020/article/bdcb3a2ddf550ce9c0a801515931ab43) | Historical Android Wear / Apple Watch functions for Polestar 1 only. |
| [S8] | [Apple Developer — What's new in watchOS 26](https://developer.apple.com/videos/play/wwdc2025/334/) | Controls can appear on Apple Watch without a full native Watch app. |
| [S9] | [Android Developers — adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive) | 108 × 108 dp layers and centered 66 × 66 dp safe zone. |
| [S10] | [Android Developers — flexible widget layouts](https://developer.android.com/develop/ui/views/appwidgets/layouts) and [Glance](https://developer.android.com/develop/ui/compose/glance/create-app-widget) | Responsive widget layouts and launcher sizing limits. |
| [S11] | [Android Developers — Wear control guidelines](https://developer.android.com/design/ui/wear/guides/patterns/media/controls) | Watch controls, tap-target and device-scale conventions. |
| [S12] | [Android Developers — accessibility / touch targets](https://developer.android.com/guide/topics/ui/accessibility/apps) | 48 × 48 dp minimum target guidance. |
| [S13] | [Polestar — Branding Guidelines, External Parties (PDF, 2023)](https://www.polestar.com/dato-assets/11286/1712578923-231011_polestar_branding-guidelines-external.pdf) | Official external policy on brand ownership and written permission for logos, cars and visual assets. |
| [S14] | [Android Developers — Widget Sizing](https://developer.android.com/design/ui/mobile/guides/widgets/sizing) | Adaptive size classes, fill-bounds discipline and breakpoints. |
| [S15] | [Android Developers — Wear OS Screen Sizes](https://developer.android.com/design/ui/wear/guides/m2-5/foundations/screen-sizes) | 192dp small-first design, 225dp breakpoint and adaptable margins. |
| [S16] | [Android Developers — Wear OS App Quality](https://developer.android.com/docs/quality-guidelines/wear-app-quality) | 48dp minimum targets, ≥12sp essential text, clipping/overlap criteria. |
| [S17] | [Android Developers — Wear OS Tile Design Principles](https://developer.android.com/design/ui/wear/guides/m2-5/surfaces/tiles-principles) | One primary task per tile; avoid duplicating the system app icon. |

**Copyright and intended use (optimized edition):** Polestar names and published artwork are the property of their respective rights holders. This is an independently authored research/design-reference document, not a brand certification, agency deliverable or claim of access to Polestar's proprietary Figma files. Use externally hosted images for reference; request licensed production assets from the rights holder for app distribution.


## Psa Android v0.8.4 user-approved amendment

The monochrome front-car master, launcher/notification rules, lock/tailgate glyphs and icon-free equal-size condition/camping buttons now follow [Psa Icon Specification v2](Psa_Icon_Design_Specification_v2.md). This supersedes conflicting earlier launcher-icon guidance; protocol and consent behavior follow [v0.8.4](Psa_v084_Update.md).


## Psa v0.9.4 motion and typography amendment

[Native motion and offline test specification](Psa_v094_Effects_And_Assets.md) supersedes previous motion and gallery notes. PS Default uses the battery percentage type size for All Good!; other skins enlarge the status label without wrapping. Solid orange charging particles, blue cooling, orange heating, green laminar ventilation/purification, and neutral live climate without a verified mode field. Native fixed pools only; pause hidden/background/power saver/system-disabled animation. Settings provide local simulation, 18-case autoplay, and immediate parameter controls, with no vehicle commands. Lossless alpha image resources retain source aspect ratio and do not change vehicle opacity.

## v0.9.5 Simple 2 and motion addendum

This addendum supersedes the historical frame caps and fixed command completion time. See [v0.9.5 specification](Psa_v095_Update.md).

Simple 2 uses a single energy/vehicle card, paired control cards, shared condition/charging slot, three priority metrics and full-details access. Day/night vehicle selections and a three-second vehicle hold are supported in every skin. Existing vehicle pixels are unchanged.

Airflow has a crisp leading tip and continuously fading tail; both through-car and cabin-absorption directions are available. Glass-region gradients use VI orange for explicitly chosen heating visuals, blue for cooling and green for purification. HVAC color selection is visual only, never inferred vehicle telemetry. Native vsync scheduling replaces the former 20fps cap; pause all effects off screen or in power saver.

Use a 230ms directional page slide and a 220ms translucent gradient dialog entrance. Android 12+ system blur is optional; older systems use a visual fallback. Experimental parked-map backdrops default off and use a single temporary snapshot with monochrome day/night grading and radial blur. Remote provider DOM compatibility requires device validation.

Display only concise command progress and result. Stop remaining read requests after three consecutive validated vehicle states; do not repeat vehicle writes. Parking labels show a resolved place name, fall back to reported coordinates, or state that parking data has not updated.

## v0.9.6-beta3 Map backdrop and particle depth

Background-only map treatment is borderless and proportionally covered. Hide share-page controls, retain provider attribution, and move the entire map with its parking marker when adjusting focal position. The default marker is VI orange (#FF6B00); its size, color and screen focus are user-adjustable without changing geographic coordinates. Interactive secondary maps retain their own controls.

Within the selected behind-car particle percentage, assign larger particles behind the car first. Keep the smaller particles above the vehicle to reduce body occlusion. Preserve user-selected depth percentages, particle counts and motion settings; cache depth ranking instead of allocating or sorting every frame.
