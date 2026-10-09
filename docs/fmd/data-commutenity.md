# Data Collection and Training Plan: CommuteNity

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.4
**Owner:** Whoever claims the data issues ([D35](state.md#5-decisions)); no fixed role owners
**Status:** Draft. Makati City only ([D20](state.md#5-decisions)), the hero trip pair ([D30](state.md#5-decisions), amended by [D37](state.md#5-decisions): V.A. Rufino St to Dela Rosa Street, Pio del Pilar), precomputed road shapes ([D22](state.md#5-decisions)), and the offline map pack ([D23](state.md#5-decisions)) are decided. The candidate trips for the hero pair ([A13](state.md#4-open-assumptions)), the shape engine ([A15](state.md#4-open-assumptions)), and the map pack size ([A14](state.md#4-open-assumptions)) are open. The travel-time baseline and the ranker data/model plan are decided ([D16](state.md#5-decisions), [D18](state.md#5-decisions)). The Valenzuela–Recto corridor ([D15](state.md#5-decisions)) is superseded by D20.
**Last reconciled:** 2026-10-10
**SDD:** [System design](sdd-commutenity.md)

This document owns five things: the pack and contribution schemas, the road-shape pipeline and offline map pack, how we collect and verify data, what we train (the route ranker, [D11](state.md#5-decisions)), and how we prove it helped.

**Rule:** the T0 baseline never waits on anything in §4–§5 ([D5](state.md#5-decisions)).

## 1. Data Inventory

| Dataset | Purpose | Feeds | Needed by |
|---|---|---|---|
| Commute pack (Makati, with road shapes) | The only source of facts in answers, and of the lines drawn on the map | PRD-F2, F3, F5, F7, F8 | T0 |
| Map pack (Makati PMTiles file) | Offline base map | PRD-F1, F3 | T0 |
| Refresh manifest | Versions and checksums of the pack and map pack | PRD-F4 | T1 |
| Community contributions | Alternatives and vote signals | PRD-F5, F6; ranker features | T1 |
| Rider Q&A mock file (`data/mock/rider-qa.json`) | Hand-written sample threads and answers; tied answers give bounded tie-break evidence ([D34](state.md#5-decisions)) | PRD-F12 | T1 (first thing cut) |
| GPS test tracks (mock-location) | Replaying on-route, deviation, return, and GPS loss for tracking tests | PRD-F7; QA-10, QA-11 | T2 |
| Trip question set | Place search and LLM-extraction eval, and end-to-end eval | PRD-F8; AI-03 | T3 |
| Route preference set | Ranker training and eval | PRD-F10 | T4 (collection starts at CP2) |
| Signboard text eval set | Correct-vehicle check eval on texts (no photos, no OCR) | PRD-F8; AI-05 | T3 |

## 2. Commute Pack Schema

The pack keeps the route → segment → stop shape from the original CommuteNity ([D3](state.md#5-decisions)). It is versioned JSON built into the app assets and refreshable ([§2.3](#23-refresh-manifest-and-versioning)). Proposed source location: `data/pack/`.

| Entity | Fields | Notes |
|---|---|---|
| `meta` | `version`, `built_at`, `coverage` (Makati City boundary polygon or bounding box, with name), `sources[]`, `shape_precision` | Shown as "data as of …". `coverage` replaces the old `corridors[]` ([D20](state.md#5-decisions)); a pin outside it is "not in my data". |
| `places` | `id`, `name`, `aliases[]`, `lat`, `lng`, `kind` (landmark, area, station, terminal), `stop_ids[]` | Aliases hold Taglish and colloquial names. Searchable offline from the trip builder. |
| `stops` | `id`, `name`, `lat`, `lng`, `modes[]` | Where you board or alight |
| `routes` | `id`, `name`, `mode`, `signboards[]`, `notes` | `mode` is one of: jeepney, bus, mrt, lrt, uv_express, p2p, tricycle, walking. `signboards[]` also feeds the correct-vehicle check ([D27](state.md#5-decisions)). |
| `segments` | `route_id`, `seq`, `from_stop_id`, `to_stop_id`, `fare_php?`, `minutes?`, `distance_m`, `shape` | Ordered legs. Minutes are needed for the T0 baseline ([D16](state.md#5-decisions)). `shape` and `distance_m` are described below. |
| `transfers` | `from_stop_id`, `to_stop_id`, `walk_minutes`, `notes`, `distance_m`, `shape` | Walking links; `shape` uses the foot profile |
| `provenance` | per record: `source_class` (`collected` / `known` / `mock`), `source` (ride-verified, fare matrix, team estimate, signboard text, mock generator), `verified_by`, `verified_at` | Every fact has a class and a source ([§3.1](#31-mock-data-rules)) |

**Road shape** ([D22](state.md#5-decisions)). Each segment and transfer has one `shape`:

| Field | Meaning |
|---|---|
| `polyline` | Encoded polyline of the road-following path from `from_stop_id` to `to_stop_id`. Precision is fixed once in `meta.shape_precision` (proposed: 6 decimal places) so the Kotlin decoder and the pipeline agree. |
| `engine` | Where it came from: `osrm`, `graphhopper`, or `osm_rail` (rail follows the track geometry in OpenStreetMap). Open until [A15](state.md#4-open-assumptions) is decided. |
| `profile` | `driving` for jeepney, bus, and UV Express; `foot` for transfers; `rail` for MRT and LRT |
| `generated_at` | Timestamp of the engine run. The OSM data date is recorded in `meta.sources[]`. |

`distance_m` is the length of `shape`, computed by the pipeline and rounded to metres. A shape is derived geometry, not a verified ride path: it follows roads, and the README says so. It inherits the `source_class` of its segment, so mock segments are drawn but marked ([D13](state.md#5-decisions)). A shape never overrides a fare or minutes value.

**Integrity rules:**
- Segments reference existing stops.
- Every route has at least one segment.
- Every place maps to at least one stop.
- No fare or minutes value without a source.
- Every segment and transfer has a `shape` and a `distance_m`.
- Each shape starts and ends within N m of its two stops (N set at CP2), has more than its two endpoints, and is not a straight line between them (QA-16).
- `distance_m` matches the length of the decoded shape.

A validator runs before every pack build.

### 2.1 Contribution Schema

These records are stored locally in Room and synced through the authenticated Supabase Edge Function ([D17](state.md#5-decisions)). The database has only the two contribution tables below; Room-only `synced` and `sync_cursor` state do not sync. The Room foot-route cache for first/last-mile walks ([SDD §3](sdd-commutenity.md#3-routing-contract)) is local only and never synced.

| Entity | Fields | Notes |
|---|---|---|
| `route_suggestions` | `id` (UUID), `author_id` (server-set anonymous Auth UID), `origin_place_id`, `dest_place_id`, `legs[]` (route_id, board_stop_id, alight_stop_id), `note?` (≤ 280 chars), `created_at` | The function validates legs against the pack. Fares, minutes, distances, and shapes are always taken from the pack. Public pull excludes `author_id`. |
| `route_votes` | `author_id` (server-set anonymous Auth UID), `candidate_key`, `value` (+1/−1), `updated_at` | Unique on (`author_id`, `candidate_key`). The function upserts the current vote; clearing removes it. Public pull exposes aggregate `net_votes` and `vote_count` only. |

Anonymous Auth creates a stable per-install identity without a user-facing account. The Android client never receives the service-role key.

### 2.2 Offline Map Pack

The map pack is the offline Makati base map ([D23](state.md#5-decisions)): one Protomaps PMTiles file rendered by MapLibre Native Android. It is separate from the commute pack.

| Field | Detail |
|---|---|
| File | `makati-<version>.pmtiles`, built under `data/map/` and not committed to git (too large). Distributed through Supabase Storage ([§2.3](#23-refresh-manifest-and-versioning)); bundled in the APK or downloaded once. |
| Source | A Protomaps vector-tile build of OpenStreetMap data, cut to a Makati bounding box (for example with the `pmtiles` command-line tool's extract command). The exact source build and its date are recorded in the pack `meta.sources[]`. Unverified until the CP1 map spike. |
| Area and zoom | Makati City plus a margin, set at CP1; zoom range chosen to keep size down ([A14](state.md#4-open-assumptions)) |
| Size | TBD. Measured at CP1 and recorded here. |
| Style assets | The map style, fonts (glyphs), and sprites must be available offline and are bundled in the app. Unverified until the CP1 map spike. |
| Licence and attribution | OpenStreetMap data (ODbL). Attribution is shown on the map and in the README. The app never bulk-downloads tiles from tile.openstreetmap.org; its usage policy forbids it. |
| Fallback | If MapLibre Native Android cannot render a local PMTiles file ([A14](state.md#4-open-assumptions)), use a MapLibre offline region from a provider whose terms allow offline use. Decided at CP1. |

### 2.3 Refresh Manifest and Versioning

Refresh ([D24](state.md#5-decisions)) fetches and caches new data when online. The phone compares a small manifest with what it has installed.

| Field | Detail |
|---|---|
| `manifest.json` | Stored in Supabase Storage. Fields: `schema_version`, `generated_at`, and one entry each for `pack` and `map`. |
| Entry fields | `version`, `url`, `sha256`, `bytes` |
| Versions | Each file has its own opaque version string that increases with every build (proposed form: `makati-YYYYMMDD-n`). Regenerating shapes or changing any pack record bumps the pack version. |
| Download | Resumable. The client downloads to a temporary file, checks `sha256`, then swaps atomically. An interrupted or corrupt download leaves the previous version in use (QA-04). |
| Fallback | The pack bundled in the APK is never deleted. A refreshed pack lives in app storage and wins only if its version is newer and it validated at download. |
| Active trip | A refresh never swaps the pack or map under an active trip. |
| Community data | Suggestions and vote aggregates come through the [D17](state.md#5-decisions) Edge Function, not the manifest. |
| Foot routes | Fetched on demand through the server for first/last-mile walks and cached locally. They are not part of the manifest. |

## 3. Collection Protocol

1. **Coverage and hero trip ([D20](state.md#5-decisions), [D30](state.md#5-decisions), [A13](state.md#4-open-assumptions)):** build the pack for Makati City. The hero pair is set by D30 and amended by D37: **A = V.A. Rufino St** (Makati, next to Ayala Ave / Legazpi Village; plus code 7Q63H259+6C3; 14.558012, 121.018547) to **B = Dela Rosa Street, Pio del Pilar** (Makati; plus code 7Q63H245+R7; 14.557063, 121.008188), about 1.1 km apart in a straight line (computed), so one of the two real trips may be a walk. What stays open under A13 is the candidate trips: the team (Jrabara101 leading the pack data) lists and verifies at least two genuinely different trips for this pair (modes, boarding points, and para points) before pack v0, from `collected` and `known` data. Those candidates must be built from `collected` and `known` data. Mock records may cover adjacent stops only; they cannot supply hero-trip facts. Do not invent routes, jeepney names, fares, or minutes. (The earlier Valenzuela–Recto corridor, [D15](state.md#5-decisions), is superseded; its rules about collected, known, and mock data still apply.)
2. **Stops and segments:** use team knowledge, with OpenStreetMap for coordinates. Class each one `collected` (verified this event) or `known` (from memory). Road shapes come from the pipeline in [§3.2](#32-road-shape-pipeline-a15).
3. **Fares:** use the current official fare matrix where possible, citing the source and date (`collected`). Otherwise use team knowledge (`known`) or a `mock` value. If none of those exists, the fare is unknown. Per [fare research](https://github.com/geadlydrim/appbuildersph-hackathon/issues/5):
   - LRT-1, LRT-2, and MRT-3 matrices are `collected`, transcribed by hand from official images.
   - The MRT-3 and LRT-2 discount goes in a dated overlay.
   - Jeepney and bus fares become `collected` once a teammate saves the LTFRB fare guides from a browser.
   - UV Express and P2P are `known` or `mock`.
4. **Minutes ([D16](state.md#5-decisions)):** record the team's typical, non-peak ride and transfer estimates per segment as `known`, with estimator and date. Replace a fact only when a teammate verifies it during the event (`collected`). Mock minutes may cover adjacent stops but never the hero trip; the app states that displayed `known` times are typical estimates.
5. **Aliases:** for each stop and place, record the names people actually say.
6. **Signboards:** transcribe the exact painted text into `routes.signboards[]`. Photos may be taken to read them, and stay in `data/raw/` (gitignored); they are not an eval input. The signboard text eval set is in [§6](#6-evaluation-sets).

### 3.1 Mock Data Rules

Mock data fills the gaps that real and known data can't cover overnight ([D13](state.md#5-decisions)).

| Rule | Detail |
|---|---|
| Where mock may be used | Wider coverage around the demo trips (places, stops, routes, segments); missing fares and minutes; extra community suggestions and votes so alternatives have content; the hand-written rider Q&A file ([D34](state.md#5-decisions)); augmenting ranker training; growing the question set |
| Where mock may not be used | The demo hero question. The hero-pair ordering: mock rider Q&A is shown on hero-trip cards but never counts toward it ([D13](state.md#5-decisions), [D34](state.md#5-decisions)). Held-out evaluation labels for the ranker ship rule (AI-06). Any number quoted in the pitch as real-world accuracy. |
| How it's made | A script in `ml/` or `data/` with a fixed seed, so the mock pack is reproducible. Mock values must be plausible for Metro Manila but are never claimed as real. Never hand-type mock data into the same files as `collected` or `known` data without the class tag. Mock segments get road shapes from the same pipeline, so nothing is a straight line. |
| Tagging | Every mock record has `source_class: mock` and `source: mock generator <version>` |
| Precedence | If a collected or known record exists for the same fact, it overrides the mock one |
| Visibility | The app marks mock values with a "sample data" tag ([DSD §2](dsd-commutenity.md#2-theme-and-type)) and only on mock values (QA-17). The README reports counts per class. |
| Mock preferences | If mock preference labels are generated from a utility function, the ranker can only learn that function back. They are used for training augmentation only, never for the ship-rule eval. |
| Rider Q&A file ([D34](state.md#5-decisions)) | `data/mock/rider-qa.json`, bundled in the app and kept separate from the commute pack: a refresh never merges it into the pack. It is **hand-written**, an exception to the fixed-seed script rule above. About 5 origin–destination pairs including the hero pair ([D30](state.md#5-decisions)), with 2–4 answers each, and at least one non-hero pair where tied answers decide an otherwise exactly tied pick, so the demo can show evidence changing a choice. Every thread and answer has `source_class: mock` and `source: hand-written sample`. An answer may carry a `candidate_key`, which must be a pack-valid candidate key for its pair; an answer without one is text only and counts for nothing. Keys are filled in once pack v0 exists. The app marks every item Sample. The rider's own answers are never written to this file. |
| Hero-pair exclusion | Mock rider Q&A shows on hero-trip cards, marked Sample, but is excluded from the hero-pair ordering ([D13](state.md#5-decisions)). It counts on other pairs, inside the D16 vote clamp ([D34](state.md#5-decisions)). |

### 3.2 Road Shape Pipeline (A15)

Runs at data-build time on a laptop or server, never on the phone ([D22](state.md#5-decisions)).

1. **Input:** the pack source: stops and, for each route, the ordered segments.
2. **Per segment:** ask the routing engine for the path from `from_stop_id` to `to_stop_id` with the segment's profile (`driving` for jeepney, bus, and UV Express). Where the shortest road path differs from the route the vehicle really takes, the pack builder adds via-points to the shape source in `data/pack/` (never shipped in the app) until the shape follows the real road.
3. **Transfers:** the `foot` profile.
4. **Rail:** MRT and LRT shapes follow the track geometry from OpenStreetMap (`osm_rail`), cut at the stations.
5. **Encode:** write `shape.polyline`, `engine`, `profile`, `generated_at`; compute `distance_m` from the decoded geometry.
6. **Cache:** store the raw engine responses under `data/pack/` so a rebuild is reproducible and doesn't call the engine again. Don't hammer a public demo server.
7. **Validate:** the pack validator runs the shape rules above (QA-16). A segment without a valid shape fails the build.

**Engine ([A15](state.md#4-open-assumptions)):** OSRM public demo server, GraphHopper, or a self-hosted instance, decided before pack v0 once its usage terms are read. Unverified here. The same decision covers the rail geometry source and the engine behind the server's first/last-mile foot routes.

### 3.3 GPS Test Tracks

Tracking is tested with Android mock-location, not with a ride.

- Build tracks from the hero trip's shapes: `on-route`, `deviate-return` (past the off-route threshold, then rejoin), `blip` (one outlier fix or a deviation shorter than the threshold; must not flag off route), `approach-alight` (approaches the para point), and `gps-loss` (gap, then recovery). QA usage is in [QAD §2](qad-commutenity.md#2-data-and-environment).
- Files live in `data/tracks/`: a timestamped list of `lat, lng, accuracy`. Generated tracks are marked simulated; real Makati tracks recorded during tuning ([A16](state.md#4-open-assumptions)) are marked recorded.
- The same files feed the JVM tests of the map-matcher and the on-phone mock-location runs (QA-10, QA-11).

## 4. Training Data Collection

**There are no real users during the event.** All preference and contribution data is **team-generated** and must be disclosed as such ([JUDGING](JUDGING.md#rules): no fake benchmarks).

| Set | How | Labels | Split |
|---|---|---|---|
| Route scenarios | One JSON scenario per covered origin–destination pair and stated preference, with exactly three pack-valid candidates and their feature vectors | — | Deterministically assign 25% of origin–destination pairs to held-out before labelling |
| Preference labels | Each of the 4 teammates independently submits a strict top-3 order for every scenario | Three pairwise examples per rater: 1st > 2nd, 1st > 3rd, 2nd > 3rd; never infer a preference from an unranked candidate | The same origin–destination pair is never in both train and held-out |
| Seeded contributions | Teammates submit real alternative routes they know and vote on candidates through the app (this also exercises T1) | Votes become `net_votes` and `vote_count` features | Same pair-level split |
| Trip questions | Varied Taglish and English questions per covered pair, including typos, landmarks, out-of-coverage, and off-topic | Gold `{intent, origin, destination}` plus the expected route | Held-out eval only |

**Target volume:** set it at CP2 based on the number of Makati origin–destination pairs. Report the actual numbers and don't inflate them.

All preference labels, contributions, and questions carry `source_class` too. Labels written by teammates are `known`. Generated ones are `mock`.

### 4.1 Scenario and label files

`data/labels/scenarios.jsonl` contains one JSON object per scenario:

```json
{
  "schema_version": 1,
  "scenario_id": "mk-001-fastest",
  "origin_place_id": "<hero_origin_place_id>",
  "destination_place_id": "<hero_destination_place_id>",
  "preference": "fastest",
  "source_class": "known",
  "candidates": [
    { "candidate_key": "…", "source": "algorithm", "features": { "total_minutes": 0, "fare_php": 0, "transfers": 0, "walk_minutes": 0, "modes_count": 0, "net_votes": 0, "vote_count": 0, "is_community": false, "unknown_fare_legs": 0 } },
    { "candidate_key": "…", "source": "algorithm", "features": { "…": "…" } },
    { "candidate_key": "…", "source": "community", "features": { "…": "…" } }
  ]
}
```

The place IDs are the pack's IDs for the D30 pair (amended by D37): origin V.A. Rufino St and destination Dela Rosa Street, Pio del Pilar. They are assigned when pack v0 is built, and the scenarios are filled in once the candidate trips are verified ([A13](state.md#4-open-assumptions)). The feature list is the D18 spec and does not change with the map-first flow.

`data/labels/rankings.jsonl` contains `{ "scenario_id", "rater_id": "R1" | "R2" | "R3" | "R4", "ranking": [candidate_key, candidate_key, candidate_key], "source_class": "known" }`. Rankings are strict and contain all three candidates. The deterministic pair split is written to versioned input before raters label it.

### 4.2 Pairwise examples

For each ranking `[a, b, c]`, training emits only `(a, b)`, `(a, c)`, and `(b, c)` with label `left_preferred`. Feature values are normalized from training pairs only. The pair vector is the preferred candidate's ranker vector minus the other candidate's vector; preference-specific interactions multiply every base feature by the one-hot stated preference (`default`, `fastest`, `cheapest`, or `fewest_transfers`).

Each pair is also emitted mirrored (the negated vector, label `right_preferred`), because logistic regression cannot be fitted on a single class. Train without an intercept so a pair's score flips sign when its order flips; the exported `intercept` is `0.0`.

## 5. Training Plan

| Item | Plan |
|---|---|
| Model ([D18](state.md#5-decisions)) | Pairwise logistic regression on the [feature vector](sdd-commutenity.md#3-routing-contract), including preference interactions. Try a small GBDT only if logistic regression plateaus and there is time. |
| Baseline | The deterministic lexicographic order from SDD §3 |
| Training | A Python script in `ml/` consumes scenarios, known rankings, and seeded votes. It emits the feature spec plus the JSON model. Mock scenarios or labels may augment training only. |
| On-device export | JSON `{ feature_names, normalization, intercept, weights }`, evaluated in Kotlin. A GBDT fallback would export to ONNX only if pursued. |
| Parity | The same fixture produces identical normalized vectors and scores in Python and Kotlin (QA-15) |
| Ship rule | Higher top-1 agreement than the baseline on held-out `known` scenarios with a strict majority human top choice. Report excluded split scenarios and pairwise accuracy; otherwise the baseline ships. |

## 6. Evaluation Sets

| Eval | Metric | Baseline | Ship rule |
|---|---|---|---|
| Place search and LLM place extraction (AI-03) | Exact match on resolved place over held-out place queries and, at T3, over origin and destination extracted from held-out questions | Alias and fuzzy match; pretrained LLM plus prompt | — |
| End-to-end | % of answers whose picked trip is valid, whose facts exactly match the pack, and whose drawn shapes come from the pack | T0 build | [QAD §6](qad-commutenity.md#6-release-criteria) threshold |
| Ranker (AI-06) | Primary: top-1 agreement with the strict-majority human pick on held-out `known` scenarios. Diagnostic: pairwise accuracy across all held-out labels. | Deterministic lexicographic order | Ranker must beat the baseline on the primary metric; report the count excluded for no strict majority. |
| Signboard text eval set → correct-vehicle check (AI-05) | Correct verdict % on held-out signboard and route-name **texts** against a trip's legs: right text, wrong text, ambiguous, and noisy or abbreviated text; **0 false "ride this"** | Deterministic fuzzy matcher ([D27](state.md#5-decisions)) | — |
| Tracking replay | Expected on-route, off-route, and para-alert transitions on the [test tracks](#33-gps-test-tracks); one para alert per ride leg. Tracks are simulated or recorded, and the report says which. | Config thresholds | QA-10, QA-11 |

The signboard text set replaces the old signboard photo set: no photos are used and no OCR is run ([D26](state.md#5-decisions)). It is about 20 texts transcribed in §3 plus hand-made wrong and ambiguous ones.

Report the real numbers, sample sizes, and limits (for example, "4 raters, N held-out pairs, team-generated"). **Split every metric by data class.** Results on `mock` data show that the pipeline works. They are not evidence of real-world accuracy.

## 7. Provenance and Disclosure

The README lists:
- every data source, with OpenStreetMap attribution (map pack and shapes) and the fare matrix source and date
- the map pack source build, and the shape engine, profile, and OSM data date
- that road shapes are generated, not surveyed ride paths
- every model, with its base checkpoint and license
- the training script
- the fact that the preference and contribution data was generated by the team during the event
- which records and labels are `mock`, with counts per class and the generator script
- which GPS tracks are simulated and which are recorded
