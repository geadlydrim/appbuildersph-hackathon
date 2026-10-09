# AI Assurance Dossier: CommuteNity

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.2
**Owner:** Project owner
**Status:** Draft
**Last reconciled:** 2026-10-09
**PRD:** [Requirements](prd-commutenity.md) · **SDD:** [System design](sdd-commutenity.md) · **QAD:** [QA plan](qad-commutenity.md) · **CLR:** [Protection register](clr-commutenity.md)

## 0. Scope

AI ships in these features:

| Feature | AI role |
|---|---|
| PRD-F1 | Parsing the trip question |
| PRD-F2 | Phrasing the answer, and the deterministic baseline scorer |
| PRD-F7 | Learned ranker |
| PRD-F8 | Reading the signboard |
| PRD-F9 | Speech-to-text |

All inference runs on the phone. No model creates facts or makes a verdict on its own. The pack and deterministic code own routes, fares, minutes, and the "ride / wrong" decision. Models only parse, order, and phrase.

This is an assurance plan, not an audit that has been run.

## 1. Model and System Card

| Field | Value |
|---|---|
| Intended use | Help a rider pick and understand a commute on covered Metro Manila corridors, offline |
| Intended user | New Arrival, Occasional Commuter, Daily Rider |
| Models | Open-weight LLM, embedding, ranker, OCR, and speech models. Exact IDs pending [A4](state.md#4-open-assumptions) and [A11](state.md#4-open-assumptions). |
| Data provenance | Curated pack, plus team-generated preferences, contributions, and eval sets ([data plan](data-commutenity.md)) |
| Model-visible data | The user's question or transcript; structured candidates and features; signboard image |
| Human oversight | The rider sees the reason for the pick, alternatives with their sources, and the text read off a signboard. The app says "not in my data" instead of guessing. |
| Prohibited uses | Safety-critical navigation, promises about real-time arrival, acting as an official fare authority (fares are estimates) |
| Performance | Not measured. Targets in [QAD §6](qad-commutenity.md#6-release-criteria). |

**Limitations:**
- Part of the pack and of the training data is `mock` (synthetic). It is marked in the app, and any metric measured on mock data says nothing about real-world accuracy.
- Fares and routes change.
- Travel minutes are typical estimates made by the team.
- Coverage is a few corridors.
- The ranker reflects four raters' preferences.
- Signboard reading fails in poor light.
- A fluent answer is not necessarily a correct one.

## 2. Risk Register

| ID | Risk | Severity | Mitigation | Eval | Status |
|---|---|---|---|---|---|
| AIA-R1 | Invented route, stop, fare, or minutes | High | Facts come from the generator and pack; composer fact-check; template fallback | AI-01, AI-02 | Specified |
| AIA-R2 | A false "ride this" sends the rider the wrong way | High | Deterministic matcher, conservative threshold, shows the text read, "unreadable" state | AI-05 | Specified |
| AIA-R3 | Poisoned or spam contributions push a bad route to the top | Medium | Legs must be valid against the pack; facts always computed from the pack; vote influence bounded; rate limit per device | AI-07 | Specified |
| AIA-R4 | Ranker overfits to the team's preferences, or the claim is inflated | Medium (rules risk) | Pair-level held-out split; ship rule; disclose rater count and data origin | AI-06 | Specified |
| AIA-R5 | Wrong place resolution | Medium | Ambiguity picker; confidence threshold | AI-03 | Specified |
| AIA-R6 | Prompt injection through the question or a contribution note | Low (the model has no tools) | Schema-validated parser output; notes shown as plain text and never sent to the LLM | AI-04, AI-07 | Specified |
| AIA-R7 | Stale fares or minutes shown as current | Medium | "Data as of" date; estimate labels | QA-01 | Specified |

## 3. Audit Trail

| Stage | Evidence | State |
|---|---|---|
| Scoping | IDEA, MVP scope | Documented |
| Mapping | SDD §1, §3, §8 | Documented |
| Artifact collection | Model IDs and licenses; data sources; rater count | Pending |
| Testing | QAD §7 | Not run |

## 4. Safety Bars

| Flag | Present? | Bar | Met? |
|---|---|---|---|
| Consequential decision about a person | No | — | N/A |
| Physical-world guidance (which vehicle to board) | Yes | Facts only from the pack; 0 false "ride" verdicts on eval | Pending |
| Untrusted input reaches a model or the ranker | Yes (questions, contributions) | No tools; validated output; bounded vote influence | Pending |
| Personal data in model context | Only the user's own input, kept on the device | Nothing on the answer path leaves the device | Pending |

**If rehearsal produces an invented fact or a false "ride":** disable that path (template-only answers, baseline scorer, or T3 off) and fix it before re-tagging.

## 5. Regulatory Awareness

No AI regulatory tier or certification is claimed. This is a hackathon demo with no deployment.
