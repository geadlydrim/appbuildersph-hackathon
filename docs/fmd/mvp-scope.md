# MVP Scope

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.3
**Status:** Draft. Makati-only, map-first scope and build order decided ([D20](state.md#5-decisions), [D21](state.md#5-decisions), [D28](state.md#5-decisions)). Supersedes the Valenzuela–Recto scope and the D10 order.

This file owns the **cut line**: what ships, in what order, and what doesn't. The [PRD](prd-commutenity.md) owns behavior, and the [user stories](user-stories.md) own acceptance criteria.

## Current promise

A rider in **Makati City** opens the **Android app** with no signal and sets point A and point B on an offline map: tap, drag, search pack places, or "use my location" for A. The app computes the most efficient trip on the phone from the commute pack and draws it on stored road shapes. It shows the legs, fares, minutes, distance, walk time, the para point, and why it picked that trip. On request it shows alternatives from the algorithm and from trips other riders submitted. When the phone is online it refreshes the pack, map, and community data. During a trip, offline GPS paired with map-matching shows whether the rider is on route and fires a **para alert** before the alight stop. Riders can suggest trips and vote, and that syncs when they're back online. Later tiers add asking in words, voice, and a learned ranker.

This is **not** a turn-by-turn navigator (on-route status and the para alert are the extent of live guidance), a live transit tracker, or a social network.

## Tiers

Build strictly in order ([D28](state.md#5-decisions)). A tier starts only when the previous one is **demo-safe**: working offline, end to end, on the demo phone. Workstreams may prepare later tiers in parallel (P2 LLM and STT from now; P3 ranker data from CP2), but they merge into the demo build only in tier order.

| Tier | Name | What it adds | Features | Done when |
|---|---|---|---|---|
| **T0** | Walking skeleton | Makati pack with road shapes and the offline map. A/B pins. The best trip is computed on the phone (deterministic lexicographic order, [D16](state.md#5-decisions)) and drawn on real roads. Airplane mode. | PRD-F1, PRD-F2, PRD-F3 | [US-01](user-stories.md#us-01-pick-a-and-b-on-the-map-offline) to [US-03](user-stories.md#us-03-know-when-its-not-in-my-data) pass in airplane mode |
| **T1** | Online refresh + community | Fetch and cache pack, map, and community data when online. Alternatives. Suggest a trip, vote, local-first storage, two-phone sync. | PRD-F4, PRD-F5, PRD-F6 | US-04 to US-08 pass; [QAD gate](qad-commutenity.md#6-release-criteria) for T0+T1 |
| **T2** | In-trip tracking | On-route / off-route status and the para alert, from offline GPS and map-matching. | PRD-F7 | [US-09](user-stories.md#us-09-am-i-still-on-the-right-route) and [US-10](user-stories.md#us-10-when-should-i-para) pass on a mock-location GPS track |
| **T3** | Ask in words + voice | On-device LLM questions ("How to get from X to Y?" sets A/B), the correct-vehicle text check, and speech-to-text. | PRD-F8, PRD-F9 | [US-11](user-stories.md#us-11-ask-how-to-get-from-x-to-y-in-words) to [US-13](user-stories.md#us-13-ask-by-voice) pass; correct-vehicle eval shows 0 false "ride this" ([AI-05](qad-commutenity.md#7-ai-evaluation)) |
| **T4** | Trained ranker | An on-device learned ranker replaces the D16 ordering, **only if it beats it** on held-out known labels | PRD-F10 | [US-14](user-stories.md#us-14-ranker-learned-from-riders) passes; eval recorded |

Ranker **data collection** (scenarios, preference labels, seeded contributions) starts in parallel at CP2 ([data plan §4](data-commutenity.md#4-training-data-collection)). Ranker **training** runs alongside T1 and T2. Neither may block T0, T1, or T2. If the ranker hasn't beaten the D16 ordering by 7:00 AM, D16 ships and the pitch says so honestly.

Cut rules (times and owners in [BUILD §1](build-commutenity.md#1-build-sequence)):
- If T0 slips past 2:30 AM, cut T3 voice and T4.
- If refresh and sync aren't working by 4:00 AM, demo with the bundled pack and local contributions, and say so.
- If T2 isn't passing by 6:30 AM, demo tracking with a recorded mock-GPS route, labelled as simulated.

## Geography and modes

- **Geography:** Makati City only ([D20](state.md#5-decisions)). The hero trip is an origin–destination pair inside Makati with at least two genuinely different trips; the pair is chosen by P4 and the team ([A13](state.md#4-open-assumptions)). Build its facts from `collected` and `known` data; `mock` data may fill adjacent stops only and is labelled as sample data ([D13](state.md#5-decisions)).
- **Modes:** the modes in the pack schema ([data plan §2](data-commutenity.md#2-commute-pack-schema)): jeepney, city bus, MRT, LRT, UV Express, P2P, tricycle, walking. The Makati pack includes only those that were actually recorded. Rail geometry follows OSM track ([D22](state.md#5-decisions)).
- **Map:** a Makati PMTiles extract rendered offline ([D23](state.md#5-decisions)), with OSM attribution.

## Parked / Won't (later, not this event)

| Item | Why |
|---|---|
| OCR signboard scanning | Dropped ([D26](state.md#5-decisions)). The correct-vehicle check is a text match instead ([D27](state.md#5-decisions)). |
| TTS (spoken answers) | Not planned ([D26](state.md#5-decisions)). The para alert is vibration, a notification, and an on-screen banner. |
| Coverage beyond Makati, including the earlier Valenzuela–Recto corridor | Accuracy beats coverage ([D20](state.md#5-decisions)). The corridor work is superseded history ([D15](state.md#5-decisions)). |
| Full social layer: feed, posts, comments, profiles, accounts (original CommuteNity) | Scope. The thin contribution layer carries the community value for now. |
| Browser app | Mobile first ([D8](state.md#5-decisions)). The browser version comes second, after the event. |
| iOS | Android first |
| Moderation workflow for contributions | Demo contributions come from the team. Real users would need moderation. |
| On-device routing engine | Road shapes are precomputed at data-build time ([D22](state.md#5-decisions)). |

## In and out of scope

| In scope | Out of scope |
|---|---|
| Map trip builder: A/B pins by tap, drag, search, "use my location" | Turn-by-turn live navigation (voice or step-by-step guidance) |
| Best trip drawn on stored road shapes over an offline Makati map | Real-time vehicle location (needs hardware or third-party feeds) |
| Online refresh and cache of pack, map, community data, and first/last-mile foot routes | Cloud LLM answering the core question. It breaks the theme; cloud may only be secondary ([JUDGING](JUDGING.md#rules)). |
| In-trip tracking: on-route / off-route status, current leg, distance to the para point, para alert | User-facing accounts. Contributions use an internal anonymous Auth identity; riders never sign up ([D17](state.md#5-decisions)). |
| Alternatives labelled Algorithm / Community; suggest, vote, two-phone sync | Payments and fare collection (regulatory; out of lane) |
| Ask in words, correct-vehicle text check, speech-to-text (T3) | Ride-hailing (separate category) |
| Learned ranker, shipped only if it wins (T4) | Bulk-downloading tiles from tile.openstreetmap.org (its usage policy forbids offline and bulk use, [D23](state.md#5-decisions)) |

## Definition of done (MVP = T0 + T1 + T2)

- With the demo phone in airplane mode, the rider sets A and B on the offline Makati map and gets an auto-picked best trip drawn on road shapes. Its legs, fares, minutes, distance, walk time, and para point match the pack records, and the app shows why it picked that trip. The only straight line on the map is the dashed, labelled offline first/last-mile walk ([D22](state.md#5-decisions)).
- "Show alternatives" lists other candidates, each labelled Algorithm or Community.
- A point outside Makati, or a pair with no route, returns "not in my data", with no invented trip.
- After a refresh while online, the app works in airplane mode with the refreshed pack, map, and community data. An interrupted download recovers.
- A suggestion and a vote made offline are stored on the phone. They sync when the phone is back online and appear as alternatives on a second phone. A sync or refresh failure never blocks the UI.
- With an active trip and a mock-location GPS track, the app shows on-route, deviation, and return, and fires the para alert once at the threshold ([D25](state.md#5-decisions)). If T2 hits its cut rule, tracking is demoed on a recorded mock-GPS route and labelled as simulated.
- Trip computation, ranking, and tracking run on the phone and make zero network requests. Refresh is the only network use besides contribution sync ([D24](state.md#5-decisions)).
- The README discloses the models, data sources (including OSM, Protomaps, and the routing engine used for shapes), tools, the team-generated contribution data, and the reused CommuteNity ideas. OSM attribution is shown on the map.
