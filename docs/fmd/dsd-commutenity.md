# Design Specification: CommuteNity

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.4
**Owner:** Implementer
**Status:** Draft
**Last reconciled:** 2026-10-09
**PRD:** [Product requirements](prd-commutenity.md)
**Figma brief:** [Figma update brief](figma-brief-commutenity.md) (design source of truth is this DSD; Figma follows it, [D29](state.md#5-decisions))

## 0. Brand Stance

- **Mode:** a task-first product.
- **Reference:** the jeepney itself: painted signboards, route names in bold caps, the warm livery, and the rider's shout of *"Para!"*.
- **Voice:** a kind *suki* who knows the route. Short, comfortable with Taglish, never cute at the cost of clarity.
- **Human detail:** every trip says where to say "para", using the landmark people actually see.
- **Place:** Makati City only ([D20](state.md#5-decisions)). Copy never claims wider coverage.

The **"On-device · Offline OK"** badge belongs to the brand. It makes the Local AI value visible, and it tells the truth: it shows only when the commute pack and the map pack are loaded.

**Community labels** ("Community · 12 👍") make the people behind the alternatives visible without turning the app into a social feed.

Carried over from the CommuteNity "Jeepney Gold" baseline: amber primary, achromatic base, and the transit mode palette. Avoid glowing AI gradients, stock photos, and chat-bubble clutter.

## 1. Surface and Hierarchy

A native Android app (Jetpack Compose, Material 3), phone portrait only. Design frame: Android compact, 412 × 915 dp.

The map is the app. Everything else floats over it or slides up from the bottom.

### 1.1 Map home (T0)

Full-bleed map ([D23](state.md#5-decisions)), always the root screen.

1. **Top overlay** (over the map, on surface chips): CommuteNity wordmark, `OfflineBadge`, `RefreshStatus` icon.
2. **`TripBuilderCard`** under the top overlay: two `SearchField`s, A ("Saan ka manggagaling?", with "use my location") and B ("Saan ka pupunta?"). It collapses to a one-line `A → B` chip when the trip sheet is half or expanded; tap the chip to expand it.
3. **Pins:** `PinMarker` A and B on the map. Tap the map to place the next empty pin (A, then B). Drag a pin to move it. Tap a field to search pack places by name. A "my location" button sits bottom-end above the sheet.
4. **`TripSheet`** (bottom sheet) with states `collapsed`, `half`, `expanded`. Collapsed is a peek of about 96 dp; half is 50 % of screen height; expanded is 80 % and always leaves a map strip.
5. **Ask bar** (secondary input, MVP per [D31](state.md#5-decisions)): a compact `AskBar` pill, collapsed ("Magtanong…" with mic), directly under `TripBuilderCard`. It stays under the `A → B` chip when the card collapses, and it is visible whenever the map home or the best-trip sheet is shown (a focused `SearchField` and its results cover its spot while typing). It never replaces the pins or the fields: the map trip builder stays the primary way in.
6. **OSM attribution** ([§1.7](#17-osm-attribution)) on the map, bottom-end, riding the sheet's top edge.

Setting A and B computes the best trip on the phone, with no network, and opens the sheet at `half`.

### 1.2 Best-trip sheet (T0)

`TripSheet` content, top to bottom:

1. **`TripSummary`:** total fare, total minutes, distance, total walk time, transfers; `ReasonChip` (at most 4 words, e.g. "Fewest transfers").
2. **Ordered `LegRow`s**, one per leg: `ModeChip`, "Sakay sa" (board) stop, "Para sa" (alight) stop, fare, minutes, distance, walk time for walk legs, and the signboard text to look for on jeepney, bus, and UV legs.
3. **`ParaPointRow`:** the para point as a landmark, "Para sa <landmark>".
4. **Actions:** "Start trip" (T2), "Show alternatives" (T1), `VoteButtons` (T1), "Suggest a trip" (T1).

**On the map**, the best trip is drawn as `MapLine`s that follow stored road shapes ([D22](state.md#5-decisions)), one per segment, colored by mode (never a straight line between stops). `PinMarker` `stop` marks boarding and alighting stops; the para stop is marked as the para point. Selecting a `LegRow` highlights its line and fits the camera to it.

**First/last-mile walk:**
- Online: a foot route fetched via our server and cached, drawn as a solid `walk` `MapLine`.
- Offline: a dashed straight `MapLine` (`dashed-walk-offline`) with a `WalkLabel` "walk ~N m". This is the only straight line the app draws.

**Sample data:** a `SourceLabel` `sample` sits beside every value that comes from `mock` data. Real values never carry it ([QA-17](qad-commutenity.md#6-release-criteria)). The demo never hides it. The hero pair is real ([D30](state.md#5-decisions), amended by [D37](state.md#5-decisions)): A = "V.A. Rufino St" (Legazpi Village), B = "Dela Rosa St" (Pio del Pilar). Until the candidate trips are listed ([A13](state.md#4-open-assumptions)), the legs' routes, fares, and minutes in examples and mockups are placeholders marked `TBD (A13)` and carry `sample`.

**Unknown values:** a missing fare or minutes shows "unknown" for that value and the total is marked unknown. Never show a partial total ([QA-19](qad-commutenity.md#6-release-criteria)).

### 1.3 Alternatives, suggest, vote (T1)

- **Alternatives list:** "Show alternatives" switches the sheet to a ranked list of at most 5 `AltRow`s. Each row has a `SourceLabel` (Algorithm, or Community with votes), the reason, fare, and minutes. Tap a row to expand its legs and preview its road-following line on the map; "Use this trip" makes it the active candidate.
- **Suggest a trip:** a full-screen leg builder (`LegBuilderRow`s). Pick a route, then board and alight stops from pack lists, add legs, add an optional note, save. Invalid legs show the reason inline.
- **Vote:** `VoteButtons` (👍 worked / 👎 didn't) on any trip. The state is filled once voted, with an "unsynced" dot until synced. Votes toggle.

### 1.4 Refresh and sync status (T1)

`RefreshStatus` lives in the top overlay and opens a status sheet: commute pack version, map pack version, last refresh time, pending contributions, and "Refresh now". It covers two things: **refresh** (commute pack, map pack, community data, and foot routes fetched when online; [D24](state.md#5-decisions)) and **sync** (contributions only, pushed through the [D17](state.md#5-decisions) function).

- First-run or map-pack download shows size, percent, and text progress. Interrupted downloads resume or retry. Never shows "ready" until verified.
- A failure or offline state never blocks the UI: the last good commute pack and map pack keep working, and the status says so.

### 1.5 Active-trip tracking mode (T2)

"Start trip" makes the trip the **active trip** and starts tracking ([D25](state.md#5-decisions)). The screen becomes tracking mode:

1. **Map:** camera follows the rider's location puck; the trip's line is drawn by mode with the part already travelled at 40 % opacity.
2. **`RouteStatusBanner`** at the top: `on` ("Nasa tamang ruta ✓"), `off` ("Lumihis ka sa ruta · N m"), or `gps-lost` ("Walang GPS. Hinahanap ulit…"). It shows no on/off claim while GPS is lost.
3. **`ActiveTripPanel`** at the bottom (not draggable beyond `half`): `TripProgress` (progress along the trip), the current `LegRow` (state `current`), distance and stops to the para point, and the next leg as a preview. "End trip" asks for confirmation.
4. **`ParaAlertBanner`:** ~300 m before the alight stop ([A16](state.md#4-open-assumptions) tunes this), a heads-up banner at the top of the screen, above `RouteStatusBanner`, plus vibration and a system heads-up notification (so it works with the screen off). It fires once per para point. **No sound, no TTS.**
5. **Simulated GPS:** when the demo runs a recorded mock-GPS route, a `SimulatedTag` "Simulated GPS" shows on the panel and the screen is never presented as live.
6. **Permissions:** location (and notifications, on Android versions that ask) are requested with a one-line reason before the first trip. Denied: tracking is unavailable, the trip sheet still works.

### 1.6 Ask in words, voice, correct-vehicle check (T3)

- **`AskBar`:** secondary input, part of the MVP ([D31](state.md#5-decisions)); the label "T3" is only the tier name. Collapsed pill "Magtanong…" with mic, under `TripBuilderCard`; expanded text field, mic, submit, and example questions built from pack places, e.g. "Paano pumunta sa Dela Rosa St., Pio del Pilar galing V.A. Rufino?" / "How to get from V.A. Rufino St to Pio del Pilar?". Submitting sets pins A and B and shows what it understood as chips; an ambiguous place opens the place picker. Disabled with progress while the model loads. The model failing to load never blocks the map builder.
- **Voice (`MicButton`, optional, F9):** listening, then transcribing; the transcript fills the field for confirmation before use ([D26](state.md#5-decisions)). Mic denied or nothing heard shows a message and leaves the text field usable. Voice is cut before the ask bar.
- **Correct-vehicle check (MVP, F8):** from a jeepney, bus, or UV `LegRow` (on the best-trip sheet and from tracking mode), "Tama ba 'tong sasakyan?" opens a text field (or mic) for the signboard text or route name. The `VerdictCard` returns `ride`, `wrong`, or `unsure` from a deterministic text match ([D27](state.md#5-decisions)). The LLM never decides the verdict.

### 1.7 OSM attribution

"© OpenStreetMap contributors" is visible on the map in every screen that shows it, including tracking mode. It is bottom-end, anchored to the sheet's top edge so it is never covered, at least 12 sp, with contrast per §6. The final text follows the map provider's terms ([A14](state.md#4-open-assumptions)); the `AttributionLabel` component text is a property.

## 2. Theme and Type

- **Primary color:** amber-orange, about `oklch(0.72 0.17 55)` (≈ `#F3821D` in sRGB), mapped to the Material 3 `primary`. Text on primary is the dark neutral (white on this amber fails contrast). Neutral surfaces. Dynamic color off, so the brand stays consistent.
- **Mode palette:** jeepney amber/gold, bus blue, MRT purple, LRT red-orange, UV teal, P2P green, tricycle yellow, walking gray. The same palette colors `ModeChip` and `MapLine`.
- **Source labels:** Algorithm is neutral outline; Community is amber tonal; sample is a small neutral tag with an info icon.
- **Sample data marker:** the `sample` `SourceLabel`. Visible but quiet.
- **Verdict colors:** ride is green, wrong is red, unsure is neutral. Each always has a text label and an icon.
- **Status colors** reuse existing tokens, with no new hues: on route = verdict ride; off route = verdict wrong; GPS lost = verdict unsure (neutral); para alert = primary container; refresh failed = verdict wrong; refreshing = primary; synced and offline = neutral.
- **Map lines:** 5 dp with a 2 dp casing in the surface color so lines read on any map tile; walk-offline is dashed (4 dp dash, 6 dp gap). Proposed values; tune on the POCO X6 5G.
- **Type:** bundled fonts only (no downloadable fonts at runtime). Headings use a geometric display face (Plus Jakarta Sans by default; Space Grotesk if the Figma file already uses it), bold; body uses the system sans (Roboto).
- **Dark mode:** follows the system, including the map style. The amber keeps the same perceived lightness.
- **Shapes:** Material 3 shape scale (4 / 8 / 12 / 16 / 28 dp).

Token names and values are in the [Figma brief §6](figma-brief-commutenity.md#6-figma-variables).

## 3. Layout

- 4 dp base grid and 16 dp screen padding.
- Touch targets ≥ 48 dp, including pins (the tap area is larger than the drawn pin) and sheet handles.
- Leg text ≥ 16 sp with stop names in bold, readable at arm's length on a moving jeep.
- The primary action stays reachable by thumb at the bottom: the sheet, "Start trip", and "End trip" are bottom-anchored.
- Pins never sit under the sheet: the camera fits A and B above the sheet.

## 4. Components and States

Component names are shared with the [Figma brief §5](figma-brief-commutenity.md#5-component-inventory).

| Component | Default | Active | Disabled / error |
|---|---|---|---|
| `OfflineBadge` | "On-device · Offline OK" when pack and map are loaded | — | "Hindi pa ready offline" when either is missing |
| `TripBuilderCard` | Two empty `SearchField`s | Field focused, results list | Compact `A → B` chip once both pins are set |
| `SearchField` | "Saan ka manggagaling?" / "Saan ka pupunta?" | Focus ring in amber | "Wala pa sa data ko 'yan." |
| `PinMarker` | A, B, stop | Dragging (lifted) | — |
| `TripSheet` | `collapsed`, `half`, `expanded` | Dragging between states | Not-in-data and error messages |
| `LegRow` | Mode chip, stops, fare, minutes, distance, signboard | Expanded for notes; `current` in tracking | Unknown values shown as "unknown" |
| `MapLine` | Solid, colored by mode | Selected leg highlighted | Dashed `walk` with `WalkLabel` offline |
| `AltRow` | Source label, reason, fare, minutes | Expanded with legs | "Wala pang ibang ruta. Mag-suggest?" |
| `LegBuilderRow` | Empty leg | Picking route or stop | Invalid leg, with the reason |
| `VoteButtons` | Outline thumbs | Filled, plus an "unsynced" dot until synced | — |
| `RefreshStatus` | Synced (timestamp) | Refreshing (text progress) | Offline or failed: will retry. Never blocks the UI. |
| `RouteStatusBanner` | `on` | `off` | `gps-lost` |
| `ActiveTripPanel` / `TripProgress` | Current leg and distance to para point | Leg changes | "Walang GPS" overlay on the distance |
| `ParaAlertBanner` | Hidden | Shown once at threshold | — |
| `AskBar` (MVP, D31) | Collapsed pill "Magtanong…" under `TripBuilderCard` | Expanded, focus ring in amber | Disabled while the model loads, with progress |
| `MicButton` (T3, optional) | Idle | Listening, transcribing | Mic denied; nothing heard |
| `VerdictCard` (MVP, D31) | — | `ride`, `wrong`, `unsure` | — |
| `StateMessage` | — | — | Not in data, no route, no alternatives, map error, pack error, model error |
| Setup / download | Size and progress | Percent and text | Retry. Never shows "ready" until verified. |

### 4.1 Empty, error, and offline states

| State | Where | Message (Taglish) | Action |
|---|---|---|---|
| No pins yet | Map home | "Pumili ng A at B sa mapa." | Tap map or search |
| Outside Makati | Sheet | "Wala pa sa data ko 'yan. Makati lang muna ang covered." | Move a pin |
| No route found | Sheet | "Walang ruta sa pagitan ng A at B sa data ko." | Move a pin; "Suggest a trip" (T1) |
| No alternatives | Alternatives list | "Wala pang ibang ruta. Mag-suggest?" | "Suggest a trip" |
| Map pack missing or corrupt | Map home | "Hindi ma-load ang mapa." | Retry, or Refresh when online |
| Pack failed to load | Map home | "Hindi ma-load ang commute pack." | Retry |
| Offline | Top overlay | `RefreshStatus` offline; badge stays "On-device · Offline OK" | None: everything still works |
| Offline place search | Search results | "Offline: mga lugar sa data ko lang." | Pick from pack places |
| Refresh failed | Status sheet | "Hindi na-refresh · susubukan ulit." | "Refresh now" |
| GPS lost | Tracking | "Walang GPS. Hinahanap ulit…" | None; recovers automatically |
| Location denied | Before first trip | "Kailangan ang location para masubaybayan ang biyahe." | Open settings; trip sheet still works |
| Model failed to load (MVP, D31) | Ask bar | "Hindi ma-load ang AI model." | Retry; the map builder still works |
| Mic denied (voice, optional) | Mic | "Walang access sa mic. I-type na lang." | Open settings |
| Nothing heard (voice, optional) | Mic | "Walang narinig. Subukan ulit." | Retry |

**Copy:**
- Loading: "Nag-iisip sa phone mo…"
- Fare unknown: "Pamasahe: hindi alam"
- Saved suggestion: "Na-save sa phone mo · isi-sync pag may signal"
- Walk offline: "walk ~N m"
- Ride: "Oo, sakay ka dito ✓"
- Wrong: "Hindi 'to. Hanapin ang '<signboard>'"
- Unsure: "Hindi sigurado. Tingnan ang signboard."

Full per-component copy is in the [Figma brief §7](figma-brief-commutenity.md#7-copy-deck).

## 5. Motion

Use Material defaults. Progress is shown as text, not just a spinner. Respect the system's remove-animations setting: the sheet snaps and the camera jumps instead of animating. Never show "done" before the result exists. The para alert never plays sound.

## 6. Accessibility

- TalkBack labels on every control, including the mode chips, pins ("Point A, <place>"), map lines' leg summaries, and verdicts.
- The map is not the only way in: every pin can be set by search, and the trip is fully readable as `LegRow` text.
- Status changes are announced: refresh state, on/off route, GPS lost, and the para alert (as an assertive announcement).
- Contrast is 4.5:1 for text and 3:1 for UI elements and map lines against the map, in both themes.
- Fares are written as "₱" plus the number.
- Mode, source, status, and verdict are never shown by color alone: each has text or an icon. Walk-offline differs by dash pattern as well as color.
- The para alert has three channels (banner, vibration, notification) so no single sense is required.

## 7. Density Decisions

- One best trip is expanded.
- The alternatives list shows at most 5 rows.
- Reason lines are at most 4 words.
- One banner at a time per kind: `ParaAlertBanner` above `RouteStatusBanner`; no stacking beyond these two.
- No chat history, feed, or profiles. The ask bar keeps no transcript list.

## 8. Quality Gate

Before the demo, check:
- TalkBack pass on the main path: set A and B, read the trip, start trip, hear the para alert announcement
- both themes, including the dark map style
- small and large phones
- every state in [PRD §5](prd-commutenity.md#5-app-flow-and-ux-intent) and §4.1 above
- the badge and refresh status tell the truth in airplane mode
- OSM attribution visible on every map screen, including tracking mode
- no straight line on the map except the labelled offline walk
- `sample` marker shown only on mock values
- the `AskBar` is visible under `TripBuilderCard` on the map home and best-trip screens, and the hero question ("How to get from V.A. Rufino St to Pio del Pilar?") sets A = "V.A. Rufino St" and B = "Dela Rosa St" ([D30](state.md#5-decisions), [D37](state.md#5-decisions), [D31](state.md#5-decisions))

No P0 or P1 UI defects may be open. Covered by QAD [QA-18](qad-commutenity.md#6-release-criteria), with QA-01..QA-14 and QA-17 for the behavior behind each state.
