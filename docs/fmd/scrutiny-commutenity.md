# Scrutiny Gate: CommuteNity

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.4
**Owner:** Project owner
**Status:** Draft. Re-run at about 10:40 PM on Oct 9 for the Makati-only, map-first scope ([D20](state.md#5-decisions)–[D28](state.md#5-decisions)). Supersedes the v0.2 verdict. Updated at about 11:05 PM for the hero pair ([D30](state.md#5-decisions)) and the Local-AI floor ([D31](state.md#5-decisions)), which mitigates FC-18.
**Last reconciled:** 2026-10-10
**IDEA:** [Idea brief](idea-commutenity.md)

## 1. Verdict

**Decision: PROCEED WITH FIXES.**

The theme fit is still strong: the whole trip works in airplane mode, and GPS tracking works without data. The risk got bigger with the re-scope. At about 10:40 PM, roughly 11 hours remain before the 10:00 AM code freeze. On top of the pack, the Android app, and the sync backend, a team of 4 now has to deliver five parts nobody has proven on the POCO X6 5G:
- an offline Makati map (MapLibre with PMTiles)
- a road-shape precompute pipeline
- a foreground tracking service with map-matching
- online refresh
- on-device LLM (release-critical per [D31](state.md#5-decisions)), with optional STT and ranker

The map is the first thing a judge sees, so a map that fails sinks T0. The fixes keep a demo available at every checkpoint.

**The sharpest finding (mitigated by [D31](state.md#5-decisions)):** as scoped by D28, the MVP (T0 + T1 + T2) contained no learned model, and all on-device AI landed at T3 and T4. The rules require that "a meaningful part of AI inference executes locally" ([JUDGING](JUDGING.md#rules)), and the Local AI criterion is 25% of the score. D31 makes PRD-F8 (ask in words, the on-device LLM) release-critical, so the MVP is now T0 + T1 + T2 + PRD-F8 and carries the Local AI claim. Voice and the ranker are cut before F8, and if the LLM speed test fails the fallbacks stay on-device (a smaller model, then llama.cpp, then the rule-based parser plus on-device embedding place search). See FC-18.

| Fix | Downstream home |
|---|---|
| Spike the offline map first: MapLibre renders a Makati PMTiles file on the POCO at CP1, with a fallback provider ready | [A14](state.md#4-open-assumptions), [BUILD §1](build-commutenity.md#1-build-sequence) |
| Facts come from the pack via deterministic code; models parse, rank, and phrase; the LLM never decides the correct-vehicle verdict | [SDD §1](sdd-commutenity.md#1-architecture), [§3](sdd-commutenity.md#3-routing-contract), [D27](state.md#5-decisions) |
| Strict tiers, with T0 + T1 + T2 + PRD-F8 as the MVP ([D31](state.md#5-decisions)); hard cut rules if checkpoints slip | [MVP scope](mvp-scope.md#tiers), [BUILD §1](build-commutenity.md#1-build-sequence) |
| The hero pair is set ([D30](state.md#5-decisions): Ayala Center to Dela Rosa St., Pio del Pilar). List and verify at least 2 real candidate trips for it before pack v0; settle the shape routing engine and its terms before pack v0 | [A13](state.md#4-open-assumptions), [A15](state.md#4-open-assumptions), [data plan §3](data-commutenity.md#3-collection-protocol) |
| Build the pack validator to reject straight-line or mismatched shapes, so a bad shape fails the build, not the demo | [QAD QA-16](qad-commutenity.md#3-scenarios) |
| Tune tracking thresholds on mock tracks first and on a real Makati trip when possible; if not passing by 6:30 AM, demo with a labelled simulated route | [A16](state.md#4-open-assumptions), [QAD §2](qad-commutenity.md#2-data-and-environment) |
| Keep the LLM work running in parallel from now, so PRD-F8 (ask in words) merges behind its flag as soon as T0 is demo-safe (CP-F8, ~4:30 AM). Drop voice and the ranker before dropping the LLM, because the LLM carries the Local AI claim. *(Decided: [D31](state.md#5-decisions); answers §7.)* | [BUILD §1](build-commutenity.md#1-build-sequence), FC-18 |
| The ranker ships only if it beats the deterministic baseline on held-out pairs | [Data plan §5](data-commutenity.md#5-training-plan) |
| Contribution and preference data is team-generated, and disclosed as such | [Data plan §4](data-commutenity.md#4-training-data-collection), [CLR §5](clr-commutenity.md#5-ip-provenance-and-disclosure) |
| Fresh repo; disclose what we reused from the original CommuteNity concept and the CommuteNity-Web ideas | [D3](state.md#5-decisions), [D21](state.md#5-decisions) |

## 2. Claim & Reference Audit

**Coverage:** 18 load-bearing claims. 3 verified, 15 unverified, 0 contradicted. FC-9 is retired.

| ID | Category | Claim | Finding | Source | Severity if wrong |
|---|---|---|---|---|---|
| FC-1 | Rules | Meaningful inference must run locally; the cloud can only be secondary | Verified | [JUDGING](JUDGING.md#rules) | Fatal |
| FC-2 | Rules | A pre-existing project can be disputed; existing code and assets must be disclosed | Verified | [JUDGING](JUDGING.md#rules) | Fatal |
| FC-3 | Rules | No live deployment is required; the repo must let judges recreate the project | Verified | [JUDGING](JUDGING.md#submission) | Significant |
| FC-4 | Feasibility | A 0.5–2B quantized LLM runs on the team's demo phone within the latency target | Unverified; the LLM speed test (issue #8) is running. It picks the F8 model and runtime; a failure falls back to a smaller model, llama.cpp, then the rule-based parser plus on-device embeddings, never the cloud ([D31](state.md#5-decisions)). | — | Significant |
| FC-5 | Feasibility | Kotlin bindings for the chosen runtime integrate in under about 2 hours | Unverified | — | Significant |
| FC-6 | Feasibility | Team-generated preferences (4 raters) are enough for a ranker to beat hand-set weights on held-out pairs | Unverified; plausibly *not*, given the small data | — | Minor (the D16 order ships) |
| FC-7 | Data | Per-segment minutes for Makati can be estimated credibly by the team | Unverified | — | Significant ("efficient" degrades to "cheapest / fewest transfers") |
| FC-8 | Data | Current official fares for Makati modes are findable and citable | Unverified | — | Significant ("fare unknown" otherwise) |
| FC-9 | — | Retired: signboard OCR was dropped ([D26](state.md#5-decisions)) | — | — | — |
| FC-10 | Feasibility | Whisper-class models on the phone transcribe Taglish acceptably | Unverified | — | Minor (T3 voice only, cut first) |
| FC-11 | Problem | Riders often lack signal or data at the moment they need to decide | Unverified; plausible from team experience. GPS still works without data, which helps the tracking story. | — | Significant for the pitch |
| FC-12 | Rules | Mock data is allowed | Owner-reported organizer allowance. It is not in the written briefing. | Owner, 2026-10-09 | Significant. Mitigation: label everything and disclose it; confirm in Telegram if unsure. |
| FC-13 | Feasibility | MapLibre Native Android renders a local PMTiles file offline on the POCO X6 5G, and the Makati extract is small enough to bundle or download once ([A14](state.md#4-open-assumptions)) | Unverified; CP1 map spike. Protomaps documents `pmtiles extract` for regional cutouts ([Protomaps downloads](https://docs.protomaps.com/basemaps/downloads)), which covers the extract, not MapLibre Android's PMTiles support. | — | **Fatal to T0** unless the fallback works (a MapLibre offline region from a provider whose terms allow offline) |
| FC-14 | Data | Shapes precomputed through an open routing engine are allowed by its terms and are plausible paths for jeepney, bus, UV, and walk legs ([A15](state.md#4-open-assumptions)) | Unverified. The OSRM wiki limits its public demo server to reasonable, non-commercial use at 1 request per second, with no guarantees ([OSRM demo server](https://github.com/Project-OSRM/osrm-backend/wiki/Demo-server)); GraphHopper terms are unchecked. A driving profile may not follow the real jeepney path. | — | Significant (fall back to a self-hosted engine, or hand-checked shapes for the hero trip only) |
| FC-15 | Rules | Protomaps and OSM data may be bundled in the APK or hosted by us with only OSM attribution | Partly verified: Protomaps calls its basemap an ODbL Produced Work needing OSM attribution. The OSMF tile policy forbids offline use of `tile.openstreetmap.org` ([OSMF policy](https://operations.osmfoundation.org/policies/tiles/)), so we don't use it. Unchecked: the basemap style, fonts, and sprites; whether stored shapes are a derivative database. | [CLR §5](clr-commutenity.md#5-ip-provenance-and-disclosure) | Significant |
| FC-16 | Feasibility | GPS in Makati is accurate enough for an off-route flag (more than 100 m for at least 30 s) and a para alert about 300 m before the stop ([A16](state.md#4-open-assumptions)) | Unverified. Ayala CBD high-rises and MRT-3 or EDSA overhead are likely to degrade it. [INFERENCE] | — | Significant (wrong alert on a physical-world path; fall back to a labelled simulated route) |
| FC-17 | Data | Makati has at least 2 genuinely different candidate trips between the D30 pair (Ayala Center to Dela Rosa St., Pio del Pilar) that the team can verify ([A13](state.md#4-open-assumptions)) | Unverified; the pair is decided ([D30](state.md#5-decisions)), the candidate trips are listed and verified by the team before pack v0 | — | Significant (no real alternatives makes "best trip" and "alternatives" look trivial) |
| FC-18 | Rules | The demo satisfies "a meaningful part of AI inference executes locally" | **Mitigated by [D31](state.md#5-decisions) and [D32](state.md#5-decisions).** The T0–T2 path has no learned model (map-matching, ordering, and the para alert are deterministic), but PRD-F8 (the on-device LLM) is release-critical and part of the MVP. The F8 parser passed the LLM speed test on the demo phone (Gemma 4 E2B, worst p95 2.84 s, 10/10 exact on 10 questions); the claim still needs F8 merged and passing US-11 and US-12. Any later failure falls back to on-device inference only; never a cloud model. | [JUDGING](JUDGING.md#rules); [RESULTS](https://github.com/geadlydrim/appbuildersph-hackathon/blob/prototype/llm-speed-test/spikes/llm-speed-test/RESULTS.md) | **Fatal** if F8 and every on-device fallback fail on the demo phone |
| FC-19 | Feasibility | A foreground service keeps tracking with the screen off on the POCO X6 5G (HyperOS) | Unverified. Vendor battery limits are a known risk on Android phones. [INFERENCE] | — | Significant (QA-11 background case) |

### 2.1 Reference Integrity

External sources read on 2026-10-09: the OSMF tile policy, the Protomaps basemap downloads page, and the OSRM demo-server wiki. MapLibre Native Android PMTiles support, GraphHopper terms, the Protomaps style, font, and sprite terms, and the rail geometry source have **not** been read. Models, runtimes, fare sources, and the backend get cited with dates when they're chosen.

## 3. Gap Analysis

| Gap | Needed by | Treatment |
|---|---|---|
| Offline map not proven on our phone | T0 | A14; CP1 map spike |
| LLM runtime and model | PRD-F8 (MVP) | Decided and measured on our phone ([D32](state.md#5-decisions)): Gemma 4 E2B on LiteRT-LM with the hybrid parser. Still to prove: the 30-question eval |
| The D30 pair's candidate trips not yet listed or verified, and no pair minutes | Pack, demo | A13 (before pack v0); data plan §3 |
| No road shapes, and the routing engine's terms are unread | Pack v0 | A15 (before pack v0); QA-16 |
| GPS behaviour in Makati unknown | T2 | A16 (during T2) |
| Sync backend not deployed or tested | T1 | D17; the CP4 fallback is a bundled pack plus local contributions |
| No problem evidence | Pitch | [VALIDATION](val-commutenity.md) |
| Nothing AI in the MVP | Submission | Mitigated by D31 (FC-18): PRD-F8 is in the MVP; §7 answered |

## 4. Assumption Stress-Test

**Load-bearing assumption:** a rider can set A and B on an offline Makati map, see a trip on real roads that locals would call the best, and be told when to say "para" while riding. The rider can also ask in words and check the vehicle through the on-device LLM (PRD-F8, part of the MVP per [D31](state.md#5-decisions)).

**Strongest argument against it:**
- The stack is wide and new to the team: MapLibre with PMTiles, a shape pipeline, a foreground service, refresh, and on-device models. Any one failing can block the demo, and the map is on screen the whole time.
- A driving profile may draw a road that a jeepney doesn't take, so a "real roads" line can be confidently wrong.
- Makati may have few genuinely different trips, so "best trip" and "alternatives" look trivial.
- GPS between high-rises can put the rider on the wrong street, which makes the para alert early, late, or absent.
- Four teammates' preferences are a thin basis for claiming a "learned" ranker.

**Finding:** it holds with caveats.
- The map spike at CP1 settles the largest unknown within about an hour. If it fails, switch to the fallback provider before any UI work.
- The pack validator, plus checking the hero trip by hand against `collected` and `known` knowledge, bounds the wrong-road risk.
- The D30 pair was chosen by the owner; at least 2 genuinely different candidate trips for it must still be verified (A13). If it turns out to have fewer, that is a revisit trigger on D30.
- The tracking claim stays modest: "on-route status and a para alert, tuned on N mock tracks and M real trips", and it's labelled as simulated if it was.
- The ranker claim stays modest: "learned from our labelled preferences; beats or doesn't beat the baseline by X on N held-out pairs."

**Cheapest tests:**
- **Map:** the CP1 spike renders a Makati PMTiles file in airplane mode on the POCO.
- **Shapes:** route the hero trip's segments through the engine, view them on the map, and compare with the known path.
- **Tracking:** replay `on-route`, `deviate-return`, and `blip` GPX tracks ([QAD §2](qad-commutenity.md#2-data-and-environment)) with a mock-location app.
- **Place search and parser (PRD-F8):** 30 Taglish place queries and questions through the search and the LLM with no UI. Aim for at least 80% exact origin and destination.
- **Ranker data:** the four teammates rank 10 scenarios independently. If they disagree heavily, a ranker can't learn a consistent preference, so add the user's stated preference as a feature.

### 4.1 Audience Stress

| Check | Finding |
|---|---|
| User and pain clear in ten seconds | Pass: "Which way? No signal." |
| Specific user and moment | Pass: a New Arrival in Makati |
| Repeatable one-liner | Pass: "Pick two points on the map, offline. It draws the trip locals take and tells you when to say para." |
| Why local? | Pass: no signal, no load, seconds to decide, private trips, and GPS that works with no data |
| Why change current behavior (asking strangers)? | At risk; needs validation |
| Where is the AI? | Answered by [D31](state.md#5-decisions): the on-device LLM (PRD-F8) is in the MVP. Still needs F8 to pass on the demo phone (FC-18). |

## 5. Feasibility & Scope

- **T0 (target CP3, about 1:30 AM):** bigger than before. It needs the map spike, pack v0 with shapes, A/B pins, candidate generation, and the drawn trip. Feasible only if the CP1 spike passes. If T0 slips past 2:30 AM, the cut rule drops T3 voice and T4, never F8 ([D31](state.md#5-decisions)).
- **T1 (CP4, about 3:30 AM):** adds refresh and sync, which is where hackathons lose hours. Keep the backend to two tables, a push/pull, and file storage. If it isn't working by 4:00 AM, demo with the bundled pack and local contributions, and say so.
- **T2 (CP5, about 5:30 AM):** the foreground service and map-matching are conventional, but GPS accuracy is the risk. If it isn't passing by 6:30 AM, demo tracking with a recorded mock-GPS route, labelled as simulated.
- **PRD-F8 (CP-F8, about 4:30 AM):** built in parallel and merged behind its flag once T0 is demo-safe, independent of T1 and T2. If it isn't passing by 6:30 AM, ship the smallest model that produces valid JSON with template phrasing, plus the D27 matcher. F8 carries the local-AI claim (FC-18).
- **T3 voice and T4 (5:30–8:00 AM):** optional; the ranker is cheap to train and expensive to *prove*, and replaces the D16 order only if it wins by 7:00 AM. Both are cut before F8.

## 6. Risk Pre-flight

- AI risks are covered in [AIA](aia-commutenity.md): invented facts, a false "ride this", a wrong para alert, location privacy, a poisoned contribution, and ranker overfit.
- Location and tracking, online lookups, and map and routing-engine terms are covered in [CLR](clr-commutenity.md).
- There are no payments and no deployment.

## 7. Blocking Questions

None block the docs. Building T0 is blocked on A13 (the candidate trips for the D30 pair), A14, and A15 ([state](state.md#4-open-assumptions)); the hero pair itself is decided ([D30](state.md#5-decisions)). The PRD-F8 model and runtime are decided ([D32](state.md#5-decisions)).

**Owner question (answered):** the owner has decided that a submission whose MVP has no learned model is not acceptable. [D31](state.md#5-decisions) makes PRD-F8 (the LLM) release-critical for the 10:00 AM submission, with voice (PRD-F9) and the ranker giving way to it, not the reverse. This amends the strict tier order in [D28](state.md#5-decisions).
