# Design Specification: CommuteNity

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.2
**Owner:** Implementer
**Status:** Draft
**Last reconciled:** 2026-10-09
**PRD:** [Product requirements](prd-commutenity.md)

## 0. Brand Stance

- **Mode:** a task-first product.
- **Reference:** the jeepney itself: painted signboards, route names in bold caps, the warm livery, and the rider's shout of *"Para!"*.
- **Voice:** a kind *suki* who knows the route. Short, comfortable with Taglish, never cute at the cost of clarity.
- **Human detail:** every answer says where to say "para", using the landmark people actually see.

The **"On-device · Offline OK"** badge belongs to the brand. It makes the Local AI value visible.

**Community labels** ("Community · 12 👍") make the people behind the alternatives visible without turning the app into a social feed.

Carried over from the CommuteNity "Jeepney Gold" baseline: amber primary, achromatic base, and the transit mode palette. Avoid glowing AI gradients, stock photos, and chat-bubble clutter.

## 1. Surface and Hierarchy

A native Android app (Jetpack Compose, Material 3), phone portrait only.

1. **Top bar:** CommuteNity wordmark, on-device/offline badge, sync status icon.
2. **Ask bar:** text field, mic (T4), submit. Example questions show when empty.
3. **Best-route card:**
   - summary line: total fare, minutes, transfers
   - reason chip, e.g. "Fewest transfers"
   - ordered legs, each with a mode chip, "Sakay sa" (board), "Para sa" (alight), fare, minutes, and the signboard text to look for
   - vote buttons (👍 worked / 👎 didn't)
4. **"Show alternatives"** opens a bottom sheet: a ranked list where each row has a source label (Algorithm / Community + votes), its reason, fare, and minutes. Tap a row to expand its legs.
5. **"Suggest a route"** opens a full-screen leg builder: pick a route, then board and alight stops from pack lists, add legs, add an optional note, save.
6. **"Check signboard"** (T3) on jeepney and UV legs opens a camera screen with a large verdict.

## 2. Theme and Type

- **Primary color:** amber-orange, about `oklch(0.72 0.17 55)`, mapped to the Material 3 `primary`. Neutral surfaces. Dynamic color off, so the brand stays consistent.
- **Mode palette:** jeepney amber/gold, bus blue, MRT purple, LRT red-orange, UV teal, P2P green, tricycle yellow, walking gray.
- **Source labels:** Algorithm is neutral outline; Community is amber tonal.
- **Sample data marker:** a small neutral "sample" tag with an info icon on any value or route that comes from `mock` data. It is visible but quiet, and it is never hidden for the demo.
- **Verdict colors:** ride is green, wrong is red, unreadable is neutral. Each always has a text label and an icon.
- **Type:** bundled fonts only (no downloadable fonts at runtime). Headings use a geometric display face (Space Grotesk or Plus Jakarta Sans, bold); body uses the system or bundled sans.
- **Dark mode:** follows the system. The amber keeps the same perceived lightness.

## 3. Layout

- 4dp base grid and 16dp screen padding.
- Touch targets ≥ 48dp.
- Leg text ≥ 16sp with stop names in bold, readable at arm's length on a moving jeep.
- The primary action stays reachable by thumb at the bottom.

## 4. Components and States

| Component | Default | Active | Disabled / error |
|---|---|---|---|
| Ask bar | "Saan ka pupunta?" | Focus ring in amber | Disabled while models load, with progress shown |
| Best-route card | Legs, reason chip, votes | Expand a leg for notes | "Wala pa sa data ko 'yan. Covered: …" |
| Alternatives sheet | Ranked rows | Expanded row | "Wala pang ibang ruta. Mag-suggest?" |
| Leg builder | Empty leg | Picking route/stop | Invalid leg, with the reason |
| Vote | Outline thumbs | Filled, plus an "unsynced" dot until synced | — |
| Sync icon | Synced (timestamp) | Syncing | Offline / will retry. Never blocks the UI. |
| Setup screen | Size and progress | — | Retry. Never shows "ready" until verified. |
| Signboard verdict (T3) | Text read plus a large verdict | — | "Hindi mabasa, subukan ulit" |

**Copy:**
- Loading: "Nag-iisip sa phone mo…"
- Fare unknown: "Pamasahe: hindi alam"
- Saved suggestion: "Na-save sa phone mo · isi-sync pag may signal"
- Ride: "Sakay ka dito ✓"
- Wrong: "Hindi 'to. Hanapin ang '<signboard>'"

## 5. Motion

Use Material defaults. Progress is shown as text, not just a spinner. Respect the system's remove-animations setting. Never show "done" before the result exists.

## 6. Accessibility

- TalkBack labels on every control, including the mode chips and verdicts.
- Status changes are announced.
- Contrast is 4.5:1 for text and 3:1 for UI elements, in both themes.
- Fares are written as "₱" plus the number.
- Mode and source are never shown by color alone.

## 7. Density Decisions

- One best route is expanded.
- The alternatives sheet shows at most 5 rows.
- Reason lines are at most 4 words.
- No chat history, feed, or profiles.

## 8. Quality Gate

Before the demo, check:
- TalkBack pass on the main path
- both themes
- small and large phones
- every state in [PRD §5](prd-commutenity.md#5-app-flow-and-ux-intent)
- the badge and sync icon tell the truth in airplane mode

No P0 or P1 UI defects may be open. Covered by QAD QA-01..QA-06.
