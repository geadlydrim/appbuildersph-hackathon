# Scrutiny Gate: CommuteNity

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.2
**Owner:** Project owner
**Status:** Draft
**Last reconciled:** 2026-10-09
**IDEA:** [Idea brief](idea-commutenity.md)

## 1. Verdict

**Decision: PROCEED WITH FIXES.**

The theme fit is strong. Offline use is the core value, and the app trains and runs its own ranker on the device. The main danger is scope. A team of 4 has to deliver a native Android app, an on-phone LLM, a routing engine, a sync backend, a trained ranker, signboard reading, and voice, all in under 20 hours. The fixes below keep a working demo available at every point in that schedule.

| Fix | Downstream home |
|---|---|
| Facts come from the pack via deterministic code; models parse, rank, and phrase | [SDD §1](sdd-commutenity.md#1-architecture), [§3](sdd-commutenity.md#3-routing-contract) |
| Strict tiers, with T0+T1 as the MVP; hard cut rules if checkpoints slip | [MVP scope](mvp-scope.md#tiers), [BUILD §1](build-commutenity.md#1-build-sequence) |
| The ranker ships only if it beats the deterministic baseline on held-out pairs | [Data plan §5](data-commutenity.md#5-training-plan) |
| Contribution and preference data is team-generated, and disclosed as such | [Data plan §4](data-commutenity.md#4-training-data-collection), [CLR §5](clr-commutenity.md#5-ip-provenance-and-disclosure) |
| Prove the on-phone LLM runtime first (CP1); have a fallback ready | [BUILD §1](build-commutenity.md#1-build-sequence) |
| Fresh repo; disclose what we reused from the original CommuteNity concept | [D3](state.md#5-decisions) |

## 2. Claim & Reference Audit

**Coverage:** 11 load-bearing claims. 3 verified, 8 unverified, 0 contradicted.

| ID | Category | Claim | Finding | Source | Severity if wrong |
|---|---|---|---|---|---|
| FC-1 | Rules | Meaningful inference must run locally; the cloud can only be secondary | Verified | [JUDGING](JUDGING.md#rules) | Fatal |
| FC-2 | Rules | A pre-existing project can be disputed; existing code and assets must be disclosed | Verified | [JUDGING](JUDGING.md#rules) | Fatal |
| FC-3 | Rules | No live deployment is required; the repo must let judges recreate the project | Verified | [JUDGING](JUDGING.md#submission) | Significant |
| FC-4 | Feasibility | A 0.5–2B quantized LLM runs on the team's demo phone within the latency target | Unverified; CP1 spike | — | Significant |
| FC-5 | Feasibility | Kotlin bindings for the chosen runtime integrate in under about 2 hours | Unverified | — | Significant |
| FC-6 | Feasibility | Team-generated preferences (4 raters) are enough for a ranker to beat hand-set weights on held-out pairs | Unverified; plausibly *not*, given the small data | — | Minor (the baseline ships) |
| FC-7 | Data | Per-segment minutes can be estimated credibly by the team | Unverified | — | Significant ("efficient" degrades to "cheapest / fewest transfers") |
| FC-8 | Data | Current official fares are findable and citable | Unverified | — | Significant ("fare unknown" otherwise) |
| FC-9 | Feasibility | Pretrained on-device OCR reads painted jeepney signboards well enough | Unverified | — | Minor (T3 only) |
| FC-10 | Feasibility | Whisper-class models on the phone transcribe Taglish acceptably | Unverified | — | Minor (T4 only) |
| FC-11 | Problem | Riders often lack signal or data at the moment they need to decide | Unverified; plausible from team experience | — | Significant for the pitch |
| FC-12 | Rules | Mock data is allowed | Owner-reported organizer allowance. It is not in the written briefing. | Owner, 2026-10-09 | Significant. Mitigation: label everything and disclose it; confirm in Telegram if unsure. |

### 2.1 Reference Integrity

The only external authority cited so far is the official briefing ([JUDGING](JUDGING.md)). Models, runtimes, fare sources, and the backend get cited with dates when they're chosen.

## 3. Gap Analysis

| Gap | Needed by | Treatment |
|---|---|---|
| Runtime and model not proven on our phone | SDD, BUILD | A4 and A12; CP1 |
| No corridor data or minutes | Pack, ranker, demo | D15, D16; data plan §3 |
| No sync backend chosen | T1 | A9 |
| No problem evidence | Pitch | [VALIDATION](val-commutenity.md) |
| Role owners unassigned | BUILD | A8 |

## 4. Assumption Stress-Test

**Load-bearing assumption:** a small on-phone model plus the pack can resolve Taglish questions, and the scorer picks a route that riders would agree is best.

**Strongest argument against it:**
- Taglish place names are messy, and a 1B model on a phone is slow.
- With 1–3 corridors, there may be few real alternatives, so "most efficient" and "alternatives" look trivial.
- Four teammates' preferences are a thin basis for claiming a "learned" ranker.

**Finding:** it holds with caveats.
- The resolver (aliases plus embeddings) absorbs the messiness.
- At least one corridor must be chosen *because* it has real alternatives.
- The ranker claim stays modest: "learned from our labelled preferences; beats or doesn't beat the baseline by X on N held-out pairs."

**Cheapest tests:**
- **Parser:** 30 Taglish questions through the parser and resolver on the phone, with no UI. Aim for ≥ 80% exact origin and destination.
- **Ranker data:** the four teammates rank 10 scenarios independently. If they disagree heavily, a ranker can't learn a consistent preference, so add the user's stated preference as a feature.

### 4.1 Audience Stress

| Check | Finding |
|---|---|
| User and pain clear in ten seconds | Pass: "Which jeep? No signal." |
| Specific user and moment | Pass: a New Arrival at a terminal |
| Repeatable one-liner | Pass: "Ask in Taglish, offline, it picks the route locals take" |
| Why local? | Pass: no signal, no load, seconds to decide, private trips |
| Why change current behavior (asking strangers)? | At risk; needs validation |

## 5. Feasibility & Scope

- **T0:** feasible if CP1 passes early. The Android scaffold, pack loader, and graph search are conventional.
- **T1:** adds a backend and sync, which is where hackathons lose hours. Keep it to two tables and a push/pull.
- **T2:** cheap to train, expensive to *prove*. The data and the parity test both matter.
- **T3–T4:** stretch goals only.

## 6. Risk Pre-flight

- AI risks are covered in [AIA](aia-commutenity.md): invented facts, a poisoned contribution, ranker overfit, and wrong verdicts.
- Data leaving the device (contributions) and photos of public spaces are covered in [CLR](clr-commutenity.md).
- There are no payments and no deployment.

## 7. Blocking Questions

None block the docs. Building is blocked on A4, A6, A8, and A12 ([state](state.md#4-open-assumptions)).
