# Validation Brief: CommuteNity

**Project:** CommuteNity
**Date:** 2026-10-09
**Version:** 0.2
**Owner:** Project owner
**Status:** Draft
**Last reconciled:** 2026-10-09
**IDEA:** [Idea brief](idea-commutenity.md)

## 1. Problem Evidence

**None collected yet.** The problem statement in [IDEA §1](idea-commutenity.md#1-the-spark) rests on two things, and neither is cited evidence:
- the team's own commuting experience
- the original CommuteNity thesis that commute knowledge lives in riders' heads

**Accepted gaps:**
- no interviews
- no usage data
- no measured figures on signal or data costs
- no real community contributions; all are team-generated during the event
- part of the pack is `mock` (synthetic). It is labelled and excluded from any real-world claim.

**What we can honestly show at Demo Day:**
- an offline demo on the phone
- real eval numbers from [data plan §6](data-commutenity.md#6-evaluation-sets), with sample sizes
- team-sourced examples of confusing corridors, labelled as anecdote

### Value measurement

| Level | Metric |
|---|---|
| Demo | Correct picks and facts on held-out questions; zero invented facts; offline proof; latency; ranker vs. baseline on held-out pairs |
| After the event | Time to a correct plan vs. asking around; rate of taking the wrong jeep; rate of real contributions. These need real users and are not claimed. |

## 2. Competitors and Substitutes

These are working hypotheses. A research ticket must verify them before the pitch.

| Substitute | Hypothesis about it | Our wedge (to test) |
|---|---|---|
| Asking barkers, drivers, strangers | Free and local, but you have to find someone, and it's awkward for newcomers | Instant, private, offline |
| Google Maps transit | Strong on rail and bus; informal modes uneven; routing needs a connection | Offline, jeepney-aware, Taglish |
| Local transit planners (e.g., Sakay.ph), Moovit | Cover jeepneys to some degree; offline support unknown | On-device AI; community alternatives; signboard check |
| Facebook commuter groups | Rich community knowledge, but online, slow, unstructured | Same community knowledge, structured, usable offline |

Don't claim a competitor limitation in the pitch until it has been checked.

## 3. Feasibility in the Timebox

The build window runs to the 8:00 AM Oct 10 feature freeze ([BUILD §1](build-commutenity.md#1-build-sequence)). Main uncertainties, in order:
1. The on-phone LLM runtime (FC-4, FC-5).
2. Sync backend setup.
3. Pack data entry with minutes and fares.

## 4. Tests and Kill Criteria

| Assumption | Cheapest test | Success threshold | Failure → decision | Owner |
|---|---|---|---|---|
| The LLM runs on the demo phone | CP1 spike: parse 5 questions offline in a bare Android app | Valid JSON; ≤ 5 s warm | Try a smaller model, then the other runtime; as a last resort, rule-based parser plus embedding resolver | AI owner |
| The parser and resolver handle Taglish | 30-question set on the phone | ≥ 80% exact origin/destination | Add aliases or change the model before UI work | AI owner |
| There are alternatives worth ranking | Generate candidates for every covered pair | ≥ 2 distinct candidates on the demo pair | Choose a different demo corridor | Data owner |
| Teammates agree on preferences | 10 scenarios ranked independently | Majority pick is clear in most scenarios | Add stated preference as a feature, or ship the baseline | Ranker owner |
| The ranker beats the baseline | Held-out pairs | Strictly better, honestly reported | Ship the baseline and say so | Ranker owner |
| Sync works between phones | Phone A suggests, phone B pulls | It appears as a Community alternative | Demo with local contributions only | Android owner |
| The problem resonates | Ask other participants about their worst commute moment *(confirm with organizers this isn't "external help")* | A recognizable story within 10 seconds | Reframe the pitch | Presenter |

**Decision:** go ahead with the demo. Make no commercial or impact claims.

## 5. Concept Visual Reactions

None collected. UI quality is checked by the [DSD quality gate](dsd-commutenity.md#8-quality-gate).
