# Validation Brief: CommuteNity

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.4
**Owner:** Project owner
**Status:** Draft. Tests and kill criteria updated for the Makati-only, map-first scope ([D20](state.md#5-decisions)–[D28](state.md#5-decisions)), the hero pair ([D30](state.md#5-decisions)), and the Local-AI floor ([D31](state.md#5-decisions)).
**Last reconciled:** 2026-10-10
**IDEA:** [Idea brief](idea-commutenity.md)

## 1. Problem Evidence

**None collected yet.** The problem statement in [IDEA §1](idea-commutenity.md#1-the-spark) rests on two things, and neither is cited evidence:
- the team's own commuting experience
- the original CommuteNity thesis that commute knowledge lives in riders' heads

**Accepted gaps:**
- no interviews
- no usage data
- no measured figures on signal or data costs
- no measurement of how well phone GPS performs in Makati; the first numbers come from our own tests ([A16](state.md#4-open-assumptions))
- no real community contributions; all are team-generated during the event
- part of the pack, including some road shapes, is `mock` (synthetic). It is labelled and excluded from any real-world claim.

**What we can honestly show at Demo Day:**
- an offline demo on the phone, in airplane mode
- the Makati hero trip, Ayala Center to Dela Rosa Street, Pio del Pilar ([D30](state.md#5-decisions)), on real roads, with `collected` or `known` facts
- tracking with a mock-location track if it isn't field-tested, labelled as simulated
- real eval numbers from [QAD §7](qad-commutenity.md#7-ai-evaluation) and [data plan §6](data-commutenity.md#6-evaluation-sets), with sample sizes, for the on-device LLM (PRD-F8, in the MVP) and for whichever of voice and the ranker ships
- team-sourced examples of confusing Makati trips, labelled as anecdote

### Value measurement

| Level | Metric |
|---|---|
| Demo | Correct facts on the hero trip and held-out pairs; shapes that match the real path; correct on/off-route and para alert on the test tracks; offline proof; latency; zero invented facts and 0 false "ride this" (PRD-F8, MVP); ranker vs. baseline on held-out pairs (if T4 ships) |
| After the event | Time to a correct plan vs. asking around; rate of taking the wrong jeep; rate of missed stops; rate of real contributions. These need real users and are not claimed. |

## 2. Competitors and Substitutes

These are working hypotheses. A research ticket must verify them before the pitch.

| Substitute | Hypothesis about it | Our wedge (to test) |
|---|---|---|
| Asking barkers, drivers, strangers | Free and local, but you have to find someone, and it's awkward for newcomers | Instant, private, offline |
| Google Maps transit | Strong on rail and bus; informal modes uneven. Offline downloaded areas and offline transit routing are *not verified*. | Offline, jeepney-aware, Taglish, a para alert |
| Local transit planners (e.g., Sakay.ph), Moovit | Cover jeepneys to some degree; offline support unknown | An offline map-first trip builder; on-device AI; community alternatives; on-route status and a para alert |
| Facebook commuter groups | Rich community knowledge, but online, slow, unstructured | Same community knowledge, structured, usable offline |

Don't claim a competitor limitation in the pitch until it has been checked.

## 3. Feasibility in the Timebox

The build window runs to the 8:00 AM Oct 10 feature freeze ([BUILD §1](build-commutenity.md#1-build-sequence)). The re-scope was decided at about 10:40 PM on Oct 9. Main uncertainties, in order:
1. The offline Makati map on the demo phone: MapLibre with PMTiles ([A14](state.md#4-open-assumptions), FC-13).
2. Pack data entry for Makati, including the candidate trips for the hero pair, Ayala Center to Dela Rosa St., Pio del Pilar ([D30](state.md#5-decisions), [A13](state.md#4-open-assumptions)), and the road-shape pipeline and its terms ([A15](state.md#4-open-assumptions)).
3. GPS tracking accuracy in Makati and foreground-service reliability ([A16](state.md#4-open-assumptions), FC-16, FC-19).
4. Refresh and the sync backend.
5. The on-phone LLM runtime (PRD-F8, in the MVP per [D31](state.md#5-decisions); FC-4, FC-5), and STT if voice ships.

## 4. Tests and Kill Criteria

| Assumption | Cheapest test | Success threshold | Failure → decision | Who |
|---|---|---|---|---|
| **Map spike:** MapLibre renders a Makati PMTiles file offline on the demo phone ([A14](state.md#4-open-assumptions)) | CP1 spike: load a `pmtiles extract` of Makati in a bare MapLibre Android app, airplane mode on | The map pans and zooms on the POCO with labels, the Makati extract size is recorded, and OSM attribution shows | Try the fallback: a MapLibre offline region from a provider whose terms allow offline. If neither works at CP1 (about 11:30 PM), tell the owner at once. T0 can't ship without a map. | Issue claimant |
| The D30 hero pair (Ayala Center to Dela Rosa St., Pio del Pilar) has real alternatives ([A13](state.md#4-open-assumptions)) | List and verify the candidate trips for the pair from `collected` and `known` data | ≥ 2 genuinely different trips on the D30 pair, with `collected` or `known` facts | Tell the owner at once; the pair is an owner decision ([D30](state.md#5-decisions)) and revisiting it needs a new decision row | Issue claimant + team |
| **Shapes:** a routing engine gives plausible road shapes and its terms allow it ([A15](state.md#4-open-assumptions)) | Route every hero-trip segment through the engine and view the result on the map | Each shape starts and ends within N m of its stops, follows the known path, and is not a straight line (the QA-16 validator passes) | Use a self-hosted engine or a different one; hand-check or hand-fix the hero trip's shapes; mark the rest `mock` | Issue claimant |
| Rail geometry is available for the Makati rail legs | Locate rail track geometry in OSM for the rail segments on the hero trip | Shapes follow the tracks | Draw the rail leg with stored station-to-station points, labelled as approximate | Issue claimant |
| **Tracking:** on-route and para alert behave on mock tracks, then on a real trip ([A16](state.md#4-open-assumptions)) | Replay `on-route`, `deviate-return`, `blip`, `approach-alight`, and `gps-loss` GPX tracks ([QAD §2](qad-commutenity.md#2-data-and-environment)); then one real walk or ride in Makati, including the CBD and under MRT-3 or EDSA | QA-10 and QA-11 pass on mock tracks. On the real trip: no false off-route flag, and the alert fires once and in time to alight, with thresholds tuned (starting values: 100 m, 30 s, about 300 m). | Raise the thresholds or add a fix-accuracy filter. Past 6:30 AM, demo with a recorded mock route, labelled as simulated. | Issue claimant |
| The foreground service keeps tracking with the screen off on HyperOS (FC-19) | QA-11 background case on the POCO | Alert fires with the screen off | Ask the rider to keep the screen on, and note it; or use the labelled simulated route | Issue claimant |
| Refresh works and survives interruption | Refresh on Wi-Fi, kill it mid-download, retry, then go to airplane mode | The pack, map pack, and community data load offline; an interrupted refresh recovers | Past 4:00 AM, demo with the bundled pack and local contributions, and say so | Issue claimant |
| Sync works between phones | Phone A suggests, phone B syncs | It appears as a Community alternative | Demo with local contributions only | Issue claimant |
| The LLM runs on the demo phone (PRD-F8, MVP) | The LLM speed test (issue #8): parse 5 questions offline in a bare Android app | Valid JSON; ≤ 5 s warm | Try a smaller model, then llama.cpp; as a last resort, the rule-based parser plus on-device embedding place search ([D31](state.md#5-decisions)). Always on-device; never a cloud model. T0–T2 are unaffected. | Issue claimant |
| Place search and LLM place extraction handle Taglish | 30-query and 30-question set on the phone | ≥ 80% exact origin/destination | Add aliases or change the model before UI work | Issue claimant |
| The correct-vehicle check never says "Yes, ride this" for a wrong vehicle (PRD-F8) | Held-out signboard and route-name texts, including wrong and near-match ones | **0** false "Yes, ride this" (AI-05) | Make the match stricter, so more answers become "Not sure, check the signboard" | Issue claimant |
| Whisper transcribes Taglish acceptably (T3 voice, optional) | A few short voice clips | The transcript is usable after confirmation | Cut voice; typing remains. This is the first cut. | Issue claimant |
| Teammates agree on preferences (T4) | 10 scenarios ranked independently | Majority pick is clear in most scenarios | Add stated preference as a feature, or ship the D16 order | Issue claimant |
| The ranker beats the baseline (T4) | Held-out pairs | Strictly better, honestly reported; decided by 7:00 AM | Ship the D16 order and say so | Issue claimant |
| The problem resonates | Ask other participants about their worst commute moment *(confirm with organizers this isn't "external help")* | A recognizable story within 10 seconds | Reframe the pitch | Presenter |

**Decision:** go ahead with the demo. The map spike is the first gate. Make no commercial or impact claims.

## 5. Concept Visual Reactions

None collected. UI quality is checked by the [DSD quality gate](dsd-commutenity.md#8-quality-gate).
