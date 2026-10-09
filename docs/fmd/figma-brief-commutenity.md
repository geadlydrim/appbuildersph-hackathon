# Figma Update Brief: CommuteNity

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.4
**Owner:** Implementer
**Status:** Draft
**Last reconciled:** 2026-10-09
**PRD:** [Product requirements](prd-commutenity.md)
**DSD:** [Design specification](dsd-commutenity.md)

## 1. Purpose and how to use this brief

**Audience:** an AI agent with Figma MCP tools. **Job:** update the EXISTING CommuteNity Figma file (made from the earlier, Metro Manila / ask-first docs) to the current direction ([D20](state.md#5-decisions)–[D31](state.md#5-decisions)): Makati City only, map-first trip builder, road-following lines, offline map, online refresh, in-trip tracking with para alert, the ask bar (ask in words and correct-vehicle check) as a secondary input that is part of the MVP ([D31](state.md#5-decisions)), voice optional, no OCR. The hero trip pair is fixed ([D30](state.md#5-decisions), amended by [D37](state.md#5-decisions)): A = "V.A. Rufino St", B = "Dela Rosa St" (Pio del Pilar).

Docs are canonical; Figma follows docs ([D29](state.md#5-decisions)). If this brief and the DSD disagree on behavior or copy, the DSD wins. If they disagree on Figma names or structure, this brief wins. Record any mismatch in the Change Report; do not ask.

### Procedure (do in this order)

1. Read the [DSD](dsd-commutenity.md) and [PRD §5](prd-commutenity.md#5-app-flow-and-ux-intent) first.
2. **Inventory before changing anything:** list every page, top-level frame, component and component set, style, and variable collection in the file. Put the result in a frame `Change Report` on page `00 Cover` (columns: old name, type, action, new name, reason).
3. If a tool can add a version-history entry, name it `Before Makati map-first update`. If not, continue.
4. Match old items to this brief **by function, not by name** (section 2.4). **Rename and update; never duplicate.** A duplicate is any second frame or component doing the same job.
5. Create or rename pages (section 3), then variables (section 6), then components (section 5), then screens (section 4), then flows (section 8), then archive (section 2.3).
6. **Never delete.** Removed frames and components move to page `99 Archive` and get the `ARCHIVED` prefix (section 2.3).
7. Run the acceptance checklist (section 10). Finish the Change Report with a "Not done" list.

**No questions.** If a tool cannot do a step (for example prototype wiring, variable modes, or component-set variants), do the closest thing it can, and list the step under `Change Report › Not done` with the exact reason. Then continue. If a value is missing (section 6), use the stated fallback and list it under `Change Report › Values to confirm`.

## 2. Change log vs the previous design

### 2.1 Added

| Item | DSD | Tier |
|---|---|---|
| Map-first home (full-bleed map, bottom sheet) | [§1.1](dsd-commutenity.md#11-map-home-t0) | T0 |
| A/B trip builder (pins, search fields, "use my location") | §1.1 | T0 |
| Road-following lines, colored by mode; offline dashed walk with label | [§1.2](dsd-commutenity.md#12-best-trip-sheet-t0) | T0 |
| OSM attribution on every map frame | [§1.7](dsd-commutenity.md#17-osm-attribution) | T0 |
| Refresh and sync status | [§1.4](dsd-commutenity.md#14-refresh-and-sync-status-t1) | T1 |
| Tracking mode: progress, current leg, on/off-route banner, para alert (banner + vibration + notification) | [§1.5](dsd-commutenity.md#15-active-trip-tracking-mode-t2) | T2 |
| Correct-vehicle check (text match, three verdicts) | [§1.6](dsd-commutenity.md#16-ask-in-words-voice-correct-vehicle-check-t3) | T3 label, MVP ([D31](state.md#5-decisions)) |
| Ask in words: `AskBar` visible under `TripBuilderCard` on the map home; "understood" chips | §1.1, §1.6 | T3 label, MVP ([D31](state.md#5-decisions)) |
| STT mic states with transcript confirmation | §1.6 | T3, optional |

### 2.2 Changed

| Item | From | To |
|---|---|---|
| Ask bar | Primary, top of an ask-first home | Secondary input, MVP ([D31](state.md#5-decisions)): compact `AskBar` pill under or near `TripBuilderCard`, visible on the MVP map frames; the map trip builder stays primary |
| Best-route card | Full-width card on the ask home | `TripSheet` content over the map |
| Place picker | Separate ambiguity screen | `T0-02 Place Search` (search and ambiguity in one screen) |
| Alternatives sheet, Suggest route, Vote, Sync icon, Setup screen | Metro Manila content | Same jobs, renamed (section 4), Makati content (the [D30](state.md#5-decisions) pair names; leg values stay placeholders), map pack download in Setup |
| Voice | T4 | T3, optional (STT only, transcript confirmed before use); cut before F8 |
| Signboard text | Read by camera | Typed or spoken by the rider; still shown on `LegRow` as "Hanapin ang '…'" |
| Sample content | Valenzuela–Recto hero trip | Makati hero pair from [D30](state.md#5-decisions), amended by [D37](state.md#5-decisions): A = "V.A. Rufino St", B = "Dela Rosa St". Legs (routes, fares, minutes) stay placeholders, marked `TBD (A13)` until the candidate trips are listed ([A13](state.md#4-open-assumptions)) |

### 2.3 Removed: move to `99 Archive`

Rename each moved frame `ARCHIVED – <original name>`, and add a sticky next to it: `Archived 2026-10-09 · <reason> · superseded by <D-ID>`. Do not delete anything.

| Match by function | Reason | Superseded by |
|---|---|---|
| Signboard camera screen, OCR scan, verdict screen driven by a camera read, "Check signboard" button on legs, "Reading sign…", "Hindi mabasa" states | OCR dropped | [D26](state.md#5-decisions), [D27](state.md#5-decisions) |
| Chat-style answer history, message bubbles, conversation list | No chat history ([DSD §7](dsd-commutenity.md#7-density-decisions)) | [D21](state.md#5-decisions) |
| Ask-first home (ask bar as the main control, example-question list as the main content) | Map is the home | [D21](state.md#5-decisions) |
| Any frame or copy whose purpose is Metro-Manila-wide coverage (splash, onboarding, coverage list, "Covered: …" lists naming other areas) | Makati only | [D20](state.md#5-decisions) |
| Any frame or sample content for Valenzuela, Malanday, Monumento, Recto, Doroteo Jose, e-jeep from that corridor | Corridor dropped | [D20](state.md#5-decisions) |

**Text sweep:** search all remaining text layers for `Metro Manila`, `Valenzuela`, `Malanday`, `Monumento`, `Recto`, `Doroteo`, `OCR`, `Scan`, `camera`, `Check signboard`. Replace with the Makati copy in section 7, or archive the frame if it has no purpose left. Keep the phrase "signboard text" when it labels what to look for on a leg.

### 2.4 Old-to-new mapping

Old surfaces (from the earlier DSD v0.2 and PRD §5) and their new homes. Match by function; record the old and new names in the Change Report.

| Old surface | New frame or component |
|---|---|
| Top bar | Top overlay on `T0-01 Home – Map` (`OfflineBadge`, `RefreshStatus`) |
| Ask bar | `AskBar` component, shown on `T0-01 Home – Map` and the other MVP map frames (D31), and in its states on `T3-01 Ask Bar – States` |
| Best-route card | `TripSheet` (`T0-03 Best Trip – Sheet`) |
| Place picker | `T0-02 Place Search` |
| Alternatives sheet | `T1-03 Alternatives – List` |
| Suggest route / leg builder | `T1-04 Suggest Trip – Leg Builder` |
| Vote buttons | `VoteButtons`, `T1-05 Vote – States` |
| Sync icon / sync status | `RefreshStatus`, `T1-01 Refresh – Status Sheet` |
| Setup / first-run screen | `T1-02 Setup – Map Pack Download` |
| "Not in my data" state | `T0-04 Not In My Data` |
| Voice screen | `T3-03 Voice – Mic` |
| Signboard verdict | Archive. New `VerdictCard` is for text match: `T3-05 Vehicle Check – Verdict` |

## 3. Page structure

Create, rename, or reorder pages to exactly this list. Use the en dash in `Screens – T0` etc.

| # | Page name | Holds |
|---|---|---|
| 1 | `00 Cover` | Title, version, `Change Report` frame, legend of annotation tags |
| 2 | `01 Foundations` | Variable swatches (color Light/Dark, spacing, radius, type), text styles, `MapPlaceholder`, mode palette strip, icon list |
| 3 | `02 Components` | All components and component sets (section 5) |
| 4 | `03 Screens – T0` | Frames `T0-01`..`T0-04` |
| 5 | `04 Screens – T1` | Frames `T1-01`..`T1-06` |
| 6 | `05 Screens – T2` | Frames `T2-01`..`T2-04` |
| 7 | `06 Screens – T3` | Frames `T3-01`..`T3-05`. The page keeps its name. Per [D31](state.md#5-decisions) the ask-in-words frames (`T3-01`, `T3-02`) and the correct-vehicle frames (`T3-04`, `T3-05`) are MVP; only the voice frame (`T3-03`) is optional |
| 8 | `07 Flows` | Prototype flow clones (section 8) |
| 9 | `99 Archive` | Archived frames and components |

T4 (learned ranker) has no screen: the `ReasonChip` text may change but the layout does not. The page name `06 Screens – T3` stays even though most of its frames are MVP (D31).

## 4. Frame list

**Device frame:** Android compact, **412 × 915 dp**, for every screen frame. Keep the file's existing status bar and gesture bar.
**Naming:** `<Tier>-<NN> <Name>` for the base. Each state is its own frame named `<base> / <state>`, laid out left to right in the order listed, 80 dp apart, one row per base. Dark copies are named `<base> / <state> / dark` and use the `Dark` mode of `Color` (set explicitly on the frame).
**Placeholders:** use the tokens in section 7.1. Never invent real fares, minutes, stops, or routes.
**Map on every map frame:** `MapPlaceholder` plus vector `MapLine`s that follow the placeholder's curved roads (never straight between stops, except the offline walk). Show `AttributionLabel` on every frame that has a map.

### 4.1 `03 Screens – T0`

| Frame | Purpose | Contents | States | PRD-F / US |
|---|---|---|---|---|
| `T0-01 Home – Map` | Root screen. Set A and B. | Full-bleed `MapPlaceholder`; top overlay (wordmark, `OfflineBadge`, `RefreshStatus`); `TripBuilderCard`; `AskBar collapsed` as a secondary pill directly under `TripBuilderCard` (visible, never replacing the pins or the fields; [D31](state.md#5-decisions)); `PinMarker` A/B; my-location button; `TripSheet collapsed`; `AttributionLabel`. | `empty`, `a-set`, `ab-set`, `map-error`, `pack-error`, `ab-set / dark` | F1, F3, F8 / US-01, US-11 |
| `T0-02 Place Search` | Search pack places for A or B; resolve ambiguity. | `SearchField` focused over the map; list of `PlaceResultRow`; "use my location" row for A; pin-drop hint. | `typing`, `results`, `ambiguous`, `no-results`, `offline-limited` | F1 / US-01, US-03 |
| `T0-03 Best Trip – Sheet` | Show the best trip on real roads. | Compact `TripBuilderCard` (`A → B`) with the `AskBar collapsed` pill under it; `MapLine`s by mode; `PinMarker` stops and para stop; `TripSheet` with `TripSummary`, `ReasonChip`, `LegRow`s (jeepney, bus, UV legs show the "Tama ba 'tong sasakyan?" entry, `showVehicleCheck`, [D31](state.md#5-decisions)), `ParaPointRow`, action row ("Start trip" is hidden here and shown in the T2 frames; "Show alternatives", `VoteButtons` and "Suggest a trip" are hidden until T1). | `collapsed`, `half`, `expanded`, `half / sample`, `half / unknown-fare`, `half / walk-offline`, `half / leg-selected`, `half / dark` | F2, F3, F8 / US-02, US-03, US-12 (QA-19) |
| `T0-04 Not In My Data` | Say clearly when it can't help. | Pins on the map; `StateMessage` in `TripSheet half`; `AskBar collapsed` pill under `TripBuilderCard`. | `outside-makati`, `no-route` | F2 / US-03 |

The `AskBar` is visible on `T0-01`, `T0-03` and `T0-04`. On `T0-02 Place Search` the focused `SearchField` and the results list cover its spot while typing; it returns as soon as the field loses focus. No tier flag hides it.

`half / sample` shows `SourceLabel sample` next to every mock value. `half / unknown-fare` shows "Pamasahe: hindi alam" and an "unknown" total (no partial total). `half / walk-offline` shows the dashed `MapLine` with `WalkLabel` for the first and last mile.

### 4.2 `04 Screens – T1`

| Frame | Purpose | Contents | States | PRD-F / US |
|---|---|---|---|---|
| `T1-01 Refresh – Status Sheet` | Show refresh and sync status; trigger refresh. | `RefreshStatus` in the top overlay; bottom sheet with commute pack version, map pack version, last refresh time, pending contributions, "Refresh now". | `synced`, `refreshing`, `offline`, `failed`, `pending-contributions` | F4, F6 / US-04, US-08 |
| `T1-02 Setup – Map Pack Download` | First-run or update download of the map pack and commute pack. | Title, size, percent, text progress, retry. Never "ready" until verified. | `downloading`, `interrupted`, `failed`, `ready` | F3, F4 / US-04 |
| `T1-03 Alternatives – List` | Compare other trips. | Map with the previewed trip's `MapLine`s; `TripSheet half` showing up to 5 `AltRow`s; "Use this trip". | `list`, `row-expanded`, `empty` | F5 / US-05 |
| `T1-04 Suggest Trip – Leg Builder` | Suggest a trip. | Full screen; list of `LegBuilderRow`s; "Add leg"; note; "Save". | `empty`, `picking-route`, `picking-stop`, `invalid-leg`, `ready`, `saved` | F6 / US-06 |
| `T1-05 Vote – States` | Vote on a trip. | `TripSheet half` with `VoteButtons`. | `none`, `up-unsynced`, `up-synced`, `down-unsynced` | F6 / US-07 |
| `T1-06 Sync – Two Phones` | Show a contribution moving between phones. | Two device frames side by side: phone 1 shows its suggestion with the unsynced dot; phone 2 shows it as a `Community` `AltRow` after refresh. | `phone-1-unsynced`, `phone-2-after-sync` | F6 / US-08 |

### 4.3 `05 Screens – T2`

| Frame | Purpose | Contents | States | PRD-F / US |
|---|---|---|---|---|
| `T2-01 Trip – Permissions` | Ask for GPS (and notifications) with a reason, before the first trip. | Reason text, "Allow" and "Not now". | `rationale`, `location-denied`, `notifications-denied` | F7 / US-09, US-10 |
| `T2-02 Trip Active – Tracking` | Show progress and route status. | Map camera on the location puck; `MapLine`s with the travelled part at 40 % opacity; `RouteStatusBanner`; `ActiveTripPanel` (`TripProgress`, current `LegRow` with `state=current`, next-leg preview); "End trip". | `on-route`, `off-route`, `gps-lost`, `simulated`, `end-confirm`, `on-route / dark` | F7 / US-09 |
| `T2-03 Trip Active – Para Alert` | Tell the rider to para. | Same as `T2-02`, plus `ParaAlertBanner` at the top, above `RouteStatusBanner`; the para stop pin highlighted. | `banner`, `banner-over-off-route`, `banner / dark` | F7 / US-10 |
| `T2-04 Para Alert – Notification` | Show the system heads-up and the lock-screen notification. | `ParaAlertBanner surface=notification` over the status area, and on a lock screen. Annotate "vibration; no sound; no TTS". | `heads-up`, `lock-screen` | F7 / US-10 |

### 4.4 `06 Screens – T3`

| Frame | Purpose | Contents | States | PRD-F / US |
|---|---|---|---|---|
| `T3-01 Ask Bar – States` | Ask in words, secondary to the map (MVP, D31). | Home map with the `AskBar` pill under `TripBuilderCard`. | `collapsed`, `expanded-empty`, `thinking`, `disabled-loading`, `model-error` | F8 / US-11 |
| `T3-02 Ask – Understood` | Show that the question set A and B. | Pins A and B placed; chips "A: …", "B: …"; confirm. Ambiguous places link to `T0-02 Place Search / ambiguous`. | `understood`, `ambiguous` | F8 / US-11 |
| `T3-03 Voice – Mic` | Voice question with confirmation. | `MicButton`; transcript field; confirm or edit. | `listening`, `transcribing`, `confirm-transcript`, `mic-denied`, `nothing-heard` | F9 / US-13 |
| `T3-04 Vehicle Check – Input` | Ask "Is this the right vehicle?" | Opened from a `LegRow` in `T0-03` or from `T2-02`; text field with mic. | `from-leg`, `from-tracking`, `typing` | F8 / US-12 |
| `T3-05 Vehicle Check – Verdict` | Show the match result. | `VerdictCard` with large text label and icon, over the leg. | `ride`, `wrong`, `unsure` | F8 / US-12 |

Tags per [D31](state.md#5-decisions): `T3-01`, `T3-02`, `T3-04`, `T3-05` (F8) carry the flag `MVP (D31)`. `T3-03` (voice, F9) is optional; tag it `T3 – optional`. Voice and the ranker are cut before F8.

## 5. Component inventory

Build on `02 Components`. Reuse existing components (renamed) wherever the old file has the same job. Every component uses variables from section 6 only (no hard-coded colors, sizes, or radii). Set the component description to the `contentDescription` line in section 7.3.

Properties: **V** = variant property, **T** = text, **B** = boolean, **S** = instance swap.

| Component | Properties | Notes |
|---|---|---|
| `ModeChip` | V `variant` = `jeepney` \| `bus` \| `mrt` \| `lrt` \| `uv` \| `p2p` \| `tricycle` \| `walk`; V `size` = `default` \| `compact`; T `label` | Icon plus text always (never color alone). Fill `mode/<variant>`, text `mode/<variant>-on`. |
| `SourceLabel` | V `source` = `algorithm` \| `community` \| `sample`; T `votes` (community only) | `algorithm`: neutral outline. `community`: amber tonal, "Community · {n} 👍". `sample`: small neutral tag with info icon; it is the sample-data marker. |
| `ReasonChip` | T `text` (≤ 4 words) | Neutral tonal. |
| `LegRow` | S `mode` (a `ModeChip`); T `board`, `alight`, `fare`, `minutes`, `distance`, `walk`, `signboard`; B `showSample`; B `showVehicleCheck`; V `state` = `default` \| `current` \| `done` \| `upcoming` | Stop names bold, ≥ 16 sp. `signboard` hidden for walk, MRT, LRT. `showVehicleCheck` only for jeepney, bus, UV (MVP, D31). `done` at 40 % opacity. |
| `ParaPointRow` | T `landmark` | Para icon plus "Mag-para sa {landmark}". |
| `TripSummary` | T `fare`, `minutes`, `distance`, `walk`, `transfers`; S `reason` (`ReasonChip`); B `showSample` | Unknown values read "unknown"; total unknown if any part is unknown. |
| `TripSheet` | V `state` = `collapsed` \| `half` \| `expanded`; slot `content` | Heights: `collapsed` 96 dp, `half` 50 % (≈ 458 dp), `expanded` 80 % (≈ 732 dp). Drag handle. Top radius `radius/xl`. |
| `TripBuilderCard` | V `state` = `expanded` \| `compact`; S `fieldA`, `fieldB` | `compact` is one `A → B` line. |
| `SearchField` | V `kind` = `A` \| `B`; V `state` = `empty` \| `focused` \| `filled` \| `error`; T `value`; B `showMyLocation` (A only) | Focus ring in `brand/primary`. |
| `PlaceResultRow` | V `kind` = `pack` \| `online`; T `name`, `area` | `online` shows an "Online" tag; hidden offline. |
| `PinMarker` | V `kind` = `A` \| `B` \| `stop`; V `state` = `default` \| `dragging`; B `isPara` (`stop` only) | Letter inside A and B (never color alone). `stop`: white circle with mode-colored ring. `isPara`: primary-container halo plus "Para" label. Tap area 48 dp. |
| `UserPuck` | V `state` = `ok` \| `lost` | Location puck in tracking. |
| `MapLine` | V `mode` = `jeepney` \| `bus` \| `mrt` \| `lrt` \| `uv` \| `p2p` \| `tricycle` \| `walk`; V `style` = `solid` \| `dashed-walk-offline` | Create 9 variants: 8 `solid` plus `walk` + `dashed-walk-offline`. No other dashed combinations. 5 dp stroke, 2 dp casing `map/line-casing`. Dashed: 4 dp dash, 6 dp gap. Vector paths, not images. |
| `WalkLabel` | T `meters` | "walk ~{meters} m", sits on the dashed line. |
| `MapPlaceholder` | V `theme` = `light` \| `dark` | Flat neutral fill with 3–4 curved gray road strokes. No tile images and no screenshots of any map provider. |
| `AttributionLabel` | T `text` = "© OpenStreetMap contributors" | ≥ 12 sp, on `surface/raised` at 80 %. Bottom-end, on the sheet's top edge. |
| `OfflineBadge` | V `state` = `ready` \| `not-ready` | `ready` = "On-device · Offline OK". |
| `RefreshStatus` | V `state` = `synced` \| `refreshing` \| `offline` \| `failed`; T `time`; T `pending` (count) | Icon plus short text; opens the status sheet. |
| `AltRow` | S `source` (`SourceLabel`); T `reason`, `fare`, `minutes`; B `expanded`; B `showSample` | Expanded shows `LegRow`s. |
| `VoteButtons` | V `vote` = `none` \| `up` \| `down`; V `synced` = `true` \| `false` | `false` adds the "unsynced" dot to the filled thumb. |
| `LegBuilderRow` | V `state` = `empty` \| `picking-route` \| `picking-stop` \| `invalid` \| `valid`; T `route`, `board`, `alight`, `error` | `invalid` shows `error` inline. |
| `RouteStatusBanner` | V `status` = `on` \| `off` \| `gps-lost`; T `meters` | Icon plus text, never color alone. |
| `ParaAlertBanner` | V `surface` = `in-app` \| `notification`; T `stop`, `meters` | Heads-up. `in-app` sits at the top, above `RouteStatusBanner`. No sound. |
| `TripProgress` | T `legIndex`, `legCount`, `metersToPara`, `stopsToPara`; B `gpsLost` | `gpsLost` replaces distances with "Walang GPS". |
| `ActiveTripPanel` | S `progress`, `currentLeg`, `nextLeg`; B `simulated` | Contains "End trip". `simulated` shows `SimulatedTag`. |
| `SimulatedTag` | none | "Simulated GPS". |
| `AskBar` (MVP, D31) | V `state` = `collapsed` \| `expanded` \| `thinking` \| `disabled-loading` \| `error`; T `value`, `progress` | Secondary input: collapsed is a pill with mic, placed under or near `TripBuilderCard`; never replaces the pins or fields. The mic is optional (F9), the pill stays visible without it. |
| `MicButton` (T3, optional) | V `state` = `idle` \| `listening` \| `transcribing` \| `denied` | |
| `VerdictCard` (MVP, D31) | V `verdict` = `ride` \| `wrong` \| `unsure`; T `signboard` | Large text label plus icon; colors `verdict/*`. |
| `StateMessage` | V `kind` = `not-in-data` \| `no-route` \| `no-alternatives` \| `map-error` \| `pack-error` \| `model-error` \| `offline-search`; T `title`, `body`, `action` | Copy in section 7. |

Material 3 buttons, icon buttons, text fields, dialogs, and the status and gesture bars come from the file's existing Material 3 kit. Do not rebuild them. Icons are the Material Symbols already in the file. No new logo, illustration, or photo: use the existing wordmark layer, or the text "CommuteNity" in the display face if none exists.

Move these old components to `99 Archive`, with the `ARCHIVED` prefix: signboard camera frame, scan overlay, camera verdict card, chat bubbles.

## 6. Figma variables

Create or update these collections. Do not change a value that already exists in the file and that the DSD does not contradict. Where the DSD gives no value, use the existing file value; if the variable is missing, use the fallback and list it under `Values to confirm`.

### 6.1 `Color` (modes: `Light`, `Dark`)

| Variable | Light | Dark | Source |
|---|---|---|---|
| `brand/primary` | `#F3821D` (≈ `oklch(0.72 0.17 55)`) | same | DSD §2 |
| `brand/on-primary` | existing dark neutral (must be ≥ 4.5:1 on primary; white fails) | same dark neutral | DSD §2 |
| `brand/primary-container` | existing amber tonal | existing | Fallback: lighter tint of primary (Light), darker shade of primary (Dark) |
| `brand/on-primary-container` | existing | existing | Fallback: ≥ 4.5:1 on the container |
| `surface/base` | existing neutral | existing | |
| `surface/raised` | existing neutral | existing | |
| `surface/sheet` | alias `surface/raised` | alias | |
| `text/primary` | existing | existing | |
| `text/secondary` | existing | existing | |
| `outline/default` | existing | existing | |
| `mode/jeepney` | amber/gold family | same lightness | DSD §2 |
| `mode/bus` | blue | | |
| `mode/mrt` | purple | | |
| `mode/lrt` | red-orange | | |
| `mode/uv` | teal | | |
| `mode/p2p` | green | | |
| `mode/tricycle` | yellow | | |
| `mode/walk` | gray | | |
| `mode/<name>-on` (8) | text color with ≥ 4.5:1 on `mode/<name>` | | |
| `source/algorithm` | alias `outline/default` | alias | Neutral outline |
| `source/community` | alias `brand/primary-container` | alias | Amber tonal |
| `source/community-on` | alias `brand/on-primary-container` | alias | |
| `source/sample` | existing neutral tonal | existing | Quiet |
| `source/sample-on` | alias `text/secondary` | alias | |
| `verdict/ride`, `verdict/ride-on` | green, text ≥ 4.5:1 | | DSD §2 |
| `verdict/wrong`, `verdict/wrong-on` | red, text ≥ 4.5:1 | | |
| `verdict/unsure`, `verdict/unsure-on` | neutral, text ≥ 4.5:1 | | |
| `status/on-route` | alias `verdict/ride` | alias | DSD §2 (no new hues) |
| `status/off-route` | alias `verdict/wrong` | alias | |
| `status/gps-lost` | alias `verdict/unsure` | alias | |
| `status/para` | alias `brand/primary-container` | alias | |
| `status/para-on` | alias `brand/on-primary-container` | alias | |
| `status/refreshing` | alias `brand/primary` | alias | |
| `status/failed` | alias `verdict/wrong` | alias | |
| `status/synced` | alias `text/secondary` | alias | |
| `status/offline` | alias `text/secondary` | alias | |
| `map/line-casing` | alias `surface/base` | alias | |

Mode and family colors: use the file's existing Jeepney Gold values. Fallback only where missing: pick a color in the stated family so that `mode/<name>` has ≥ 3:1 against `surface/base` in both modes. Keep each mode's hue the same in Light and Dark.

### 6.2 `Spacing` (single mode `Default`), numbers in dp

`space/1` = 4, `space/2` = 8, `space/3` = 12, `space/4` = 16 (screen padding), `space/6` = 24, `space/8` = 32, `touch/min` = 48, `sheet/collapsed` = 96, `sheet/half-pct` = 50, `sheet/expanded-pct` = 80, `line/width` = 5, `line/casing` = 2, `line/dash` = 4, `line/gap` = 6, `target/pin` = 48.

### 6.3 `Radius` (single mode `Default`), Material 3 shape scale, dp

`radius/xs` = 4, `radius/sm` = 8, `radius/md` = 12, `radius/lg` = 16, `radius/xl` = 28 (sheet top corners).

### 6.4 `Type` (single mode `Default`)

| Variable | Value |
|---|---|
| `type/family/display` | Plus Jakarta Sans, unless the file already uses Space Grotesk (keep that) |
| `type/family/body` | Roboto |
| `type/size/leg` | 16 (minimum for leg text; stop names bold) |
| `type/size/body` | 14 |
| `type/size/label` | 12 (minimum for `AttributionLabel`) |
| `type/size/title` | 22 |
| `type/weight/display` | 700 |

Text styles (create or update): `Title / Display` (display, bold, 22), `Leg / Stop` (body, bold, 16), `Leg / Detail` (body, regular, 16), `Body` (body, 14), `Label` (body, medium, 12). Bundled fonts only.

## 7. Copy deck

Taglish is the default display language; EN is the reference for review. `{x}` marks a variable text. Use these strings verbatim.

### 7.1 Placeholder tokens (Makati hero trip: pair fixed by [D30](state.md#5-decisions); legs TBD per [A13](state.md#4-open-assumptions))

| Token | Text |
|---|---|
| Origin (A) | `V.A. Rufino St`, sublabel `Legazpi Village` |
| Destination (B) | `Dela Rosa St`, sublabel `Pio del Pilar` |
| Stops | `Stop 1 (TBD)`, `Stop 2 (TBD)`, … |
| Landmark | `Landmark (TBD)` |
| Route and signboard | `Route X (TBD)`, `SIGNBOARD (TBD)` |
| Fare, minutes, distance, walk | `₱XX`, `XX min`, `X.X km`, `X min` |

The pair names are real (D30) and carry no `SourceLabel sample`. Best trip legs, illustrative only: walk, one ride, walk. Alternatives: one `Algorithm` row, one `Community · 12 👍` row. Every placeholder leg value (route, stop, fare, minutes, distance, signboard, landmark) carries `SourceLabel sample`. Add a sticky `Placeholder legs: routes, fares, minutes TBD (A13)` on each frame that shows leg values. Never invent routes, jeepney names, fares, or minutes for the pair.

### 7.2 Strings

| Component / state | EN | Taglish |
|---|---|---|
| `SearchField` A | Where from? | Saan ka manggagaling? |
| `SearchField` A, my location | Use my location | Gamitin ang location ko |
| `SearchField` B | Where to? | Saan ka pupunta? |
| `SearchField` error | Not in my data yet. | Wala pa sa data ko 'yan. |
| `OfflineBadge` ready | On-device · Offline OK | On-device · Offline OK |
| `OfflineBadge` not-ready | Not offline-ready yet | Hindi pa ready offline |
| Home `empty` | Pick A and B on the map. | Pumili ng A at B sa mapa. |
| Computing | Thinking on your phone… | Nag-iisip sa phone mo… |
| `TripSummary` | ₱{fare} · {minutes} min · {distance} km · walk {walk} min · {n} transfers | ₱{fare} · {minutes} min · {distance} km · lakad {walk} min · {n} lipat |
| Fare unknown | Fare: unknown | Pamasahe: hindi alam |
| Minutes unknown | Time: unknown | Oras: hindi alam |
| `ReasonChip` | Fewest transfers / Fastest / Cheapest | Pinakakaunting lipat / Pinakamabilis / Pinakamura |
| `LegRow` board | Board at {stop} | Sakay sa {stop} |
| `LegRow` alight | Alight at {stop} | Para sa {stop} |
| `LegRow` signboard | Look for '{signboard}' | Hanapin ang '{signboard}' |
| `ParaPointRow` | Say para at {landmark} | Mag-para sa {landmark} |
| `SourceLabel` | Algorithm / Community · {n} 👍 / Sample | Algorithm / Community · {n} 👍 / Sample |
| `SourceLabel sample` tooltip | Sample data, not verified | Sample lang, hindi pa verified |
| `WalkLabel` | walk ~{meters} m | walk ~{meters} m |
| `AttributionLabel` | © OpenStreetMap contributors | © OpenStreetMap contributors |
| `StateMessage` not-in-data | Not in my data yet. Only Makati is covered for now. | Wala pa sa data ko 'yan. Makati lang muna ang covered. |
| `StateMessage` no-route | No route between A and B in my data. | Walang ruta sa pagitan ng A at B sa data ko. |
| `StateMessage` map-error | Can't load the map. | Hindi ma-load ang mapa. |
| `StateMessage` pack-error | Can't load the commute pack. | Hindi ma-load ang commute pack. |
| `StateMessage` offline-search | Offline: only places in my data. | Offline: mga lugar sa data ko lang. |
| Retry | Retry | Subukan ulit |
| Continue | Continue | Magpatuloy |
| `RefreshStatus` synced | Updated {time} | Na-update {time} |
| `RefreshStatus` refreshing | Refreshing… | Nire-refresh… |
| `RefreshStatus` offline | Offline · using last data | Offline · gamit ang huling data |
| `RefreshStatus` failed | Refresh failed · will retry | Hindi na-refresh · susubukan ulit |
| `RefreshStatus` pending | {n} to sync | {n} na isi-sync |
| Status sheet action | Refresh now | I-refresh na |
| Setup downloading | Downloading map… {size} · {pct}% | Dina-download ang mapa… {size} · {pct}% |
| Setup interrupted | Download paused. Resuming… | Naputol. Itutuloy… |
| Setup failed | Download failed. Retry. | Hindi na-download. Subukan ulit. |
| Setup ready | Ready offline · {size} used | Ready offline · {size} ang gamit |
| Alternatives title | Other trips | Iba pang ruta |
| Alternatives action | Show alternatives | Ipakita ang iba pang ruta |
| Alternatives use | Use this trip | Gamitin 'to |
| `StateMessage` no-alternatives | No other trips yet. Suggest one? | Wala pang ibang ruta. Mag-suggest? |
| Suggest action | Suggest a trip | Mag-suggest ng ruta |
| `LegBuilderRow` | Pick a route / Board at / Alight at / Add leg / Note (optional) / Save | Pumili ng ruta / Sakay sa / Para sa / Dagdag na leg / Note (optional) / I-save |
| Suggest saved | Saved on your phone · will sync | Na-save sa phone mo · isi-sync pag may signal |
| `VoteButtons` | Worked / Didn't work / Not synced yet | Gumana / Hindi gumana / Hindi pa naka-sync |
| Start / end trip | Start trip / End trip | Simulan ang biyahe / Tapusin ang biyahe |
| End confirm | End this trip? | Tapusin na ang biyahe? |
| `RouteStatusBanner` on | On route ✓ | Nasa tamang ruta ✓ |
| `RouteStatusBanner` off | Off route · {meters} m | Lumihis ka sa ruta · {meters} m |
| `RouteStatusBanner` gps-lost | No GPS. Searching… | Walang GPS. Hinahanap ulit… |
| `TripProgress` | Leg {i} of {n} · {meters} m to para · {stops} stops | Leg {i} sa {n} · {meters} m bago mag-para · {stops} stop |
| `SimulatedTag` | Simulated GPS | Simulated GPS |
| `ParaAlertBanner` title | Para na! | Para na! |
| `ParaAlertBanner` body | Get off at {stop}. About {meters} m to go. | Bumaba sa {stop}. {meters} m na lang. |
| Permission rationale | CommuteNity uses GPS, even offline, to tell you when you're off route and when to say para. | Gagamitin ng CommuteNity ang GPS, kahit offline, para sabihin kung lihis ka at kung kailan mag-para. |
| Location denied | Location is needed to track your trip. | Kailangan ang location para masubaybayan ang biyahe. |
| Notifications denied | Allow notifications so the para alert can reach you. | I-allow ang notifications para maabisuhan ka sa para. |
| `AskBar` collapsed | Ask… | Magtanong… |
| `AskBar` hint | How do I get from {A} to {B}? | Paano pumunta sa {B} mula {A}? |
| `AskBar` example (hero pair) | How to get from V.A. Rufino St to Pio del Pilar? | Paano pumunta sa Dela Rosa St., Pio del Pilar galing V.A. Rufino? |
| `AskBar` loading | Loading model… {pct}% | Nilo-load ang model… {pct}% |
| `AskBar` error | Can't load the AI model. | Hindi ma-load ang AI model. |
| Ask understood | Understood: from {A} to {B} | Naintindihan: mula {A} papunta {B} |
| Ask confirm | Is this right? | Tama ba 'to? |
| `MicButton` listening / transcribing | Listening… / Transcribing… | Nakikinig… / Isinusulat… |
| Mic denied | No mic access. Type instead. | Walang access sa mic. I-type na lang. |
| Nothing heard | Nothing heard. Try again. | Walang narinig. Subukan ulit. |
| Vehicle check prompt | Is this the right vehicle? | Tama ba 'tong sasakyan? |
| Vehicle check hint | Type or say the signboard or route name | I-type o sabihin ang nakasulat sa signboard o pangalan ng ruta |
| `VerdictCard` ride | Yes, ride this ✓ | Oo, sakay ka dito ✓ |
| `VerdictCard` wrong | No. Look for '{signboard}' | Hindi 'to. Hanapin ang '{signboard}' |
| `VerdictCard` unsure | Not sure. Check the signboard. | Hindi sigurado. Tingnan ang signboard. |

### 7.3 TalkBack labels (`contentDescription`, set in component descriptions)

| Component | Label |
|---|---|
| `PinMarker` A / B | Point A, {place} / Point B, {place} |
| `ModeChip` | {mode} |
| `LegRow` | Leg {i}: {mode}, board at {stop}, alight at {stop}, ₱{fare}, {minutes} minutes |
| `TripSheet` | Trip details, {state}; drag to resize |
| `RouteStatusBanner` | On route / Off route by {meters} meters / GPS lost |
| `ParaAlertBanner` | Para now. Get off at {stop}. (assertive) |
| `VoteButtons` | Worked, not selected / Worked, selected, not synced |
| `RefreshStatus` | Refresh status: {state} |
| `VerdictCard` | Yes, ride this / No, look for {signboard} / Not sure, check the signboard |

## 8. Prototype connections

Prototype connections work only inside one page. So wire **only on `07 Flows`**, using clones of the screen frames. The tier pages (`03`–`06`) stay static and are the source of truth; edit them first, then refresh the clones.

**Clone rule:** name each clone `<Flow>-<NN> <source frame name> / <state>`. Put the source frame's full name in the clone's description. One section per flow, named `Flow <letter> – <name>`, with the first clone set as the flow's starting point. Defaults: Navigate to, Smart animate, 300 ms ease-out. Sheet state changes use Change to (component variants) with Smart animate. Use "Open overlay" for dialogs (`end-confirm`).

| Flow | Steps (From element → trigger → To) | Covers |
|---|---|---|
| `Flow A – Plan a trip offline` | A-01 `T0-01 Home – Map / empty`: map tap → on tap → A-02 `… / a-set`. A-02: map tap → A-03 `… / ab-set`. A-02: Field B → A-04 `T0-02 Place Search / typing` → on tap result → A-03. A-03: after delay 600 ms (computing) → A-05 `T0-03 Best Trip – Sheet / half`. A-05: sheet handle drag up → A-06 `… / expanded`; drag down → A-07 `… / collapsed`. A-05: `LegRow` tap → A-08 `… / leg-selected`. A-03: pin dragged to outside Makati → A-09 `T0-04 Not In My Data / outside-makati`. | F1, F2, F3 / US-01, US-02, US-03 |
| `Flow B – Refresh, then offline` | B-01 `T1-02 Setup – Map Pack Download / downloading`: after delay → B-02 `T1-02 … / interrupted`: after delay → B-03 `T1-02 … / ready`: "Continue" → B-04 `T0-01 Home – Map / empty`. B-04: `RefreshStatus` tap → B-05 `T1-01 Refresh – Status Sheet / synced`: "Refresh now" → B-06 `T1-01 … / refreshing` → after delay → B-05. Offline path: B-05 "Refresh now" → B-07 `T1-01 … / offline`. Failure path: B-06 → B-08 `T1-01 … / failed` → "Refresh now" → B-06. B-01 failure → B-09 `T1-02 … / failed` → "Retry" → B-01. | F3, F4 / US-04 (QA-04, QA-09) |
| `Flow C – Alternatives, vote, suggest, sync` | C-01 `T0-03 … / half` (T1 build: action row visible): "Show alternatives" → C-02 `T1-03 Alternatives – List / list`: row tap → C-03 `… / row-expanded`: "Use this trip" → C-01. C-01: 👍 → C-04 `T1-05 Vote – States / up-unsynced` → after delay → C-05 `… / up-synced`; tap again → C-06 `… / none`. C-02 or C-01: "Suggest a trip" → C-07 `T1-04 Suggest Trip – Leg Builder / empty` → C-08 `… / picking-route` → C-09 `… / picking-stop` → C-10 `… / ready` → "Save" → C-11 `… / saved` → C-12 `T1-06 Sync – Two Phones / phone-1-unsynced` → after delay → C-13 `… / phone-2-after-sync`. C-07 invalid → C-14 `… / invalid-leg`. | F5, F6 / US-05–US-08 |
| `Flow D – Active trip and para` | D-01 `T0-03 … / half` (T2 build): "Start trip" → D-02 `T2-01 Trip – Permissions / rationale`: "Allow" → D-03 `T2-02 Trip Active – Tracking / on-route`: after delay → D-04 `… / off-route` → after delay → D-03; D-03: after delay → D-05 `… / gps-lost` → after delay → D-03; D-03: after delay → D-06 `T2-03 Trip Active – Para Alert / banner` → after delay → D-07 `T2-04 Para Alert – Notification / heads-up`. D-03: "End trip" → D-08 `… / end-confirm` (overlay): confirm → D-09 `T0-01 Home – Map / empty`. D-02: "Not now" → D-10 `… / location-denied`. | F7 / US-09, US-10 |
| `Flow E – Ask in words and voice` | E-01 `T3-01 Ask Bar – States / collapsed`: pill tap → E-02 `… / expanded-empty`: submit → E-03 `… / thinking` → after delay → E-04 `T3-02 Ask – Understood / understood` → confirm → E-05 `T0-03 … / half`. E-04 variant `ambiguous` → E-06 `T0-02 Place Search / ambiguous` → choose → E-04. E-02: mic tap → E-07 `T3-03 Voice – Mic / listening` → after delay → E-08 `… / transcribing` → E-09 `… / confirm-transcript` → confirm → E-03. E-07 denied → E-10 `… / mic-denied`; nothing heard → E-11 `… / nothing-heard`. E-01 model error → E-12 `… / model-error`. | F8, F9 / US-11, US-13 |
| `Flow F – Correct vehicle` | F-01 `T0-03 … / half` (MVP build): `showVehicleCheck` tap → F-02 `T3-04 Vehicle Check – Input / from-leg` → type → F-03 `… / typing` → submit → F-04 `T3-05 … / ride`; alternates: F-05 `… / wrong`, F-06 `… / unsure`. Start from tracking: F-07 `T2-02 … / on-route` → vehicle check → F-08 `T3-04 … / from-tracking`. | F8 / US-12 |

Where a flow shows a T1/T2 control on a T0 clone, show it in that clone only (a clone state such as `half / t1-actions`); the tier page's frame stays at its own tier's contents. The `AskBar` and the vehicle-check entry are MVP (D31), so they show on the T0 frames themselves. Flow E's voice branch (E-07 to E-11, F9) is optional; the rest of Flow E and Flow F are MVP.

## 9. Annotation conventions

Each screen frame (and each clone) gets one text layer named `meta – <frame name>`, placed 24 dp above the frame's top-left, outside it, containing exactly this block:

```
Tier: T0 | PRD: F1, F3 | US: US-01 | QA: QA-01, QA-18 | DSD: §1.1
States: empty, a-set, ab-set, map-error, pack-error
```

- IDs come from section 4. QA IDs come from [QAD §6](qad-commutenity.md#6-release-criteria); use QA-18 on every frame, plus QA-17 where `sample` is shown, QA-19 where totals can be unknown, QA-02 where the road-following line is shown, QA-10/QA-11 on `T2-*`, QA-12 on `T3-01/02` and on the `AskBar` frames `T0-01`, `T0-03`, `T0-04`, QA-13 on `T3-04/05`, QA-14 on `T3-03`.
- Decision tags on stickies (use the file's default sticky style): `D20`..`D31`, `D30` (hero pair), `D31` (ask bar in the MVP), `A13` (candidate trips for the D30 pair; leg placeholders), `A14` (attribution text from the map provider), `A16` (thresholds tunable).
- State flags: `TBD (A13)` on placeholder leg content (routes, fares, minutes); `SIMULATED` on `simulated` frames; `MVP (D31)` on the F8 frames (`T3-01`, `T3-02`, `T3-04`, `T3-05`) and on the `AskBar` layer; `T3 – optional` on `T3-03` (voice).
- Behavior notes as stickies on `T2-03` and `T2-04`: "Fires once at ~300 m before the alight stop. Vibration + heads-up notification + banner. No sound. No TTS."
- Page `00 Cover` carries a legend frame listing these tags.

## 10. Acceptance checklist

The agent confirms each line in `Change Report › Checklist` as done, or lists it under `Not done`.

- [ ] Inventory table exists, listing every old page, frame, component, style, and variable collection, with the action taken.
- [ ] Pages are exactly the nine in section 3, in that order.
- [ ] Every frame in section 4 exists, at 412 × 915 dp, with every listed state, named exactly.
- [ ] No duplicate frames or components; old items were renamed and updated.
- [ ] Every component in section 5 exists with the listed variants and properties; all colors, spacing, radii, and type use variables.
- [ ] Variable collections `Color` (Light, Dark), `Spacing`, `Radius`, `Type` exist as in section 6; aliases resolve; `brand/primary` is `#F3821D` in both modes; contrast ≥ 4.5:1 text and ≥ 3:1 UI is checked in both modes.
- [ ] Dark copies exist for `T0-01 / ab-set`, `T0-03 / half`, `T2-02 / on-route`, `T2-03 / banner`.
- [ ] Every map frame shows `AttributionLabel`; no straight line on any map except `dashed-walk-offline` with `WalkLabel`.
- [ ] `SourceLabel sample` appears beside every placeholder value; no real fares, stops, or routes were invented.
- [ ] `AskBar` is visible on `T0-01`, `T0-03`, `T0-04` and on every `T3` frame that shows the map home, as a secondary pill under or near `TripBuilderCard`, with the map trip builder still primary; no tier flag hides it in the MVP. F8 frames are tagged `MVP (D31)`, and only `T3-03` (voice) is tagged optional.
- [ ] Pair names are `V.A. Rufino St` (sublabel `Legazpi Village`) and `Dela Rosa St` (sublabel `Pio del Pilar`) on every frame that shows A and B; no `Place A (TBD)` / `Place B (TBD)` remains; only the legs are `TBD (A13)`.
- [ ] Text sweep (section 2.3) returns zero hits for `Metro Manila`, `Valenzuela`, `Recto`, `OCR`, `Scan`, `camera` outside `99 Archive`.
- [ ] Archived items are in `99 Archive` with the `ARCHIVED` prefix and a sticky with reason and D-ID; nothing was deleted.
- [ ] Every frame has a `meta –` layer with Tier, PRD, US, QA, DSD, and States, and every PRD-F (F1–F9) and US (US-01–US-13) from section 4 is covered by at least one frame.
- [ ] `07 Flows` has Flows A–F wired as in section 8, or the unwired connections are listed under `Not done`.
- [ ] The Change Report lists old → new names, `Values to confirm`, and `Not done`.

## 11. Out of scope for Figma

- Product code, Compose theme export, or code generation.
- Real map tiles, map screenshots, or any imagery from a map provider. Use `MapPlaceholder` only.
- New brand assets: logo, illustration, photography, icon sets beyond the Material Symbols already in the file.
- Real hero-trip leg content (stops, fares, minutes, routes). It stays TBD until the candidate trips for the [D30](state.md#5-decisions) pair are listed ([A13](state.md#4-open-assumptions)); the pair names themselves are in scope.
- Screens for OCR signboard scanning, TTS, social feed or profiles, a browser app, iOS, or coverage beyond Makati ([PRD-F11](prd-commutenity.md#3-features-and-priorities)).
- A screen for the learned ranker (T4); it changes only the reason text.
- Android system configuration: notification channels, vibration patterns, foreground-service details.
- Editing any document under `docs/`.
