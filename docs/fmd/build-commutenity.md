# Build Guide: CommuteNity

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.4
**Owner:** Implementer
**Status:** Draft. Re-planned for Makati only, map-first, new tier order ([D20](state.md#5-decisions) to [D29](state.md#5-decisions)), with the hero pair ([D30](state.md#5-decisions)) and the Local-AI floor ([D31](state.md#5-decisions)) added. Map pack support is pending [A14](state.md#4-open-assumptions) (CP1); the LLM runtime, pending [A4](state.md#4-open-assumptions), picks the model for PRD-F8, which is part of the MVP. Contribution sync is decided in [D17](state.md#5-decisions).
**Last reconciled:** 2026-10-10
**PRD:** [Requirements](prd-commutenity.md) · **SDD:** [System design](sdd-commutenity.md) · **Data:** [Data plan](data-commutenity.md)

## 1. Build Sequence

Start with [state](state.md), then read only the doc your issue needs. Tiers merge into the demo build strictly in order ([MVP scope](mvp-scope.md#tiers)): T0 walking skeleton, T1 refresh and community, T2 tracking, T3 ask in words and voice, T4 ranker. **The MVP is T0 + T1 + T2 + PRD-F8** ([D31](state.md#5-decisions)): ask in words (F8) keeps its T3 label but is release-critical, so it merges behind its feature flag as soon as T0 is demo-safe, independent of T1 and T2 (CP-F8, ~4:30 AM). LLM work starts now; STT and ranker data (from CP2) merge only in tier order, and voice and the ranker are cut before F8.

| Checkpoint | Target (Oct 9–10) | Work | Done when | Who |
|---|---|---|---|---|
| CP0 Decisions | Now | D20 to D31 recorded in [state](state.md) | Recorded | Project owner |
| CP1 Spikes | ~11:30 PM | **Map spike:** MapLibre Native Android renders a Makati PMTiles file offline on the POCO X6 5G; record file size ([A14](state.md#4-open-assumptions)). **LLM speed test** (issue #8): **done** ([D32](state.md#5-decisions)). Gemma 4 E2B on LiteRT-LM 0.18.0, GPU, hybrid parser: worst p95 2.84 s, 10/10 exact on 10 Makati questions. | Map renders in airplane mode, or the A14 fallback is chosen. LLM: passed; PRD-F8 is built on it. | Issue claimants |
| CP2 Foundations | in parallel with CP1; shape engine chosen before pack v0 ([A15](state.md#4-open-assumptions)); the ≥2 candidate trips for the D30 pair (Ayala Center to Dela Rosa St., Pio del Pilar) listed and verified before pack v0 ([A13](state.md#4-open-assumptions)) | **Pack v0 for Makati with road shapes** plus validator (incl. shape rules), mock generator, eval sets. **Trip finder module** (candidate generator and scorer) as a pure Kotlin module with fixture tests. App shell, Room, feature flags, map screen. | Fixture tests pass; pack validates (QA-16) | Issue claimants |
| CP3 T0 walking skeleton | ~1:30 AM | Pins A and B on the offline map → trip finder → best-trip card and shapes drawn on real roads; airplane mode | US-01 to US-03 pass offline on the phone (QA-01 to QA-03, QA-16, QA-17, QA-19); tag `demo-safe-t0` plus APK | Issue claimants |
| CP4 T1 refresh and community | ~3:30 AM | Refresh client (pack, map pack, community), foot routes for first/last-mile, alternatives sheet, suggest/vote, Room plus sync backend (2 tables, push/pull), two-phone sync; rider Q&A evidence (PRD-F12, [D34](state.md#5-decisions)), the first thing cut | US-04 to US-08 pass (US-15 if rider Q&A is built; QA-20 to QA-23); [QAD gate](qad-commutenity.md#6-release-criteria) for T0 + T1 (QA-04 to QA-09); tag `demo-safe-t1` | Whole team |
| CP-F8 Ask in words (T3 label, MVP) | ~4:30 AM | Query parser LLM, place search and the correct-vehicle matcher merge behind the F8 flag once `demo-safe-t0` exists, independent of T1 and T2; built from now in parallel. Falls back as in [D31](state.md#5-decisions); never a cloud model. | US-11 and US-12 pass offline on the phone (QA-12, QA-13, AI-01 to AI-05); tag `demo-safe-f8` | Issue claimants |
| CP5 T2 in-trip tracking | ~5:30 AM | Foreground service, GPS, map-matcher, on/off-route status, para alert | US-09 and US-10 pass on the phone with a mock-location GPS track (QA-10, QA-11); tag `demo-safe-t2` | Issue claimants |
| CP6 T3 voice / T4 | 5:30–8:00 AM | T3 voice: STT feeding F8. T4: train the ranker, evaluate on held-out pairs, parity test, swap in behind a flag if it wins. Both are optional and are cut before F8. | T3 voice: US-13 (QA-14). T4: AI-06 and QA-15 pass; tag `demo-safe-t3` / `demo-safe-t4`. | Issue claimants |
| **Freeze** | **8:00 AM** | No new features; fixes only | — | Project owner |
| Submit | 8:00–9:30 AM | README with disclosures, ~1 min demo video, X/LinkedIn post (#AppBuildersPH, tag Devin/Cognition), the "why local" answer | Submitted once, before **10:00 AM** | Issue claimants |

The feature freeze (8:00 AM) and code freeze (10:00 AM) are fixed.

**Ranker data runs in parallel from CP2.** Teammates rank scenarios independently between tasks (about 2 minutes per scenario). Once T1 exists, seeded contributions go through the app.

**Cut rules (no debate at 2 AM):**
- If time runs short at any point, cut rider Q&A (PRD-F12) first, before T2 tracking and before F8 ([D34](state.md#5-decisions)). Its LLM extraction stretch starts only after `demo-safe-f8` exists and is cut if not working by 7:00 AM.
- If T0 slips past 2:30 AM, cut T3 voice and T4. **Keep F8**: it is release-critical ([D31](state.md#5-decisions)).
- If refresh or sync isn't working by 4:00 AM, demo with the bundled pack and local contributions only, and say so.
- If T2 isn't passing by 6:30 AM, demo tracking with a recorded mock-GPS route, labelled as simulated.
- If F8 isn't passing by 6:30 AM, ship the smallest model that produces valid JSON with template phrasing, plus the D27 matcher. Never drop F8; drop F9 (voice) and F10 (ranker) first.
- If the ranker hasn't beaten the D16 ordering by 7:00 AM, ship D16 and report both numbers.
- CP1 fallbacks: if the map spike fails, switch to the A14 fallback (a MapLibre offline region from a provider whose terms allow offline). If the LLM speed test fails, switch to the fallback runtime, then a smaller model; as a last resort use a rule-based parser plus on-device embedding place search (EmbeddingGemma, [A4](state.md#4-open-assumptions)) so the submission still runs meaningful local inference. Never a cloud model. T0 to T2 are not affected.

## 2. Team Workstreams

Four people ([D12](state.md#5-decisions)): geadlydrim, pablo-pica, storms23, and Jrabara101. Fixed role owners are retired ([D35](state.md#5-decisions); [D19](state.md#5-decisions) is superseded). Work is broken into GitHub issues as we go. Anyone claims any issue by assigning themselves, and no one is limited to one area. Checkpoints, tier order, and cut rules stay.

The areas below are a map of the work, not owners:

| Area | Work |
|---|---|
| Android app | App shell, MapLibre map and PMTiles map pack, A/B trip builder UI, trip card and alternatives UI, refresh and sync client, tracking UI and foreground service, APK builds, `demo-safe-*` tags |
| On-device AI | LLM (PRD-F8, MVP: the hybrid parser on Gemma 4 E2B per [D32](state.md#5-decisions); LLM phrasing is off), STT (T3 voice, optional), place search and alias matching, correct-vehicle text matcher, latency |
| Routing, tracking, ranker | Candidate generator and D16 ordering (trip finder), map-matching and tracking computation (T2), GPS test tracks, ranker training and eval in `ml/`, Kotlin parity, AI-06 (T4) |
| Data, evals and story | Makati pack with the road-shape precompute pipeline ([A15](state.md#4-open-assumptions)), fares, minutes, distances, aliases, signboards, mock-data generator, validator, eval sets, labelling sessions, QA runs, README disclosures, pitch, video, post |
| Community layer | Suggest and vote with sync (T1) and rider Q&A evidence (PRD-F12, [D34](state.md#5-decisions)): the Questions screen, the bundled `data/mock/rider-qa.json`, the evidence counter feeding the D16 vote term. Rider Q&A is the first thing cut. |

Work comes from GitHub issues. Decisions go through the wayfinder map, not chat. `docs/fmd/` beats code, branches, PR text, and chat ([D35](state.md#5-decisions)): when they conflict, the code changes, or a decision updates the doc first. Only the owner (geadlydrim) merges changes to decisions in `state.md`; teammates propose them by issue or PR.

## 3. Stack Currency

Only the LLM runtime is pinned so far ([D32](state.md#5-decisions)). Record exact versions when installing, and never fabricate them.

| Layer | Candidate | Verified | Pin |
|---|---|---|---|
| App | Kotlin, Jetpack Compose (Material 3), Room, Gradle | No | — |
| Map | MapLibre Native Android; confirm it renders a local PMTiles file ([A14](state.md#4-open-assumptions)) | No | — |
| Map pack | Protomaps PMTiles (vector tiles from OSM data), extract with the `pmtiles` command-line tool | No | — |
| Road shapes (data build only) | OSRM or GraphHopper for driving and foot; OSM track geometry for rail ([A15](state.md#4-open-assumptions)). Runs on a laptop or server, never in the app. | No | — |
| GPS | Android `LocationManager` GPS provider or `FusedLocationProviderClient`, as a foreground service of type `location`; mock-location for tests | No | — |
| LLM runtime (PRD-F8) | LiteRT-LM `com.google.ai.edge.litertlm:litertlm-android:0.18.0`, GPU backend; model `gemma-4-E2B-it.litertlm` (2,588,147,712 bytes) from `litert-community/gemma-4-E2B-it-litert-lm` ([D32](state.md#5-decisions)). Fallback llama.cpp via JNI | Yes (LLM speed test on the POCO X6 5G, 2026-10-10) | `litertlm-android:0.18.0` |
| Embeddings / ranker | LiteRT-LM EmbeddingEngine + EmbeddingGemma (T3, optional); ranker in plain Kotlin (T4) | No | — |
| Speech (T3) | whisper.cpp via JNI | No | — |
| Sync and refresh backend | Supabase: anonymous Auth, authenticated Edge Function, Postgres, Storage (manifest, pack, map pack) | No | — |
| Training / eval | Python (scikit-learn for logistic regression; GBDT library only if needed); the same Python environment runs the shape pipeline and validator | No | — |
| Demo mirroring | scrcpy over USB | No | — |

Removed: ML Kit Text Recognition (OCR is dropped, [D26](state.md#5-decisions)). No TTS library either.

## 4. Golden Paths

- **Facts:** the pack goes to the candidate generator, then the scorer or ranker, then the template. With PRD-F8, the LLM never outputs a fare, stop, or minutes value that isn't in `factsUsed`.
- **Shapes:** road shapes are generated at data-build time by the engine (A15), validated (QA-16), and stored in the pack. The phone decodes and draws them; there is no routing engine in the app. The only runtime line not from the pack is the first/last-mile walk: a cached foot route online, a dashed "walk ~N m" line offline.
- **Refresh:** read the manifest, download a newer pack or map pack to a temporary file, check `sha256`, swap atomically. The bundled pack always remains as the fallback. Never swap during an active trip. A failed refresh never blocks the UI.
- **Tracking:** GPS fix → map-matcher (pure Kotlin) → status and para alert. No network call anywhere on this path. Thresholds live in the one `TrackingConfig` file. Test with mock-location tracks from `data/tracks/`; tune on real Makati tracks ([A16](state.md#4-open-assumptions)).
- **Rider Q&A ([D34](state.md#5-decisions)):** the bundled `data/mock/rider-qa.json` plus the rider's own local answers feed `evidenceFor` ([SDD §4](sdd-commutenity.md#4-module-contracts)). Counts only enter the D16 vote term inside its clamp, mock answers are left out of the hero-pair ordering, and nothing is synced.
- **Contributions:** validate against the pack, save to Room, then sync batched mutations through the authenticated Supabase Edge Function. Fares, minutes, distances, and shapes always come from the pack; only aggregates are pulled for votes.
- **Ranker:** one feature spec file shared by Python and Kotlin. Retrain, evaluate on held-out pairs, run the parity test, then flip the flag.
- **Models (PRD-F8, and Whisper if voice ships):** downloaded once and cached in app storage. Model IDs live in one config file.
- **Pack:** edit the source, run the shape pipeline and the validator, build. Never hand-edit the built pack.
- **Offline proof:** before every tag, turn on airplane mode and use the network inspector. Zero requests on the answer path and on the tracking path, and the map still renders.

## 5. Conventions and Definition of Done

**Repo layout** (proposed; confirm at CP0):

| Path | Contents |
|---|---|
| `android/` | Gradle project |
| `data/pack/` | Pack source, shape source and cached engine responses, and pack builds |
| `data/map/` | Map pack build script and the PMTiles file (the file itself is not committed) |
| `data/tracks/` | GPS test tracks for mock-location runs |
| `data/mock/` | Hand-written mock files, such as `rider-qa.json` (rider Q&A, [D34](state.md#5-decisions)); bundled in the app, separate from the commute pack |
| `data/eval/` | Held-out sets, including the signboard text eval set |
| `data/labels/` | Preference rankings |
| `data/raw/` | Raw source material such as photos (gitignored) |
| `ml/` | Training and eval scripts |
| `backend/` | Schema SQL, Edge Function, manifest |
| `spikes/` | Throwaway spikes (map, LLM speed test). Not shipped; delete or fold into `android/` when a spike graduates. |
| `docs/fmd/` | Project docs (this suite) |
| `docs/agents/` | Agent skill config |

**Version control:**
- Short-lived branches, with PRs into `main`.
- One issue per task.
- Each `demo-safe-t0`, `-t1`, `-t2`, `-f8`, … tag has an archived APK. These are the rollback points ([PRD §9](prd-commutenity.md#9-implementation-and-rollback)).

**A task is done** when its story's acceptance criteria pass on the demo phone (offline where required) and its QAD rows are updated with the result.

### 5.1 Future Changes

When a wayfinder ticket closes:
1. Update the affected doc.
2. Move the assumption from [state §4](state.md#4-open-assumptions) to [§5](state.md#5-decisions).
3. Append to the [LOG](log-commutenity.md).

### 5.2 Public Surface

The repo is public. The README is the judges' entry point. It covers what the app is, how to build the APK and get the map pack and models, what runs locally, what needs the internet (refresh, foot routes, geocoding, sync), and the disclosures, including OpenStreetMap attribution and that road shapes are generated ([JUDGING](JUDGING.md#submission)).

### 5.3 Engineering Maturity

- **Now: L0.** Docs only.
- **Target by the freeze:**
  - JVM unit tests for routing, scoring, map-matching, and validators
  - a runnable eval script
  - the ranker parity test
  - a reproducible APK build

CI is optional.
