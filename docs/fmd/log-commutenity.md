# Build Session Log (LOG)

**Project:** CommuteNity
**Project slug:** `commutenity`
**Session started:** 2026-10-09

Append-only. One row per meaningful action or decision. Never edit past rows.

---

## 1. Action log

| # | Date | Trigger / action | Docs written / updated | Verdict |
|---|---|---|---|---|
| 1 | 2026-10-09 | Drafted the Local AI proposal from the original CommuteNity docs and the hackathon briefing | proposal (since merged into IDEA) | — |
| 2 | 2026-10-09 | Owner decision: park the community layer; use an app-provided commute pack | proposal → IDEA, MVP scope | D2 |
| 3 | 2026-10-09 | Transcribed the official briefing PDF; removed the screenshots and PDF | JUDGING | — |
| 4 | 2026-10-09 | Set up agent skills: GitHub tracker on the public repo `geadlydrim/appbuildersph-hackathon`, default triage labels, `wayfinder:*` labels, single-context domain docs | AGENTS.md, docs/agents/* | D6 |
| 5 | 2026-10-09 | Owner decisions: fresh start, docs first, working product before ambition, data collection and training planned. Restructured docs into the full suite modelled on the reference format. Removed the reference folder and the superseded CommuteNity docs (product-vision, plan, backend-decision, data-model, design-baseline-prompt, proposal); their surviving content was merged into IDEA, the data plan, and DSD. | All docs | D3, D4, D5; A1–A8 opened |
| 6 | 2026-10-09 | Owner decisions: mobile first as native Android (Kotlin/Compose), browser second after the event; name CommuteNity; team of 4; auto route picker with alternatives from the algorithm and community; thin community layer (suggest and vote, local-first sync) back in scope; learned on-device route ranker as the training target; build order T0 pick → T1 alternatives + community → T2 ranker → T3 signboard → T4 voice. Docs revised to v0.2. | All docs, AGENTS.md | D2 amended; D8–D12; A1, A2, A5, A7 closed; A9–A12 opened |
| 7 | 2026-10-09 | Owner decision: mixed data is allowed (owner-reported organizer allowance). Every record is classed `collected`, `known`, or `mock`. Mock fills coverage and training gaps, is marked in the app and README, never drives the demo hero question or the ranker ship-rule eval, and metrics are reported per class. | state, data plan, mvp-scope, stories, PRD, SDD, DSD, QAD, AIA, CLR, VALIDATION, SCRUTINY, PITCH, BUILD, JUDGING, AGENTS.md | D13; A6, A10, A11 amended |
| 8 | 2026-10-09 | Moved the project doc suite into `docs/fmd/` for a cleaner layout. `docs/agents/` stays where it is because the skills expect that path. Paths updated in AGENTS.md, the index, and BUILD. | AGENTS.md, index, BUILD | — |
| 9 | 2026-10-09 | Charted the wayfinder map: issue #1 with 9 tickets, cutoff 7:00 PM. Added GLOSSARY.md (route = one line, trip = door-to-door). Research resolved the runtime question (LiteRT-LM replaces MediaPipe as the primary LLM runtime) and the fare-source question (rail fares `collected`, jeepney and bus `known` until the LTFRB guides are saved). Results are on the `research/*` branches. | state A4, SDD §8, BUILD §3, data plan §3 | A4 narrowed |
| 10 | 2026-10-09 | Owner decision: planning cutoff moved from 7:00 PM to 9:00 PM. Build checkpoints shifted about 1 h later (T0 ~1 AM, T1 ~4 AM, T2 ~6 AM, T3/T4 6–8 AM) and cut rules tightened to match. Feature freeze (8:00 AM) and code freeze (10:00 AM) unchanged. | team guide, BUILD §1, map #1, ticket defaults | — |
| 11 | 2026-10-09 | Closed the fourth-teammate ticket: the team is geadlydrim, pablo-pica, storms23, and Jrabara101 (invites pending for the last two). Role assignment split into a new ticket at the top of the map. | state D12, A8; team guide | D12 amended |
| 12 | 2026-10-09 | Closed the demo-phone ticket: the owner's POCO X6 Pro (Dimensity 8300-Ultra). The LLM speed-test ticket is now unblocked. | state D14 (A12 closed), team guide | D14 |
| 13 | 2026-10-09 | Owner decision: cover the Valenzuela–Recto corridor. The hero trip is Malanday to the Recto area, comparing a direct Malanday–Recto e-jeep with Malanday → LRT-1 Monumento → LRT-1 Doroteo Jose → walk. Keanu (geadlydrim) and Jeff (storms23) verify it; mock data may cover adjacent stops only. | state D15 (A6 closed), MVP scope, data plan | D15 |
| 14 | 2026-10-09 | Owner decision: hero-trip minutes are typical, non-peak team estimates (`known`) until event verification makes them `collected`; mock minutes never drive the hero trip. T0 uses an explainable lexicographic candidate order, with a requested preference promoted and bounded votes only as a final tie-break. | state D16 (A10 closed), SDD §3, data plan §3 | D16 |
| 15 | 2026-10-09 | Owner decision: use Supabase anonymous Auth and one authenticated Edge Function for local-first contribution sync. It validates and rate-limits mutations server-side; only suggestions and vote aggregates are readable. | state D17 (A9 closed), SDD §4–§5, data plan §2.1, CLR, BUILD | D17 |
| 16 | 2026-10-09 | Owner decision: ranker labelling uses three-candidate JSON scenarios and strict top-3 rankings from four teammates. A deterministic pair split protects held-out `known` evaluation; pairwise logistic regression with preference interactions exports JSON for Kotlin. | state D18 (A11 closed), data plan §§4–6, SDD §3, QAD AI-06, GLOSSARY | D18 |
| 17 | 2026-10-09 | P3 review of the ranker training plan: the pairwise examples had only one label class, so logistic regression could not be fitted. Each pair is now also emitted mirrored and trained without an intercept (exported `intercept` is `0.0`); D18 is otherwise unchanged. | data plan §4.2 | — |
| 18 | 2026-10-09 | Owner decision: P1 geadlydrim, P2 pablo-pica, P3 storms23, P4 Jrabara101. The LLM speed test is assigned to P2 as the first build task. Repaired special characters corrupted in the issue bodies by non-UTF-8 edits. | state D19 (A8 closed), BUILD §2 | D19 |
| 19 | 2026-10-09 | Owner re-scope (~22:40 +08): Makati City only (the Valenzuela–Recto corridor is superseded); map-first trip builder; road shapes precomputed at data-build time; offline PMTiles map; online refresh with offline compute; in-trip tracking from offline GPS and map-matching with a para alert; voice is STT only, OCR signboard scan dropped, TTS not planned; correct-vehicle check is a text match; ranker kept as T4; CommuteNity-Web ideas only, no code; Figma follows docs via a new brief. New tier order T0 walking skeleton → T1 online refresh + community → T2 in-trip tracking → T3 ask in words + voice → T4 ranker; MVP = T0+T1+T2. Docs revised to v0.3; planning is over and building starts. | state, IDEA, MVP scope, stories, PRD, SDD, data plan, DSD, Figma brief, QAD, AIA, CLR, SCRUTINY, VALIDATION, BUILD, PITCH, GLOSSARY, AGENTS.md, team guide, index | D20–D29 (D10 superseded by D28, D15 by D20); A13–A16 opened; A3, A4 reworded |

---

## 2. Friction

| # | Area | What happened | Fix |
|---|---|---|---|
| 1 | Rules | The briefing PDF lists the reveal at both 1:00 and 2:00 PM, and the Tutorials Dojo award layout is ambiguous | Noted in JUDGING; confirm in Telegram if it matters |
