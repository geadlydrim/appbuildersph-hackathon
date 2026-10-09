# MVP Scope

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.2
**Status:** Draft. Build order decided ([D10](state.md#5-decisions)).

This file owns the **cut line**: what ships, in what order, and what doesn't. The [PRD](prd-commutenity.md) owns behavior, and the [user stories](user-stories.md) own acceptance criteria.

## Current promise

A rider on a covered Metro Manila corridor opens the **Android app** with no signal and asks a trip question in Taglish or English. On-device AI parses the question. The app automatically picks the most efficient route from the commute pack on the phone and shows the legs, boarding and alighting points, fares, and an estimated time. On request it shows alternatives from the algorithm and from routes other riders submitted. Riders can suggest routes and vote, and that syncs when they're back online.

This is **not** a turn-by-turn navigator, a live transit tracker, or a social network.

## Tiers

Build strictly in order. A tier starts only when the previous one is **demo-safe**: working offline, end to end, on the demo phone.

| Tier | Name | What it adds | Features | Done when |
|---|---|---|---|---|
| **T0** | Walking skeleton | Pack on the phone. A typed question goes through parse → candidate routes → deterministic lexicographic order, which auto-picks the best trip. Grounded answer. Pretrained models only. | PRD-F1, PRD-F2, PRD-F3 | [US-01](user-stories.md#us-01-ask-a-trip-question-offline) to [US-03](user-stories.md#us-03-know-when-its-not-in-my-data) pass in airplane mode |
| **T1** | Demo-ready plus community | First-run model download. "Show alternatives". Suggest a route, vote "this worked", local-first storage, sync when online. | PRD-F4, PRD-F5, PRD-F6 | US-04 to US-08 pass; [QAD gate](qad-commutenity.md#6-release-criteria) for T0+T1 |
| **T2** | Trained ranker | An on-device learned ranker replaces the deterministic baseline, **only if it beats it** on held-out preferences | PRD-F7 | [US-09](user-stories.md#us-09-a-ranker-that-learned-from-riders) passes; eval recorded |
| **T3** | Signboard check | Camera → on-device OCR → match against the trip's legs | PRD-F8 | [US-10](user-stories.md#us-10-check-a-jeepney-signboard) passes on held-out photos |
| **T4** | Voice | Spoken Taglish → on-device speech-to-text → same flow as T0 | PRD-F9 | [US-11](user-stories.md#us-11-ask-by-voice) passes |

Ranker **data collection** (scenarios, preference labels, seeded contributions) starts in parallel at CP2 ([data plan §4](data-commutenity.md#4-training-data-collection)). Ranker **training** runs alongside T1. Neither may block T0 or T1. If the ranker doesn't beat the baseline by the feature freeze, the deterministic lexicographic order ships and the pitch says so honestly.

## Geography and modes

- **Geography:** the Valenzuela–Recto corridor, with the hero trip from Malanday to the Recto area. It has a direct Malanday–Recto e-jeep candidate and a Malanday → LRT-1 Monumento → LRT-1 Doroteo Jose → walk candidate. Build these from `collected` and `known` data; `mock` data may cover adjacent stops only and is labelled as sample data ([D13](state.md#5-decisions), [D15](state.md#5-decisions)).
- **Modes:** jeepney, city bus, MRT, LRT, UV Express, P2P, tricycle, walking.

## Parked (later, not this event)

| Item | Why parked |
|---|---|
| Full social layer: feed, posts, comments, profiles, accounts (original CommuteNity) | Scope. The thin contribution layer carries the community value for now. |
| Browser app | Mobile first ([D8](state.md#5-decisions)). The browser version comes second, after the event. |
| iOS | Android first |
| Moderation workflow for contributions | Demo contributions come from the team. Real users would need moderation. |
| Map rendering of routes | Offline map tiles add weight. The leg card carries the demo. |
| Wider geography | Accuracy beats coverage |

## Out of scope

| Feature | Reason |
|---|---|
| Accounts and auth | Contributions use an anonymous device ID ([A9](state.md#4-open-assumptions)) |
| Cloud LLM answering the core question | Breaks the theme. Cloud may only be secondary ([JUDGING](JUDGING.md#rules)). |
| Turn-by-turn live navigation | Needs real-time GPS. It's a different product. |
| Real-time vehicle location | Needs hardware or third-party feeds |
| Payments and fare collection | Regulatory; out of lane |
| Ride-hailing | Separate category |

## Definition of done (MVP = T0 + T1)

- With the demo phone in airplane mode, a Taglish trip question for a covered corridor returns an auto-picked route. Its legs, boarding and alighting points, and fares match the pack records, and the app shows why it picked that route.
- "Show alternatives" lists other candidates, each labelled as algorithm or community.
- A question outside the pack returns "not in my data", with no invented route.
- A route suggestion and a vote made offline are stored on the phone. They sync when the phone is back online and appear as alternatives on a second phone.
- Every AI step in the answer path runs on the phone and makes zero network requests.
- The README discloses the models, data sources, tools, the team-generated contribution data, and the reused CommuteNity concepts.
