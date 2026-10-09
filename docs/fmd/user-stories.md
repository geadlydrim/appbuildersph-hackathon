# User Stories

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.2
**Status:** Draft

Format: *As a [persona], I want [capability] so that [outcome].* Acceptance criteria use Given/When/Then.

Related docs:
- Personas: [IDEA §2](idea-commutenity.md#2-who-its-for)
- Tiers: [MVP scope](mvp-scope.md#tiers)
- Feature IDs: [PRD §3](prd-commutenity.md#3-features-and-priorities)

"Offline" in acceptance criteria means the demo phone is in airplane mode and the app makes no outbound network requests.

The old CommuteNity stories (US-100+ feed and auth; US-001–016 catalog) are retired. The route-picker and contribution ideas come back below in their thin form ([D2](state.md#5-decisions)).

---

## T0: Walking skeleton

### US-01: Ask a trip question offline
**PRD-F1, PRD-F2**

> As a **New Arrival**, I want to type "paano pumunta sa X galing Y" in my own words so that I get a commute plan without signal or load.

- Given the pack is on the phone and the phone is offline, when I ask about a covered origin–destination pair in Taglish or English, then I see the auto-picked route: an ordered list of legs, each with mode, where to board, and where to alight. It appears within the [latency target](sdd-commutenity.md#7-non-functional-targets).
- Given my question uses a landmark or alias that's in the pack, when I ask, then it resolves to the right stop.
- Given the origin or destination is ambiguous, when I ask, then I pick from the matching places. The app doesn't guess.

### US-02: See fares, time, where to get off, and why this route
**PRD-F2**

> As an **Occasional Commuter**, I want the fare, estimated time, and "para" point for each leg, plus a one-line reason why this route was picked, so that I trust the pick and don't miss my stop.

- Given an answer, when it renders, then each leg shows a fare from the pack, the stop or landmark to alight at, and minutes if known.
- The answer shows the pick reason built from the scored features, for example "fewest transfers · ₱X · ~N min".
- Given a leg has no fare in the pack, when the answer renders, then that leg reads "fare unknown". No number is invented.
- Every fare and stop shown matches a record ([AI-01](qad-commutenity.md#7-ai-evaluation)).
- Given any leg uses a `mock` record (stop, fare, or minutes), when the answer renders, then that value carries a "sample data" marker ([D13](state.md#5-decisions)).

### US-03: Know when it's not in my data
**PRD-F2**

> As a **Daily Rider** off my corridor, I want the app to admit when it doesn't know so that I don't trust a made-up route.

- Given an origin or destination outside coverage, when I ask, then I see "not in my data" and which areas are covered.
- Given an off-topic question, when I ask, then the app redirects me to trip questions and produces no route.

## T1: Demo-ready plus community

### US-04: Install once, use offline
**PRD-F4**

> As a **New Arrival** on prepaid load, I want to download everything once on Wi-Fi so that later questions cost no data.

- Given first launch on a network, when setup completes, then the app shows that the models and pack are stored on the phone, with their size.
- Given setup completed, when I reopen the app in airplane mode, then it answers US-01 with no network requests.
- Given setup was interrupted, when I reopen the app, then it resumes or retries without getting stuck in a broken state.

### US-05: See alternatives on request
**PRD-F5**

> As a **Daily Rider**, I want to tap "other routes" so that I can choose a cheaper, less crowded, or more familiar option than the auto pick.

- Given an answer, when I tap "Show alternatives", then I see up to N other candidates. Each is ranked, shows its reason line, and is labelled **Algorithm** or **Community** (with its vote count).
- Given no alternatives exist, when I tap, then I see "no other routes yet. Suggest one?".
- This works offline using everything synced so far.

### US-06: Suggest a route
**PRD-F6**

> As a **Daily Rider** who knows a shortcut, I want to submit my route for a trip so that other riders see it as an alternative.

- Given an origin–destination pair, when I build a route from pack stops and routes leg by leg, with an optional note, and submit it, then it's saved on the phone right away, even offline, and marked "not yet synced".
- A submission that references unknown stops or breaks leg continuity is rejected with the reason.

### US-07: Vote "this worked"
**PRD-F6**

> As a **rider** who just took a route, I want to mark it worked or didn't so that good routes rise for everyone.

- Given any shown route (algorithm or community), when I vote up or down, then my vote is stored locally. Voting again the same way clears it.
- My vote influences ranking only through synced aggregate counts and the ranker's features. It is never treated as fact for fares or stops.

### US-08: Sync when online
**PRD-F6**

> As a **rider**, I want my contributions to upload and others' to download automatically when I have signal so that offline use never waits on the network.

- Given unsynced contributions and connectivity, when the app syncs, then they upload and are marked synced. Contributions from others download into the local overlay.
- Given phone A suggests a route and syncs, when phone B syncs, then B shows that route as a Community alternative for the same pair.
- Given sync fails, then nothing is lost. The app retries later and never blocks asking or answering.

## T2: Trained ranker

### US-09: A ranker that learned from riders
**PRD-F7**

> As the **team**, we want the route pick learned from preferences and votes so that it matches what riders actually choose better than our hand-set weights do.

- Given the held-out preference set ([data plan §6](data-commutenity.md#6-evaluation-sets)), when we compare the trained ranker with the deterministic baseline, then the ranker ships only if it's better on the agreed metric.
- It runs on the phone, offline. Its training data (team-generated, disclosed as such), method, and real results go in the README. No fabricated benchmarks ([JUDGING](JUDGING.md#rules)).

## T3: Signboard check

### US-10: Check a jeepney signboard
**PRD-F8**

> As a **New Arrival** at a terminal, I want to point my camera at a signboard so that I know in seconds whether it's the right jeep for my trip.

- Given an active trip and a clear signboard, when I scan, then I see the text the app read and either "ride this" (it matches one of my legs) or "wrong jeep".
- Given an unreadable photo, when I scan, then I see "can't read it, try again". The app shows no guessed verdict.
- Inference runs on the phone. The image never leaves it.

## T4: Voice

### US-11: Ask by voice
**PRD-F9**

> As an **Occasional Commuter** holding a handrail, I want to speak my question so that I don't have to type.

- Given microphone permission and an offline phone, when I speak a Taglish trip question, then the transcript appears for me to confirm and then follows the US-01 flow.
- Audio is processed on the phone. It is never stored or sent.
