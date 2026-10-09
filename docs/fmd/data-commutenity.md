# Data Collection and Training Plan: CommuteNity

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.2
**Owner:** Data owner and ranker owner ([A8](state.md#4-open-assumptions))
**Status:** Draft. Corridors pending [A6](state.md#4-open-assumptions), travel times pending [A10](state.md#4-open-assumptions), ranker model pending [A11](state.md#4-open-assumptions).
**Last reconciled:** 2026-10-09
**SDD:** [System design](sdd-commutenity.md)

This document owns four things: the pack and contribution schemas, how we collect and verify data, what we train (the route ranker, [D11](state.md#5-decisions)), and how we prove it helped.

**Rule:** the T0 baseline never waits on anything in §4–§5 ([D5](state.md#5-decisions)).

## 1. Data Inventory

| Dataset | Purpose | Feeds | Needed by |
|---|---|---|---|
| Commute pack | The only source of facts in answers | PRD-F2, F3, F5, F8 | T0 |
| Community contributions | Alternatives and vote signals | PRD-F5, F6; ranker features | T1 |
| Trip question set | Parser and end-to-end eval | PRD-F1; AI evals | T0 |
| Route preference set | Ranker training and eval | PRD-F7 | T2 (collection starts at CP2) |
| Signboard photo set | Signboard eval only (pretrained OCR) | PRD-F8 | T3 |

## 2. Commute Pack Schema

The pack keeps the route → segment → stop shape from the original CommuteNity ([D3](state.md#5-decisions)). It is versioned JSON built into the app assets. Proposed source location: `data/pack/`.

| Entity | Fields | Notes |
|---|---|---|
| `meta` | `version`, `built_at`, `corridors[]`, `sources[]` | Shown as "data as of …" |
| `places` | `id`, `name`, `aliases[]`, `lat`, `lng`, `kind` (landmark, area, station, terminal), `stop_ids[]` | Aliases hold Taglish and colloquial names |
| `stops` | `id`, `name`, `lat`, `lng`, `modes[]` | Where you board or alight |
| `routes` | `id`, `name`, `mode`, `signboards[]`, `notes` | `mode` is one of: jeepney, bus, mrt, lrt, uv_express, p2p, tricycle, walking |
| `segments` | `route_id`, `seq`, `from_stop_id`, `to_stop_id`, `fare_php?`, `minutes?` | Ordered legs. Minutes are needed for "efficient" ([A10](state.md#4-open-assumptions)). |
| `transfers` | `from_stop_id`, `to_stop_id`, `walk_minutes`, `notes` | Walking links |
| `provenance` | per record: `source_class` (`collected` / `known` / `mock`), `source` (ride-verified, fare matrix, team estimate, signboard photo, mock generator), `verified_by`, `verified_at` | Every fact has a class and a source ([§3.1](#31-mock-data-rules)) |

**Integrity rules:**
- Segments reference existing stops.
- Every route has at least one segment.
- Every place maps to at least one stop.
- No fare or minutes value without a source.

A validator runs before every pack build.

### 2.1 Contribution Schema

These records are stored locally in Room and synced to the backend ([A9](state.md#4-open-assumptions)).

| Entity | Fields | Notes |
|---|---|---|
| `route_suggestions` | `id` (UUID), `device_id`, `origin_place_id`, `dest_place_id`, `legs[]` (route_id, board_stop_id, alight_stop_id), `note?` (≤ 280 chars), `created_at`, `synced` | Legs must be pack-valid. Fares and minutes are always computed from the pack. |
| `route_votes` | `device_id`, `candidate_key`, `value` (+1/−1), `created_at`, `synced` | `candidate_key` is a hash of the leg sequence. One vote per device and key. |
| `sync_cursor` | `last_pulled_at` | Pull-since cursor |

`device_id` is a random UUID generated on the phone. It is not tied to an account or the hardware.

## 3. Collection Protocol

1. **Corridors ([A6](state.md#4-open-assumptions)):** pick 1–3 demo corridors the team rides. At least one should have two or more genuinely different ways to make the trip. Build them from `collected` and `known` data.
2. **Stops and segments:** use team knowledge, with OpenStreetMap for coordinates. Class each one `collected` (verified this event) or `known` (from memory).
3. **Fares:** use the current official fare matrix where possible, citing the source and date (`collected`). Otherwise use team knowledge (`known`) or a `mock` value. If none of those exists, the fare is unknown. Per [fare research](https://github.com/geadlydrim/appbuildersph-hackathon/issues/5):
   - LRT-1, LRT-2, and MRT-3 matrices are `collected`, transcribed by hand from official images.
   - The MRT-3 and LRT-2 discount goes in a dated overlay.
   - Jeepney and bus fares become `collected` once a teammate saves the LTFRB fare guides from a browser.
   - UV Express and P2P are `known` or `mock`.
4. **Minutes ([A10](state.md#4-open-assumptions)):** the team's ride estimates per segment, labelled `team estimate`. Use typical, not peak, times, and state that in the app.
5. **Aliases:** for each stop, record the names people actually say.
6. **Signboards:** transcribe the exact painted text into `routes.signboards[]`. About 20 photos go into the signboard eval set.

### 3.1 Mock Data Rules

Mock data fills the gaps that real and known data can't cover overnight ([D13](state.md#5-decisions)).

| Rule | Detail |
|---|---|
| Where mock may be used | Wider coverage around the demo corridors (places, stops, routes, segments); missing fares and minutes; extra community suggestions and votes so alternatives have content; augmenting ranker training; growing the question set |
| Where mock may not be used | The demo hero question. Held-out evaluation labels for the ranker ship rule (AI-06). Any number quoted in the pitch as real-world accuracy. |
| How it's made | A script in `ml/` or `data/` with a fixed seed, so the mock pack is reproducible. Mock values must be plausible for Metro Manila but are never claimed as real. Never hand-type mock data into the same files as `collected` or `known` data without the class tag. |
| Tagging | Every mock record has `source_class: mock` and `source: mock generator <version>` |
| Precedence | If a collected or known record exists for the same fact, it overrides the mock one |
| Visibility | The app marks mock values with a "sample data" tag ([DSD §2](dsd-commutenity.md#2-theme-and-type)). The README reports counts per class. |
| Mock preferences | If mock preference labels are generated from a utility function, the ranker can only learn that function back. They are used for training augmentation only, never for the ship-rule eval. |

## 4. Training Data Collection

**There are no real users during the event.** All preference and contribution data is **team-generated** and must be disclosed as such ([JUDGING](JUDGING.md#rules): no fake benchmarks).

| Set | How | Labels | Split |
|---|---|---|---|
| Route scenarios | For each covered origin–destination pair, plus variants (time of day, preference), generate the candidate set from the pack and add the team's community suggestions | — | — |
| Preference labels | Each of the 4 teammates independently picks the route they would take, or ranks the top 3, per scenario, with their preference stated | Pairwise preferences derived from rankings; inter-rater agreement recorded | **Split by origin–destination pair** (held-out pairs are never seen in training) |
| Seeded contributions | Teammates submit real alternative routes they know and vote on candidates through the app (this also exercises T1) | Votes become `net_votes` and `vote_count` features | Same pair-level split |
| Trip questions | Varied Taglish and English questions per covered pair, including typos, landmarks, out-of-coverage, and off-topic | Gold `{intent, origin, destination}` plus the expected route | Held-out eval only |

**Target volume:** set it at CP2 based on corridor count. Report the actual numbers and don't inflate them.

All preference labels, contributions, and questions carry `source_class` too. Labels written by teammates are `known`. Generated ones are `mock`.

## 5. Training Plan

| Item | Plan |
|---|---|
| Model ([A11](state.md#4-open-assumptions)) | Start with pairwise logistic regression on the [feature vector](sdd-commutenity.md#3-routing-contract). Try a small GBDT only if logistic regression plateaus and there is time. |
| Baseline | The hand-weighted score from SDD §3 |
| Training | A Python script in `ml/`. Its inputs are the scenarios, labels, and seeded votes. Its output is the weights or model file plus the feature spec. |
| On-device export | Logistic regression: a JSON weights file evaluated in Kotlin. GBDT: ONNX, run with ONNX Runtime Android. |
| Parity | The same fixture produces the same scores in Python and Kotlin (QA-11) |
| Ship rule | Better than the baseline on held-out pairs on the metric in §6. Otherwise the baseline ships. |

## 6. Evaluation Sets

| Eval | Metric | Baseline | Ship rule |
|---|---|---|---|
| Parser | Exact match on origin and destination over held-out questions | Pretrained LLM plus prompt | — |
| End-to-end | % of answers whose picked route is valid and whose facts exactly match the pack | T0 build | [QAD §6](qad-commutenity.md#6-release-criteria) threshold |
| Ranker | Top-1 agreement with the majority human pick on held-out pairs; pairwise accuracy | Hand-weighted score | Ranker must beat the baseline |
| Signboard | Correct verdict % on held-out photos; unreadable rate; **0 false "ride"** | Pretrained OCR plus matcher | — |

Report the real numbers, sample sizes, and limits (for example, "4 raters, N held-out pairs, team-generated"). **Split every metric by data class.** Results on `mock` data show that the pipeline works. They are not evidence of real-world accuracy.

## 7. Provenance and Disclosure

The README lists:
- every data source, with OpenStreetMap attribution and the fare matrix source and date
- every model, with its base checkpoint and license
- the training script
- the fact that the preference and contribution data was generated by the team during the event
- which records and labels are `mock`, with counts per class and the generator script
