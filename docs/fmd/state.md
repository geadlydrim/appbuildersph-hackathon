# Project State: CommuteNity

**Project slug:** `commutenity`
**Owner:** Project owner
**Last updated:** 2026-10-09

Read this first. It says where the project stands, what's decided, and what's still open. Every other document hangs off it ([index](index.md)).

## 1. Position

| Field | Value |
|---|---|
| Current milestone | Specify / Shape. Docs only; no product code exists. |
| Entered on | 2026-10-09 |
| Exit condition | The wayfinder frontier is resolved enough that the [build guide](build-commutenity.md) checkpoints start without reopening scope |
| Next milestone | Implement the walking skeleton ([BUILD §1](build-commutenity.md#1-build-sequence)) |
| Hard deadline | **10:00 AM, Oct 10, 2026.** Submission and code freeze ([JUDGING](JUDGING.md#submission)). |
| Current blocker | Open assumptions in §4. Resolve them through `/wayfinder` on the GitHub tracker. |
| Next-step owner | Project owner, with the team of 4 |

CommuteNity is a **native Android app** that works as an on-device commute assistant for Metro Manila. A rider asks in Taglish how to get somewhere. The app automatically picks the most efficient route from a commute pack stored on the phone and shows the legs, fares, and where to say "para". It works with no signal. On request it shows alternatives, which come from the routing algorithm or from routes other riders submitted. Riders can suggest routes and vote "this worked"; those contributions sync when the phone is online and train an on-device route ranker.

## 2. Document Set

| Need | Document |
|---|---|
| Rules, rubric, deadline | [JUDGING](JUDGING.md) |
| What and why | [IDEA](idea-commutenity.md), [MVP scope](mvp-scope.md) |
| Is it sound? | [SCRUTINY](scrutiny-commutenity.md), [VALIDATION](val-commutenity.md) |
| Behavior | [PRD](prd-commutenity.md), [user stories](user-stories.md) |
| How it's built | [SDD](sdd-commutenity.md), [data and training plan](data-commutenity.md), [DSD](dsd-commutenity.md), [BUILD](build-commutenity.md) |
| Correct and safe? | [QAD](qad-commutenity.md), [AIA](aia-commutenity.md), [CLR](clr-commutenity.md) |
| Demo Day | [PITCH](pitch-commutenity.md) |
| History | [LOG](log-commutenity.md) |

Deliberately absent: ops runbook, go-to-market, pricing, store release. Nothing is deployed, and the [rules](JUDGING.md#submission) don't require it.

## 3. Working Framework

- **Working product before ambition.** Build tiers T0→T4 in order ([MVP scope](mvp-scope.md#tiers)). The T0 walking skeleton (pretrained models, the curated pack, and a deterministic lexicographic trip order, all offline on the phone) must be demoable before anything else lands. Trained models are upgrades that replace a working baseline. Nothing depends on them.
- **Decisions live in the tracker.** Open questions are `wayfinder` tickets on `geadlydrim/appbuildersph-hackathon`. When a ticket closes, update the affected doc and move its row from §4 to §5.
- **Stable IDs:** features `PRD-F1..F10`, stories `US-01..`, QA cases `QA-*` and `AI-*`, assumptions `A*`, decisions `D*`.
- **Draft** means complete for everything decided so far, with open items marked. It doesn't mean an unfilled template.

## 4. Open Assumptions

| ID | Assumption / unresolved input | Risk if wrong | Reversible? | Resolve by |
|---|---|---|---|---|
| A3 | **Route facts:** deterministic candidate generation over the pack. The LLM only parses the question and phrases the grounded answer. | Answers feel rigid, or Taglish parsing fails | Yes | Wayfinder prototype |
| A4 | **Android models and runtimes** (narrowed by [runtime research](https://github.com/geadlydrim/appbuildersph-hackathon/issues/4)): LLM through LiteRT-LM (Gemma3-1B int4 first, then ungated Qwen2.5-0.5B or Qwen3-0.6B, with JSON-schema output), with llama.cpp via JNI as the fallback. Embeddings: EmbeddingGemma via LiteRT-LM. OCR: bundled ML Kit Text Recognition v2. Ranker: plain Kotlin weights. Voice: whisper.cpp. Speed on mid-range phones is unpublished; the spike decides. | Too slow on the demo phone | Yes | LLM spike ticket on the map |
| A8 | **Role owners:** who of the 4 owns each workstream ([BUILD §2](build-commutenity.md#2-team-workstreams)). Ticket: [Who owns each workstream?](https://github.com/geadlydrim/appbuildersph-hackathon/issues/11) | Unowned workstreams | Yes | Before CP1 |
| A11 | **Ranker model:** pairwise logistic regression or a small GBDT exported for on-device inference. It is trained on team preference labels, contribution votes, and optional `mock` preferences. Evaluation uses held-out **human** labels only. | Too little data to beat the deterministic baseline | Yes; the baseline stays | Wayfinder plus the ranker owner |

## 5. Decisions

| ID | Decision | Evidence class | Revisit if |
|---|---|---|---|
| D1 | Product: an on-device AI commute assistant for Metro Manila riders, fitting the Local AI theme | Human decision, 2026-10-09 | Rubric fit fails scrutiny |
| D2 | Data: a curated commute pack is the baseline source of facts. A **thin community contribution layer** (suggest a route; vote "this worked"; local-first, synced when online) feeds alternatives and ranker training. Full social (feed, posts, comments, profiles, accounts) stays parked. | Human decision, 2026-10-09; supersedes "community parked" | MVP done with time left |
| D3 | Fresh repo. No CommuteNity code is reused (including the `../CommuteNity/` Compose scaffold). The idea, personas, route/stop data shape, route-picker concept, and visual direction carry over and are disclosed ([CLR §5](clr-commutenity.md#5-ip-provenance-and-disclosure)). | Human decision; [JUDGING](JUDGING.md#rules) | Never during this event |
| D4 | Docs first. No code until the decisions the walking skeleton needs are made. | Human decision, 2026-10-09 | Deadline forces a cut |
| D5 | Working product first. Training and contribution sync never block the T0 baseline demo. | Human decision, 2026-10-09 | — |
| D6 | Tracker: GitHub Issues on public `geadlydrim/appbuildersph-hackathon`, with default triage and `wayfinder:*` labels | Human decision, 2026-10-09 | — |
| D7 | Cloud is secondary only. One-way model and pack downloads, plus the contribution sync backend. Every core AI path (parse, pick, phrase, rank, read sign, transcribe) runs on the phone and works offline. | Rules ([JUDGING](JUDGING.md#rules)) and D2 | — |
| D8 | **Platform:** mobile first, as a native Android app (Kotlin and Jetpack Compose). A browser app comes second, after the event, as a separate effort. | Human decision, 2026-10-09: "a travel app is used on the phone" | Android on-device runtime fails CP1 |
| D9 | **Name:** CommuteNity | Human decision, 2026-10-09 | — |
| D10 | **Build order:** T0 offline question → auto-picked best route; T1 alternatives plus community suggest and vote, with sync; T2 trained ranker; T3 signboard check; T4 voice | Human decision, 2026-10-09 | CP3 slips (see BUILD cut rules) |
| D11 | **Training target:** a learned on-device route ranker. It replaces the deterministic T0 baseline only if it beats it on held-out preferences. Signboard and voice use pretrained models. | Human decision, 2026-10-09 | Ranker data too thin by CP5 |
| D12 | **Team:** 4 people: geadlydrim (owner), pablo-pica, storms23, Jrabara101 | Human decision, 2026-10-09; [ticket](https://github.com/geadlydrim/appbuildersph-hackathon/issues/2) | — |
| D13 | **Mixed data is allowed.** Data is a mix of `collected` (verified during the event), `known` (team knowledge), and `mock` (synthetic). Every record carries its class. Precedence for the same fact: collected > known > mock. Mock is labelled in the app, the evals, and the README, and is never presented as real ([data plan §3.1](data-commutenity.md#31-mock-data-rules)). | Human decision, 2026-10-09; owner reports that the organizers allow mock data (not stated in the briefing) | Organizers say otherwise |
| D14 | **Demo phone:** the owner's POCO X6 Pro (MediaTek Dimensity 8300-Ultra, HyperOS), mirrored with scrcpy over USB to the laptop, then HDMI. The spare is any teammate's Android phone with the APK and models pre-installed. RAM variant and the mirroring test are still to confirm. | Human decision, 2026-10-09; [ticket](https://github.com/geadlydrim/appbuildersph-hackathon/issues/3) | Model doesn't fit in RAM |
| D15 | **Coverage:** the Valenzuela–Recto corridor, with the hero trip from Malanday to the Recto area. Its candidates are a direct Malanday–Recto e-jeep and Malanday → LRT-1 Monumento → LRT-1 Doroteo Jose → walk to Recto. Keanu (geadlydrim) and Jeff (storms23) verify it. Mock data extends only to adjacent stops and never supplies hero-trip facts. | Human decision, 2026-10-09; [Which corridors does the pack cover?](https://github.com/geadlydrim/appbuildersph-hackathon/issues/6) | A verifier cannot substantiate either hero candidate |
| D16 | **Travel time and T0 pick:** record typical, non-peak team estimates as `known` per segment and transfer, then replace them only with event-verified `collected` values. Mock minutes never drive the hero trip. Rank candidates lexicographically: default/fewest-transfers = transfers → total minutes → fare → walk minutes; `fastest` or `cheapest` promotes that criterion, retaining the rest. Net votes (clamped to −3…+3) break only an otherwise equal result; stable candidate key is last. | Human decision, 2026-10-09; [Where do segment minutes come from, and how is "most efficient" weighted?](https://github.com/geadlydrim/appbuildersph-hackathon/issues/7) | Team estimates contradict collected timing or the hero candidates cannot be ranked plausibly |
| D17 | **Contribution sync:** Supabase with anonymous Auth and one authenticated Edge Function for batched push/pull. It validates every pack reference and note, enforces a server-side per-identity mutation limit, and is the only writer using the service role. The two contribution tables are `route_suggestions` and `route_votes`; raw vote rows and identity values never leave the function, while readers receive suggestions and vote aggregates only. | Human decision, 2026-10-09; [Which sync backend stores contributions?](https://github.com/geadlydrim/appbuildersph-hackathon/issues/9) | Supabase setup or Edge Function deployment cannot complete before CP4 |

## 6. Context Exclusions

The original CommuteNity planned a Next.js web app with a Supabase backend, social feed, auth, comments, profiles, and catalog moderation. What survives:
- the personas
- the route → segment → stop data shape
- the auto route picker with alternatives
- the idea of community-submitted routes and votes, now as the thin layer in D2
- the "Jeepney Gold" visual direction

Do **not** bring back its feed, comments, profiles, auth flows, Next.js app, RLS policies, or moderation thresholds.

## 7. Next Actions

1. Run `/wayfinder` to chart the map. Destination: the decisions that unblock the walking skeleton. Seed it with A3, A4, A6, A8–A12.
2. Resolve frontier tickets. Fold each answer into the docs and move the row from §4 to §5.
3. Start the [build sequence](build-commutenity.md#1-build-sequence) once A4, A6, A8, and A12 are decided.

## 8. Delivery Checks

Docs only. No tests, code, or models have been run. Cross-links are checked at each revision ([LOG](log-commutenity.md)).
