# Data Collection and Training Plan: CommuteNity

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.2
**Owner:** Data owner and ranker owner ([A8](state.md#4-open-assumptions))
**Status:** Draft. The Valenzuela–Recto corridor, travel-time baseline, and ranker data/model plan are decided ([D15](state.md#5-decisions), [D16](state.md#5-decisions), [D18](state.md#5-decisions)).
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
| `segments` | `route_id`, `seq`, `from_stop_id`, `to_stop_id`, `fare_php?`, `minutes?` | Ordered legs. Minutes are needed for the T0 baseline ([D16](state.md#5-decisions)). |
| `transfers` | `from_stop_id`, `to_stop_id`, `walk_minutes`, `notes` | Walking links |
| `provenance` | per record: `source_class` (`collected` / `known` / `mock`), `source` (ride-verified, fare matrix, team estimate, signboard photo, mock generator), `verified_by`, `verified_at` | Every fact has a class and a source ([§3.1](#31-mock-data-rules)) |

**Integrity rules:**
- Segments reference existing stops.
- Every route has at least one segment.
- Every place maps to at least one stop.
- No fare or minutes value without a source.

A validator runs before every pack build.

### 2.1 Contribution Schema

These records are stored locally in Room and synced through the authenticated Supabase Edge Function ([D17](state.md#5-decisions)). The database has only the two contribution tables below; Room-only `synced` and `sync_cursor` state do not sync.

| Entity | Fields | Notes |
|---|---|---|
| `route_suggestions` | `id` (UUID), `author_id` (server-set anonymous Auth UID), `origin_place_id`, `dest_place_id`, `legs[]` (route_id, board_stop_id, alight_stop_id), `note?` (≤ 280 chars), `created_at` | The function validates legs against the pack. Fares and minutes are always recomputed from the pack. Public pull excludes `author_id`. |
| `route_votes` | `author_id` (server-set anonymous Auth UID), `candidate_key`, `value` (+1/−1), `updated_at` | Unique on (`author_id`, `candidate_key`). The function upserts the current vote; clearing removes it. Public pull exposes aggregate `net_votes` and `vote_count` only. |

Anonymous Auth creates a stable per-install identity without a user-facing account. The Android client never receives the service-role key.

## 3. Collection Protocol

1. **Corridor ([D15](state.md#5-decisions)):** build the Valenzuela–Recto corridor around the Malanday → Recto-area hero trip. Its direct Malanday–Recto e-jeep and Malanday → LRT-1 Monumento → LRT-1 Doroteo Jose → walk candidates must be built from `collected` and `known` data. Mock records may cover adjacent stops only; they cannot supply hero-trip facts.
2. **Stops and segments:** use team knowledge, with OpenStreetMap for coordinates. Class each one `collected` (verified this event) or `known` (from memory).
3. **Fares:** use the current official fare matrix where possible, citing the source and date (`collected`). Otherwise use team knowledge (`known`) or a `mock` value. If none of those exists, the fare is unknown. Per [fare research](https://github.com/geadlydrim/appbuildersph-hackathon/issues/5):
   - LRT-1, LRT-2, and MRT-3 matrices are `collected`, transcribed by hand from official images.
   - The MRT-3 and LRT-2 discount goes in a dated overlay.
   - Jeepney and bus fares become `collected` once a teammate saves the LTFRB fare guides from a browser.
   - UV Express and P2P are `known` or `mock`.
4. **Minutes ([D16](state.md#5-decisions)):** record the team's typical, non-peak ride and transfer estimates per segment as `known`, with estimator and date. Replace a fact only when a teammate verifies it during the event (`collected`). Mock minutes may cover adjacent stops but never the hero trip; the app states that displayed `known` times are typical estimates.
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
| Route scenarios | One JSON scenario per covered origin–destination pair and stated preference, with exactly three pack-valid candidates and their feature vectors | — | Deterministically assign 25% of origin–destination pairs to held-out before labelling |
| Preference labels | Each of the 4 teammates independently submits a strict top-3 order for every scenario | Three pairwise examples per rater: 1st > 2nd, 1st > 3rd, 2nd > 3rd; never infer a preference from an unranked candidate | The same origin–destination pair is never in both train and held-out |
| Seeded contributions | Teammates submit real alternative routes they know and vote on candidates through the app (this also exercises T1) | Votes become `net_votes` and `vote_count` features | Same pair-level split |
| Trip questions | Varied Taglish and English questions per covered pair, including typos, landmarks, out-of-coverage, and off-topic | Gold `{intent, origin, destination}` plus the expected route | Held-out eval only |

**Target volume:** set it at CP2 based on corridor count. Report the actual numbers and don't inflate them.

All preference labels, contributions, and questions carry `source_class` too. Labels written by teammates are `known`. Generated ones are `mock`.

### 4.1 Scenario and label files

`data/labels/scenarios.jsonl` contains one JSON object per scenario:

```json
{
  "schema_version": 1,
  "scenario_id": "vr-001-fastest",
  "origin_place_id": "malanday",
  "destination_place_id": "recto",
  "preference": "fastest",
  "source_class": "known",
  "candidates": [
    { "candidate_key": "…", "source": "algorithm", "features": { "total_minutes": 0, "fare_php": 0, "transfers": 0, "walk_minutes": 0, "modes_count": 0, "net_votes": 0, "vote_count": 0, "is_community": false, "unknown_fare_legs": 0 } },
    { "candidate_key": "…", "source": "algorithm", "features": { "…": "…" } },
    { "candidate_key": "…", "source": "community", "features": { "…": "…" } }
  ]
}
```

`data/labels/rankings.jsonl` contains `{ "scenario_id", "rater_id": "R1" | "R2" | "R3" | "R4", "ranking": [candidate_key, candidate_key, candidate_key], "source_class": "known" }`. Rankings are strict and contain all three candidates. The deterministic pair split is written to versioned input before raters label it.

### 4.2 Pairwise examples

For each ranking `[a, b, c]`, training emits only `(a, b)`, `(a, c)`, and `(b, c)` with label `left_preferred`. Feature values are normalized from training pairs only. The pair vector is the preferred candidate's ranker vector minus the other candidate's vector; preference-specific interactions multiply every base feature by the one-hot stated preference (`default`, `fastest`, `cheapest`, or `fewest_transfers`).

## 5. Training Plan

| Item | Plan |
|---|---|
| Model ([D18](state.md#5-decisions)) | Pairwise logistic regression on the [feature vector](sdd-commutenity.md#3-routing-contract), including preference interactions. Try a small GBDT only if logistic regression plateaus and there is time. |
| Baseline | The deterministic lexicographic order from SDD §3 |
| Training | A Python script in `ml/` consumes scenarios, known rankings, and seeded votes. It emits the feature spec plus the JSON model. Mock scenarios or labels may augment training only. |
| On-device export | JSON `{ feature_names, normalization, intercept, weights }`, evaluated in Kotlin. A GBDT fallback would export to ONNX only if pursued. |
| Parity | The same fixture produces identical normalized vectors and scores in Python and Kotlin (QA-11) |
| Ship rule | Higher top-1 agreement than the baseline on held-out `known` scenarios with a strict majority human top choice. Report excluded split scenarios and pairwise accuracy; otherwise the baseline ships. |

## 6. Evaluation Sets

| Eval | Metric | Baseline | Ship rule |
|---|---|---|---|
| Parser | Exact match on origin and destination over held-out questions | Pretrained LLM plus prompt | — |
| End-to-end | % of answers whose picked route is valid and whose facts exactly match the pack | T0 build | [QAD §6](qad-commutenity.md#6-release-criteria) threshold |
| Ranker | Primary: top-1 agreement with the strict-majority human pick on held-out `known` scenarios. Diagnostic: pairwise accuracy across all held-out labels. | Deterministic lexicographic order | Ranker must beat the baseline on the primary metric; report the count excluded for no strict majority. |
| Signboard | Correct verdict % on held-out photos; unreadable rate; **0 false "ride"** | Pretrained OCR plus matcher | — |

Report the real numbers, sample sizes, and limits (for example, "4 raters, N held-out pairs, team-generated"). **Split every metric by data class.** Results on `mock` data show that the pipeline works. They are not evidence of real-world accuracy.

## 7. Provenance and Disclosure

The README lists:
- every data source, with OpenStreetMap attribution and the fare matrix source and date
- every model, with its base checkpoint and license
- the training script
- the fact that the preference and contribution data was generated by the team during the event
- which records and labels are `mock`, with counts per class and the generator script
