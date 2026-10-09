# Build Guide: CommuteNity

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.2
**Owner:** Implementer
**Status:** Draft. Runtime pending [A4](state.md#4-open-assumptions) and role owners pending [A8](state.md#4-open-assumptions). Contribution sync is decided in [D17](state.md#5-decisions).
**Last reconciled:** 2026-10-09
**PRD:** [Requirements](prd-commutenity.md) · **SDD:** [System design](sdd-commutenity.md) · **Data:** [Data plan](data-commutenity.md)

## 1. Build Sequence

Start with [state](state.md), then read only the doc your workstream needs.

| Checkpoint | Target (Oct 9–10) | Work | Done when | Owner |
|---|---|---|---|---|
| CP0 Decisions | ASAP | Resolve A4, A6, A8, A12 through wayfinder | Recorded in [state](state.md) | Project owner |
| CP1 Runtime spike | CP0 + 1.5 h | A bare Android app loads the candidate LLM **on the demo phone** and parses 5 Taglish questions offline | Valid JSON at ≤ 5 s warm. If not, switch model or runtime now. | P2 (AI) |
| CP2 Foundations | in parallel with CP1 | P1: Compose scaffold, Room, feature flags. P3: candidate generator and scorer as a pure Kotlin module with fixture tests. P4: pack v0 for one corridor plus validator, 30 eval questions, 10 ranking scenarios | Fixture tests pass; pack validates | P1, P3, P4 |
| CP3 T0 skeleton | ~1:00 AM | Wire parser → resolver → generator → scorer → card → phrasing | US-01..US-03 pass offline on the phone; tag `demo-safe-t0` plus APK | P1 + P2 + P3 |
| CP4 T1 demo-ready | ~4:00 AM | Setup screen, alternatives sheet, suggest/vote, Room plus sync backend (2 tables, push/pull) | [QAD gate](qad-commutenity.md#6-release-criteria) passes; tag `demo-safe-t1` | Whole team |
| CP5 T2 ranker | ~6:00 AM | Train on the labelled scenarios and seeded votes; evaluate on held-out pairs; parity test; swap in behind a flag if it wins | AI-06 and QA-11 pass; tag `demo-safe-t2` | P3 |
| CP6 T3 → T4 | 6:00–8:00 AM | Signboard check, then voice | Tier gates pass | P2 |
| **Freeze** | **8:00 AM** | No new features; fixes only | — | Project owner |
| Submit | 8:00–9:30 AM | README with disclosures, ~1 min demo video, X/LinkedIn post (#AppBuildersPH, tag Devin/Cognition), the "why local" answer | Submitted once, before **10:00 AM** | P4 |

**Ranker data runs in parallel from CP2.** Teammates rank scenarios independently between tasks (about 2 minutes per scenario). Once T1 exists, seeded contributions go through the app.

**Cut rules (no debate at 2 AM):**
- If CP1 isn't passing at its target time, switch to the fallback runtime, then to a smaller model. As a last resort, use a rule-based parser plus the embedding resolver.
- If CP3 slips past 2:00 AM, cut T3 and T4.
- If sync isn't working by 4:30 AM, demo contributions from the local store only and say so.
- If CP4 slips past 5:00 AM, T2 ships only if it is already passing; otherwise polish T1.
- If the ranker doesn't beat the baseline by 7:00 AM, ship the baseline and report both numbers.

## 2. Team Workstreams

Four people ([D12](state.md#5-decisions)). Names are assigned at CP0 ([A8](state.md#4-open-assumptions)).

| Role | Owns |
|---|---|
| **P1: Android app** | Compose UI, navigation, Room, feature flags, setup and model download, sync client plus backend (2 tables), APK builds, `demo-safe-*` tags |
| **P2: On-device AI** | LLM runtime (CP1), parser and phrasing prompts, embedding resolver, latency; later T3 OCR and T4 speech |
| **P3: Routing and ranker** | Candidate generator, scorer, feature spec, ranker training and eval in `ml/`, Kotlin parity, AI-06 |
| **P4: Data, evals and story** | Pack (corridors, stops, fares, minutes, aliases, signboards), mock-data generator, validator, eval sets, labelling sessions, QA runs, README disclosures, pitch, video, post |

Work comes from GitHub issues. Decisions go through the wayfinder map, not chat.

## 3. Stack Currency

Nothing is pinned yet. Record exact versions when installing, and never fabricate them.

| Layer | Candidate | Verified | Pin |
|---|---|---|---|
| App | Kotlin, Jetpack Compose (Material 3), Room, Gradle | No | — |
| LLM runtime | LiteRT-LM (`litertlm-android`, research: 0.18.0); fallback llama.cpp via JNI | No | — |
| Embeddings / ranker | LiteRT-LM EmbeddingEngine + EmbeddingGemma; ranker in plain Kotlin | No | — |
| Signboard OCR | ML Kit Text Recognition v2, bundled Latin model | No | — |
| Speech | whisper.cpp via JNI | No | — |
| Sync backend | Supabase: anonymous Auth, authenticated Edge Function, Postgres | No | — |
| Training / eval | Python (scikit-learn for logistic regression; GBDT library only if needed) | No | — |
| Demo mirroring | scrcpy over USB | No | — |

## 4. Golden Paths

- **Facts:** the pack goes to the candidate generator, then the scorer or ranker, then the template. The LLM never outputs a fare, stop, or minutes value that isn't in `factsUsed`.
- **Contributions:** validate against the pack, save to Room, then sync batched mutations through the authenticated Supabase Edge Function. Fares and minutes are always recomputed from the pack; only aggregates are pulled for votes.
- **Ranker:** one feature spec file shared by Python and Kotlin. Retrain, evaluate on held-out pairs, run the parity test, then flip the flag.
- **Models:** downloaded once and cached in app storage. Model IDs live in one config file.
- **Pack:** edit the source, run the validator, build. Never hand-edit the built pack.
- **Offline proof:** before every tag, turn on airplane mode and use the network inspector. Zero requests on the answer path.

## 5. Conventions and Definition of Done

**Repo layout** (proposed; confirm at CP0):

| Path | Contents |
|---|---|
| `android/` | Gradle project |
| `data/pack/` | Pack source and builds |
| `data/eval/` | Held-out sets |
| `data/labels/` | Preference rankings |
| `data/raw/` | Raw photos (gitignored) |
| `ml/` | Training and eval scripts |
| `backend/` | Schema SQL |
| `docs/fmd/` | Project docs (this suite) |
| `docs/agents/` | Agent skill config |

**Version control:**
- Short-lived branches, with PRs into `main`.
- One issue per task.
- Each `demo-safe-t0`, `-t1`, `-t2`, … tag has an archived APK. These are the rollback points ([PRD §9](prd-commutenity.md#9-implementation-and-rollback)).

**A task is done** when its story's acceptance criteria pass on the demo phone (offline where required) and its QAD rows are updated with the result.

### 5.1 Future Changes

When a wayfinder ticket closes:
1. Update the affected doc.
2. Move the assumption from [state §4](state.md#4-open-assumptions) to [§5](state.md#5-decisions).
3. Append to the [LOG](log-commutenity.md).

### 5.2 Public Surface

The repo is public. The README is the judges' entry point. It covers what the app is, how to build the APK and download the models, what runs locally, what needs the internet, and the disclosures ([JUDGING](JUDGING.md#submission)).

### 5.3 Engineering Maturity

- **Now: L0.** Docs only.
- **Target by the freeze:**
  - JVM unit tests for routing, scoring, and validators
  - a runnable eval script
  - the ranker parity test
  - a reproducible APK build

CI is optional.
